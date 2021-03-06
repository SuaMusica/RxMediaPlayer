package br.com.suamusica.rxmediaplayer.android


import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Looper
import android.util.Log
import br.com.suamusica.rxmediaplayer.domain.CompletedState
import br.com.suamusica.rxmediaplayer.domain.ErrorState
import br.com.suamusica.rxmediaplayer.domain.IdleState
import br.com.suamusica.rxmediaplayer.domain.LoadingState
import br.com.suamusica.rxmediaplayer.domain.MediaBoundState
import br.com.suamusica.rxmediaplayer.domain.MediaItem
import br.com.suamusica.rxmediaplayer.domain.MediaProgress
import br.com.suamusica.rxmediaplayer.domain.MediaServiceState
import br.com.suamusica.rxmediaplayer.domain.Optional
import br.com.suamusica.rxmediaplayer.domain.PausedState
import br.com.suamusica.rxmediaplayer.domain.PlayingState
import br.com.suamusica.rxmediaplayer.domain.RepeatState
import br.com.suamusica.rxmediaplayer.domain.RxMediaPlayer
import br.com.suamusica.rxmediaplayer.domain.StoppedState
import br.com.suamusica.rxmediaplayer.exception.PlayerNotConnectedToInternetException
import br.com.suamusica.rxmediaplayer.parser.CustomHlsPlaylistParser
import br.com.suamusica.rxmediaplayer.parser.SMHlsPlaylistParserFactory
import br.com.suamusica.rxmediaplayer.utils.isConnected
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.hls.playlist.DefaultHlsPlaylistParserFactory
import com.google.android.exoplayer2.source.hls.playlist.HlsPlaylistParserFactory
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.FileDataSourceFactory
import com.google.android.exoplayer2.util.Util
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.io.File
import java.net.HttpCookie
import java.util.HashMap
import java.util.concurrent.TimeUnit

class RxExoPlayer (
    private val context: Context,
    private val resolveDataSourceForMediaItem: (MediaItem) -> String = { it.url },
    signedCookies: List<HttpCookie>? = emptyList()
) : RxMediaPlayer {

  private val TAG = RxExoPlayer::class.java.simpleName

  private lateinit var exoPlayer: SimpleExoPlayer
  private val stateDispatcher = BehaviorSubject.create<MediaServiceState>()
  private var progressDisposable = Disposables.disposed()
  private val compositeDisposable = CompositeDisposable()
  private var currentMediaItem: MediaItem? = null
  private var mediaState: MediaPlayerState = MediaPlayerState.IDLE
  private var cookies: List<HttpCookie> = signedCookies ?: emptyList()
  private var customScheduler = Schedulers.single()

  init {
    initializeExoPlayer(context)
  }

  override fun play(): Completable = currentMediaItem?.let { play(it) } ?: Completable.complete()

  @Synchronized
  override fun play(mediaItem: MediaItem): Completable = Completable.create { completableEmitter ->
    if (isConnectedToInternet(mediaItem)) {
      completableEmitter.onError(PlayerNotConnectedToInternetException())
      completableEmitter.onComplete()
    } else {
      try {
        when (mediaState) {
          MediaPlayerState.END -> {
            initializeExoPlayer(context)
            prepare(mediaItem)
            start(mediaItem)
          }
          MediaPlayerState.IDLE, MediaPlayerState.STOPPED -> {
            prepare(mediaItem)
            start(mediaItem)
          }
          MediaPlayerState.PAUSED, MediaPlayerState.STARTED, MediaPlayerState.READY -> {
            if (currentMediaItem?.id != mediaItem.id)
              prepare(mediaItem)

            start(mediaItem)
          }
          MediaPlayerState.ERROR -> {
            completableEmitter.onError(
                if (isConnectedToInternet(mediaItem))
                  PlayerNotConnectedToInternetException()
                else
                  IllegalStateException("Can't playCurrentItem ${mediaItem.name} from mediaState $mediaState")
            )
          }
          else -> { Log.d(TAG, "playing ${mediaItem.name} from mediaState $mediaState") }
        }

        completableEmitter.onComplete()
      } catch (e: Exception) {
        completableEmitter.onError(e)
      }
    }
  }

  override fun pause(): Completable = Completable.fromAction {
    exoPlayer.playWhenReady = false
    Log.d("ExoPlayer - RxMedia", "pause(playWhenReady: false) - $this")

    mediaState = MediaPlayerState.PAUSED

    currentMediaItem?.let {
      stateDispatcher.onNext(PausedState(it, currentMediaProgress()))
    }
  }

  override fun prepareMedia(currentItem: MediaItem): Completable = Completable.fromAction {
    exoPlayer.playWhenReady = false
    Log.d("ExoPlayer - RxMedia", "prepareMedia(playWhenReady: false) : $this")

    mediaState = MediaPlayerState.STOPPED

    prepare(currentItem)
  }

  override fun stop(): Completable = Completable.fromAction {
    Log.d("ExoPlayer - RxMedia", "stop : $this")
    if (exoPlayer.playWhenReady) exoPlayer.stop()

    mediaState = MediaPlayerState.STOPPED

    currentMediaItem?.let {
      stateDispatcher.onNext(StoppedState(it, MediaProgress(0, exoPlayer.duration)))
    }
  }

  override fun seekTo(position: Long): Completable =
      Completable.fromAction {
        exoPlayer.seekTo(position)
        exoPlayer.playWhenReady = true
      }

  override fun setVolume(volume: Float): Completable =
      Completable.fromAction { exoPlayer.volume = volume }

  override fun release(): Completable {
    Log.d("ExoPlayer - RxMedia", "release: $this")
    return Single.fromCallable {
      exoPlayer.stop(true)
    }
        .doOnSuccess { mediaState = MediaPlayerState.END }
        .doOnSuccess { stateDispatcher.onNext(IdleState()) }
        .toCompletable()
        .doOnTerminate { compositeDisposable.clear() }
        .doOnError { Log.e("ExoPlayer - RxMedia", "release()", it) }
        .onErrorComplete()
  }

  override fun nowPlaying(): Single<Optional<MediaItem>> = Single.fromCallable { Optional.ofNullable(currentMediaItem) }

  override fun currentState(): Single<MediaServiceState> = stateDispatcher.singleOrError()

  override fun stateChanges(): Observable<MediaServiceState> = stateDispatcher.distinctUntilChanged()
      .doOnNext {
        when (it) {
          is LoadingState, is PausedState, is StoppedState, is CompletedState, is ErrorState -> {
            progressDisposable.dispose()
          }
          else -> {
            if (progressDisposable.isDisposed) currentMediaItem?.let{ mediaItem -> observePlayingState(mediaItem) }
          }
        }
      }

  override fun isPlaying(): Single<Boolean> =
      Single.fromCallable { exoPlayer.playWhenReady }

  override fun isPaused(): Single<Boolean> = Single.fromCallable { mediaState == MediaPlayerState.PAUSED }

  override fun setCookies(list: List<HttpCookie>): Single<Unit> =
      Single.fromCallable {
        mediaState = MediaPlayerState.IDLE
        cookies = list
      }

  private fun initializeExoPlayer(context: Context) {
    exoPlayer = ExoPlayerFactory.newSimpleInstance(
        context, DefaultRenderersFactory(context), DefaultTrackSelector(), DefaultLoadControl(), null, Looper.myLooper())

    exoPlayer.addListener(eventListener())
    exoPlayer.audioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.CONTENT_TYPE_MUSIC).build()
  }

  private fun eventListener(): Player.EventListener {
    return object : Player.EventListener {
      override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) { }

      override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) { }

      override fun onLoadingChanged(isLoading: Boolean) {
        if (isLoading) {
          currentMediaItem?.let { stateDispatcher.onNext(LoadingState(it)) }
        } else if (mediaState != MediaPlayerState.ERROR) {
          currentMediaItem?.let { stateDispatcher.onNext(PlayingState(it, currentMediaProgress())) }
        }
      }

      override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when (playbackState) {
          Player.STATE_ENDED -> {
            mediaState = MediaPlayerState.STOPPED
            currentMediaItem?.let { stateDispatcher.onNext(CompletedState(it)) }
          }

          Player.STATE_READY -> {
            mediaState = MediaPlayerState.READY
            currentMediaItem?.let { stateDispatcher.onNext(PlayingState(it, currentMediaProgress())) }
          }

          Player.STATE_BUFFERING -> {
            mediaState = MediaPlayerState.BUFFERING
          }
          Player.STATE_IDLE -> mediaState = MediaPlayerState.IDLE
        }
      }

      override fun onRepeatModeChanged(repeatMode: Int) { }

      override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) { }

      override fun onPlayerError(error: ExoPlaybackException) {
        currentMediaItem?.let {
          mediaState = MediaPlayerState.ERROR
          val errorState = ErrorState(it, progress = currentMediaProgress(), exception = error)
          stateDispatcher.onNext(errorState)
        }
      }

      override fun onPositionDiscontinuity(reason: Int) { }

      override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) { }

      override fun onSeekProcessed() { }
    }
  }

  private fun isConnectedToInternet(mediaItem: MediaItem) =
      context.isConnected().not() && mediaItem.url.startsWith("http")

  private fun prepare(mediaItem: MediaItem) {
    exoPlayer.playWhenReady = false
    currentMediaItem = mediaItem

    val mediaSource = buildMediaSource(retrieveUri(resolveDataSourceForMediaItem(mediaItem)), buildHttpDataSource(cookies))
    exoPlayer.prepare(mediaSource)
  }

  private fun start(mediaItem: MediaItem) {
    exoPlayer.playWhenReady = true

    observePlayingState(mediaItem)
    mediaState = MediaPlayerState.STARTED
    stateDispatcher.onNext(PlayingState(mediaItem, currentMediaProgress()))
  }

  private fun currentMediaProgress() = MediaProgress(exoPlayer.currentPosition, exoPlayer.duration)

  private fun observePlayingState(mediaItem: MediaItem) {
    if (progressDisposable.isDisposed.not()) {
      progressDisposable.dispose()
    }

    progressDisposable = Observable.interval(1, TimeUnit.SECONDS)
        .observeOn(customScheduler)
        .map {
          when (mediaState) {
            MediaPlayerState.PAUSED -> PausedState(mediaItem, currentMediaProgress())
            MediaPlayerState.STOPPED -> StoppedState(mediaItem, currentMediaProgress())
            MediaPlayerState.END, MediaPlayerState.IDLE -> IdleState()
            else -> PlayingState(mediaItem, currentMediaProgress())
          }
        }
        .doOnNext { stateDispatcher.onNext(it) }
        .retry()
        .subscribe()
  }

  @SuppressLint("SwitchIntDef")
  private fun buildMediaSource(uri: Uri, dataSourceFactory: DataSource.Factory): MediaSource {
    @C.ContentType val type = Util.inferContentType(uri)
    when (type) {
      C.TYPE_HLS -> return HlsMediaSource.Factory(dataSourceFactory)
          .setPlaylistParserFactory(SMHlsPlaylistParserFactory())
          .setAllowChunklessPreparation(true)
          .createMediaSource(uri)
      C.TYPE_OTHER -> {
        val factory: DataSource.Factory =
            if (uri.scheme != null && uri.scheme?.startsWith("http") == true) {
              dataSourceFactory
            } else {
              FileDataSourceFactory()
            }

        return ExtractorMediaSource.Factory(factory)
            .createMediaSource(uri)
      }
      else -> {
        throw IllegalStateException("Unsupported type: $type")
      }
    }
  }

  private fun buildHttpDataSource(httpCookies: List<HttpCookie>): DefaultHttpDataSourceFactory {
    val dataSourceFactory = DefaultHttpDataSourceFactory(javaClass.simpleName, DefaultBandwidthMeter())

    if (httpCookies.isEmpty())
      return dataSourceFactory

    val cookies = StringBuilder()
    httpCookies.map { cookies.append(it.toString().replace("\"", "")).append(";") }

    val mapCookies = HashMap<String, String>()
    mapCookies["Cookie"] = cookies.toString()
    dataSourceFactory.defaultRequestProperties.clearAndSet(mapCookies)

    return dataSourceFactory
  }

  private fun retrieveUri(url: String): Uri =
      if (url.startsWith("http")) {
        Uri.parse(url)
      } else {
        Uri.fromFile(File(url))
      }

  fun setCustomScheduler(scheduler: Scheduler) {
    customScheduler = scheduler
  }
}


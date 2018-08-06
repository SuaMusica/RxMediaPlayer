package br.com.suamusica.rxmediaplayer.android

import android.content.Context
import android.net.Uri
import android.util.Log
import br.com.suamusica.rxmediaplayer.domain.CompletedState
import br.com.suamusica.rxmediaplayer.domain.LoadingState
import br.com.suamusica.rxmediaplayer.domain.MediaItem
import br.com.suamusica.rxmediaplayer.domain.MediaProgress
import br.com.suamusica.rxmediaplayer.domain.MediaServiceState
import br.com.suamusica.rxmediaplayer.domain.PausedState
import br.com.suamusica.rxmediaplayer.domain.PlayingState
import br.com.suamusica.rxmediaplayer.domain.RxMediaPlayer
import br.com.suamusica.rxmediaplayer.domain.StoppedState
import br.com.suamusica.rxmediaplayer.utils.CustomHlsPlaylistParser
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
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
import io.reactivex.Single
import io.reactivex.disposables.Disposables
import io.reactivex.subjects.BehaviorSubject
import java.io.File
import java.net.HttpCookie
import java.util.HashMap
import java.util.concurrent.TimeUnit

class RxExoPlayer (
    private val context: Context,
    private val resolveDataSourceForMediaItem: (MediaItem) -> String = { it.url },
    private val cookies: List<HttpCookie> = emptyList()
) : RxMediaPlayer {

  private lateinit var exoPlayer: ExoPlayer
  private val stateDispatcher = BehaviorSubject.create<MediaServiceState>()
  private var progressDisposable = Disposables.disposed()
  private var currentMediaItem: MediaItem? = null
  private var state: MediaPlayerState = MediaPlayerState.IDLE

  init {
    initializeExoPlayer(context)
  }

  override fun play(): Completable = Single.fromCallable { currentMediaItem }
      .filter { it != null }
      .flatMapCompletable { play(it!!) }

  @Synchronized
  override fun play(mediaItem: MediaItem): Completable = Completable.create { completableEmitter ->
    try {
      when (state) {
        MediaPlayerState.END -> {
          initializeExoPlayer(context)
          prepare(mediaItem)
          start(mediaItem)
        }
        MediaPlayerState.IDLE, MediaPlayerState.STOPPED -> {
          prepare(mediaItem)
          start(mediaItem)
        }
        MediaPlayerState.PAUSED, MediaPlayerState.PREPARED -> {
          start(mediaItem)
        }
        MediaPlayerState.ERROR -> {
          throw IllegalStateException("Can't play $mediaItem from state $state")
        }
        else -> Log.d(RxExoPlayer::class.java.simpleName, "Can't play $mediaItem from state $state")
      }

      completableEmitter.onComplete()
    } catch (e: Exception) {
      completableEmitter.onError(e)
    }
  }

  override fun pause(): Completable = Completable.fromAction {
    exoPlayer.playWhenReady = false

    state = MediaPlayerState.PAUSED

    currentMediaItem?.let {
      stateDispatcher.onNext(PausedState(it, currentMediaProgress()))
    }
  }

  override fun stop(): Completable = Completable.fromAction {
    exoPlayer.stop()

    state = MediaPlayerState.STOPPED

    currentMediaItem?.let {
      stateDispatcher.onNext(StoppedState(it, MediaProgress(0, exoPlayer.duration)))
    }
  }

  override fun release(): Completable {
    return Completable.fromAction { exoPlayer.release() }.andThen { state = MediaPlayerState.END }
  }

  override fun nowPlaying(): Maybe<MediaItem> = Maybe.create { emitter ->
    currentMediaItem?.let { emitter.onSuccess(it) } ?: emitter.onComplete()
  }

  override fun stateChanges(): Observable<MediaServiceState> = stateDispatcher.distinctUntilChanged()
      .doOnNext {
        if (it !is PlayingState) {
          progressDisposable.dispose()
        }
      }

  override fun isPlaying(): Single<Boolean> = Single.fromCallable { exoPlayer.playWhenReady }

  override fun isPaused(): Single<Boolean> = Single.fromCallable { state == MediaPlayerState.PAUSED }

  private fun initializeExoPlayer(context: Context) {
    val renderersFactory = DefaultRenderersFactory(context)
    val trackSelector = AdaptiveTrackSelection.Factory(DefaultBandwidthMeter())

    exoPlayer = ExoPlayerFactory.newSimpleInstance(renderersFactory, DefaultTrackSelector(trackSelector))
    exoPlayer.addListener(eventListener())

  }

  private fun eventListener(): Player.EventListener {
    return object : Player.EventListener {
      override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) { }

      override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) { }

      override fun onLoadingChanged(isLoading: Boolean) {
        if (isLoading) {
          currentMediaItem?.let { stateDispatcher.onNext(LoadingState(it)) }
        } else {
          currentMediaItem?.let { stateDispatcher.onNext(PlayingState(it, currentMediaProgress())) }
        }
      }

      override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when (playbackState) {
          Player.STATE_ENDED -> {
            currentMediaItem?.let {
              state = MediaPlayerState.STOPPED
              stateDispatcher.onNext(CompletedState(it))
            }
          }
          Player.STATE_READY -> state = MediaPlayerState.PREPARED

          Player.STATE_BUFFERING -> currentMediaItem?.let { stateDispatcher.onNext(LoadingState(it)) }

          Player.STATE_IDLE -> {
            currentMediaItem?.let {
              state = MediaPlayerState.IDLE
              stateDispatcher.onNext(CompletedState(it))
            }
          }
        }
      }

      override fun onRepeatModeChanged(repeatMode: Int) { }

      override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) { }

      override fun onPlayerError(error: ExoPlaybackException?) {
        currentMediaItem?.let {
          state = MediaPlayerState.ERROR
          stateDispatcher.onNext(CompletedState(it))
        }
      }

      override fun onPositionDiscontinuity(reason: Int) { }

      override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) { }

      override fun onSeekProcessed() { }
    }
  }

  private fun prepare(mediaItem: MediaItem) {
    currentMediaItem = mediaItem
    stateDispatcher.onNext(LoadingState(mediaItem))

    val mediaSource = buildMediaSource(retrieveUri(resolveDataSourceForMediaItem(mediaItem)), buildHttpDataSource(cookies))

    exoPlayer.stop(true)
    exoPlayer.prepare(mediaSource)
  }

  private fun start(mediaItem: MediaItem) {
    exoPlayer.playWhenReady = true
    observePlayingState(mediaItem)
    state = MediaPlayerState.STARTED
    stateDispatcher.onNext(PlayingState(mediaItem, currentMediaProgress()))
  }

  private fun currentMediaProgress() =
      MediaProgress(exoPlayer.currentPosition, exoPlayer.duration)

  private fun observePlayingState(mediaItem: MediaItem) {
    if (progressDisposable.isDisposed.not()) {
      progressDisposable.dispose()
    }

    progressDisposable = Observable.interval(1, TimeUnit.SECONDS)
        .map { PlayingState(mediaItem, currentMediaProgress()) }
        .doOnNext { stateDispatcher.onNext(it) }
        .doOnNext { }
        .retry()
        .subscribe()
  }

  private fun buildMediaSource(uri: Uri, dataSourceFactory: DataSource.Factory): MediaSource {
    @C.ContentType val type = Util.inferContentType(uri)
    when (type) {
      C.TYPE_HLS -> return HlsMediaSource.Factory(dataSourceFactory)
          .setPlaylistParser(CustomHlsPlaylistParser())
          .setAllowChunklessPreparation(true)
          .createMediaSource(uri)
      C.TYPE_OTHER -> {
        val factory: DataSource.Factory =
            if (uri.scheme != null && uri.scheme.startsWith("http")) {
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
}
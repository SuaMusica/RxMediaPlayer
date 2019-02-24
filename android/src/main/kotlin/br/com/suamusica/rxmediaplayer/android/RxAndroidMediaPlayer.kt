package br.com.suamusica.rxmediaplayer.android

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.PowerManager
import android.support.v4.media.AudioAttributesCompat
import br.com.suamusica.rxmediaplayer.android.MediaPlayerState.END
import br.com.suamusica.rxmediaplayer.android.MediaPlayerState.IDLE
import br.com.suamusica.rxmediaplayer.android.MediaPlayerState.INITIALIZED
import br.com.suamusica.rxmediaplayer.android.MediaPlayerState.PAUSED
import br.com.suamusica.rxmediaplayer.android.MediaPlayerState.READY
import br.com.suamusica.rxmediaplayer.android.MediaPlayerState.STARTED
import br.com.suamusica.rxmediaplayer.android.MediaPlayerState.STOPPED
import br.com.suamusica.rxmediaplayer.domain.CompletedState
import br.com.suamusica.rxmediaplayer.domain.LoadingState
import br.com.suamusica.rxmediaplayer.domain.MediaItem
import br.com.suamusica.rxmediaplayer.domain.MediaProgress
import br.com.suamusica.rxmediaplayer.domain.MediaServiceState
import br.com.suamusica.rxmediaplayer.domain.Optional
import br.com.suamusica.rxmediaplayer.domain.PausedState
import br.com.suamusica.rxmediaplayer.domain.PlayingState
import br.com.suamusica.rxmediaplayer.domain.RxMediaPlayer
import br.com.suamusica.rxmediaplayer.domain.StoppedState
import io.reactivex.Completable
import io.reactivex.Completable.create
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposables
import io.reactivex.subjects.BehaviorSubject
import java.net.HttpCookie
import java.util.concurrent.TimeUnit

class RxAndroidMediaPlayer(
    private val context: Context,
    private val resolveDataSourceForMediaItem: (MediaItem) -> String = { it.url }
) : RxMediaPlayer {

  private lateinit var mediaPlayer: MediaPlayer
  private val stateDispatcher = BehaviorSubject.create<MediaServiceState>()
  private var progressDisposable = Disposables.disposed()
  private var currentMediaItem: MediaItem? = null
  private var state: MediaPlayerState = IDLE

  init {
    initializeMediaPlayer(context)
  }

  override fun play(): Completable =
      Single.fromCallable { currentMediaItem }
          .flatMapCompletable { play(it) }

  @Synchronized
  override fun play(mediaItem: MediaItem): Completable = create { completableEmitter ->
    try {
      when (state) {
        END -> {
          initializeMediaPlayer(context)
          prepare(mediaItem)
          start(mediaItem)
        }
        IDLE, STOPPED -> {
          prepare(mediaItem)
          start(mediaItem)
        }
        PAUSED, READY -> {
          start(mediaItem)
        }
        else -> throw IllegalStateException("Can't playCurrentItem $mediaItem from state $state")
      }

      completableEmitter.onComplete()
    } catch (e: Exception) {
      completableEmitter.onError(e)
    }
  }

  override fun prepareMedia(currentItem: MediaItem): Completable = Completable.fromAction {
    mediaPlayer.pause()

    state = PAUSED

    prepare(currentItem)
  }

  override fun pause(): Completable = Completable.fromAction {
    mediaPlayer.pause()

    state = PAUSED

    currentMediaItem?.let {
      stateDispatcher.onNext(PausedState(it, currentMediaProgress()))
    }
  }

  override fun stop(): Completable = Completable.fromAction {
    mediaPlayer.stop()

    state = STOPPED

    currentMediaItem?.let {
      stateDispatcher.onNext(StoppedState(it, MediaProgress(0, mediaPlayer.duration.toLong())))
    }
  }

  override fun seekTo(position: Long): Completable = Completable.fromAction { mediaPlayer.seekTo(position.toInt()) }

  override fun setVolume(volume: Float): Completable = Completable.fromAction { mediaPlayer.setVolume(volume, volume) }

  override fun release(): Completable {
    return Completable.fromAction { mediaPlayer.release() }.andThen { state = END }
  }

  override fun nowPlaying(): Single<Optional<MediaItem>> = Single.fromCallable { Optional.ofNullable(currentMediaItem) }

  override fun currentState(): Maybe<MediaServiceState> = Maybe.create { emitter ->
    stateDispatcher.value?.let { emitter.onSuccess(it) } ?: emitter.onComplete()
  }

  override fun stateChanges(): Observable<MediaServiceState> = stateDispatcher.distinctUntilChanged()
      .doOnNext {
        if (it !is PlayingState) {
          progressDisposable.dispose()
        }
      }

  override fun isPlaying(): Single<Boolean> = Single.fromCallable { mediaPlayer.isPlaying }

  override fun isPaused(): Single<Boolean> = Single.fromCallable { state == PAUSED }

  override fun setCookies(list: List<HttpCookie>): Single<Unit> = Single.error(NotImplementedError())

  private fun initializeMediaPlayer(context: Context) {
    mediaPlayer = MediaPlayer()
    mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      mediaPlayer.setAudioAttributes(
          AudioAttributesCompat.Builder()
              .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
              .setLegacyStreamType(AudioAttributesCompat.USAGE_MEDIA)
              .build().unwrap() as AudioAttributes)
    } else {
      mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
    }

    mediaPlayer.setOnCompletionListener {
      currentMediaItem?.let {
        state = STOPPED
        stateDispatcher.onNext(CompletedState(it))
      }
    }
    state = IDLE
  }

  private fun prepare(mediaItem: MediaItem) {
    currentMediaItem = mediaItem
    stateDispatcher.onNext(LoadingState(mediaItem))
    mediaPlayer.reset()
    mediaPlayer.setDataSource(resolveDataSourceForMediaItem(mediaItem))
    state = INITIALIZED
    mediaPlayer.prepare()
    state = READY
  }

  private fun start(mediaItem: MediaItem) {
    mediaPlayer.start()
    observePlayingState(mediaItem)
    state = STARTED
    stateDispatcher.onNext(PlayingState(mediaItem, currentMediaProgress()))
  }

  private fun currentMediaProgress() =
      MediaProgress(mediaPlayer.currentPosition.toLong(), mediaPlayer.duration.toLong())

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
}
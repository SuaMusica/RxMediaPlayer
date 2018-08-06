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
import br.com.suamusica.rxmediaplayer.android.MediaPlayerState.PREPARED
import br.com.suamusica.rxmediaplayer.android.MediaPlayerState.STARTED
import br.com.suamusica.rxmediaplayer.android.MediaPlayerState.STOPPED
import br.com.suamusica.rxmediaplayer.domain.CompletedState
import br.com.suamusica.rxmediaplayer.domain.LoadingState
import br.com.suamusica.rxmediaplayer.domain.MediaItem
import br.com.suamusica.rxmediaplayer.domain.MediaProgress
import br.com.suamusica.rxmediaplayer.domain.MediaServiceState
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
          .filter { it != null }
          .onErrorComplete()
          .flatMapCompletable { play(it!!) }

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
        PAUSED, PREPARED -> {
          start(mediaItem)
        }
        else -> throw IllegalStateException("Can't play $mediaItem from state $state")
      }

      completableEmitter.onComplete()
    } catch (e: Exception) {
      completableEmitter.onError(e)
    }
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

  override fun release(): Completable {
    return Completable.fromAction { mediaPlayer.release() }.andThen { state = END }
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

  override fun isPlaying(): Single<Boolean> = Single.fromCallable { mediaPlayer.isPlaying }

  override fun isPaused(): Single<Boolean> = Single.fromCallable { state == PAUSED }

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
    state = PREPARED
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
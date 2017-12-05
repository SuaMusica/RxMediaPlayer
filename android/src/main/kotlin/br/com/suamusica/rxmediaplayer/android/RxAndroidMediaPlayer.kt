package br.com.suamusica.rxmediaplayer.android

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.PowerManager
import br.com.suamusica.rxmediaplayer.domain.*
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.disposables.Disposables
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

class RxAndroidMediaPlayer(context: Context) : RxMediaPlayer {

  private var state by Delegates.observable<MediaPlayerState>(IdleState) { _, _, newState ->
    stateDispatcher.onNext(newState)
  }

  private val mediaPlayer: MediaPlayer = MediaPlayer()
  private val stateDispatcher = BehaviorSubject.create<MediaPlayerState>()
  private var progressDisposable = Disposables.disposed()
  private var currentMediaItem: MediaItem? = null

  init {
    mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
    mediaPlayer.setOnCompletionListener {
      currentMediaItem?.let { state = CompletedState(it) }
    }

    stateChanges()
      .filter { it !is PlayingState }
      .doOnNext { progressDisposable.dispose() }
      .retry()
      .subscribe()
  }

  override fun play(mediaItem: MediaItem) = Completable.create { completableEmitter ->
    try {
      if (currentMediaItem != mediaItem) {
        currentMediaItem = mediaItem
        mediaPlayer.setDataSource(mediaItem.url)
        mediaPlayer.prepare()
      }

      observePlayingState(mediaItem)

      mediaPlayer.start()

      completableEmitter.onComplete()
    } catch (e: Exception) {
      completableEmitter.onError(e)
    }
  }

  override fun pause(): Completable = Completable.fromAction {
    mediaPlayer.pause()

    currentMediaItem?.let {
      state = PausedState(it, currentMediaProgress())
    }
  }

  override fun stop(): Completable = Completable.fromAction {
    mediaPlayer.stop()

    currentMediaItem?.let {
      state = StoppedState(it, MediaProgress(0, mediaPlayer.duration))
    }
  }
  override fun nowPlaying(): Maybe<MediaItem> = Maybe.create { emitter ->
    currentMediaItem?.let { emitter.onSuccess(it) } ?: emitter.onComplete()
  }

  override fun stateChanges(): Observable<MediaPlayerState> = stateDispatcher.distinctUntilChanged()

  private fun currentMediaProgress() =
    MediaProgress(mediaPlayer.currentPosition, mediaPlayer.duration)

  private fun observePlayingState(mediaItem: MediaItem) {
    if (progressDisposable.isDisposed.not()) {
      progressDisposable.dispose()
    }

    progressDisposable = Observable.timer(1, TimeUnit.SECONDS)
      .map { PlayingState(mediaItem, currentMediaProgress()) }
      .doOnNext { state = it }
      .retry()
      .subscribe()
  }
}
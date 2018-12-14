package br.com.suamusica.rxmediaplayer.domain

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

interface RxMediaService {
  // manage queue
  fun add(mediaItem: MediaItem, playWhenReady: Boolean = true): Completable

  fun add(mediaItems: List<MediaItem>, playWhenReady: Boolean = true): Completable

  fun remove(index: Int): Completable

  fun remove(mediaItems: List<MediaItem>): Completable

  fun removeAll(): Completable

  fun reorder(indexA: Int, indexB: Int): Completable

  fun queue(): Observable<MediaItem>

  fun changeRandomState(randomized: Boolean): Completable

  // query state
  fun isRandomized(): Single<Boolean>

  fun isPlaying(): Single<Boolean>

  fun isPaused(): Single<Boolean>

  // manage stateChanges state
  fun play(): Completable

  fun play(mediaItem: MediaItem): Completable

  fun play(mediaItems: List<MediaItem>): Completable

  fun next(): Completable

  fun previous(): Completable

  fun pause(): Completable

  fun stop(): Completable

  fun seekTo(position: Long): Completable

  fun changeRepeatState(repeatMode: RepeatState) : Completable

  fun goTo(mediaItem: MediaItem): Completable

  fun setVolume(volume: Float): Completable

  // event streams
  fun stateChanges(): Observable<MediaServiceState>

  // release resources
  fun release(): Completable

  // queue update streams
  fun queueChanges(): Observable<List<MediaItem>>

  companion object {
    fun create(rxMediaPlayer: RxMediaPlayer, scheduler: Scheduler = Schedulers.computation()): RxMediaService =
        RxMediaServiceImpl(rxMediaPlayer, scheduler)
  }
}

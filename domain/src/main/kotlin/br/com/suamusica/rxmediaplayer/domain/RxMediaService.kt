package br.com.suamusica.rxmediaplayer.domain

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

interface RxMediaService {
  // manage queue
  fun add(mediaItem: MediaItem) : Completable

  fun add(mediaItem: List<MediaItem>) : Completable

  fun remove(index: Int) : Completable
  fun removeAll() : Completable

  fun reorder(indexA: Int, indexB: Int) : Completable

  fun queue(): Observable<MediaItem>

  fun changeRandomState(randomized: Boolean): Completable

  fun isRandomized(): Single<Boolean>

  // manage stateChanges state
  fun play(): Completable
  fun play(mediaItem: MediaItem): Completable
  fun next(): Completable
  fun previous(): Completable

  fun pause(): Completable
  fun stop(): Completable

  // event streams
  fun stateChanges(): Observable<MediaPlayerState>

  companion object {
    fun create(rxMediaPlayer: RxMediaPlayer, scheduler: Scheduler = Schedulers.computation()): RxMediaService =
      RxMediaServiceImpl(rxMediaPlayer, scheduler)
  }
}

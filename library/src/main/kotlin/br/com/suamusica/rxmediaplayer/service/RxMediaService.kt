package br.com.suamusica.rxmediaplayer.service

import br.com.suamusica.rxmediaplayer.domain.MediaItem
import br.com.suamusica.rxmediaplayer.domain.MediaProgress
import br.com.suamusica.rxmediaplayer.domain.Status
import br.com.suamusica.rxmediaplayer.infra.RxMediaPlayer
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

interface RxMediaService {
  // manage queue
  fun add(mediaItem: MediaItem) : Completable

  fun remove(index: Int) : Completable
  fun removeAll() : Completable

  fun reorder(indexA: Int, indexB: Int) : Completable

  fun queue(): Observable<MediaItem>

  fun changeRandomState(randomized: Boolean): Completable

  fun isRandomized(): Single<Boolean>

  // manage status state
  fun play(): Completable
  fun next(): Completable
  fun previous(): Completable

  fun pause(): Completable
  fun stop(): Completable

  // event streams
  fun status(): Observable<Triple<MediaItem, Status, MediaProgress>>

  companion object {
    fun create(rxMediaPlayer: RxMediaPlayer, scheduler: Scheduler = Schedulers.computation()): RxMediaService =
      RxMediaServiceImpl(rxMediaPlayer, scheduler)
  }
}

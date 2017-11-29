package br.com.suamusica.rxmediaplayer.domain

import io.reactivex.*
import io.reactivex.Observable
import java.util.*

internal class RxMediaServiceImpl(
  private val rxMediaPlayer: RxMediaPlayer,
  private val scheduler: Scheduler
) : RxMediaService {
  private val queue: LinkedList<MediaItem> = LinkedList()
  private var randomized = false

  override fun add(mediaItem: MediaItem): Completable =
    Completable.fromCallable { queue.add(mediaItem.copy()) }
      .subscribeOn(scheduler)

  override fun remove(index: Int): Completable =
    Completable.fromCallable { queue.removeAt(index) }
      .subscribeOn(scheduler)

  override fun removeAll(): Completable =
    Completable.fromCallable { queue.clear() }
      .subscribeOn(scheduler)

  override fun reorder(indexA: Int, indexB: Int): Completable =
    Completable.fromCallable {
      val itemA = queue.get(indexA)
      val itemB = queue.get(indexB)

      queue.set(indexA, itemB)
      queue.set(indexB, itemA)
    }.subscribeOn(scheduler)

  override fun queue(): Observable<MediaItem> =
    Observable.create<MediaItem> { emitter ->
      try {
        queue.forEach { emitter.onNext(it) }
        emitter.onComplete()
      } catch (t: Throwable) {
        emitter.onError(t)
      }
    }.subscribeOn(scheduler)

  override fun changeRandomState(randomized: Boolean): Completable =
    Completable.fromAction {
      this@RxMediaServiceImpl.randomized = randomized
    }.subscribeOn(scheduler)

  override fun isRandomized(): Single<Boolean> =
    Single.fromCallable { randomized }
      .subscribeOn(scheduler)

  override fun play(): Completable =
    rxMediaPlayer.nowPlaying()
      .switchIfEmpty(maybeFirst())
      .flatMapCompletable { rxMediaPlayer.play(it) }
      .subscribeOn(scheduler)

  override fun next(): Completable =
    maybeNext().flatMapCompletable { rxMediaPlayer.play(it) }
      .subscribeOn(scheduler)

  override fun previous(): Completable =
    maybePrevious().flatMapCompletable { rxMediaPlayer.play(it) }
      .subscribeOn(scheduler)

  override fun pause() = rxMediaPlayer.pause()

  override fun stop() = rxMediaPlayer.stop()

  override fun status() = rxMediaPlayer.status()

  private fun maybeFirst() = maybeMediaItemBasedOnCurrentIndex { 0 }

  private fun maybeNext() =
    maybeRandom().switchIfEmpty(maybeMediaItemBasedOnCurrentIndex { it + 1 })

  private fun maybePrevious() =
    maybeRandom().switchIfEmpty(maybeMediaItemBasedOnCurrentIndex { it - 1 })

  private fun maybeRandom() =
    maybeMediaItemBasedOnCurrentIndex { Random().nextInt(queue.size) }
      .filter { randomized }

  private fun maybeMediaItemBasedOnCurrentIndex(mapIndex: (Int) -> Int): Maybe<MediaItem> {
    return rxMediaPlayer.nowPlaying()
      .flatMap {
        if (queue.isEmpty()) {
          return@flatMap Maybe.empty<MediaItem>()
        }

        return@flatMap Maybe.fromCallable {
          val currentIndex = queue.indexOf(it)
          val index = mapIndex(currentIndex)
          return@fromCallable queue[index]
        }
      }
      .onErrorComplete()
      .subscribeOn(scheduler)
  }
}

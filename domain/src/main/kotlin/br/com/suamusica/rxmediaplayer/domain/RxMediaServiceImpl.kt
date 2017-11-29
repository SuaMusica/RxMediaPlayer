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
    Completable.fromAction { queue.push(mediaItem.copy()) }
      .subscribeOn(scheduler)

  override fun add(mediaItem: List<MediaItem>): Completable =
    Observable.fromIterable(mediaItem.toList())
      .concatMapCompletable { add(it) }

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

  private fun maybeFirst() = Maybe.create<MediaItem> {
    when (queue.peek()) {
      null -> it.onComplete()
      else -> {
        it.onSuccess(queue.peek())
        it.onComplete()
      }
    }
  }

  private fun maybeNext() =
    maybeRandom().switchIfEmpty(
      maybeMediaItemBasedOnElement { current ->
        queue.getMappingReferenceIndex(referenceItem = current, mapReferenceIndex = { it + 1 })
      }
    )

  private fun maybePrevious() =
    maybeRandom().switchIfEmpty(
      maybeMediaItemBasedOnElement { current ->
        queue.getMappingReferenceIndex(referenceItem = current, mapReferenceIndex = { it - 1 })
      }
    )

  private fun maybeRandom(): Maybe<MediaItem> =
    maybeMediaItemBasedOnElement { current -> queue.getRandomElement(ignore = current) }
      .filter { randomized }


  private fun maybeMediaItemBasedOnElement(mapElement: (MediaItem) -> MediaItem?): Maybe<MediaItem> {
    return rxMediaPlayer.nowPlaying()
      .flatMap { playingItem ->
        if (queue.isEmpty()) {
          return@flatMap Maybe.empty<MediaItem>()
        }

        val mapped = mapElement(playingItem)

        if (mapped == null) Maybe.empty<MediaItem>() else Maybe.just(mapped)
      }
      .onErrorComplete()
  }
}

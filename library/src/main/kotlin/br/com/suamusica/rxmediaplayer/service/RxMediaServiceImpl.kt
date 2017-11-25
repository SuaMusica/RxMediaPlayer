package br.com.suamusica.rxmediaplayer.service

import br.com.suamusica.rxmediaplayer.domain.MediaItem
import br.com.suamusica.rxmediaplayer.infra.MediaPlayer
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Scheduler
import java.util.*

internal class RxMediaServiceImpl(
  private val mediaPlayer: MediaPlayer,
  private val scheduler: Scheduler
) : RxMediaService {
  private val queue: LinkedList<MediaItem> = LinkedList()

  override fun add(mediaItem: MediaItem): Completable =
    Completable.fromCallable { queue.add(mediaItem.copy()) }
      .subscribeOn(scheduler)

  override fun remove(index: Int): Completable =
    Completable.fromCallable { queue.removeAt(index) }
      .subscribeOn(scheduler)

  override fun removeAll(): Completable =
    Completable.fromCallable { queue.clear() }
      .subscribeOn(scheduler)

  override fun reorder(indexA: Int, indexB: Int) =
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

  override fun play(): Completable =
    mediaPlayer.nowPlaying()
      .switchIfEmpty(maybeFirst())
      .flatMapCompletable { mediaPlayer.play(queue.first) }
      .subscribeOn(scheduler)

  private fun maybeFirst() = maybeMediaItemBasedOnCurrentIndex { 0 }
  private fun maybeNext() = maybeMediaItemBasedOnCurrentIndex { it + 1 }
  private fun maybePrevious() = maybeMediaItemBasedOnCurrentIndex { it - 1 }

  private fun maybeMediaItemBasedOnCurrentIndex(mapIndex: (Int) -> Int): Maybe<MediaItem> {
    return mediaPlayer.nowPlaying()
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


  override fun next() = maybeNext().flatMapCompletable { mediaPlayer.play(it) }

  override fun previous() = maybePrevious().flatMapCompletable { mediaPlayer.play(it) }

  override fun pause() = mediaPlayer.pause()

  override fun stop() = mediaPlayer.stop()

  override fun nowPlaying() = mediaPlayer.status()
}

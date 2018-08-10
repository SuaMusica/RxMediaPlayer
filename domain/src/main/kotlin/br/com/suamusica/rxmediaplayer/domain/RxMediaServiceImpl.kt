package br.com.suamusica.rxmediaplayer.domain

import io.reactivex.Completable
import io.reactivex.CompletableSource
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import java.util.LinkedList

internal class RxMediaServiceImpl(
    private val rxMediaPlayer: RxMediaPlayer,
    private val scheduler: Scheduler
) : RxMediaService {
  private val queue: LinkedList<MediaItem> = LinkedList()
  private var randomized = false
  private var repeatState = RepeatState.OFF
  private val disposables = CompositeDisposable()

  init {
    disposables.add(
        rxMediaPlayer.stateChanges()
            .subscribeOn(scheduler)
            .filter { it is CompletedState }
            .flatMapCompletable(this::handleCompletedState)
            .subscribe()
    )
  }

  override fun add(mediaItem: MediaItem): Completable =
      Single.fromCallable { queue.isEmpty() }
          .subscribeOn(scheduler)
          .flatMapCompletable { isEmptyQueue ->
            queue.offer(mediaItem)

            if (isEmptyQueue) {
              play(mediaItem)
            } else {
              Completable.complete()
            }
          }

  override fun add(mediaItems: List<MediaItem>): Completable =
      Observable.fromIterable(mediaItems)
          .subscribeOn(scheduler)
          .concatMapCompletable { add(it) }

  override fun remove(index: Int): Completable =
      Completable.fromCallable { queue.removeAt(index) }
          .subscribeOn(scheduler)

  override fun removeAll(): Completable =
      Completable.fromCallable { queue.clear() }
          .subscribeOn(scheduler)

  override fun reorder(indexA: Int, indexB: Int): Completable =
      Completable.fromCallable {
        val itemA = queue[indexA]
        val itemB = queue[indexB]

        queue[indexA] = itemB
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

  override fun isRandomized(): Single<Boolean> = Single.fromCallable { randomized }.subscribeOn(scheduler)

  override fun isPlaying(): Single<Boolean> = rxMediaPlayer.isPlaying().subscribeOn(scheduler)

  override fun isPaused(): Single<Boolean> = rxMediaPlayer.isPaused().subscribeOn(scheduler)

  override fun play(): Completable = rxMediaPlayer.play().subscribeOn(scheduler)

  override fun play(mediaItem: MediaItem): Completable = stop()
      .subscribeOn(scheduler)
      .andThen(Single.fromCallable { mediaItem })
      .flatMapCompletable { rxMediaPlayer.play(it) }

  override fun play(mediaItems: List<MediaItem>): Completable = stop()
      .subscribeOn(scheduler)
      .andThen(Completable.fromAction { queue.addAll(0, mediaItems.map { it }) })
      .andThen(maybeFirst())
      .flatMapCompletable { rxMediaPlayer.play(it) }

  override fun next(): Completable = maybeNext()
          .flatMapCompletable {
            rxMediaPlayer.stop()
              .andThen(rxMediaPlayer.play(it))
          }
          .subscribeOn(scheduler)

  override fun previous(): Completable = maybePrevious()
          .flatMapCompletable {
            rxMediaPlayer.stop()
                .andThen(rxMediaPlayer.play(it))
          }
          .subscribeOn(scheduler)

  override fun pause(): Completable = rxMediaPlayer.pause().subscribeOn(scheduler)

  override fun stop(): Completable = rxMediaPlayer.stop().subscribeOn(scheduler)

  override fun seekTo(position: Long): Completable = rxMediaPlayer.seekTo(position).subscribeOn(scheduler)

  override fun changeRepeatState(repeatMode: RepeatState): Completable =
      Completable.fromAction {
        this@RxMediaServiceImpl.repeatState = repeatMode
      }.subscribeOn(scheduler)

  override fun stateChanges(): Observable<MediaServiceState> =
      rxMediaPlayer.stateChanges()
          .map { it.setRandomizedState(randomized) }
          .map { it.setRepeatModeState(repeatState) }

  override fun release(): Completable = rxMediaPlayer.release().subscribeOn(scheduler)

  private fun handleCompletedState(it: MediaServiceState): CompletableSource? {
    println("RxMediaServiceImpl/Player: handleCompletedState - mediaServiceState: ${it.javaClass} - repeatState: $repeatState")
    return when (repeatState) {
      RepeatState.OFF -> next()
      RepeatState.ONE -> seekTo(0)
      RepeatState.ALL -> {
        val mediaItem = it.setRandomizedState(randomized).item
        val isLastItem = queue.indexOf(mediaItem) + 1 == queue.size
        if (isLastItem) {
          playFirst()
        } else {
          next()
        }
      }
    }
  }

  private fun playFirst() = maybeFirst()
      .flatMapCompletable {
        println("RxMediaServiceImpl/Player: playFirst: ${it.name} - repeatState: $repeatState")
        rxMediaPlayer.play(it)
      }
      .subscribeOn(scheduler)

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

package br.com.suamusica.rxmediaplayer.domain

import io.reactivex.Completable
import io.reactivex.CompletableSource
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject
import java.net.HttpCookie
import java.util.LinkedList

internal class RxMediaServiceImpl(
    private val rxMediaPlayer: RxMediaPlayer,
    private val scheduler: Scheduler
) : RxMediaService {
  private val stateDispatcher = PublishSubject.create<MediaServiceState>()
  private val queueDispatcher = PublishSubject.create<List<MediaItem>>()
  private var originalQueue: LinkedList<MediaItem> = LinkedList()
  private var queue: LinkedList<MediaItem> = LinkedList()
  private var randomized = false
  private var repeatState = RepeatState.OFF
  private val disposables = CompositeDisposable()

  init {
    disposables.add(
        rxMediaPlayer.stateChanges()
            .subscribeOn(scheduler)
            .onErrorReturnItem(IdleState())
            .filter { it is CompletedState }
            .flatMapCompletable(this::handleCompletedState)
            .onErrorComplete()
            .subscribe()
    )
  }

  override fun add(mediaItem: MediaItem, playWhenReady: Boolean): Completable =
      Single.fromCallable { queue.isEmpty() }
          .subscribeOn(scheduler)
          .flatMapCompletable { isEmptyQueue ->
            queue.offer(mediaItem)

            if (isEmptyQueue) {
              if (playWhenReady)
                play(mediaItem)
              else
                prepareMedia(mediaItem)
            } else {
              Completable.complete()
            }
          }

  override fun add(mediaItems: List<MediaItem>, playWhenReady: Boolean, fromBeginning: Boolean): Completable =
      Single.fromCallable { queue.isEmpty() }
          .subscribeOn(scheduler)
          .flatMapCompletable { isEmptyQueue ->
            if (fromBeginning)
              queue.addAll(0, mediaItems)
            else
              queue.addAll(mediaItems)

            val firstItem = queue.peek()

            if (isEmptyQueue && firstItem != null)
              if (playWhenReady)
                play(firstItem)
              else
                prepareMedia(firstItem)
            else
              Completable.complete()
          }

  override fun remove(index: Int): Completable =
      Completable.fromCallable {
        if (queue.size > index) queue.removeAt(index)
      }.subscribeOn(scheduler)

  override fun remove(mediaItems: List<MediaItem>): Completable =
      if (queue.containsAll(mediaItems) && queue.count() == mediaItems.count())
        removeAll(true)
      else Observable.fromIterable(mediaItems)
          .zipWith(nowPlaying().toObservable(),
              BiFunction { itemToRemove: MediaItem, nowPlaying: Optional<MediaItem> ->
                if (queue.contains(itemToRemove)) queue.remove(itemToRemove)

                nowPlaying.isPresent() && nowPlaying.get() == itemToRemove
          })
          .flatMapCompletable { removedMediaIsPlaying ->
            if (removedMediaIsPlaying)
              stop().andThen(next())
            else
              Completable.complete()
          }
          .andThen {
            if (queue.isEmpty()) rxMediaPlayer.release()
            else Completable.complete()
          }
          .subscribeOn(scheduler)


  override fun removeAll(release: Boolean): Completable =
      Single.fromCallable { queue.clear() }
          .flatMapCompletable {
            if (release)
              rxMediaPlayer.release()
            else
              rxMediaPlayer.stop()
          }
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

  override fun countQueue(): Single<Int> = Single.fromCallable { queue.size }

  override fun isRandomized(): Single<Boolean> = Single.fromCallable { randomized }.subscribeOn(scheduler)

  override fun isPlaying(): Single<Boolean> = rxMediaPlayer.isPlaying().subscribeOn(scheduler)

  override fun isPaused(): Single<Boolean> = rxMediaPlayer.isPaused().subscribeOn(scheduler)

  override fun play(): Completable = rxMediaPlayer.play().subscribeOn(scheduler)

  override fun nowPlaying(): Single<Optional<MediaItem>> =
      rxMediaPlayer.nowPlaying().subscribeOn(scheduler)

  override fun play(mediaItem: MediaItem): Completable =
      stop()
          .andThen(
              Completable.fromAction {
                if (queue.isEmpty()) queue.addLast(mediaItem)
                if (queue.contains(mediaItem).not()) queue.addFirst(mediaItem)
              })
          .subscribeOn(scheduler)
          .andThen(Single.fromCallable { mediaItem })
          .flatMapCompletable { rxMediaPlayer.play(mediaItem) }

  override fun play(mediaItems: List<MediaItem>): Completable =
      stop()
          .andThen(
              Completable.fromAction {
                queue.addAll(0, mediaItems)
              })
          .subscribeOn(scheduler)
          .andThen(maybeFirst())
          .flatMapCompletable { rxMediaPlayer.play(it) }

  override fun next(): Completable =
      maybeNext()
          .flatMapCompletable { stop().andThen(rxMediaPlayer.play(it)) }
          .subscribeOn(scheduler)

  override fun previous(): Completable =
      maybePrevious()
          .flatMapCompletable { stop().andThen(rxMediaPlayer.play(it)) }
          .subscribeOn(scheduler)

  override fun pause(): Completable = rxMediaPlayer.pause().subscribeOn(scheduler)

  override fun stop(): Completable = rxMediaPlayer.stop().subscribeOn(scheduler)

  override fun seekTo(position: Long): Completable = rxMediaPlayer.seekTo(position).subscribeOn(scheduler)

  override fun changeRandomState(randomized: Boolean): Completable =
      Completable.fromAction { this@RxMediaServiceImpl.randomized = randomized }
          .mergeWith(dispatchObservables(randomized))
          .mergeWith(shuffleQueue(randomized))
          .subscribeOn(scheduler)

  override fun changeRepeatState(repeatMode: RepeatState): Completable =
      rxMediaPlayer.currentState()
          .doOnError { println(it) }
          .doOnSubscribe { this@RxMediaServiceImpl.repeatState = repeatMode }
          .flatMapCompletable { mediaServiceState ->
            Single.fromCallable { mediaServiceState.setRepeatModeState(repeatState) }
                .doOnSuccess { stateDispatcher.onNext(it) }
                .toCompletable()
          }
          .onErrorComplete()
          .subscribeOn(scheduler)

  override fun goTo(mediaItem: MediaItem) =
      maybeGoTo(mediaItem)
          .toObservable()
          .zipWith(nowPlaying().toObservable(),
              BiFunction { media: MediaItem, nowPlaying: Optional<MediaItem> ->
                Pair(media, nowPlaying.isPresent() && nowPlaying.get() == media)
              })
          .flatMapCompletable { newMedia ->
            if (newMedia.second)
              rxMediaPlayer.play()
            else
              stop().andThen(rxMediaPlayer.play(newMedia.first))
          }


  override fun setVolume(volume: Float): Completable = rxMediaPlayer.setVolume(volume).subscribeOn(scheduler)

  override fun  stateChanges(): Observable<MediaServiceState> =
      Observable.merge(rxMediaPlayer.stateChanges(), stateDispatcher)
          .map { it.setRandomizedState(randomized) }
          .map { it.setRepeatModeState(repeatState) }

  override fun release(): Completable = rxMediaPlayer.release().subscribeOn(scheduler)

  override fun queueChanges(): Observable<List<MediaItem>> = queueDispatcher.subscribeOn(scheduler)

  override fun setCookies(cookies: List<HttpCookie>): Single<Unit> =
      rxMediaPlayer.setCookies(cookies).subscribeOn(scheduler)

  private fun prepareMedia(mediaItem: MediaItem) =
      stop()
          .subscribeOn(scheduler)
          .andThen(Completable.fromAction {
            if (queue.isEmpty()) queue.addLast(mediaItem)
            if (queue.contains(mediaItem).not()) queue.addFirst(mediaItem)
          })
          .andThen(Single.fromCallable { mediaItem })
          .flatMapCompletable { rxMediaPlayer.prepareMedia(it) }

  private fun shuffleQueue(randomized: Boolean) =
      rxMediaPlayer.currentState()
          .doOnError { println(it) }
          .doOnSubscribe {
            if (randomized) {
              originalQueue = queue.clone() as LinkedList<MediaItem>
              queue.shuffle()
            } else queue = originalQueue.clone() as LinkedList<MediaItem>
          }
          .flatMapCompletable { mediaServiceState ->
            Single.fromCallable { mediaServiceState.setRandomizedState(randomized) }
                .doOnSuccess {
                  val currentIndex = queue.indexOf(it.item)
                  if (currentIndex != 0) {
                    reorder(0, currentIndex).onErrorComplete().blockingGet()
                  }
                }
                .toCompletable()
          }
          .onErrorComplete()
          .subscribeOn(scheduler)

  private fun dispatchObservables(randomized: Boolean) =
      rxMediaPlayer.currentState()
          .flatMapCompletable { mediaServiceState ->
            Single.fromCallable { mediaServiceState.setRandomizedState(randomized) }
                .doOnSuccess {
                  stateDispatcher.onNext(it)
                  queueDispatcher.onNext(queue)
                }
                .toCompletable()
          }
          .onErrorComplete()
          .subscribeOn(scheduler)

  private fun handleCompletedState(it: MediaServiceState): CompletableSource? {
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

  private fun playFirst() =
      maybeFirst()
          .flatMapCompletable { stop().andThen(rxMediaPlayer.play(it)) }
          .subscribeOn(scheduler)

  private fun maybeGoTo(mediaItem: MediaItem) = Maybe.create<MediaItem> {
    val mediaIndex = queue.indexOf(mediaItem)
    if (mediaIndex < 0) it.onComplete()
    else {
      it.onSuccess(queue[mediaIndex])
      it.onComplete()
    }
  }

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
      maybeMediaItemBasedOnElement { current ->
        queue.getMappingReferenceIndex(referenceItem = current, mapReferenceIndex = { it + 1 })
      }

  private fun maybePrevious() =
      maybeMediaItemBasedOnElement { current ->
        queue.getMappingReferenceIndex(referenceItem = current, mapReferenceIndex = { it - 1 })
      }


  private fun maybeMediaItemBasedOnElement(mapElement: (MediaItem) -> MediaItem?): Maybe<MediaItem> {
    return nowPlaying()
        .filter { it.isPresent() }
        .flatMap { playingItem ->
          if (queue.isEmpty()) {
            return@flatMap Maybe.empty<MediaItem>()
          }

          val mapped = mapElement(playingItem.get())

          if (mapped == null) Maybe.empty<MediaItem>() else Maybe.just(mapped)
        }
        .onErrorComplete()
  }
}


package br.com.suamusica.rxmediaplayer.service

import br.com.suamusica.rxmediaplayer.domain.MediaItem
import br.com.suamusica.rxmediaplayer.domain.MediaProgress
import br.com.suamusica.rxmediaplayer.domain.Status
import br.com.suamusica.rxmediaplayer.extensions.add
import br.com.suamusica.rxmediaplayer.infra.MediaPlayer
import io.reactivex.*
import io.reactivex.Observable
import java.util.*
import java.util.concurrent.SynchronousQueue

class RxMediaServiceImpl(
  private val mediaPlayer: MediaPlayer,
  private val scheduler: Scheduler
) : RxMediaService {

  val queue: Queue<Pair<MediaItem, Status>> = SynchronousQueue()

  override fun add(mediaItem: MediaItem): Completable =
    Completable.fromCallable { queue.add(mediaItem, Status.IDLE) }
      .subscribeOn(scheduler)

  override fun remove(mediaItem: MediaItem): Completable {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun removeAll(): Completable {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun play(): Maybe<MediaItem> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun next(): Maybe<MediaItem> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun previous(): Maybe<MediaItem> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun pause(): Completable {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun stop(): Completable {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun nowPlaying(): Observable<Triple<MediaItem, Status, MediaProgress>> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun queueUpdated(): Flowable<Unit> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}

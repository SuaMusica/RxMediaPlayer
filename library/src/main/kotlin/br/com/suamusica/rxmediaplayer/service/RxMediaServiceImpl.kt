package br.com.suamusica.rxmediaplayer.service

import br.com.suamusica.rxmediaplayer.domain.MediaItem
import br.com.suamusica.rxmediaplayer.domain.Status
import br.com.suamusica.rxmediaplayer.infra.MediaPlayer
import io.reactivex.Observable
import java.util.*
import java.util.concurrent.SynchronousQueue

abstract class RxMediaServiceImpl(
  private val mediaPlayer: MediaPlayer
) : RxMediaService {

  val queue: Queue<Pair<MediaItem, Status>> = SynchronousQueue()

  override fun add(mediaItem: MediaItem) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun remove(mediaItem: MediaItem) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun removeAll() {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun play() {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun pause() {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun next() {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun previous() {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun stop() {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun nowPlaying(): Observable<Pair<MediaItem, Status>> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}

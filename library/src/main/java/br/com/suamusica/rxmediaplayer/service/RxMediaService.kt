package br.com.suamusica.rxmediaplayer.service

import br.com.suamusica.rxmediaplayer.domain.MediaItem
import br.com.suamusica.rxmediaplayer.domain.Status
import io.reactivex.Observable

interface RxMediaService {
  // manage queue
  fun add(mediaItem: MediaItem)
  fun remove(mediaItem: MediaItem)
  fun removeAll()

  // manage playing state
  fun play()
  fun pause()
  fun next()
  fun previous()
  fun stop()

  // event streams
  fun nowPlaying(): Observable<Pair<MediaItem, Status>>

}

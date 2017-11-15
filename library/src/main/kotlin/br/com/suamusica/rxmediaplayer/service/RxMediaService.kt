package br.com.suamusica.rxmediaplayer.service

import br.com.suamusica.rxmediaplayer.domain.MediaItem
import br.com.suamusica.rxmediaplayer.domain.MediaProgress
import br.com.suamusica.rxmediaplayer.domain.Status
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable

interface RxMediaService {
  // manage queue
  fun add(mediaItem: MediaItem) : Completable

  fun remove(mediaItem: MediaItem) : Completable
  fun removeAll() : Completable

  // manage playing state
  fun play(): Maybe<MediaItem>
  fun next(): Maybe<MediaItem>
  fun previous(): Maybe<MediaItem>

  fun pause(): Completable
  fun stop(): Completable

  // event streams
  fun nowPlaying(): Observable<Triple<MediaItem, Status, MediaProgress>>
}

package br.com.suamusica.rxmediaplayer.infra

import br.com.suamusica.rxmediaplayer.domain.MediaItem
import br.com.suamusica.rxmediaplayer.domain.MediaProgress
import br.com.suamusica.rxmediaplayer.domain.Status
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable


interface MediaPlayer {
  fun play(mediaItem: MediaItem): Completable
  fun pause(): Completable
  fun stop(): Completable

  fun nowPlaying(): Maybe<MediaItem>

  fun status(): Observable<Triple<MediaItem, Status, MediaProgress>>
}

package br.com.suamusica.rxmediaplayer.infra

import br.com.suamusica.rxmediaplayer.domain.MediaItem
import br.com.suamusica.rxmediaplayer.domain.MediaProgress
import io.reactivex.Completable
import io.reactivex.Observable


interface MediaPlayer {
  fun play(mediaItem: MediaItem): Observable<MediaProgress>
  fun pause(): Completable
  fun stop(): Completable
}

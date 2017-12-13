package br.com.suamusica.rxmediaplayer.domain

import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable


interface RxMediaPlayer {
  fun play(mediaItem: MediaItem): Completable
  fun pause(): Completable
  fun stop(): Completable

  fun nowPlaying(): Maybe<MediaItem>

  fun stateChanges(): Observable<MediaPlayerState>
  fun isPlaying(mediaItem: MediaItem): Observable<Boolean>?
}

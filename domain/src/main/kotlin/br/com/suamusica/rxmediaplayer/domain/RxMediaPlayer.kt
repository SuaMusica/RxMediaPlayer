package br.com.suamusica.rxmediaplayer.domain

import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import java.net.HttpCookie

interface RxMediaPlayer {
  fun play(): Completable
  fun play(mediaItem: MediaItem): Completable
  fun prepareMedia(currentItem: MediaItem): Completable
  fun pause(): Completable
  fun stop(): Completable
  fun seekTo(position: Long): Completable
  fun setVolume(volume: Float): Completable
  fun release(): Completable

  fun nowPlaying(): Single<Optional<MediaItem>>
  fun currentState(): Single<MediaServiceState>

  fun stateChanges(): Observable<MediaServiceState>

  fun isPlaying(): Single<Boolean>

  fun isPaused(): Single<Boolean>

  fun setCookies(list: List<HttpCookie>): Single<Unit>
}

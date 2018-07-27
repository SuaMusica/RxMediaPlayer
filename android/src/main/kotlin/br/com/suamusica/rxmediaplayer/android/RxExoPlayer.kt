package br.com.suamusica.rxmediaplayer.android

import android.content.Context
import br.com.suamusica.rxmediaplayer.domain.MediaItem
import br.com.suamusica.rxmediaplayer.domain.MediaServiceState
import br.com.suamusica.rxmediaplayer.domain.RxMediaPlayer
import com.google.android.exoplayer2.ExoPlayer
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposables
import io.reactivex.subjects.BehaviorSubject


class RxExoPlayer (
    private val context: Context,
    private val resolveDataSourceForMediaItem: (MediaItem) -> String = { it.url }
) : RxMediaPlayer {

  lateinit var exoPlayer: ExoPlayer
  private val stateDispatcher = BehaviorSubject.create<MediaServiceState>()
  private var progressDisposable = Disposables.disposed()
  private var currentMediaItem: MediaItem? = null
  private var state: MediaPlayerState = MediaPlayerState.IDLE

  init {
    initializeExoPlayer(context)
  }

  override fun play(): Completable {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun play(mediaItem: MediaItem): Completable {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun pause(): Completable {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun stop(): Completable {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun release(): Completable {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun nowPlaying(): Maybe<MediaItem> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun stateChanges(): Observable<MediaServiceState> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun isPlaying(): Single<Boolean> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun isPaused(): Single<Boolean> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  private fun initializeExoPlayer(context: Context) {

  }

}

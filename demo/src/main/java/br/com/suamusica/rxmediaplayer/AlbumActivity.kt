package br.com.suamusica.rxmediaplayer

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.widget.Toast
import br.com.suamusica.rxmediaplayer.android.RxMediaServiceActivity
import br.com.suamusica.rxmediaplayer.domain.RxMediaService
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_demo.item_miniplayer_view
import kotlinx.android.synthetic.main.activity_demo.recyclerViewMediaItems

class AlbumActivity : RxMediaServiceActivity() {

  private val compositeDisposable = CompositeDisposable()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_demo)

    recyclerViewMediaItems.layoutManager = LinearLayoutManager(this)
    val mediaItemAdapter = MediaItemAdapter(MediaRepository.musics())

    observeAdapterEvents(mediaItemAdapter)

    recyclerViewMediaItems.adapter = mediaItemAdapter

    bindMiniPlayer()
  }

  private fun bindMiniPlayer() {
    item_miniplayer_view.onClickPlay = {
      rxMediaService()
          .flatMapCompletable { it.play() }
          .observeOn(AndroidSchedulers.mainThread())
          .doOnError { showError(it) }
          .subscribe()
    }

    item_miniplayer_view.onClickPause = {
      rxMediaService()
          .flatMapCompletable { it.pause() }
          .observeOn(AndroidSchedulers.mainThread())
          .doOnError { showError(it) }
          .subscribe()
    }

    item_miniplayer_view.onClickNext = {
      rxMediaService()
          .flatMapCompletable { it.next() }
          .observeOn(AndroidSchedulers.mainThread())
          .doOnError { showError(it) }
          .subscribe()
    }

    item_miniplayer_view.onClickPrev = {
      rxMediaService()
          .flatMapCompletable { it.previous() }
          .observeOn(AndroidSchedulers.mainThread())
          .doOnError { showError(it) }
          .subscribe()
    }
  }

  private fun observeAdapterEvents(mediaItemAdapter: MediaItemAdapter) {
    mediaItemAdapter.itemClicks()
        .flatMapMaybe { it.withMediaService() }
        .flatMapCompletable { (item, service) -> service.play(item) }
        .observeOn(AndroidSchedulers.mainThread())
        .doOnError { showError(it) }
        .retry()
        .subscribe()
        .compose()

    mediaItemAdapter.addClicks()
        .flatMapMaybe { it.withMediaService() }
        .flatMapCompletable { (item, service) -> service.add(item) }
        .retry()
        .subscribe()
        .compose()
  }

  private fun showError(throwable: Throwable) {
    Log.e("DemoApp", throwable.message, throwable)
    Toast.makeText(this, throwable.message, Toast.LENGTH_LONG).show()
  }

  override fun onRxMediaServiceBound(rxMediaService: RxMediaService) {
    rxMediaService.stateChanges()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe {
          Toast.makeText(this, it.toString(), Toast.LENGTH_LONG).show()
        }
  }

  private fun <T> T.withMediaService(): Maybe<Pair<T, RxMediaService>> {
    return rxMediaService().map { this to it }
  }

  private fun Disposable.compose() = compositeDisposable.add(this)
}
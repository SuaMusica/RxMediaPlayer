package br.com.suamusica.rxmediaplayer.android

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import br.com.suamusica.rxmediaplayer.domain.RxMediaService
import io.reactivex.schedulers.Schedulers


class RxAndroidMediaService : Service() {
  private val rxMediaPlayer = RxAndroidMediaPlayer(this)
  private val rxMediaService = RxMediaService.create(rxMediaPlayer, Schedulers.computation())
  private val binder = LocalBinder()

  override fun onBind(p0: Intent?): IBinder = binder

  inner class LocalBinder : Binder() {
    fun rxMediaService(): RxMediaService = rxMediaService
  }
}
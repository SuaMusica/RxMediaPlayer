package br.com.suamusica.rxmediaplayer.android

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import br.com.suamusica.rxmediaplayer.domain.RxMediaPlayer
import br.com.suamusica.rxmediaplayer.domain.RxMediaService
import io.reactivex.schedulers.Schedulers


class RxAndroidMediaService : Service() {

  private lateinit var rxMediaPlayer: RxMediaPlayer
  private lateinit var rxMediaService: RxMediaService

  private val binder = LocalBinder()

  override fun onBind(p0: Intent?): IBinder = binder

  override fun onCreate() {
    super.onCreate()

    rxMediaPlayer = RxAndroidMediaPlayer(this)
    rxMediaService = RxMediaService.create(rxMediaPlayer, Schedulers.computation())
  }

  inner class LocalBinder : Binder() {
    fun rxMediaService(): RxMediaService = rxMediaService
  }
}
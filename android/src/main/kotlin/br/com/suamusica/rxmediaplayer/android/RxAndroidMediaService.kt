package br.com.suamusica.rxmediaplayer.android

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import br.com.suamusica.rxmediaplayer.domain.RxMediaService
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers


class RxAndroidMediaService : Service() {
  private val rxMediaPlayer = RxAndroidMediaPlayer(this)
  private val rxMediaService = RxMediaService.create(rxMediaPlayer, Schedulers.computation())
  private val binder = LocalBinder()

  override fun onBind(p0: Intent?): IBinder = binder

  inner class LocalBinder : Binder() {
    fun rxMediaService(): RxMediaService = rxMediaService
  }

  companion object {

    fun bind(context: Context): Observable<LocalBinder> {
      return Observable.create { emitter ->
        val intent = Intent(context, RxAndroidMediaService::class.java)
        val connection = object : ServiceConnection {
          override fun onServiceDisconnected(p0: ComponentName?) {
            if (emitter.isDisposed.not()) {
              emitter.onComplete()
            }
          }

          override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
          }

        }
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
      }
    }
  }
}
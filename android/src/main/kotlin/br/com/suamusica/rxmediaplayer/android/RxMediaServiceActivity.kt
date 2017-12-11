package br.com.suamusica.rxmediaplayer.android

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import br.com.suamusica.rxmediaplayer.domain.RxMediaService
import io.reactivex.Maybe

abstract class RxMediaServiceActivity : AppCompatActivity() {
  private var rxMediaService: RxMediaService? = null

  private val connection: ServiceConnection = object : ServiceConnection {
    override fun onServiceDisconnected(componentName: ComponentName?) {
      rxMediaService = null
    }

    override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
      when (binder) {
        is RxAndroidMediaService.LocalBinder -> rxMediaService = binder.rxMediaService()
      }
    }
  }

  override fun onStart() {
    super.onStart()

    val serviceIntent = Intent(this, RxAndroidMediaService::class.java)
    bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
  }

  override fun onStop() {
    super.onStop()
    unbindService(connection)
  }

  fun rxMediaService() : Maybe<RxMediaService> = Maybe.create { emitter ->
    rxMediaService?.let { emitter.onSuccess(it) } ?: emitter.onComplete()
  }
}

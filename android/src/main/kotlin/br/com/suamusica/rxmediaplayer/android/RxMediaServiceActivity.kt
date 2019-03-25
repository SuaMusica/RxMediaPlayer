package br.com.suamusica.rxmediaplayer.android

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import br.com.suamusica.rxmediaplayer.domain.RxMediaService
import io.reactivex.Maybe
import kotlin.properties.Delegates

abstract class RxMediaServiceActivity : AppCompatActivity() {
  private var rxMediaService by Delegates.observable<RxMediaService?>(null) { _,_, newValue ->
    newValue?.let { onRxMediaServiceBound(it) } ?: onRxMediaServiceUnbound()
  }

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

  open fun onRxMediaServiceBound(rxMediaService: RxMediaService) {
    // ignored
  }

  open fun onRxMediaServiceUnbound() {
    // ignored
  }

  fun rxMediaService() : Maybe<RxMediaService> = Maybe.create { emitter ->
    rxMediaService?.let { emitter.onSuccess(it) } ?: emitter.onComplete()
  }
}

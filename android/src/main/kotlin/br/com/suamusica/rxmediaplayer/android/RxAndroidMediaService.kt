package br.com.suamusica.rxmediaplayer.android

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
import android.media.AudioManager.AUDIOFOCUS_GAIN
import android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.media.AudioAttributesCompat
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import br.com.suamusica.rxmediaplayer.audio.AudioFocusRequestCompat
import br.com.suamusica.rxmediaplayer.domain.MediaBoundState
import br.com.suamusica.rxmediaplayer.domain.MediaServiceState
import br.com.suamusica.rxmediaplayer.domain.PlayingState
import br.com.suamusica.rxmediaplayer.domain.RxMediaPlayer
import br.com.suamusica.rxmediaplayer.domain.RxMediaService
import br.com.suamusica.rxmediaplayer.entity.NotificationState
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject


abstract class RxAndroidMediaService : Service() {
  private val telephonyManager by lazy { getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }
  private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }

  private val binder = LocalBinder()
  private var disposable = CompositeDisposable()
  private var notificationDisposable = CompositeDisposable()

  private lateinit var phoneStateListener: PhoneStateListener
  private lateinit var onAudioFocusChangeListener: AudioManager.OnAudioFocusChangeListener

  private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

  override fun onCreate() {
    super.onCreate()

    val rxMediaPlayer = createRxMediaPlayer()
    RxMediaService.create(rxMediaPlayer, Schedulers.computation()).also {
      rxMediaService = it
      phoneStateListener = RxMediaServiceSystemListeners.CustomPhoneStateListener(it)
      onAudioFocusChangeListener = RxMediaServiceSystemListeners.OnAudioFocusChangeListener(it)

      disposable.add(
          it.stateChanges()
              .filter { state ->  state is MediaBoundState }
              .distinctUntilChanged { m1, m2 ->
                val id1 = (m1 as MediaBoundState).item?.id
                val id2 = (m2 as MediaBoundState).item?.id

                return@distinctUntilChanged id1 == id2 && m1::class == m2::class
              }
              .observeOn(AndroidSchedulers.mainThread())
              .doOnNext { state -> createNotification(state) }
              .doAfterTerminate { removeNotification() }
              .doAfterTerminate { rxMediaService?.release() }
              .subscribe()
      )
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    return Service.START_STICKY
  }

  override fun onBind(p0: Intent?): IBinder = binder

  override fun onTaskRemoved(rootIntent: Intent?) {
    Log.d("RxMediaService", "onTaskRemoved()")
    super.onTaskRemoved(rootIntent)
    removeNotification()
    stopSelf()
  }

  override fun onDestroy() {
    super.onDestroy()
    disposable.clear()
    notificationDisposable.clear()
  }

  abstract fun createRxMediaPlayer(): RxMediaPlayer

  abstract fun createNotification(state: MediaServiceState)

  abstract fun observeNotification(): PublishSubject<NotificationState>

  protected open fun removeNotification() {
    stopForeground(true)
    notificationManager.cancel(NOTIFICATION_ID)
    telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
  }

  fun notify(notification: Notification) {
    notificationManager.notify(NOTIFICATION_ID, notification)
  }

  open fun showNotificationOnScreen(state: NotificationState) {
    notify(state.notification)

    if (state.mediaState is PlayingState)
      startForeground(NOTIFICATION_ID, state.notification)
    else
      stopForeground(false)
  }

  fun unsubscribeNotificationOnScreen() {
    notificationDisposable.clear()
  }

  fun subscribeNotificationOnScreen() {
    if (notificationDisposable.size() < 1 || notificationDisposable.isDisposed) {
      notificationDisposable.add(
          observeNotification()
              .observeOn(AndroidSchedulers.mainThread())
              .doOnNext { showNotificationOnScreen(it) }
              .subscribe()
      )
    }
  }

  companion object {
    const val NOTIFICATION_ID = 0x0a0c
    var rxMediaService: RxMediaService? = null
  }

  inner class LocalBinder : Binder() {
    fun rxMediaService(): RxMediaService? = rxMediaService
  }
}
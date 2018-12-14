package br.com.suamusica.rxmediaplayer.android

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
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
import br.com.suamusica.rxmediaplayer.domain.CompletedState
import br.com.suamusica.rxmediaplayer.domain.IdleState
import br.com.suamusica.rxmediaplayer.domain.MediaBoundState
import br.com.suamusica.rxmediaplayer.domain.MediaServiceState
import br.com.suamusica.rxmediaplayer.domain.RxMediaPlayer
import br.com.suamusica.rxmediaplayer.domain.RxMediaService
import br.com.suamusica.rxmediaplayer.domain.StoppedState
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject


abstract class RxAndroidMediaService : Service() {
  private val telephonyManager by lazy { getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }
  private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }

  private val binder = LocalBinder()
  private var disposable = CompositeDisposable()

  private lateinit var phoneStateListener: PhoneStateListener
  private lateinit var onAudioFocusChangeListener: AudioManager.OnAudioFocusChangeListener
  private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

  override fun onCreate() {
    super.onCreate()

    val rxMediaPlayer = createRxMediaPlayer()
    rxMediaService = RxMediaService.create(rxMediaPlayer, Schedulers.computation())
    phoneStateListener = RxMediaServiceSystemListeners.CustomPhoneStateListener(rxMediaService)
    onAudioFocusChangeListener = RxMediaServiceSystemListeners.OnAudioFocusChangeListener(rxMediaService)

    disposable.add(
        rxMediaService.stateChanges()
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext { notify(it) }
        .doAfterTerminate { removeNotification() }
        .doAfterTerminate { rxMediaService.release() }
        .subscribe()
    )

    disposable.add(
        observeNotification()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { bindNotification(it) }
            .doOnDispose { removeNotification() }
            .subscribe()
    )
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    return Service.START_STICKY
  }

  override fun onBind(p0: Intent?): IBinder = binder

  override fun onTaskRemoved(rootIntent: Intent?) {
    Log.d("RxMediaService", "onTaskRemoved()")
    super.onTaskRemoved(rootIntent)
    rxMediaService.stop().subscribe()
    removeNotification()
    stopSelf()
  }

  override fun onDestroy() {
    super.onDestroy()
    disposable.dispose()
  }

  abstract fun createRxMediaPlayer(): RxMediaPlayer

  abstract fun createNotification(state: MediaBoundState)

  abstract fun observeNotification(): PublishSubject<Notification>

  protected open fun removeNotification() {
    Log.d("RxMediaService", "removeNotification()")
    stopForeground(true)
    notificationManager.cancel(NOTIFICATION_ID)
    telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
  }

  private fun notify(state: MediaServiceState) {
    when (state) {
      is CompletedState, is IdleState -> {
        Log.d("RxMediaService", "removeNotification(state: ${state::class.java.simpleName})")
        removeNotification()
      }
      else -> {
        Log.d("RxMediaService", "showNotification(state: ${state::class.java.simpleName})")
        showNotification(state as MediaBoundState)
      }
    }
  }

  private fun showNotification(state: MediaBoundState) {
    Log.d("RxMediaService", "showNotification()")
    val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      requestAudioFocusForAndroidO()
    } else {
      requestAudioFocusPreAndroidO()
    }

    if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
      // Start playback
      createNotification(state)
    }
  }

  private fun bindNotification(notification: Notification) {
    notificationManager.notify(NOTIFICATION_ID, notification)
    showNotificationOnScreen(notification)
  }

  fun showNotificationOnScreen(notification: Notification) {
    notificationManager.notify(NOTIFICATION_ID, notification)
    startForeground(NOTIFICATION_ID, notification)
    telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
  }

  private fun requestAudioFocusPreAndroidO(): Int {
    return audioManager.requestAudioFocus(
        onAudioFocusChangeListener,
        AudioManager.STREAM_MUSIC,
        AUDIOFOCUS_GAIN.and(AUDIOFOCUS_GAIN_TRANSIENT).or(AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        )
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun requestAudioFocusForAndroidO(): Int {
    val audioFocusRequest = AudioFocusRequestCompat.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setFocusGain(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(
            AudioAttributesCompat.Builder()
                .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
                .setLegacyStreamType(AudioAttributesCompat.USAGE_MEDIA)
                .build())
        .setWillPauseWhenDucked(true)
        .setOnAudioFocusChangeListener(onAudioFocusChangeListener)
        .build()

    return audioManager.requestAudioFocus(audioFocusRequest.audioFocusRequest)
  }

  companion object {
    const val NOTIFICATION_ID = 0x0a0c
    lateinit var rxMediaService: RxMediaService
  }

  inner class LocalBinder : Binder() {
    fun rxMediaService(): RxMediaService = rxMediaService
  }
}
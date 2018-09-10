package br.com.suamusica.rxmediaplayer.android

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import br.com.suamusica.rxmediaplayer.domain.CompletedState
import br.com.suamusica.rxmediaplayer.domain.IdleState
import br.com.suamusica.rxmediaplayer.domain.MediaBoundState
import br.com.suamusica.rxmediaplayer.domain.MediaServiceState
import br.com.suamusica.rxmediaplayer.domain.PausedState
import br.com.suamusica.rxmediaplayer.domain.PlayingState
import br.com.suamusica.rxmediaplayer.domain.RxMediaPlayer
import br.com.suamusica.rxmediaplayer.domain.RxMediaService
import br.com.suamusica.rxmediaplayer.domain.StoppedState
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers


abstract class RxAndroidMediaService : Service() {
  private val telephonyManager by lazy { getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }
  private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }

  private val binder = LocalBinder()
  private var disposable = Disposables.disposed()

  private lateinit var phoneStateListener: PhoneStateListener
  private lateinit var onAudioFocusChangeListener: AudioManager.OnAudioFocusChangeListener
  private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

  override fun onCreate() {
    super.onCreate()

    val rxMediaPlayer = createRxMediaPlayer()
    rxMediaService = RxMediaService.create(rxMediaPlayer, Schedulers.computation())
    phoneStateListener = RxMediaServiceSystemListeners.CustomPhoneStateListener(rxMediaService)
    onAudioFocusChangeListener = RxMediaServiceSystemListeners.OnAudioFocusChangeListener(rxMediaService)

    disposable = rxMediaService.stateChanges()
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext { notify(it) }
        .doAfterTerminate { removeNotification() }
        .doAfterTerminate { rxMediaService.release() }
        .subscribe()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    return Service.START_STICKY
  }

  override fun onBind(p0: Intent?): IBinder = binder

  override fun onTaskRemoved(rootIntent: Intent?) {
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

  abstract fun createNotification(state: MediaBoundState): NotificationCompat.Builder

  protected open fun removeNotification() {
    stopForeground(true)
    notificationManager.cancel(NOTIFICATION_ID)
    telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
  }

  private fun notify(state: MediaServiceState) {
    when (state) {
      is CompletedState, is IdleState -> removeNotification()
      else -> showNotification(state as MediaBoundState)
    }
  }

  private fun showNotification(state: MediaBoundState) {
    val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      requestAudioFocusForAndroidO()
    } else {
      requestAudioFocusPreAndroidO()
    }

    if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
      // Start playback
      val notificationBuilder = createNotification(state)

      Glide.with(this)
          .asBitmap()
          .load(state .item?.coverUrl)
          .into(object : SimpleTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?){
              notificationBuilder.setLargeIcon(resource)
              showNotificationOnScreen(notificationBuilder.build())
            }
          })
    }
  }

  fun showNotificationOnScreen(notification: Notification) {
    notificationManager.notify(NOTIFICATION_ID, notification)
    startForeground(NOTIFICATION_ID, notification)
    telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
  }

  private fun requestAudioFocusPreAndroidO(): Int {
    return audioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun requestAudioFocusForAndroidO(): Int {
    val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attrs)
        .setAcceptsDelayedFocusGain(true)
        .setOnAudioFocusChangeListener(onAudioFocusChangeListener)
        .build()

    return audioManager.requestAudioFocus(audioFocusRequest)
  }

  companion object {
    const val NOTIFICATION_ID = 1342134
    lateinit var rxMediaService: RxMediaService
  }

  inner class LocalBinder : Binder() {
    fun rxMediaService(): RxMediaService = rxMediaService
  }
}
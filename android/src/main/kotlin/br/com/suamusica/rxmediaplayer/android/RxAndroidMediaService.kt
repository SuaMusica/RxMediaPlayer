package br.com.suamusica.rxmediaplayer.android

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.content.ContextCompat
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import br.com.suamusica.rxmediaplayer.domain.MediaBoundState
import br.com.suamusica.rxmediaplayer.domain.MediaServiceState
import br.com.suamusica.rxmediaplayer.domain.PausedState
import br.com.suamusica.rxmediaplayer.domain.PlayingState
import br.com.suamusica.rxmediaplayer.domain.RxMediaPlayer
import br.com.suamusica.rxmediaplayer.domain.RxMediaService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers


abstract class RxAndroidMediaService : Service() {

  protected lateinit var rxMediaService: RxMediaService

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

  abstract fun createNotification(state: MediaBoundState): Notification

  protected open fun removeNotification() {
    stopForeground(true)
    notificationManager.cancel(NOTIFICATION_ID)
    telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
  }

  private fun notify(state: MediaServiceState) {
    when (state) {
      is PlayingState, is PausedState -> showNotification(state as MediaBoundState)
      else -> removeNotification()
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
      val notification = createNotification(state)
      notificationManager.notify(NOTIFICATION_ID, notification)
      startForeground(NOTIFICATION_ID, notification)
      telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)

      //      mediaSession = MediaSessionCompat(this, "SuaMusicaRemoteControl")
      //      mediaSession!!.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
      //      mediaSession!!.setCallback(mediaSessionCallback)
      //      mediaSession!!.setActive(true)
      //      mediaSession!!.setMetadata(
      //          MediaMetadataCompat.Builder().putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.name())
      //              .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, song.name())
      //              .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, song.artistName())
      //              .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, song.name())
      //              .putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, song.artistName())
      //              .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artistName())
      //              .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.albumName())
      //              .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, player.getDuration().toLong())
      //              .build())
      //
      //      val state = PlaybackStateCompat.Builder().setActions(
      //          PlaybackStateCompat.ACTION_PLAY or
      //              PlaybackStateCompat.ACTION_PLAY_PAUSE or
      //              PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
      //              PlaybackStateCompat.ACTION_PAUSE or
      //              PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
      //              PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
      //              PlaybackStateCompat.ACTION_STOP)
      //          .setState(
      //              PlaybackStateCompat.STATE_PLAYING,
      //              0, 1.0f,
      //              SystemClock.elapsedRealtime())
      //          .build()
      //
      //      mediaSession!!.setPlaybackState(state)
    }
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
    val NOTIFICATION_ID = 1342134
  }

  inner class LocalBinder : Binder() {
    fun rxMediaService(): RxMediaService = rxMediaService
  }
}
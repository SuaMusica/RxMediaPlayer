package br.com.suamusica.rxmediaplayer.android

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import br.com.suamusica.rxmediaplayer.domain.CompletedState
import br.com.suamusica.rxmediaplayer.domain.IdleState
import br.com.suamusica.rxmediaplayer.domain.MediaBoundState
import br.com.suamusica.rxmediaplayer.domain.MediaItem
import br.com.suamusica.rxmediaplayer.domain.MediaServiceState
import br.com.suamusica.rxmediaplayer.domain.RxMediaPlayer
import br.com.suamusica.rxmediaplayer.domain.RxMediaService
import br.com.suamusica.rxmediaplayer.extensions.toMediaDescription
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers


abstract class RxAndroidMediaService : MediaBrowserServiceCompat() {
  private lateinit var mediaSession: MediaSessionCompat
  private lateinit var mediaController: MediaControllerCompat
  private lateinit var becomingNoisyReceiver: BecomingNoisyReceiver

  private var isForegroundService = false
  private var disposable = Disposables.disposed()

  private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

  override fun onCreate() {
    super.onCreate()

    initMediaSession()
    initMediaController()
    initNoisyReceiver()
    initMediaPlayer()

    disposable = rxMediaService.queueChanges()
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext { notifyQueue(it) }
        .doAfterTerminate { removeNotification() }
        .doAfterTerminate { rxMediaService.release() }
        .subscribe()

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

  override fun onTaskRemoved(rootIntent: Intent?) {
    super.onTaskRemoved(rootIntent)
    rxMediaService.stop(true).subscribe()
    removeNotification()
    stopSelf()
  }

  override fun onDestroy() {
    mediaSession.run {
      isActive = false
      release()
    }

    disposable.dispose()
  }

  abstract fun createRxMediaPlayer(): RxMediaPlayer

  abstract fun createNotification(state: MediaBoundState): NotificationCompat.Builder

  abstract fun buildNotification(token: MediaSessionCompat.Token): NotificationCompat.Builder

  protected open fun removeNotification() {
    stopForeground(true)
  }

  private fun notifyQueue(queue: List<MediaItem>) {
    val newQueue = queue.map { it.toMediaDescription() }
    val oldQueue = mediaController.queue.map { it.queueItem as MediaDescriptionCompat }
    val diffQueue = oldQueue.minus(newQueue)

    diffQueue.forEach { mediaController.removeQueueItem(it) }

    newQueue.forEach { media ->
      if (oldQueue.contains(media).not())
        mediaController.addQueueItem(media)
    }
  }

  private fun notify(state: MediaServiceState) {
    when (state) {
      is CompletedState, is IdleState -> removeNotification()
      else -> showNotification(state as MediaBoundState)
    }
  }

  private fun showNotification(state: MediaBoundState) {
//    state.progress?.let { progress ->
//      val current = mediaController.playbackState
//
//      mediaController.playbackInfo.
//    }


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

  fun showNotificationOnScreen(notification: Notification) {
    notificationManager.notify(NOTIFICATION_ID, notification)
    startForeground(NOTIFICATION_ID, notification)
  }

  private fun initMediaPlayer() {
    val rxMediaPlayer = createRxMediaPlayer()
    rxMediaService = RxMediaService.create(rxMediaPlayer, Schedulers.computation())
  }

  private fun initNoisyReceiver() {
    becomingNoisyReceiver = BecomingNoisyReceiver(context = this, sessionToken = mediaSession.sessionToken)
  }

  private fun initMediaController() {
    mediaController = MediaControllerCompat(this, mediaSession).also {
      it.registerCallback(MediaControllerCallback())
    }
  }

  private fun initMediaSession() {
    // Build a PendingIntent that can be used to launch the UI.
    val sessionIntent = packageManager?.getLaunchIntentForPackage(packageName)
    val sessionActivityPendingIntent = PendingIntent.getActivity(this, 0, sessionIntent, 0)

    // Create a new MediaSession.
    mediaSession = MediaSessionCompat(this, "MusicService")
        .apply {
          setSessionActivity(sessionActivityPendingIntent)
          setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
          isActive = true
        }

    sessionToken = mediaSession.sessionToken
  }


  private inner class MediaControllerCallback : MediaControllerCompat.Callback() {
    override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
      mediaController.playbackState?.let { updateNotification(it) }
    }

    override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
      state?.let { updateNotification(it) }
    }

    private fun updateNotification(state: PlaybackStateCompat) {
      val updatedState = state.state
      if (mediaController.metadata == null) {
        return
      }

      // Skip building a notification when state is "none".
      val notification: Notification? = if (updatedState != PlaybackStateCompat.STATE_NONE) {
        buildNotification(mediaSession.sessionToken).build()
      } else {
        null
      }

      when (updatedState) {
        PlaybackStateCompat.STATE_BUFFERING,
        PlaybackStateCompat.STATE_PLAYING -> {
          becomingNoisyReceiver.register()

          /**
           * This may look strange, but the documentation for [Service.startForeground]
           * notes that "calling this method does *not* put the service in the started
           * state itself, even though the name sounds like it."
           */
          if (!isForegroundService) {
            startService(Intent(applicationContext, this@RxAndroidMediaService.javaClass))
            startForeground(NOW_PLAYING_NOTIFICATION, notification)
            isForegroundService = true
          } else if (notification != null) {
            notificationManager.notify(NOW_PLAYING_NOTIFICATION, notification)
          }
        }
        else -> {
          becomingNoisyReceiver.unregister()

          if (isForegroundService) {
            stopForeground(false)
            isForegroundService = false

            // If playback has ended, also stop the service.
            if (updatedState == PlaybackStateCompat.STATE_NONE) {
              stopSelf()
            }

            if (notification != null) {
              notificationManager.notify(NOW_PLAYING_NOTIFICATION, notification)
            } else {
              removeNotification()
            }
          }
        }
      }
    }
  }

  companion object {
    const val NOW_PLAYING_NOTIFICATION: Int = 0xb339
    const val NOTIFICATION_ID: Int = 0xb339
    lateinit var rxMediaService: RxMediaService
  }
}

/**
 * Helper class for listening for when headphones are unplugged (or the audio
 * will otherwise cause playback to become "noisy").
 */
private class BecomingNoisyReceiver(private val context: Context,
    sessionToken: MediaSessionCompat.Token)
  : BroadcastReceiver() {

  private val noisyIntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
  private val controller = MediaControllerCompat(context, sessionToken)

  private var registered = false

  fun register() {
    if (!registered) {
      context.registerReceiver(this, noisyIntentFilter)
      registered = true
    }
  }

  fun unregister() {
    if (registered) {
      context.unregisterReceiver(this)
      registered = false
    }
  }

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
      controller.transportControls.pause()
    }
  }
}
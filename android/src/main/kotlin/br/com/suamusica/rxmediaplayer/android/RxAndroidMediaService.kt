package br.com.suamusica.rxmediaplayer.android

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Binder
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import br.com.suamusica.rxmediaplayer.domain.MediaBoundState
import br.com.suamusica.rxmediaplayer.domain.MediaServiceState
import br.com.suamusica.rxmediaplayer.domain.PlayingState
import br.com.suamusica.rxmediaplayer.domain.RxMediaPlayer
import br.com.suamusica.rxmediaplayer.domain.RxMediaService
import br.com.suamusica.rxmediaplayer.entity.NotificationState
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject


abstract class RxAndroidMediaService : Service() {
  private val telephonyManager by lazy { getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }
  private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }

  private val binder = LocalBinder()
  private var disposable = CompositeDisposable()
  private var notificationDisposable = CompositeDisposable()

  private var phoneStateListener: PhoneStateListener? = null
  private var onAudioFocusChangeListener: AudioManager.OnAudioFocusChangeListener? = null

  private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

  override fun onCreate() {
    super.onCreate()
    Log.d("RxMediaService", "onCreate()")

    disposable.add(
        Single.fromCallable {
          Log.d("RxMediaService", "createRxMediaPlayer()")
          createRxMediaPlayer(playerThread())
        }
            .subscribeOn(playerThread())
            .doOnSuccess { rxMediaPlayer ->
              Log.d("RxMediaService", "doOnSuccess()")
              RxMediaService.create(rxMediaPlayer, playerThread()).also {
                Log.d("RxMediaService", "RxMediaService.create()")
                rxMediaService = it
                phoneStateListener = RxMediaServiceSystemListeners.CustomPhoneStateListener(it)
                onAudioFocusChangeListener = RxMediaServiceSystemListeners.OnAudioFocusChangeListener(it)

                disposable.add(
                    it.stateChanges()
                        .filter { state ->  state is MediaBoundState }
                        .distinctUntilChanged { m1, m2 ->
                          Log.d("RxMediaService", "distinctUntilChanged")
                          val id1 = (m1 as MediaBoundState).item?.id
                          val id2 = (m2 as MediaBoundState).item?.id

                          return@distinctUntilChanged id1 == id2 && m1::class == m2::class
                        }
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext { state -> createNotification(state) }
                        .doAfterTerminate {
                          Log.d("RxMediaService", "doAfterTerminate.removeNotification()")
                          removeNotification()
                        }
                        .doAfterTerminate {
                          Log.d("RxMediaService", "doAfterTerminate.rxMediaService?.release()")
                          rxMediaService?.release()
                        }
                        .subscribe()
                )
              }
            }
            .doOnError { Log.e("RxMediaService", it.message, it) }
            .toCompletable()
            .onErrorComplete()
            .subscribe()
    )
  }

  override fun onRebind(intent: Intent?) {
    Log.d("RxMediaService", "onRebind()")
    super.onRebind(intent)
  }

  override fun onStart(intent: Intent?, startId: Int) {
    Log.d("RxMediaService", "onStart()")
    super.onStart(intent, startId)
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
    Log.d("RxMediaService", "onDestroy()")
    super.onDestroy()
    disposable.clear()
    notificationDisposable.clear()
  }

  abstract fun createRxMediaPlayer(scheduler: Scheduler? = null): RxMediaPlayer

  abstract fun createNotification(state: MediaServiceState)

  abstract fun observeNotification(): PublishSubject<NotificationState>

  protected open fun playerThread(): Scheduler = AndroidSchedulers.mainThread()

  protected open fun removeNotification() {
    Log.d("RxMediaService", "removeNotification()")
    stopForeground(true)
    notificationManager.cancel(NOTIFICATION_ID)
    phoneStateListener?.let { telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE) }
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
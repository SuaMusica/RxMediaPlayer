package br.com.suamusica.rxmediaplayer.android

import android.media.AudioManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import br.com.suamusica.rxmediaplayer.domain.RxMediaService

object RxMediaServiceSystemListeners {
  class OnAudioFocusChangeListener(
      private val rxMediaService: RxMediaService
  ) : AudioManager.OnAudioFocusChangeListener {
    private val MEDIA_VOLUME_DEFAULT = 1.0f
    private val MEDIA_VOLUME_DUCK = 0.2f

    private var wasPlaying: Boolean = false
    private var lossType: Int = 0

    override fun onAudioFocusChange(focusChange: Int) {
      Log.d("OnAudioFocusChange", "onAudioFocusChange: $focusChange")

      if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
          focusChange == AudioManager.AUDIOFOCUS_LOSS) {
        Log.d("OnAudioFocusChange", "AUDIOFOCUS_LOSS+")
        // Pause playback
        wasPlaying = rxMediaService.isPlaying().blockingGet()
        lossType = focusChange

        if (wasPlaying) {
          Log.d("OnAudioFocusChange", "pause")
          rxMediaService.pause().blockingAwait()
        }
      }
      else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
        Log.d("OnAudioFocusChange", "MEDIA_VOLUME_DUCK")
        rxMediaService.setVolume(MEDIA_VOLUME_DUCK).blockingAwait()
      }
      else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
        Log.d("OnAudioFocusChange", "AUDIOFOCUS_GAIN - lossType: $lossType")
        // Resume playback
        if (wasPlaying && rxMediaService.isPaused().blockingGet() && lossType != AudioManager.AUDIOFOCUS_LOSS) {
          Log.d("OnAudioFocusChange", "play")
          rxMediaService.play()
              .doOnError { Log.e("OnAudioFocusChange", it.message, it) }
              .onErrorComplete()
              .blockingAwait()
        }

        rxMediaService.apply {
          Log.d("OnAudioFocusChange", "MEDIA_VOLUME_DEFAULT")
          if (isPlaying().blockingGet()) setVolume(MEDIA_VOLUME_DEFAULT).blockingAwait()
        }
      }
    }
  }

  class CustomPhoneStateListener(
      private val rxMediaService: RxMediaService
  ) : PhoneStateListener() {

    private var wasPlaying = false

    override fun onCallStateChanged(state: Int, incomingNumber: String) {
      if (state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK) {
        //Incoming call: Pause music
        wasPlaying = rxMediaService.isPlaying().blockingGet() || wasPlaying

        if (wasPlaying) {
          rxMediaService.pause().blockingAwait()
        }
      } else if (state == TelephonyManager.CALL_STATE_IDLE) {
        //Not in call: Play music
        if (wasPlaying && rxMediaService.isPaused().blockingGet()) {
          wasPlaying = false
          rxMediaService.play()
              .doOnError { Log.e("onCallStateChanged", it.message, it) }
              .onErrorComplete()
              .blockingAwait()
        }
      }
      super.onCallStateChanged(state, incomingNumber)
    }
  }
}

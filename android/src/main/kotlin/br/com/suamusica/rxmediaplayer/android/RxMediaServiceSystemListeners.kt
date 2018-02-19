package br.com.suamusica.rxmediaplayer.android

import android.media.AudioManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import br.com.suamusica.rxmediaplayer.domain.RxMediaService

object RxMediaServiceSystemListeners {
  class OnAudioFocusChangeListener(
      private val rxMediaService: RxMediaService
  ) : AudioManager.OnAudioFocusChangeListener {
    private var wasPlaying: Boolean = false
    private var lossType: Int = 0

    override fun onAudioFocusChange(focusChange: Int) {
      if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
          focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ||
          focusChange == AudioManager.AUDIOFOCUS_LOSS) {
        // Pause playback
        wasPlaying = rxMediaService.isPlaying().blockingGet()
        lossType = focusChange

        if (wasPlaying) {
          rxMediaService.pause().subscribe()
        }
      } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
        // Resume playback
        if (wasPlaying && rxMediaService.isPaused().blockingGet() && lossType != AudioManager.AUDIOFOCUS_LOSS) {
          rxMediaService.play().subscribe()
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
          rxMediaService.pause().subscribe()
        }
      } else if (state == TelephonyManager.CALL_STATE_IDLE) {
        //Not in call: Play music
        if (wasPlaying && rxMediaService.isPaused().blockingGet()) {
          wasPlaying = false
          rxMediaService.play().subscribe()
        }
      }
      super.onCallStateChanged(state, incomingNumber)
    }
  }
}

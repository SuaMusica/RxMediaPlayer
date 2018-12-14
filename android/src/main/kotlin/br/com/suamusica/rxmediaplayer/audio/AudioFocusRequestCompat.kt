package br.com.suamusica.rxmediaplayer.audio

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.support.annotation.IntDef
import android.support.annotation.RequiresApi
import android.support.v4.media.AudioAttributesCompat

/**
 * Compatibility version of an [AudioFocusRequest].
 */
class AudioFocusRequestCompat private constructor(
    val focusGain: Int,
    /* package */ internal val onAudioFocusChangeListener: AudioManager.OnAudioFocusChangeListener,
    /* package */ internal val focusChangeHandler: Handler,
    val audioAttributesCompat: AudioAttributesCompat?,
    private val pauseOnDuck: Boolean,
    private val acceptsDelayedFocusGain: Boolean) {

  private/* package */ val audioAttributes: AudioAttributes?
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    get() = if (audioAttributesCompat != null)
      audioAttributesCompat.unwrap() as AudioAttributes
    else
      null

  internal/* package */ val audioFocusRequest: AudioFocusRequest
    @RequiresApi(Build.VERSION_CODES.O)
    get() = AudioFocusRequest.Builder(focusGain)
        .setAudioAttributes(audioAttributes)
        .setAcceptsDelayedFocusGain(acceptsDelayedFocusGain)
        .setWillPauseWhenDucked(pauseOnDuck)
        .setOnAudioFocusChangeListener(onAudioFocusChangeListener, focusChangeHandler)
        .build()

  @Retention(AnnotationRetention.SOURCE)
  @IntDef(AudioManager.AUDIOFOCUS_GAIN, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
  annotation class FocusGain

  fun willPauseWhenDucked(): Boolean {
    return pauseOnDuck
  }

  fun acceptsDelayedFocusGain(): Boolean {
    return acceptsDelayedFocusGain
  }

  /**
   * Builder for an [AudioFocusRequestCompat].
   */
  class Builder {
    private var focusGain: Int = 0
    private lateinit var onAudioFocusChangeListener: AudioManager.OnAudioFocusChangeListener
    private lateinit var focusChangeHandler: Handler
    private var audioAttributesCompat: AudioAttributesCompat? = null

    // Flags
    private var pauseOnDuck: Boolean = false
    private var acceptsDelayedFocusGain1: Boolean = false

    constructor(@FocusGain focusGain: Int) {
      this.focusGain = focusGain
    }

    constructor(requestToCopy: AudioFocusRequestCompat) {
      focusGain = requestToCopy.focusGain
      onAudioFocusChangeListener = requestToCopy.onAudioFocusChangeListener
      focusChangeHandler = requestToCopy.focusChangeHandler
      audioAttributesCompat = requestToCopy.audioAttributesCompat
      pauseOnDuck = requestToCopy.pauseOnDuck
      acceptsDelayedFocusGain1 = requestToCopy.acceptsDelayedFocusGain
    }

    fun setFocusGain(@FocusGain focusGain: Int): Builder {
      this.focusGain = focusGain
      return this
    }

    @JvmOverloads
    fun setOnAudioFocusChangeListener(listener: AudioManager.OnAudioFocusChangeListener,
        handler: Handler = Handler(Looper.getMainLooper())): Builder {
      onAudioFocusChangeListener = listener
      focusChangeHandler = handler
      return this
    }

    fun setAudioAttributes(attributes: AudioAttributesCompat): Builder {
      audioAttributesCompat = attributes
      return this
    }

    fun setWillPauseWhenDucked(pauseOnDuck: Boolean): Builder {
      this.pauseOnDuck = pauseOnDuck
      return this
    }

    fun setAcceptsDelayedFocusGain(acceptsDelayedFocusGain: Boolean): Builder {
      acceptsDelayedFocusGain1 = acceptsDelayedFocusGain
      return this
    }

    fun build(): AudioFocusRequestCompat {
      return AudioFocusRequestCompat(focusGain,
          onAudioFocusChangeListener,
          focusChangeHandler,
          audioAttributesCompat,
          pauseOnDuck,
          acceptsDelayedFocusGain1)
    }
  }
}

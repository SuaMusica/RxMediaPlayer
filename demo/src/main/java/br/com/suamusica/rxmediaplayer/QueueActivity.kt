package br.com.suamusica.rxmediaplayer

import android.os.Bundle
import br.com.suamusica.rxmediaplayer.android.RxMediaServiceActivity

class QueueActivity : RxMediaServiceActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_queue)
  }
}
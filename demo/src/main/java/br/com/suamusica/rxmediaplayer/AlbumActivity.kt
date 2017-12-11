package br.com.suamusica.rxmediaplayer

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import br.com.suamusica.rxmediaplayer.android.RxMediaServiceActivity

class AlbumActivity : RxMediaServiceActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_demo)
  }
}

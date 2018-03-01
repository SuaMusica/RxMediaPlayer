package br.com.suamusica.rxmediaplayer.domain

data class MediaProgress(val current: Int, val total: Int) {

  fun percentage(): Int = (current * 100) / total

  companion object {
    val COMPLETED = MediaProgress(100, 100)
    val NONE = MediaProgress(0, 1)
  }
}

package br.com.suamusica.rxmediaplayer.domain

data class MediaProgress(val current: Int, val total: Int) {
  companion object {
    val COMPLETED = MediaProgress(100, 100)
  }
}

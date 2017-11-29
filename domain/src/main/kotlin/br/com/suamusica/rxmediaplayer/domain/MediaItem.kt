package br.com.suamusica.rxmediaplayer.domain

data class MediaItem constructor(
  val name: String,
  val author: String,
  val url: String,
  val coverUrl: String,
  val timestamp: Long = System.currentTimeMillis()
)

package br.com.suamusica.rxmediaplayer.domain

data class MediaItem constructor(
    val id: String,
    val name: String,
    val author: String,
    val mediaUrl: String,
    val coverUrl: String,
    val timestamp: Long = System.currentTimeMillis(),
    val extras: Map<String, Any?> = emptyMap()
)

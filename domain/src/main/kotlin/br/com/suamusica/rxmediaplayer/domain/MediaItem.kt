package br.com.suamusica.rxmediaplayer.domain

data class MediaItem constructor(
    val id: String,
    val name: String,
    val author: String,
    val url: String,
    val coverUrl: String,
    val timestamp: Long = System.currentTimeMillis(),
    val itemHash: String = timestamp.toString().md5(),
    val extras: Map<String, Any?> = emptyMap()
)

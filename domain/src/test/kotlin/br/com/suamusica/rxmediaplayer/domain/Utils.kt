package br.com.suamusica.rxmediaplayer.domain

import java.util.*


fun fakeMediaItem() = Random().run {
  MediaItem(
    name = nextLong().toString(),
    author = "Author ${nextInt(100)}",
    coverUrl = "http://image.from/author_${nextInt(100)}",
    url = "http://music.from/author_${nextInt(100)}"
  )
}

package br.com.suamusica.rxmediaplayer

import br.com.suamusica.rxmediaplayer.domain.MediaItem


class MediaRepository {
  companion object {
    fun musics(): List<MediaItem> {
      val musicsNameAndResId = listOf(
          "Alô dono do bar" to "http://picosong.com/cdn/349370ef3f7f248262f97c800f0c4585.mp3",
          "Manda boi" to "http://picosong.com/cdn/b7097a8bb4a9ab77d6571057b84ce95f.mp3",
          "Amor da sua cama" to "http://picosong.com/cdn/da3273285f164e303d1215f463737e94.mp3",
          "Rabiola" to "http://picosong.com/cdn/15b6a417bbbb70727a788f2fcd0661e9.mp3",
          "Na conta da loucura" to "http://picosong.com/cdn/a2c8238afd7296f5f64d63c07fc559f9.mp3"
      )

      return musicsNameAndResId
          .map { (name, url) -> createMediaItem(name, url) }
    }

    private fun createMediaItem(name: String, uri: String): MediaItem {
      return MediaItem(
          name = name,
          author = "Wesley Safadão",
          coverUrl = "https://images.suamusica.com.br/LoyfSPrBR-0Gq3ExIazGFD_Kkqw=/240x240/41261/2013007/cd_cover.png",
          url = uri
      )
    }
  }
}
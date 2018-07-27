package br.com.suamusica.rxmediaplayer

import br.com.suamusica.rxmediaplayer.domain.MediaItem


class MediaRepository {
  companion object {
    fun musics(): List<MediaItem> {
      val musicsNameAndResId = listOf(
          "Click" to "http://www.sample-videos.com/audio/mp3/crowd-cheering.mp3",
          "Manda boi" to "http://www.sample-videos.com/audio/mp3/wave.mp3",
          "Piano Cool Edit" to "http://www.kozco.com/tech/piano2-CoolEdit.mp3",
          "Original Finale" to "http://www.kozco.com/tech/organfinale.mp3"
      )

      return musicsNameAndResId
          .map { (name, url) -> createMediaItem(name, url) }
    }

    private fun createMediaItem(name: String, uri: String): MediaItem {
      return MediaItem(
          id = "19919",
          name = name,
          author = "Wesley Safadão",
          coverUrl = "https://images.suamusica.com.br/LoyfSPrBR-0Gq3ExIazGFD_Kkqw=/240x240/41261/2013007/cd_cover.png",
          url = uri
      )
    }
  }
}
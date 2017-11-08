package br.com.suamusica.rxmediaplayer.infra

import br.com.suamusica.rxmediaplayer.domain.MediaItem


interface MediaPlayer {
  fun play(mediaItem: MediaItem)
}
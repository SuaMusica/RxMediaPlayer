package br.com.suamusica.rxmediaplayer.extensions

import android.net.Uri
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import br.com.suamusica.rxmediaplayer.domain.MediaBoundState
import br.com.suamusica.rxmediaplayer.domain.MediaItem
import android.R.attr.duration
import android.os.Bundle



fun MediaBoundState.toMediaMetadata(): MediaMetadataCompat {
  return MediaMetadataCompat.Builder().build()
}


fun MediaItem.toMediaDescription(): MediaDescriptionCompat {
  val songDuration = Bundle()
  songDuration.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration.toLong())

  return MediaDescriptionCompat.Builder()
      .setTitle(this.name)
      .setDescription(this.name)
      .setMediaId(this.id)
      .setSubtitle(this.author)
      .setIconUri(Uri.parse(this.coverUrl))
      .setMediaUri(Uri.parse(this.mediaUrl))
      .setExtras(songDuration)
      .build()
}
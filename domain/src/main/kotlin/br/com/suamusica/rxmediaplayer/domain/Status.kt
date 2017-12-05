package br.com.suamusica.rxmediaplayer.domain

sealed class MediaPlayerState

object IdleState : MediaPlayerState()

sealed class OngoingState(
  val mediaItem: MediaItem,
  val mediaProgress: MediaProgress
) : MediaPlayerState()

class PlayingState(
  mediaItem: MediaItem,
  mediaProgress: MediaProgress
) : OngoingState(mediaItem, mediaProgress)

class PausedState(
  mediaItem: MediaItem,
  mediaProgress: MediaProgress
) : OngoingState(mediaItem, mediaProgress)

class StoppedState(
  mediaItem: MediaItem,
  mediaProgress: MediaProgress
) : OngoingState(mediaItem, mediaProgress)

class CompletedState(mediaItem: MediaItem) : OngoingState(mediaItem, MediaProgress.COMPLETED)

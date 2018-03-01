package br.com.suamusica.rxmediaplayer.domain

sealed class MediaServiceState

object IdleState : MediaServiceState()

sealed class MediaBoundState(
    open val item: MediaItem,
    open val progress: MediaProgress
) : MediaServiceState()

data class LoadingState(
    override val item: MediaItem
) : MediaBoundState(item, MediaProgress.NONE)

data class PlayingState(
    override val item: MediaItem,
    override val progress: MediaProgress
) : MediaBoundState(item, progress)

data class PausedState(
    override val item: MediaItem,
    override val progress: MediaProgress
) : MediaBoundState(item, progress)

data class StoppedState(
    override val item: MediaItem,
    override val progress: MediaProgress
) : MediaBoundState(item, progress)

data class CompletedState(
    override val item: MediaItem
) : MediaBoundState(item, MediaProgress.COMPLETED)

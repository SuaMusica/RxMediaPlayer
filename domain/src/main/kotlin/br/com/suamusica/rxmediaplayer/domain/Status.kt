package br.com.suamusica.rxmediaplayer.domain

import java.lang.Exception

sealed class MediaServiceState(open val isRandomized: Boolean? = null, open val repeatState: RepeatState? = null) {
  abstract fun setRandomizedState(isRandomized: Boolean): MediaBoundState
  abstract fun setRepeatModeState(repeatState: RepeatState): MediaBoundState
}

sealed class MediaBoundState(
    open val item: MediaItem?,
    open val progress: MediaProgress?,
    override val isRandomized: Boolean? = null,
    override val repeatState: RepeatState? = null
) : MediaServiceState(isRandomized, repeatState)

data class LoadingState(
    override val item: MediaItem,
    override val isRandomized: Boolean? = null,
    override val repeatState: RepeatState? = null
) : MediaBoundState(item, null, isRandomized, repeatState) {
  override fun setRepeatModeState(repeatState: RepeatState): MediaBoundState = copy(repeatState = repeatState)
  override fun setRandomizedState(isRandomized: Boolean): MediaBoundState = copy(isRandomized = isRandomized)
}

data class PlayingState(
    override val item: MediaItem,
    override val progress: MediaProgress,
    override val isRandomized: Boolean? = null,
    override val repeatState: RepeatState? = null
) : MediaBoundState(item, progress, isRandomized, repeatState) {
  override fun setRepeatModeState(repeatState: RepeatState): MediaBoundState = copy(repeatState = repeatState)
  override fun setRandomizedState(isRandomized: Boolean): MediaBoundState = copy(isRandomized = isRandomized)
}

data class PausedState(
    override val item: MediaItem,
    override val progress: MediaProgress,
    override val isRandomized: Boolean? = null,
    override val repeatState: RepeatState? = null
) : MediaBoundState(item, progress, isRandomized, repeatState) {
  override fun setRepeatModeState(repeatState: RepeatState): MediaBoundState = copy(repeatState = repeatState)
  override fun setRandomizedState(isRandomized: Boolean): MediaBoundState = copy(isRandomized = isRandomized)
}

data class StoppedState(
    override val item: MediaItem,
    override val progress: MediaProgress,
    override val isRandomized: Boolean? = null,
    override val repeatState: RepeatState? = null
) : MediaBoundState(item, progress, isRandomized, repeatState) {
  override fun setRepeatModeState(repeatState: RepeatState): MediaBoundState = copy(repeatState = repeatState)
  override fun setRandomizedState(isRandomized: Boolean): MediaBoundState = copy(isRandomized = isRandomized)
}

data class CompletedState(
    override val item: MediaItem,
    override val isRandomized: Boolean? = null,
    override val repeatState: RepeatState? = null
) : MediaBoundState(item, MediaProgress.COMPLETED, isRandomized, repeatState) {
  override fun setRepeatModeState(repeatState: RepeatState): MediaBoundState = copy(repeatState = repeatState)
  override fun setRandomizedState(isRandomized: Boolean): MediaBoundState = copy(isRandomized = isRandomized)
}

data class ErrorState(
    override val item: MediaItem,
    override val progress: MediaProgress,
    override val isRandomized: Boolean? = null,
    override val repeatState: RepeatState? = null,
    val exception: Exception
) : MediaBoundState(item, progress, isRandomized, repeatState) {
  override fun setRepeatModeState(repeatState: RepeatState): MediaBoundState = copy(repeatState = repeatState)
  override fun setRandomizedState(isRandomized: Boolean): MediaBoundState = copy(isRandomized = isRandomized)
}

data class IdleState(
    val mediaItem: MediaItem? = null,
    override val isRandomized: Boolean? = null,
    override val repeatState: RepeatState? = null
) : MediaBoundState(null, null) {
  override fun setRepeatModeState(repeatState: RepeatState): MediaBoundState = copy(repeatState = repeatState)
  override fun setRandomizedState(isRandomized: Boolean): MediaBoundState = copy(isRandomized = isRandomized)
}
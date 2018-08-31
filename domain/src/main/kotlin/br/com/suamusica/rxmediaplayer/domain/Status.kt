package br.com.suamusica.rxmediaplayer.domain

sealed class MediaServiceState(open val isRandomized: Boolean? = null, open val isPlaying: Boolean?, open val repeatState: RepeatState? = null) {
  abstract fun setPlayingState(isPlaying: Boolean): MediaBoundState
  abstract fun setRandomizedState(isRandomized: Boolean): MediaBoundState
  abstract fun setRepeatModeState(repeatState: RepeatState): MediaBoundState
}

sealed class MediaBoundState(
    open val item: MediaItem?,
    open val progress: MediaProgress?,
    override val isRandomized: Boolean? = null,
    override val isPlaying: Boolean? = null,
    override val repeatState: RepeatState? = null
) : MediaServiceState(isRandomized, isPlaying, repeatState)

data class LoadingState(
    override val item: MediaItem,
    override val isRandomized: Boolean? = null,
    override val isPlaying: Boolean? = null,
    override val repeatState: RepeatState? = null
) : MediaBoundState(item, MediaProgress.NONE, isRandomized, isPlaying, repeatState) {
  override fun setRepeatModeState(repeatState: RepeatState): MediaBoundState = copy(repeatState = repeatState)
  override fun setRandomizedState(isRandomized: Boolean): MediaBoundState = copy(isRandomized = isRandomized)
  override fun setPlayingState(isPlaying: Boolean): MediaBoundState = copy(isPlaying = isPlaying)
}

data class PlayingState(
    override val item: MediaItem,
    override val progress: MediaProgress,
    override val isRandomized: Boolean? = null,
    override val isPlaying: Boolean? = null,
    override val repeatState: RepeatState? = null
) : MediaBoundState(item, progress, isRandomized, isPlaying, repeatState) {
  override fun setRepeatModeState(repeatState: RepeatState): MediaBoundState = copy(repeatState = repeatState)
  override fun setRandomizedState(isRandomized: Boolean): MediaBoundState = copy(isRandomized = isRandomized)
  override fun setPlayingState(isPlaying: Boolean): MediaBoundState = copy(isPlaying = isPlaying)
}

data class PausedState(
    override val item: MediaItem,
    override val progress: MediaProgress,
    override val isRandomized: Boolean? = null,
    override val isPlaying: Boolean? = null,
    override val repeatState: RepeatState? = null
) : MediaBoundState(item, progress, isRandomized, isPlaying, repeatState) {
  override fun setRepeatModeState(repeatState: RepeatState): MediaBoundState = copy(repeatState = repeatState)
  override fun setRandomizedState(isRandomized: Boolean): MediaBoundState = copy(isRandomized = isRandomized)
  override fun setPlayingState(isPlaying: Boolean): MediaBoundState = copy(isPlaying = isPlaying)
}

data class StoppedState(
    override val item: MediaItem,
    override val progress: MediaProgress,
    override val isRandomized: Boolean? = null,
    override val isPlaying: Boolean? = null,
    override val repeatState: RepeatState? = null
) : MediaBoundState(item, progress, isRandomized, isPlaying, repeatState) {
  override fun setRepeatModeState(repeatState: RepeatState): MediaBoundState = copy(repeatState = repeatState)
  override fun setRandomizedState(isRandomized: Boolean): MediaBoundState = copy(isRandomized = isRandomized)
  override fun setPlayingState(isPlaying: Boolean): MediaBoundState = copy(isPlaying = isPlaying)
}

data class CompletedState(
    override val item: MediaItem,
    override val isRandomized: Boolean? = null,
    override val isPlaying: Boolean? = null,
    override val repeatState: RepeatState? = null
) : MediaBoundState(item, MediaProgress.COMPLETED, isRandomized, isPlaying, repeatState) {
  override fun setRepeatModeState(repeatState: RepeatState): MediaBoundState = copy(repeatState = repeatState)
  override fun setRandomizedState(isRandomized: Boolean): MediaBoundState = copy(isRandomized = isRandomized)
  override fun setPlayingState(isPlaying: Boolean): MediaBoundState = copy(isPlaying = isPlaying)
}

data class IdleState(
    val mediaItem: MediaItem? = null,
    override val isRandomized: Boolean? = null,
    override val isPlaying: Boolean? = null,
    override val repeatState: RepeatState? = null
) : MediaBoundState(null, null) {
  override fun setRepeatModeState(repeatState: RepeatState): MediaBoundState = copy(repeatState = repeatState)
  override fun setRandomizedState(isRandomized: Boolean): MediaBoundState = copy(isRandomized = isRandomized)
  override fun setPlayingState(isPlaying: Boolean): MediaBoundState = copy(isPlaying = isPlaying)
}
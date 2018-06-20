package br.com.suamusica.rxmediaplayer.domain

sealed class MediaServiceState(open val isRandomized: Boolean? = null) {
  abstract fun setRandomizedState(isRandomized: Boolean): MediaBoundState
}

sealed class MediaBoundState(
    open val item: MediaItem,
    open val progress: MediaProgress,
    override val isRandomized: Boolean? = null
) : MediaServiceState(isRandomized)

data class LoadingState(
    override val item: MediaItem,
    override val isRandomized: Boolean? = null
) : MediaBoundState(item, MediaProgress.NONE, isRandomized) {
  override fun setRandomizedState(isRandomized: Boolean): MediaBoundState = copy(isRandomized = isRandomized)
}

data class PlayingState(
    override val item: MediaItem,
    override val progress: MediaProgress,
    override val isRandomized: Boolean? = null
) : MediaBoundState(item, progress, isRandomized) {
  override fun setRandomizedState(isRandomized: Boolean): MediaBoundState = copy(isRandomized = isRandomized)
}

data class PausedState(
    override val item: MediaItem,
    override val progress: MediaProgress,
    override val isRandomized: Boolean? = null
) : MediaBoundState(item, progress, isRandomized) {
  override fun setRandomizedState(isRandomized: Boolean): MediaBoundState = copy(isRandomized = isRandomized)
}

data class StoppedState(
    override val item: MediaItem,
    override val progress: MediaProgress,
    override val isRandomized: Boolean? = null
) : MediaBoundState(item, progress, isRandomized) {
  override fun setRandomizedState(isRandomized: Boolean): MediaBoundState = copy(isRandomized = isRandomized)
}

data class CompletedState(
    override val item: MediaItem,
    override val isRandomized: Boolean? = null
) : MediaBoundState(item, MediaProgress.COMPLETED, isRandomized) {
  override fun setRandomizedState(isRandomized: Boolean): MediaBoundState = copy(isRandomized = isRandomized)
}

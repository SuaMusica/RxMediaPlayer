package br.com.suamusica.rxmediaplayer

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.*
import br.com.suamusica.rxmediaplayer.android.MediaPlayerState
import br.com.suamusica.rxmediaplayer.domain.MediaBoundState
import br.com.suamusica.rxmediaplayer.domain.MediaItem
import com.squareup.picasso.Picasso
import io.reactivex.Observable

class MiniPlayerView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : ConstraintLayout(context, attrs, defStyleAttr) {
  private lateinit var playPauseButton: ImageButton
  private lateinit var nextButton: ImageButton
  private lateinit var prevButton: ImageButton

  private lateinit var albumCoverImage: ImageView
  private lateinit var musicProgress: ProgressBar

  private lateinit var nameSongText: TextView
  private lateinit var artistSongNameText: TextView

  var isPlayingSong: (() -> Observable<Boolean>)? = null

  var onClickPlay: (() -> Unit)? = null
  var onClickPause: (() -> Unit)? = null
  var onClickPrev: (() -> Unit)? = null
  var onClickNext: (() -> Unit)? = null

  constructor (context: Context) : this(context, null, 0)

  constructor(context: Context, attrs: AttributeSet): this(context, attrs, 0)

  init {
    val view = View.inflate(context, R.layout.item_player, this)

    bindViews(view)
    setOnClick()
  }

  private fun setOnClick() {
    playPauseButton.setOnClickListener { clickPlayPause() }
    nextButton.setOnClickListener { onClickNext?.invoke() }
    prevButton.setOnClickListener { onClickPrev?.invoke() }
  }

  private fun clickPlayPause() {
    isPlayingSong?.invoke()?.subscribe(
      { configPlayPauseButton(it) },
      { showError(it) }
    )
  }

  private fun configPlayPauseButton(it: Boolean?) {
    if (it != null && it) {
      onClickPause?.invoke()
      configPauseButton()
    } else {
      onClickPlay?.invoke()
      configPlayButton()
    }
  }

  private fun showError(throwable: Throwable) {
    Log.e("DemoApp MiniPlayer", throwable.message, throwable)
    Toast.makeText(context, throwable.message, Toast.LENGTH_LONG).show()
  }

  private fun configPlayButton() {
    playPauseButton.setImageResource(android.R.drawable.ic_media_play)
  }

  private fun configPauseButton() {
    playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
  }

  private fun bindViews(view: View?) {
    view?.let {
      playPauseButton = view.findViewById(R.id.play_pause_player_imagebutton)
      nextButton = view.findViewById(R.id.next_player_imagebutton)
      prevButton = view.findViewById(R.id.prev_player_imagebutton)

      albumCoverImage = view.findViewById(R.id.album_image_player_imageview)
      musicProgress = view.findViewById(R.id.current_progress_player_progressbar)

      nameSongText = view.findViewById(R.id.name_song_player_textview)
      artistSongNameText = view.findViewById(R.id.artist_name_player_textview)
    }
  }

  fun bind(state: MediaBoundState?) {
    state?.let {
      nameSongText.text = it.item.name
      artistSongNameText.text = it.item.author

      Picasso.with(context)
          .load(it.item.coverUrl)
          .fit()
          .into(albumCoverImage)

      musicProgress.progress = it.progress.current.toInt()
    } ?: run {
      nameSongText.text = ""
      artistSongNameText.text = ""
      musicProgress.progress = 0
      albumCoverImage.setImageResource(-1)
    }
  }
}
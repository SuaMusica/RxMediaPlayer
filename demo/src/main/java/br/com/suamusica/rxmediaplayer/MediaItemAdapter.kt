package br.com.suamusica.rxmediaplayer

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import br.com.suamusica.rxmediaplayer.domain.MediaItem

class MediaItemAdapter(
    private val mediaItems: List<MediaItem>
) : RecyclerView.Adapter<MediaItemAdapter.Holder>() {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
    val view = LayoutInflater.from(parent.context).inflate(Holder.LAYOUT_ID, parent)
    return Holder(view)
  }

  override fun onBindViewHolder(holder: Holder, position: Int) {
    val mediaItem = mediaItems[position]
    holder.bind(mediaItem)
  }

  override fun getItemCount(): Int = mediaItems.size

  class Holder(view: View) : RecyclerView.ViewHolder(view) {

    fun bind(mediaItem: MediaItem) {
      //textViewName.text = mediaItem.name
    }

    companion object {
      val LAYOUT_ID = R.layout.viewholder_media_item
    }
  }
}

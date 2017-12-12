package br.com.suamusica.rxmediaplayer

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import br.com.suamusica.rxmediaplayer.domain.MediaItem
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MediaItemAdapter(
    private val mediaItems: List<MediaItem>
) : RecyclerView.Adapter<MediaItemAdapter.Holder>() {

  private val itemClicksSubject = PublishSubject.create<MediaItem>()
  private val addClicksSubject = PublishSubject.create<MediaItem>()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
    return LayoutInflater.from(parent.context).run {
      val view = inflate(R.layout.viewholder_media_item, parent, false)
      Holder(view)
    }
  }

  override fun onBindViewHolder(holder: Holder, position: Int) {
    val mediaItem = mediaItems[position]
    holder.bind(mediaItem)
  }

  override fun getItemCount(): Int = mediaItems.size

  fun itemClicks(): Observable<MediaItem> = itemClicksSubject

  fun addClicks(): Observable<MediaItem> = addClicksSubject

  inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
    private val textViewName by lazy { view.findViewById<TextView>(R.id.textViewName) }
    private val buttonAdd by lazy { view.findViewById<ImageView>(R.id.buttonAdd) }

    fun bind(mediaItem: MediaItem) {
      textViewName.text = mediaItem.name

      itemView.setOnClickListener { itemClicksSubject.onNext(mediaItem) }
      buttonAdd.setOnClickListener { addClicksSubject.onNext(mediaItem) }
    }
  }
}

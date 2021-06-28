package com.sithuhein.androidstorage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_view.view.*

class SharedPhotoAdapter(private val onLongClick : (SharedStoragePhoto) -> Unit) : RecyclerView.Adapter<SharedPhotoAdapter.SharedPhotoViewHolder>()  {

    private val mData = mutableListOf<SharedStoragePhoto>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SharedPhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_view, parent,false)
        return SharedPhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: SharedPhotoViewHolder, position: Int) {
        holder.bind(mData[position])
    }

    override fun getItemCount(): Int = mData.size

    fun setNewData(list : List<SharedStoragePhoto>) {
        mData.clear()
        mData.addAll(list)
        notifyDataSetChanged()
    }


    inner class SharedPhotoViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        fun bind(data : SharedStoragePhoto) {
            itemView.image.setImageURI(data.contentUri)
            itemView.image.setOnLongClickListener {
                onLongClick(data)
                true
            }
        }
    }


}
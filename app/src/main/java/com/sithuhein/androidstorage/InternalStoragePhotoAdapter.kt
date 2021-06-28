package com.sithuhein.androidstorage

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_view.view.*

class InternalStoragePhotoAdapter
    (private val onLongClick : (InternalStoragePhoto) -> Unit ) : RecyclerView.Adapter<InternalStoragePhotoAdapter.InternalStoragePhotoViewHolder>() {

    private val mData = mutableListOf<InternalStoragePhoto>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InternalStoragePhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_view, parent, false)
        return InternalStoragePhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: InternalStoragePhotoViewHolder, position: Int) {
        holder.bind(mData[position])
    }

    override fun getItemCount(): Int = mData.size

    fun setNewData(list : List<InternalStoragePhoto>) {
        mData.clear()
        mData.addAll(list)
        notifyDataSetChanged()
    }

     inner class InternalStoragePhotoViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        fun bind(data : InternalStoragePhoto) {
            Log.d("Internal Storage", "${data.name} ${data.bmp}")
            itemView.image.setImageBitmap(data.bmp)
            itemView.image.setOnLongClickListener {
                onLongClick(data)
                true
            }
        }
    }


}
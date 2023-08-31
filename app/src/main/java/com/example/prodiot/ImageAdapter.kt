package com.example.prodiot

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ImageAdapter : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {
    private var imageUrls: List<String> = emptyList()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return ImageViewHolder(itemView)
    }
    override fun getItemCount(): Int {
        return imageUrls.size
    }
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = imageUrls[position]
        holder.bind(imageUrl)
    }
    fun setImageUrls(imageUrls: List<String>) {
        this.imageUrls = imageUrls
        notifyDataSetChanged()
    }
    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)
        fun bind(imageUrl: String) {
            // Glide를 사용하여 이미지를 로드하고 ImageView에 설정합니다.
            Glide.with(itemView.context)
                .load(imageUrl)
                .into(imageView)
        }
    }
}

package com.example.prodiot

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.firebase.ui.storage.images.FirebaseImageLoader
import com.google.firebase.storage.StorageReference
import java.io.InputStream

@GlideModule
class MyAppGlideModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        // Firebase StorageReference를 처리하기 위한 ModelLoader를 등록합니다.
        registry.append(StorageReference::class.java, InputStream::class.java, FirebaseImageLoader.Factory())
    }
}

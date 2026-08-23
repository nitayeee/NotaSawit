package com.example.notasawit.Utils

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import java.util.Locale

object AvatarHelper {

    data class AvatarColorPair(val backgroundColor: Int, val textColor: Int)

    private val COLOR_PAIRS = arrayOf(
        AvatarColorPair(Color.parseColor("#E8F5E9"), Color.parseColor("#1B5E20")), // Soft Green -> Dark Green
        AvatarColorPair(Color.parseColor("#E3F2FD"), Color.parseColor("#0D47A1")), // Soft Blue -> Dark Blue
        AvatarColorPair(Color.parseColor("#F3E5F5"), Color.parseColor("#4A148C")), // Soft Purple -> Dark Purple
        AvatarColorPair(Color.parseColor("#FFF3E0"), Color.parseColor("#E65100")), // Soft Orange -> Dark Orange
        AvatarColorPair(Color.parseColor("#FFEBEE"), Color.parseColor("#B71C1C")), // Soft Red -> Dark Red
        AvatarColorPair(Color.parseColor("#E0F2F1"), Color.parseColor("#004D40")), // Soft Teal -> Dark Teal
        AvatarColorPair(Color.parseColor("#FCE4EC"), Color.parseColor("#880E4F")), // Soft Pink -> Dark Pink
        AvatarColorPair(Color.parseColor("#E8EAF6"), Color.parseColor("#1A237E")), // Soft Indigo -> Dark Indigo
        AvatarColorPair(Color.parseColor("#EDE7F6"), Color.parseColor("#311B92")), // Soft Violet -> Dark Violet
        AvatarColorPair(Color.parseColor("#E0F7FA"), Color.parseColor("#006064")), // Soft Cyan -> Dark Cyan
        AvatarColorPair(Color.parseColor("#F0F4C3"), Color.parseColor("#33691E")), // Soft Lime -> Dark Lime
        AvatarColorPair(Color.parseColor("#EFEBE9"), Color.parseColor("#4E342E"))  // Soft Warm Sand -> Dark Brown
    )

    fun getInitials(name: String?): String {
        if (name.isNullOrBlank()) return "?"
        val cleanName = name.trim()
        val parts = cleanName.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        return when {
            parts.size >= 2 -> {
                val first = parts[0].substring(0, 1).uppercase(Locale.getDefault())
                val second = parts[1].substring(0, 1).uppercase(Locale.getDefault())
                "$first$second"
            }
            parts.size == 1 && parts[0].length >= 2 -> {
                parts[0].substring(0, 2).uppercase(Locale.getDefault())
            }
            parts.size == 1 -> {
                parts[0].substring(0, 1).uppercase(Locale.getDefault())
            }
            else -> "?"
        }
    }

    fun getColorPairForName(name: String?): AvatarColorPair {
        if (name.isNullOrEmpty()) return COLOR_PAIRS[0]
        val hash = Math.abs(name.trim().lowercase(Locale.getDefault()).hashCode())
        val index = hash % COLOR_PAIRS.size
        return COLOR_PAIRS[index]
    }

    fun setupAvatar(
        imageView: ImageView,
        textViewInitial: TextView,
        nama: String?,
        fotoPathOrUrl: String?
    ) {
        val initials = getInitials(nama)
        val colorPair = getColorPairForName(nama)

        textViewInitial.text = initials
        textViewInitial.setTextColor(colorPair.textColor)
        textViewInitial.backgroundTintList = ColorStateList.valueOf(colorPair.backgroundColor)

        val cleanPath = fotoPathOrUrl?.trim()
        if (!cleanPath.isNullOrEmpty() && cleanPath != "null") {
            val fullUrl = when {
                cleanPath.startsWith("http") -> cleanPath
                cleanPath.startsWith("storage/") -> "http://notasawit.pocari.id/$cleanPath"
                cleanPath.startsWith("profil/") -> "http://notasawit.pocari.id/storage/$cleanPath"
                cleanPath.startsWith("/") -> "http://notasawit.pocari.id/storage$cleanPath"
                else -> "http://notasawit.pocari.id/storage/profil/$cleanPath"
            }

            imageView.visibility = View.VISIBLE
            textViewInitial.visibility = View.GONE

            Glide.with(imageView.context)
                .load(fullUrl)
                .apply(RequestOptions.circleCropTransform())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.drawable.Drawable>() {
                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable,
                        transition: com.bumptech.glide.request.transition.Transition<in android.graphics.drawable.Drawable>?
                    ) {
                        imageView.setImageDrawable(resource)
                        imageView.visibility = View.VISIBLE
                        textViewInitial.visibility = View.GONE
                    }

                    override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                        imageView.setImageDrawable(placeholder)
                    }

                    override fun onLoadFailed(errorDrawable: android.graphics.drawable.Drawable?) {
                        imageView.visibility = View.GONE
                        textViewInitial.visibility = View.VISIBLE
                    }
                })
        } else {
            imageView.visibility = View.GONE
            textViewInitial.visibility = View.VISIBLE
        }
    }
}

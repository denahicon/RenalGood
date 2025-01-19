package com.example.renalgood.admin;

import android.content.Context;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;

public class ImageLoadUtils {
    public static void cargarImagen(Context context, String url, ImageView imageView, int placeholderRes) {
        if (url == null || url.isEmpty()) return;

        Glide.with(context)
                .load(url)
                .placeholder(placeholderRes)
                .error(placeholderRes)
                .into(imageView);
    }

    public static void cargarImagenDesdeStorage(Context context, String path, ImageView imageView, int placeholderRes) {
        if (path == null || path.isEmpty()) return;

        FirebaseStorage.getInstance().getReference().child(path)
                .getDownloadUrl()
                .addOnSuccessListener(uri -> cargarImagen(context, uri.toString(), imageView, placeholderRes))
                .addOnFailureListener(e -> imageView.setImageResource(placeholderRes));
    }
}
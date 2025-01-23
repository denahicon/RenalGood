package com.example.renalgood.admin;

import android.content.Context;
import android.widget.ImageView;
import android.util.Log;
import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;

public class ImageLoadUtils {
    private static final String TAG = "ImageLoadUtils";

    public static void cargarImagen(Context context, String url, ImageView imageView, int placeholderRes) {
        if (context == null || imageView == null) {
            Log.e(TAG, "Context o ImageView nulos");
            return;
        }

        try {
            if (url == null || url.isEmpty()) {
                imageView.setImageResource(placeholderRes);
                return;
            }

            Glide.with(context)
                    .load(url)
                    .placeholder(placeholderRes)
                    .error(placeholderRes)
                    .into(imageView);
        } catch (Exception e) {
            Log.e(TAG, "Error cargando imagen: " + e.getMessage());
            imageView.setImageResource(placeholderRes);
        }
    }

    public static void cargarImagenDesdeStorage(Context context, String path, ImageView imageView, int placeholderRes) {
        if (context == null || imageView == null) {
            Log.e(TAG, "Context o ImageView nulos");
            return;
        }

        if (path == null || path.isEmpty()) {
            imageView.setImageResource(placeholderRes);
            return;
        }

        try {
            FirebaseStorage.getInstance().getReference().child(path)
                    .getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        if (context != null && imageView != null) {
                            cargarImagen(context, uri.toString(), imageView, placeholderRes);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error obteniendo URL de descarga: " + e.getMessage());
                        if (imageView != null) {
                            imageView.setImageResource(placeholderRes);
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error en cargarImagenDesdeStorage: " + e.getMessage());
            imageView.setImageResource(placeholderRes);
        }
    }
}
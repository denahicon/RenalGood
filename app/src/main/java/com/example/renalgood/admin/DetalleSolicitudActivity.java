package com.example.renalgood.admin;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.renalgood.R;
import com.example.renalgood.databinding.ActivityDetallesSolicitudBinding;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class DetalleSolicitudActivity extends AppCompatActivity {

    private ActivityDetallesSolicitudBinding binding;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private String nutriologoId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetallesSolicitudBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Inicializar Firebase
        inicializarFirebase();

        // Configurar ActionBar
        configurarActionBar();

        // Obtener el ID de la solicitud
        nutriologoId = getIntent().getStringExtra("nutriologoId");
        if (nutriologoId == null) {
            Toast.makeText(this, "Error: ID de solicitud no encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Cargar datos y fotos
        cargarDatosNutriologo();
        cargarImagenes();
        configurarBotones();
    }

    private void inicializarFirebase() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
    }

    private void configurarActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Detalles de la Solicitud");
        }
    }

    private void configurarBotones() {
        binding.btnAprobar.setOnClickListener(v -> aprobarSolicitud());
        binding.btnRechazar.setOnClickListener(v -> rechazarSolicitud());
    }

    private void cargarDatosNutriologo() {
        db.collection("notificaciones_admin")
                .document(nutriologoId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        mostrarDatosNutriologo(document);
                    } else {
                        Toast.makeText(this, "No se encontró la información del nutriólogo",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al cargar datos: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void cargarImagenes() {
        cargarImagen("identificacion.jpg", binding.imgIdentificacion);
        cargarImagen("selfie.jpg", binding.imgSelfie);
    }

    private void cargarImagen(String nombreArchivo, ImageView imageView) {
        // La ruta debe coincidir con cómo se guardó inicialmente en la solicitud
        StorageReference imagenRef = storageRef.child("verificacion")
                .child(nutriologoId)
                .child("ceb3a67b-fe68-") // Esta parte la vi en tu log anterior
                .child(nombreArchivo);

        Log.d("ImagenDebug", "Intentando cargar imagen desde: " + imagenRef.getPath());

        imagenRef.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    Log.d("ImagenDebug", "URL obtenida: " + uri.toString());
                    Glide.with(this)
                            .load(uri)
                            .placeholder(R.drawable.ic_add_photo)
                            .error(R.drawable.ic_add_photo)
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                            Target<Drawable> target, boolean isFirstResource) {
                                    Log.e("ImagenDebug", "Error cargando imagen: " + e.getMessage());
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model,
                                                               Target<Drawable> target, DataSource dataSource,
                                                               boolean isFirstResource) {
                                    Log.d("ImagenDebug", "Imagen cargada exitosamente");
                                    return false;
                                }
                            })
                            .into(imageView);
                })
                .addOnFailureListener(e -> {
                    Log.e("ImagenDebug", "Error cargando " + nombreArchivo + ": " + e.getMessage());
                    Log.e("ImagenDebug", "Ruta intentada: " + imagenRef.getPath());
                    imageView.setImageResource(R.drawable.ic_add_photo);
                });
    }

    private void mostrarDatosNutriologo(DocumentSnapshot document) {
        // Actualizado para usar los IDs correctos del layout
        binding.txtNombre.setText(document.getString("nombre"));
        binding.txtCorreo.setText(document.getString("correo"));
        binding.txtCedula.setText(document.getString("numeroCedula"));
    }

    private void aprobarSolicitud() {
        db.collection("notificaciones_admin")  // Cambiar a la colección correcta
                .document(nutriologoId)
                .update("estado", "aprobado")  // Usar "aprobado" en lugar de cualquier otro estado
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Solicitud aprobada", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al aprobar: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void rechazarSolicitud() {
        new AlertDialog.Builder(this)
                .setTitle("Rechazar solicitud")
                .setMessage("¿Está seguro de rechazar esta solicitud?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    db.collection("solicitudes_nutriologos")
                            .document(nutriologoId)
                            .update("estado", "rechazado")
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Solicitud rechazada", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error al rechazar: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
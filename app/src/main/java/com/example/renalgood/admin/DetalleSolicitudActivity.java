package com.example.renalgood.admin;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
        inicializarFirebase();
        configurarActionBar();
        nutriologoId = getIntent().getStringExtra("nutriologoId");
        if (nutriologoId == null) {
            Toast.makeText(this, "Error: ID de solicitud no encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        cargarDatosNutriologo();
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
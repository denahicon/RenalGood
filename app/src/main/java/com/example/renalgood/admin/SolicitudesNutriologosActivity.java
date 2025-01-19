package com.example.renalgood.admin;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.example.renalgood.R;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.example.renalgood.admin.EmailUtils;
import com.example.renalgood.admin.ImageLoadUtils;

public class SolicitudesNutriologosActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private SolicitudesAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvNoSolicitudes;
    private List<NotificacionAdmin> solicitudes;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseStorage storage;
    private ImageView ivHome, ivCedulas, ivEmail, ivAddRecipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solicitudes_nutriologos);

        inicializarComponentes();
        cargarSolicitudes();
        inicializarVistas();
        setupClickListeners();
    }

    private void setupClickListeners() {
        ivHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminActivity.class);
            startActivity(intent);
            finish();
        });

        ivCedulas.setImageResource(R.drawable.cedulas);
        ivCedulas.setColorFilter(getResources().getColor(R.color.red));

        ivEmail.setOnClickListener(v -> {
            Intent intent = new Intent(this, BuzonAdminActivity.class);
            startActivity(intent);
            finish();
        });

        ivAddRecipe.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminRecipeActivity.class);
            startActivity(intent);
        });
    }

    private void inicializarComponentes() {
        inicializarVistas();
        inicializarFirebase();
        configurarRecyclerView();
    }

    private void inicializarVistas() {
        recyclerView = findViewById(R.id.recyclerViewSolicitudes);
        progressBar = findViewById(R.id.progressBar);
        tvNoSolicitudes = findViewById(R.id.tvNoSolicitudes);
        ivHome = findViewById(R.id.ivHome);
        ivCedulas = findViewById(R.id.ivCedulas);
        ivEmail = findViewById(R.id.ivEmail);
        ivAddRecipe = findViewById(R.id.ivAddRecipe);
    }

    private void inicializarFirebase() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        solicitudes = new ArrayList<>();
    }

    private void configurarRecyclerView() {
        adapter = new SolicitudesAdapter(this, solicitudes, new SolicitudesAdapter.OnSolicitudClickListener() {
            @Override
            public void onVerDetallesClick(NotificacionAdmin solicitud) {
                mostrarDetallesSolicitud(solicitud);
            }

            @Override
            public void onAprobarClick(NotificacionAdmin solicitud) {
                aprobarSolicitud(solicitud);
            }

            @Override
            public void onRechazarClick(NotificacionAdmin solicitud) {
                mostrarDialogoRechazo(solicitud);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void cargarSolicitudes() {
        progressBar.setVisibility(View.VISIBLE);
        Log.d("SolicitudesDebug", "Iniciando carga de solicitudes");

        db.collection("notificaciones_admin")
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(this::procesarSolicitudes)
                .addOnFailureListener(this::manejarErrorCarga);
    }

    private void procesarSolicitudes(QuerySnapshot queryDocumentSnapshots) {
        progressBar.setVisibility(View.GONE);
        solicitudes.clear();
        Log.d("SolicitudesDebug", "Consulta exitosa. Documentos: " + queryDocumentSnapshots.size());

        for (DocumentSnapshot document : queryDocumentSnapshots) {
            try {
                NotificacionAdmin notificacion = document.toObject(NotificacionAdmin.class);
                if (notificacion != null) {
                    notificacion.setId(document.getId());
                    solicitudes.add(notificacion);
                    Log.d("SolicitudesDebug", "Notificación agregada: " + notificacion.getNombre());
                }
            } catch (Exception e) {
                Log.e("SolicitudesDebug", "Error procesando documento: " + e.getMessage());
            }
        }

        actualizarVistasSolicitudes();
    }

    private void actualizarVistasSolicitudes() {
        tvNoSolicitudes.setVisibility(solicitudes.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
    }

    private void manejarErrorCarga(Exception e) {
        progressBar.setVisibility(View.GONE);
        Log.e("SolicitudesDebug", "Error cargando solicitudes: " + e.getMessage());
        Toast.makeText(this, "Error al cargar solicitudes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }

    private void mostrarDetallesSolicitud(NotificacionAdmin solicitud) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_detalles_solicitud, null);

        TextView tvDetalles = dialogView.findViewById(R.id.tv_detalles);
        ImageView ivIdentificacion = dialogView.findViewById(R.id.iv_identificacion);
        ImageView ivSelfie = dialogView.findViewById(R.id.iv_selfie);

        tvDetalles.setText(construirTextoDetalles(solicitud));

        // Cargar imágenes
        if (solicitud.getIdentificacionUrl() != null) {
            Glide.with(this)
                    .load(solicitud.getIdentificacionUrl())
                    .placeholder(R.drawable.ic_add_photo)
                    .error(R.drawable.ic_add_photo)
                    .into(ivIdentificacion);
        }

        if (solicitud.getSelfieUrl() != null) {
            Glide.with(this)
                    .load(solicitud.getSelfieUrl())
                    .placeholder(R.drawable.ic_add_photo)
                    .error(R.drawable.ic_add_photo)
                    .into(ivSelfie);
        }

        new AlertDialog.Builder(this)
                .setTitle("Detalles del Nutriólogo")
                .setView(dialogView)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private String construirTextoDetalles(NotificacionAdmin solicitud) {
        return "Nombre: " + solicitud.getNombre() + "\n" +
                "Cédula: " + solicitud.getNumeroCedula() + "\n" +
                "Universidad: " + solicitud.getUniversidad() + "\n" +
                "Año Graduación: " + solicitud.getAnoGraduacion() + "\n" +
                "Especialización: " + solicitud.getAreaEspecializacion() + "\n" +
                "Experiencia: " + solicitud.getAnosExperiencia() + " años\n" +
                "Dirección Clínica: " + solicitud.getDireccionClinica() + "\n" +
                "Correo: " + solicitud.getCorreo() + "\n\n" +
                "Mensaje: " + solicitud.getMensaje();
    }

    private void rechazarSolicitudConMotivo(NotificacionAdmin solicitud, String motivo) {
        if (solicitud == null || solicitud.getId() == null) {
            Toast.makeText(this, "Error: Solicitud inválida", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        db.collection("notificaciones_admin")
                .document(solicitud.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Usar EmailUtils en lugar de enviarCorreoRechazo
                    String asunto = "Solicitud de Registro Rechazada - RenalGood";
                    String mensaje = "Lo sentimos, tu solicitud ha sido rechazada por el siguiente motivo:\n\n" +
                            motivo + "\n\n" +
                            "Puedes intentar registrarte nuevamente corrigiendo los puntos mencionados.";
                    EmailUtils.enviarCorreo(this, solicitud.getCorreo(), asunto, mensaje);

                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Solicitud rechazada", Toast.LENGTH_SHORT).show();
                    cargarSolicitudes();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error al rechazar: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void cargarImagenes(NotificacionAdmin solicitud, ImageView ivIdentificacion, ImageView ivSelfie) {
        if (solicitud.getIdentificacionPath() != null) {
            // Usar ImageLoadUtils en lugar de cargarImagen
            ImageLoadUtils.cargarImagenDesdeStorage(
                    this,  // context
                    solicitud.getIdentificacionPath(),
                    ivIdentificacion,
                    R.drawable.ic_add_photo  // placeholder
            );
        }
        if (solicitud.getSelfiePath() != null) {
            ImageLoadUtils.cargarImagenDesdeStorage(
                    this,  // context
                    solicitud.getSelfiePath(),
                    ivSelfie,
                    R.drawable.ic_add_photo  // placeholder
            );
        }
    }

    private void mostrarDialogoRechazo(NotificacionAdmin solicitud) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_rechazo_solicitud, null);
        EditText etMotivoRechazo = dialogView.findViewById(R.id.etMotivoRechazo);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Rechazar Solicitud")
                .setView(dialogView)
                .setPositiveButton("Rechazar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String motivo = etMotivoRechazo.getText().toString().trim();
                if (motivo.isEmpty()) {
                    etMotivoRechazo.setError("Por favor ingrese un motivo");
                    return;
                }
                dialog.dismiss();
                rechazarSolicitudConMotivo(solicitud, motivo);
            });
        });

        dialog.show();
    }

    private void aprobarSolicitud(NotificacionAdmin solicitud) {
        progressBar.setVisibility(View.VISIBLE);

        crearUsuarioYEnviarEmail(solicitud)
                .addOnSuccessListener(uid -> guardarDatosNutriologo(uid, solicitud))
                .addOnFailureListener(this::manejarError);
    }

    private Task<String> crearUsuarioYEnviarEmail(NotificacionAdmin solicitud) {
        return auth.createUserWithEmailAndPassword(solicitud.getCorreo(), "temp" + System.currentTimeMillis())
                .continueWith(task -> {
                    String uid = task.getResult().getUser().getUid();
                    auth.sendPasswordResetEmail(solicitud.getCorreo());
                    return uid;
                });
    }

    private void guardarDatosNutriologo(String uid, NotificacionAdmin solicitud) {
        Map<String, Object> nutriologoData = solicitud.toNutriologo();
        nutriologoData.put("id", uid);
        nutriologoData.put("fechaVerificacion", FieldValue.serverTimestamp());

        db.collection("nutriologos")
                .document(uid)
                .set(nutriologoData)
                .addOnSuccessListener(unused -> eliminarSolicitudYNotificar(solicitud))
                .addOnFailureListener(this::manejarError);
    }

    private void eliminarSolicitudYNotificar(NotificacionAdmin solicitud) {
        db.collection("notificaciones_admin")
                .document(solicitud.getId())
                .delete()
                .addOnSuccessListener(unused -> {
                    progressBar.setVisibility(View.GONE);
                    EmailUtils.enviarCorreoSolicitud(this, solicitud.getCorreo(), true);
                    Toast.makeText(this, "Nutriólogo aprobado exitosamente", Toast.LENGTH_SHORT).show();
                    cargarSolicitudes();
                })
                .addOnFailureListener(this::manejarError);
    }

    private void manejarError(Exception e) {
        progressBar.setVisibility(View.GONE);
        Log.e("Solicitudes", "Error: " + e.getMessage());
        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }
}
package com.example.renalgood.admin;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private void aprobarSolicitud(NotificacionAdmin solicitud) {
        progressBar.setVisibility(View.VISIBLE);

        // Agregar log para verificar el ID de la solicitud
        Log.d("Aprobacion", "ID de solicitud: " + solicitud.getId());

        // Primero, obtener la URL de la imagen de perfil
        String basePath = "solicitudes/" + solicitud.getId() + "/perfil.jpg";
        StorageReference profileRef = storage.getReference().child(basePath);

        Log.d("Aprobacion", "Intentando obtener imagen desde: " + basePath);

        auth.createUserWithEmailAndPassword(solicitud.getCorreo(), "temp" + System.currentTimeMillis())
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    Log.d("Aprobacion", "Usuario creado con UID: " + uid);

                    // Enviar email de restablecimiento de contraseña
                    auth.sendPasswordResetEmail(solicitud.getCorreo());

                    // Obtener URL de la imagen de perfil
                    profileRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String photoUrl = uri.toString();
                                Log.d("Aprobacion", "URL de foto obtenida: " + photoUrl);

                                Map<String, Object> nutriologoData = new HashMap<>();
                                nutriologoData.put("id", uid);
                                nutriologoData.put("solicitudId", solicitud.getId());
                                nutriologoData.put("nombre", solicitud.getNombre());
                                nutriologoData.put("numeroCedula", solicitud.getNumeroCedula());
                                nutriologoData.put("universidad", solicitud.getUniversidad());
                                nutriologoData.put("anoGraduacion", solicitud.getAnoGraduacion());
                                nutriologoData.put("areaEspecializacion", solicitud.getAreaEspecializacion());
                                nutriologoData.put("anosExperiencia", solicitud.getAnosExperiencia());
                                nutriologoData.put("direccionClinica", solicitud.getDireccionClinica());
                                nutriologoData.put("correo", solicitud.getCorreo());
                                nutriologoData.put("photoUrl", photoUrl);
                                nutriologoData.put("profilePhotoPath", basePath);
                                nutriologoData.put("estado", "aprobado");
                                nutriologoData.put("verificado", true);
                                nutriologoData.put("fechaVerificacion", FieldValue.serverTimestamp());

                                // Guardar datos en Firestore
                                db.collection("nutriologos")
                                        .document(uid)
                                        .set(nutriologoData)
                                        .addOnSuccessListener(aVoid -> {
                                            // Verificar que los datos se guardaron correctamente
                                            db.collection("nutriologos")
                                                    .document(uid)
                                                    .get()
                                                    .addOnSuccessListener(docSnapshot -> {
                                                        Log.d("Aprobacion", "Datos guardados: " + docSnapshot.getData());
                                                    });

                                            // Eliminar la solicitud original
                                            db.collection("notificaciones_admin")
                                                    .document(solicitud.getId())
                                                    .delete()
                                                    .addOnSuccessListener(unused1 -> {
                                                        progressBar.setVisibility(View.GONE);
                                                        enviarCorreoAprobacion(solicitud.getCorreo());
                                                        Toast.makeText(this, "Nutriólogo aprobado exitosamente",
                                                                Toast.LENGTH_SHORT).show();
                                                        cargarSolicitudes();
                                                    });
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("Aprobacion", "Error guardando datos: " + e.getMessage());
                                            progressBar.setVisibility(View.GONE);
                                            Toast.makeText(this, "Error guardando datos: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Aprobacion", "Error obteniendo URL de imagen: " + e.getMessage());
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Error procesando imagen: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("Aprobacion", "Error creando usuario: " + e.getMessage());
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error al aprobar: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void enviarCorreoAprobacion(String correoDestinatario) {
        String asunto = "¡Bienvenido a RenalGood - Cuenta Aprobada!";
        String mensaje = "Tu cuenta ha sido aprobada. Recibirás un correo adicional para establecer " +
                "tu contraseña.\n\nUna vez que hayas establecido tu contraseña, podrás iniciar " +
                "sesión en la aplicación y comenzar a utilizar todas las funcionalidades.\n\n" +
                "Gracias por unirte a RenalGood.";

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + correoDestinatario));
        intent.putExtra(Intent.EXTRA_SUBJECT, asunto);
        intent.putExtra(Intent.EXTRA_TEXT, mensaje);

        try {
            startActivity(Intent.createChooser(intent, "Enviar correo de bienvenida"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No hay aplicaciones de correo instaladas",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void enviarEmailRestablecimientoYGuardarDatos(String uid, NotificacionAdmin solicitud) {
        auth.sendPasswordResetEmail(solicitud.getCorreo())
                .addOnSuccessListener(aVoid -> guardarDatosNutriologo(uid, solicitud))
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void guardarDatosNutriologo(String uid, NotificacionAdmin solicitud) {
        Map<String, Object> nutriologoData = solicitud.toNutriologo();
        nutriologoData.put("id", uid);
        nutriologoData.put("estado", "aprobado");
        nutriologoData.put("verificado", true);

        db.collection("nutriologos")
                .document(uid)
                .set(nutriologoData)
                .addOnSuccessListener(aVoid -> eliminarSolicitudYNotificar(solicitud))
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error al guardar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void eliminarSolicitudYNotificar(NotificacionAdmin solicitud) {
        db.collection("notificaciones_admin")
                .document(solicitud.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    enviarCorreoRespuesta(solicitud.getCorreo(), true);
                    Toast.makeText(this, "Solicitud aprobada correctamente", Toast.LENGTH_SHORT).show();
                    cargarSolicitudes();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error al eliminar la solicitud", Toast.LENGTH_SHORT).show();
                });
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

    private void configurarVistaDetalles(View dialogView, NotificacionAdmin solicitud) {
        TextView tvDetalles = dialogView.findViewById(R.id.tv_detalles);
        ImageView ivIdentificacion = dialogView.findViewById(R.id.iv_identificacion);
        ImageView ivSelfie = dialogView.findViewById(R.id.iv_selfie);

        tvDetalles.setText(construirTextoDetalles(solicitud));
        cargarImagenes(solicitud, ivIdentificacion, ivSelfie);
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

    private void cargarImagenes(NotificacionAdmin solicitud, ImageView ivIdentificacion, ImageView ivSelfie) {
        if (solicitud.getIdentificacionPath() != null) {
            cargarImagen(solicitud.getIdentificacionPath(), ivIdentificacion);
        }
        if (solicitud.getSelfiePath() != null) {
            cargarImagen(solicitud.getSelfiePath(), ivSelfie);
        }
    }

    private void cargarImagen(String path, ImageView imageView) {
        storage.getReference().child(path).getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    Glide.with(this)
                            .load(uri)
                            .placeholder(R.drawable.ic_add_photo)
                            .error(R.drawable.ic_add_photo)
                            .into(imageView);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al cargar imagen", Toast.LENGTH_SHORT).show();
                });
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
                    enviarCorreoRechazo(solicitud.getCorreo(), motivo);
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

    private void enviarCorreoRechazo(String correoDestinatario, String motivo) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{correoDestinatario});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Solicitud de Registro Rechazada - RenalGood");
        intent.putExtra(Intent.EXTRA_TEXT,
                "Lo sentimos, tu solicitud ha sido rechazada por el siguiente motivo:\n\n" +
                        motivo + "\n\n" +
                        "Puedes intentar registrarte nuevamente corrigiendo los puntos mencionados.");

        try {
            startActivity(Intent.createChooser(intent, "Enviar correo..."));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No hay aplicaciones de correo instaladas",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void enviarCorreoRespuesta(String correoDestinatario, boolean aprobada) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{correoDestinatario});
        configurarContenidoCorreo(intent, aprobada);

        try {
            startActivity(Intent.createChooser(intent, "Enviar correo..."));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No hay aplicaciones de correo instaladas.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void configurarContenidoCorreo(Intent intent, boolean aprobada) {
        if (aprobada) {
            intent.putExtra(Intent.EXTRA_SUBJECT, "Solicitud Aprobada - RenalGood");
            intent.putExtra(Intent.EXTRA_TEXT,
                    "Tu solicitud ha sido aprobada. Recibirás un correo adicional para establecer tu contraseña. " +
                            "Una vez que hayas establecido tu contraseña, podrás iniciar sesión en la aplicación.");
        } else {
            intent.putExtra(Intent.EXTRA_SUBJECT, "Solicitud Rechazada - RenalGood");
            intent.putExtra(Intent.EXTRA_TEXT,
                    "Lo sentimos, tu solicitud ha sido rechazada. " +
                            "Puedes intentar registrarte nuevamente corrigiendo la información proporcionada.");
        }
    }
}
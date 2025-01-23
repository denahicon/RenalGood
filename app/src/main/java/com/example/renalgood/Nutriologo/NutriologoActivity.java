package com.example.renalgood.Nutriologo;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.renalgood.CitasNutriologo.CitasActivity;
import com.example.renalgood.MainActivity;
import com.example.renalgood.R;
import com.example.renalgood.mensaje.MensajeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class NutriologoActivity extends AppCompatActivity {
    private static final String TAG = "NutriologoActivity";
    private TextView tvNombre, tvAreaEspecializacion, tvAnosExperiencia;
    private TextView tvDireccionClinica, tvCorreo, tvUniversidad;
    private CircleImageView profileImage;
    private ImageView ivHome, ivMensaje, ivCalendario, ivPacientesVinculados, ivCarta;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String universidad;
    private NavigationHelper navigationHelper;
    private FirebaseAuth auth;
    private String userId;
    private boolean isActivityActive = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nutriologo);

        isActivityActive = true;
        auth = FirebaseAuth.getInstance();
        userId = auth.getCurrentUser().getUid();
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        inicializarVistas();
        setupNavigation();
        verificarNotificaciones();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mostrarDatosIntent(extras);
        } else {
            cargarDatosFirebase();
        }
    }

    private void verificarNotificaciones() {
        if (!isActivityActive) return;

        String userId = auth.getCurrentUser().getUid();

        db.collection("notificaciones")
                .whereEqualTo("userId", userId)
                .whereEqualTo("leida", false)
                .whereEqualTo("tipoUsuario", "nutriologo")
                .addSnapshotListener((value, error) -> {
                    if (!isActivityActive) return;

                    if (error != null) {
                        Log.e(TAG, "Error al escuchar notificaciones", error);
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        mostrarNotificaciones(value.getDocuments());
                    }
                });
    }

    private void mostrarNotificaciones(List<DocumentSnapshot> notificaciones) {
        if (!isActivityActive) return;

        runOnUiThread(() -> {
            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialog_notificacion);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            TextView mensajeNotificacion = dialog.findViewById(R.id.mensajeNotificacion);
            Button btnOk = dialog.findViewById(R.id.btnOk);

            // Obtener y mostrar el mensaje completo
            DocumentSnapshot doc = notificaciones.get(0);
            String mensaje = doc.getString("mensaje");
            if (mensaje == null || mensaje.isEmpty()) {
                String tipo = doc.getString("tipo");
                mensaje = "Tu " + (tipo != null ? tipo : "solicitud") + " ha sido atendida por el administrador";
            }
            mensajeNotificacion.setText(mensaje);

            btnOk.setOnClickListener(v -> {
                // Marcar todas las notificaciones como leídas
                for (DocumentSnapshot notification : notificaciones) {
                    notification.getReference().update("leida", true);
                }
                dialog.dismiss();
            });

            // Configurar el tamaño y estilo del diálogo
            Window window = dialog.getWindow();
            if (window != null) {
                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                layoutParams.copyFrom(window.getAttributes());
                layoutParams.width = (int)(getResources().getDisplayMetrics().widthPixels * 0.9);
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
                window.setAttributes(layoutParams);
            }

            // Mostrar el diálogo si la actividad está activa
            if (!isFinishing() && !isDestroyed()) {
                dialog.show();
            }
        });
    }

    private void inicializarVistas() {
        try {
            tvNombre = findViewById(R.id.tv_nombre);
            tvAreaEspecializacion = findViewById(R.id.tv_area_especializacion);
            tvAnosExperiencia = findViewById(R.id.tv_anos_experiencia);
            tvDireccionClinica = findViewById(R.id.tv_direccion_clinica);
            tvCorreo = findViewById(R.id.tv_correo);
            tvUniversidad = findViewById(R.id.tv_universidad);
            profileImage = findViewById(R.id.profileImage);
            ivHome = findViewById(R.id.ivHome);
            ivMensaje = findViewById(R.id.ivMensaje);
            ivCalendario = findViewById(R.id.ivCalendario);
            ivPacientesVinculados = findViewById(R.id.group_2811039);
            ivCarta = findViewById(R.id.ivCarta);

            validateViews();
        } catch (Exception e) {
            Log.e(TAG, "Error al inicializar las vistas", e);
            Toast.makeText(this, "Error al inicializar la interfaz", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupNavigation() {
        navigationHelper = new NavigationHelper(
                this, ivHome, ivMensaje, ivCalendario, ivPacientesVinculados, ivCarta
        );
        navigationHelper.setupNavigation("home");
    }

    private void validateViews() throws Exception {
        if (tvNombre == null || tvAreaEspecializacion == null || tvAnosExperiencia == null ||
                tvDireccionClinica == null || tvCorreo == null || tvUniversidad == null ||
                profileImage == null || ivHome == null || ivMensaje == null ||
                ivCalendario == null || ivPacientesVinculados == null || ivCarta == null) {
            throw new Exception("Error al inicializar las vistas");
        }
    }

    private void mostrarDatosIntent(Bundle extras) {
        Log.d(TAG, "Mostrando datos desde Intent");
        try {
            String nombre = extras.getString("nombre", "");
            String areaEsp = extras.getString("areaEspecializacion", "");
            String anosExp = extras.getString("anosExperiencia", "");
            String direccion = extras.getString("direccionClinica", "");
            String correo = extras.getString("correo", "");
            String photoUrl = extras.getString("photoUrl", "");
            universidad = extras.getString("universidad", "");
            actualizarUI(nombre, areaEsp, anosExp, direccion, correo, photoUrl);
        } catch (Exception e) {
            Log.e(TAG, "Error al mostrar datos del Intent", e);
            Toast.makeText(this, "Error al cargar los datos", Toast.LENGTH_SHORT).show();
        }
    }

    private void cargarDatosFirebase() {
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "Error: No hay usuario autenticado");
            Toast.makeText(this, "Error: No hay usuario autenticado", Toast.LENGTH_SHORT).show();
            irALogin();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        db.collection("nutriologos")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        procesarDocumentoNutriologo(documentSnapshot);
                    } else {
                        Log.e(TAG, "El documento no existe");
                        Toast.makeText(this, "Error: No se encontraron datos", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar datos", e);
                    Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
                });
    }

    private void procesarDocumentoNutriologo(DocumentSnapshot documentSnapshot) {
        try {
            if (documentSnapshot.exists()) {
                String nombre = documentSnapshot.getString("nombre");
                String areaEsp = documentSnapshot.getString("areaEspecializacion");
                String anosExp = documentSnapshot.getString("anosExperiencia");
                String direccion = documentSnapshot.getString("direccionClinica");
                String correo = documentSnapshot.getString("correo");
                String photoUrl = documentSnapshot.getString("photoUrl");
                universidad = documentSnapshot.getString("universidad");

                actualizarUI(nombre, areaEsp, anosExp, direccion, correo, photoUrl);
            } else {
                Log.e(TAG, "El documento no existe");
                Toast.makeText(this, "Error: No se encontraron datos", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al procesar documento", e);
            Toast.makeText(this, "Error al procesar los datos", Toast.LENGTH_SHORT).show();
        }
    }

    private void actualizarUI(String nombre, String areaEsp, String anosExp,
                              String direccion, String correo, String photoUrl) {
        try {
            tvNombre.setText(nombre);
            tvAreaEspecializacion.setText(areaEsp);
            tvAnosExperiencia.setText(anosExp);
            tvDireccionClinica.setText(direccion);
            tvCorreo.setText(correo);
            tvUniversidad.setText(universidad);

            actualizarImagenPerfil(photoUrl);
        } catch (Exception e) {
            Log.e(TAG, "Error al actualizar UI", e);
        }
    }

    private void actualizarImagenPerfil(String photoUrl) {
        if (photoUrl != null && !photoUrl.isEmpty()) {
            cargarImagenConGlide(photoUrl);
        } else {
            profileImage.setImageResource(R.drawable.ic_camera);
        }
    }

    private void cargarImagenConGlide(String url) {
        Log.d(TAG, "Cargando imagen con URL: " + url);
        Glide.with(this)
                .load(url)
                .placeholder(R.drawable.ic_camera)
                .error(R.drawable.ic_camera)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<Drawable> target, boolean isFirstResource) {
                        Log.e(TAG, "Error cargando imagen con Glide: " + (e != null ? e.getMessage() : "Desconocido"));
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                                                   Target<Drawable> target, DataSource dataSource,
                                                   boolean isFirstResource) {
                        Log.d(TAG, "Imagen cargada exitosamente con Glide");
                        return false;
                    }
                })
                .into(profileImage);
    }

    private void irALogin() {
        try {
            if (mAuth != null) {
                mAuth.signOut();
            }
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error al ir al login: " + e.getMessage(), e);
            finish();
        }
    }
}
package com.example.renalgood.Nutriologo;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.renalgood.MainActivity;
import com.example.renalgood.R;
import com.example.renalgood.mensaje.MensajeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import de.hdodenhof.circleimageview.CircleImageView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class NutriologoActivity extends AppCompatActivity {
    private static final String TAG = "NutriologoActivity";
    private TextView tvNombre, tvAreaEspecializacion, tvAnosExperiencia;
    private TextView tvDireccionClinica, tvCorreo, tvUniversidad;
    private CircleImageView profileImage;
    private ImageView ivHome, ivMensaje, ivCalendario, ivPacientesVinculados, ivCarta;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String universidad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nutriologo);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        inicializarVistas();
        setupNavigationListeners();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mostrarDatosIntent(extras);
        } else {
            cargarDatosFirebase();
        }
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

    private void validateViews() throws Exception {
        if (tvNombre == null || tvAreaEspecializacion == null || tvAnosExperiencia == null ||
                tvDireccionClinica == null || tvCorreo == null || tvUniversidad == null ||
                profileImage == null || ivHome == null || ivMensaje == null ||
                ivCalendario == null || ivPacientesVinculados == null || ivCarta == null) {
            throw new Exception("Error al inicializar las vistas");
        }
    }

    private void setupNavigationListeners() {
        ivHome.setOnClickListener(view -> {
            highlightCurrentIcon(ivHome);
        });

        ivMensaje.setOnClickListener(view -> {
            navigateToActivity(MensajeActivity.class);
            highlightCurrentIcon(ivMensaje);
        });

        ivCalendario.setOnClickListener(view -> {
            navigateToActivity(CitasActivity.class);
            highlightCurrentIcon(ivCalendario);
        });

        ivPacientesVinculados.setOnClickListener(view -> {
            navigateToActivity(PacientesVinculadosActivity.class);
            highlightCurrentIcon(ivPacientesVinculados);
        });

        ivCarta.setOnClickListener(view -> {
            navigateToActivity(BuzonQuejasActivity.class);
            highlightCurrentIcon(ivCarta);
        });
    }

    private void navigateToActivity(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    private void highlightCurrentIcon(ImageView selectedIcon) {
        ivHome.setAlpha(0.5f);
        ivMensaje.setAlpha(0.5f);
        ivCalendario.setAlpha(0.5f);
        ivPacientesVinculados.setAlpha(0.5f);
        ivCarta.setAlpha(0.5f);
        selectedIcon.setAlpha(1.0f);
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
                    }
                    cargarImagen(userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar datos", e);
                    Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
                });
    }

    private void procesarDocumentoNutriologo(DocumentSnapshot documentSnapshot) {
        try {
            if (documentSnapshot != null && documentSnapshot.exists()) {
                String nombre = documentSnapshot.getString("nombre");
                String areaEsp = documentSnapshot.getString("areaEspecializacion");
                String anosExp = documentSnapshot.getString("anosExperiencia");
                String direccion = documentSnapshot.getString("direccionClinica");
                String correo = documentSnapshot.getString("correo");
                String photoUrl = obtenerPhotoUrl(documentSnapshot);
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

    private String obtenerPhotoUrl(DocumentSnapshot documentSnapshot) {
        String photoUrl = documentSnapshot.getString("photoUrl");
        if (photoUrl == null || photoUrl.isEmpty()) {
            photoUrl = documentSnapshot.getString("profilePhotoUrl");
        }
        if (photoUrl == null || photoUrl.isEmpty()) {
            photoUrl = documentSnapshot.getString("profilePhotoPath");
        }
        return photoUrl;
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
        Glide.with(this)
                .load(url)
                .placeholder(R.drawable.ic_camera)
                .error(R.drawable.ic_camera)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<Drawable> target, boolean isFirstResource) {
                        Log.e(TAG, "Error cargando imagen con Glide: " + e.getMessage());
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

    private void cargarImagen(String userId) {
        Log.d(TAG, "Intentando cargar imagen para userId: " + userId);

        db.collection("nutriologos")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Obtener el ID de la solicitud
                        String solicitudId = documentSnapshot.getString("solicitud");
                        Log.d(TAG, "SolicitudId obtenido: " + solicitudId);

                        if (solicitudId != null && !solicitudId.isEmpty()) {
                            // Construir la ruta exacta basada en lo que vemos en Firebase Storage
                            StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                                    .child("solicitudes")
                                    .child(solicitudId)
                                    .child("perfil.jpg");

                            // Intentar obtener la URL de descarga
                            storageRef.getDownloadUrl()
                                    .addOnSuccessListener(uri -> {
                                        Log.d(TAG, "URL de imagen obtenida: " + uri.toString());
                                        // Usar Glide para cargar la imagen
                                        Glide.with(this)
                                                .load(uri)
                                                .placeholder(R.drawable.ic_camera)
                                                .error(R.drawable.ic_camera)
                                                .into(profileImage);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error al obtener URL: " + e.getMessage());
                                        profileImage.setImageResource(R.drawable.ic_camera);
                                    });
                        } else {
                            Log.e(TAG, "No se encontró ID de solicitud");
                            profileImage.setImageResource(R.drawable.ic_camera);
                        }
                    } else {
                        Log.e(TAG, "No se encontró el documento del nutriólogo");
                        profileImage.setImageResource(R.drawable.ic_camera);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al obtener datos del nutriólogo: " + e.getMessage());
                    profileImage.setImageResource(R.drawable.ic_camera);
                });
    }

    private void verificarRutaImagen(String solicitudId) {
        StorageReference folderRef = FirebaseStorage.getInstance().getReference()
                .child("solicitudes")
                .child(solicitudId);

        folderRef.listAll()
                .addOnSuccessListener(listResult -> {
                    for (StorageReference item : listResult.getItems()) {
                        Log.d(TAG, "Archivo encontrado: " + item.getPath());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al listar archivos: " + e.getMessage());
                });
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
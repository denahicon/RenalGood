package com.example.renalgood.auth;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import com.example.renalgood.MainActivity;
import com.example.renalgood.Nutriologo.NutriologoRepository;
import com.example.renalgood.R;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import de.hdodenhof.circleimageview.CircleImageView;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class RegistroNutriologoActivity extends AppCompatActivity {
    private LinearLayout formContainer;
    private TextView tvPregunta;
    private EditText etRespuesta;
    private Button btnSiguiente;
    private CircleImageView profileImage;
    private LinearLayout photosContainer;
    private Button btnTomarPerfil;
    private EditText etMensaje;
    private Button btnEnviarSolicitud;
    private ImageView ivIdentificacion;
    private ImageView ivSelfie;
    private Button btnTomarIdentificacion;
    private Button btnTomarSelfie;
    private LinearLayout photoVerificationContainer;
    private Uri photoUri;
    private int currentStep = 0;
    private Uri identificacionUri;
    private Uri selfieUri;
    private Uri profilePhotoUri;
    private String userId;
    private String nombre = "";
    private String numeroCedula = "";
    private String universidad = "";
    private String anoGraduacion = "";
    private String areaEspecializacion = "";
    private String anosExperiencia = "";
    private String direccionClinica = "";
    private String correo = "";
    private FirebaseAuth auth;
    private NutriologoRepository nutriologoRepository;
    private ProgressBar progressBar;
    private static final int STORAGE_PERMISSION_CODE = 1001;
    private FirebaseManager firebaseManager;

    private final String[] preguntas = {
            "Para comenzar, ingresa tu número de cédula profesional",
            "¿Cuál es tu nombre completo?",
            "¿De qué universidad egresaste?",
            "¿En qué año te graduaste?",
            "¿Cuál es tu área de especialización?",
            "¿Cuántos años de experiencia tienes?",
            "¿Cuál es la dirección de tu clínica?",
            "¿Cuál es tu correo electrónico?",
            "Toma una foto de perfil",
            "Toma una foto de tu identificación oficial",
            "Toma una selfie",
    };

    private final ActivityResultLauncher<Intent> takeIdentificacionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    identificacionUri = photoUri;
                    ivIdentificacion.setImageURI(identificacionUri);
                    btnSiguiente.setEnabled(true);
                }
            }
    );

    private final ActivityResultLauncher<Intent> takeSelfieLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    selfieUri = photoUri;
                    ivSelfie.setImageURI(selfieUri);
                    btnSiguiente.setEnabled(true);
                }
            }
    );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro_nutriologo);
        firebaseManager = FirebaseManager.getInstance();
        inicializarVistas();
        solicitarPermisosAlmacenamiento();
        configurarListeners();
        mostrarPregunta(currentStep);
    }

    private void solicitarPermisosAlmacenamiento() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                }, STORAGE_PERMISSION_CODE);
            }
        }
    }

    private void inicializarVistas() {
        formContainer = findViewById(R.id.form_container);
        photosContainer = findViewById(R.id.photos_container);
        tvPregunta = findViewById(R.id.tv_pregunta);
        etRespuesta = findViewById(R.id.et_respuesta);
        btnSiguiente = findViewById(R.id.btn_siguiente);
        profileImage = findViewById(R.id.profile_image);
        btnTomarPerfil = findViewById(R.id.btn_tomar_perfil);
        etMensaje = findViewById(R.id.et_mensaje);
        btnEnviarSolicitud = findViewById(R.id.btn_enviar_solicitud);
        ivIdentificacion = findViewById(R.id.ivIdentificacion);
        ivSelfie = findViewById(R.id.ivSelfie);
        btnTomarIdentificacion = findViewById(R.id.btnTomarIdentificacion);
        btnTomarSelfie = findViewById(R.id.btnTomarSelfie);
        progressBar = findViewById(R.id.progressBar);
    }

    private void configurarListeners() {
        btnSiguiente.setOnClickListener(v -> siguientePregunta());
        btnTomarPerfil.setOnClickListener(v -> verificarPermisosCamara());
        btnTomarIdentificacion.setOnClickListener(v -> tomarFoto("identificacion"));
        btnTomarSelfie.setOnClickListener(v -> tomarFoto("selfie"));
        btnEnviarSolicitud.setOnClickListener(v -> enviarSolicitud());
    }

    private void enviarSolicitud() {
        if (!validarFormulario()) return;

        String solicitudId = UUID.randomUUID().toString();
        ProgressDialog progressDialog = mostrarProgressDialog("Enviando solicitud...");
        String baseFolder = "solicitudes/" + solicitudId + "/";

        StorageReference storageRef = firebaseManager.getStorage().getReference();
        StorageReference identRef = storageRef.child(baseFolder + "identificacion.jpg");
        StorageReference selfieRef = storageRef.child(baseFolder + "selfie.jpg");
        StorageReference profileRef = storageRef.child(baseFolder + "perfil.jpg");

        Map<String, Object> solicitudData = new HashMap<>();
        solicitudData.put("id", solicitudId);
        solicitudData.put("nutriologoId", solicitudId);
        solicitudData.put("nombre", nombre);
        solicitudData.put("numeroCedula", numeroCedula);
        solicitudData.put("universidad", universidad);
        solicitudData.put("anoGraduacion", anoGraduacion);
        solicitudData.put("areaEspecializacion", areaEspecializacion);
        solicitudData.put("anosExperiencia", anosExperiencia);
        solicitudData.put("direccionClinica", direccionClinica);
        solicitudData.put("correo", correo);
        solicitudData.put("mensaje", etMensaje.getText().toString().trim());
        solicitudData.put("fecha", FieldValue.serverTimestamp());
        solicitudData.put("estado", "pendiente");
        solicitudData.put("leida", false);
        solicitudData.put("tipo", "NUEVA_SOLICITUD_NUTRIOLOGO");

        // Subir las imágenes
        uploadImage(identificacionUri, identRef)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw Objects.requireNonNull(task.getException());
                    solicitudData.put("identificacionPath", identRef.getPath());
                    solicitudData.put("identificacionUrl", task.getResult().toString());
                    return uploadImage(selfieUri, selfieRef);
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw Objects.requireNonNull(task.getException());
                    solicitudData.put("selfiePath", selfieRef.getPath());
                    solicitudData.put("selfieUrl", task.getResult().toString());
                    return uploadImage(profilePhotoUri, profileRef);
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw Objects.requireNonNull(task.getException());
                    solicitudData.put("profilePhotoPath", profileRef.getPath());
                    solicitudData.put("photoUrl", task.getResult().toString());
                    return firebaseManager.getDb()
                            .collection("notificaciones_admin")
                            .document(solicitudId)
                            .set(solicitudData);
                })
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    enviarCorreoDirecto(correo, nombre, solicitudData.get("mensaje").toString());
                    mostrarDialogo("Solicitud Enviada",
                            "Tu solicitud ha sido enviada al administrador para verificación. " +
                                    "Recibirás un correo cuando tu solicitud sea revisada.", true);
                })
                .addOnFailureListener(e -> {
                    Log.e("Registro", "Error al guardar solicitud: " + e.getMessage());
                    progressDialog.dismiss();
                    mostrarError("Error al enviar solicitud: " + e.getMessage());
                    limpiarImagenesSubidas(solicitudId);
                });
    }

    private void mostrarError(String mensaje) {
        DialogUtils.showErrorDialog(this, "Error", mensaje);
    }

    private void mostrarDialogo(String titulo, String mensaje, boolean finalizarActividad) {
        DialogUtils.showSuccessDialog(this, titulo, mensaje,
                (dialog, which) -> {
                    if (finalizarActividad) {
                        irAMainActivity();
                    }
                });
    }

    private ProgressDialog mostrarProgressDialog(String mensaje) {
        return DialogUtils.showProgressDialog(this, mensaje);
    }

    private void limpiarImagenesSubidas(String solicitudId) {
        StorageReference storageRef = firebaseManager.getStorage().getReference();
        String baseFolder = "solicitudes/" + solicitudId + "/";
        storageRef.child(baseFolder + "identificacion.jpg").delete();
        storageRef.child(baseFolder + "selfie.jpg").delete();
        storageRef.child(baseFolder + "perfil.jpg").delete();
    }

    private boolean validarFormulario() {
        if (!validarImagenes()) {
            Toast.makeText(this, "Por favor complete todas las fotos", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Por favor verifica tu conexión a internet", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private Task<Uri> uploadImage(Uri imageUri, StorageReference ref) {
        if (imageUri == null) {
            return Tasks.forException(new IllegalArgumentException("URI de imagen es null"));
        }
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] data = baos.toByteArray();
            return ref.putBytes(data)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            throw Objects.requireNonNull(task.getException());
                        }
                        return ref.getDownloadUrl();
                    });
        } catch (IOException e) {
            return Tasks.forException(e);
        }
    }

    private void irAMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private boolean validarImagenes() {
        boolean imagesOk = identificacionUri != null &&
                selfieUri != null &&
                profilePhotoUri != null;
        if (!imagesOk) {
            String faltantes = "";
            if (identificacionUri == null) faltantes += "Identificación ";
            if (selfieUri == null) faltantes += "Selfie ";
            if (profilePhotoUri == null) faltantes += "Foto de perfil";

            Toast.makeText(this, "Faltan fotos: " + faltantes, Toast.LENGTH_LONG).show();
        }
        return imagesOk;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private void enviarCorreoDirecto(String correoDestino, String nombreNutriologo, String mensaje) {
        String asunto = "Solicitud de registro como nutriólogo - RenalGood";
        String cuerpoMensaje =
                "Estimado/a " + nombreNutriologo + ",\n\n" +
                        "Hemos recibido tu solicitud de registro como nutriólogo en RenalGood. " +
                        "Tu solicitud está siendo revisada por nuestro equipo administrativo.\n\n" +
                        "Detalles de tu solicitud:\n" +
                        "- Nombre: " + nombreNutriologo + "\n" +
                        "- Correo: " + correoDestino + "\n" +
                        "- Mensaje adjunto: " + mensaje + "\n\n" +
                        "Te notificaremos por este medio cuando tu solicitud haya sido procesada.\n\n" +
                        "Saludos cordiales,\n" +
                        "Equipo RenalGood";
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + correoDestino));
        intent.putExtra(Intent.EXTRA_SUBJECT, asunto);
        intent.putExtra(Intent.EXTRA_TEXT, cuerpoMensaje);
        try {
            startActivity(Intent.createChooser(intent, "Enviar correo de confirmación"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this,
                    "No se encontró una aplicación de correo instalada",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void verificarPermisosCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            mostrarOpcionesFoto();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void mostrarOpcionesFoto() {
        String[] opciones = {"Tomar foto", "Elegir de la galería"};
        new AlertDialog.Builder(this)
                .setTitle("Seleccionar foto")
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) tomarFoto("perfil");
                    else elegirDeGaleria();
                })
                .show();
    }

    private void tomarFoto(String tipo) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (tipo.equals("selfie")) {
            intent.putExtra("android.intent.extras.CAMERA_FACING", 1);
        }
        File photoFile = null;
        try {
            String prefix = "";
            switch(tipo) {
                case "selfie": prefix = "SELF_"; break;
                case "identificacion": prefix = "IDENT_"; break;
                case "perfil": prefix = "PROF_"; break;
            }
            photoFile = File.createTempFile(prefix + System.currentTimeMillis(), ".jpg",
                    getExternalFilesDir(null));
            photoUri = FileProvider.getUriForFile(this,
                    "com.example.renalgood.fileprovider", photoFile);
        } catch (IOException e) {
            Toast.makeText(this, "Error creando archivo", Toast.LENGTH_SHORT).show();
            return;
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        switch(tipo) {
            case "identificacion":
                takeIdentificacionLauncher.launch(intent);
                break;
            case "selfie":
                takeSelfieLauncher.launch(intent);
                break;
            case "perfil":
                takePictureLauncher.launch(intent);
                break;
        }
    }

    private void elegirDeGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) mostrarOpcionesFoto();
            });

    private final ActivityResultLauncher<Intent> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    profilePhotoUri = photoUri;
                    actualizarImagenPerfil(profilePhotoUri);
                    btnSiguiente.setEnabled(true);
                }
            });

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    profilePhotoUri = result.getData().getData();
                    actualizarImagenPerfil(profilePhotoUri);
                }
            });

    private void actualizarImagenPerfil(Uri photoUri) {
        if (photoUri != null) {
            profileImage.setImageURI(photoUri);
        }
    }

    private void mostrarPregunta(int step) {
        tvPregunta.setText(preguntas[step]);
        if (step < preguntas.length - 3) {
            formContainer.setVisibility(View.VISIBLE);
            photosContainer.setVisibility(View.GONE);
            etRespuesta.setText("");
            configurarTipoInput(step);
            btnSiguiente.setText("Siguiente");
        } else {
            formContainer.setVisibility(View.GONE);
            photosContainer.setVisibility(View.VISIBLE);

            if (step == preguntas.length - 1) {
                btnSiguiente.setText("Enviar");
                btnEnviarSolicitud.setVisibility(View.VISIBLE);
            }
        }
    }

    private void configurarTipoInput(int step) {
        switch (step) {
            case 0: // Cédula
                configurarInputNumerico(8);
                break;
            case 5: // Años experiencia
                configurarInputNumerico(null);
                break;
            case 7: // Email
                etRespuesta.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                break;
            default:
                configurarInputTexto();
                break;
        }
    }

    private void configurarInputNumerico(Integer maxLength) {
        etRespuesta.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        if (maxLength != null) {
            etRespuesta.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
        }
    }

    private void configurarInputTexto() {
        etRespuesta.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        etRespuesta.setFilters(new InputFilter[]{});
    }

    private void siguientePregunta() {
        String respuesta = etRespuesta.getText().toString().trim();

        if (!validarRespuestaActual(respuesta)) {
            return;
        }

        guardarRespuesta(respuesta);
        currentStep++;

        if (currentStep == preguntas.length - 1) {
            mostrarCamposAdicionales();
        } else {
            mostrarPregunta(currentStep);
        }
    }

    private boolean validarRespuestaActual(String respuesta) {
        if (respuesta.isEmpty()) {
            Toast.makeText(this, "Por favor, completa el campo", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (currentStep == 7) { // Email
            return ValidationUtils.validateEmail(this, etRespuesta);
        }
        return true;
    }

    private void mostrarCamposAdicionales() {
        etMensaje.setVisibility(View.VISIBLE);
        btnEnviarSolicitud.setVisibility(View.VISIBLE);
        btnSiguiente.setVisibility(View.GONE);
    }

    private void guardarRespuesta(String respuesta) {
        switch (currentStep) {
            case 0: numeroCedula = respuesta; break;
            case 1: nombre = respuesta; break;
            case 2: universidad = respuesta; break;
            case 3: anoGraduacion = respuesta; break;
            case 4: areaEspecializacion = respuesta; break;
            case 5: anosExperiencia = respuesta; break;
            case 6: direccionClinica = respuesta; break;
            case 7: correo = respuesta; break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido
                Toast.makeText(this, "Permiso de almacenamiento concedido", Toast.LENGTH_SHORT).show();
            } else {
                // Permiso denegado
                Toast.makeText(this, "Se requiere permiso de almacenamiento para guardar las fotos", Toast.LENGTH_LONG).show();
            }
        }
    }
}
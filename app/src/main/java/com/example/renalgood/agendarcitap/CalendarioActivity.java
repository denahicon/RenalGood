package com.example.renalgood.agendarcitap;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.renalgood.Chat.ChatActivity;
import com.example.renalgood.ListadeAlimentos.ListadeAlimentosActivity;
import com.example.renalgood.Paciente.BuzonQuejasPaciente;
import com.example.renalgood.Paciente.PacienteActivity;
import com.example.renalgood.R;
import com.example.renalgood.recetas.RecetasActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.content.Intent;
import android.widget.Toast;

public class CalendarioActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;
    private LinearLayout calendarioLayout;
    private TextView mensajeNoVinculado;
    private CalendarView calendarView;
    private TimePicker timePicker;
    private Button btnAgendar;
    private String nutriologoId;
    private ImageView ivHome, ivLupa, ivChef, ivMensaje, ivCarta, ivCalendario;
    private CardView cardEstadoCita;
    private TextView tvFechaHoraCita;
    private TextView tvEstadoCita;
    private Button btnCancelarCita;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendario);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();
        initViews();
        timePicker.setIs24HourView(true);
        verificarVinculacionNutriologo();
        btnAgendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                agendarCita();
            }
        });
        setupNavigationListeners();
        verificarVinculacionNutriologo();
        verificarCitaExistente();
        btnCancelarCita = findViewById(R.id.btnCancelarCita);
        btnCancelarCita.setOnClickListener(v -> mostrarDialogoCancelar());
    }

    private void agendarCita() {
        if (nutriologoId == null || nutriologoId.isEmpty()) {
            Toast.makeText(this, "Error: No hay nutriólogo vinculado", Toast.LENGTH_LONG).show();
            return;
        }

        Date fechaSeleccionada = new Date(calendarView.getDate());

        int hora = timePicker.getHour();
        int minuto = timePicker.getMinute();
        String horaSeleccionada = String.format(Locale.getDefault(), "%02d:%02d", hora, minuto);

        Date hoy = new Date();
        if (fechaSeleccionada.before(hoy) && !esHoy(fechaSeleccionada)) {
            Toast.makeText(this, "Por favor selecciona una fecha futura", Toast.LENGTH_LONG).show();
            return;
        }

        AlertDialog.Builder confirmDialog = new AlertDialog.Builder(this);
        confirmDialog.setTitle("Confirmar Cita");
        confirmDialog.setMessage("¿Deseas agendar una cita para el día " +
                formatearFecha(fechaSeleccionada) +
                " a las " + horaSeleccionada + "?");

        confirmDialog.setPositiveButton("Confirmar", (dialog, which) -> {
            agendarCita(nutriologoId, fechaSeleccionada, horaSeleccionada);
        });

        confirmDialog.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        confirmDialog.show();
    }

    private boolean esHoy(Date fecha) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(fecha);
        cal2.setTime(new Date());
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }

    private String formatearFecha(Date fecha) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(fecha);
    }

    private void initViews() {
        calendarioLayout = findViewById(R.id.calendarioLayout);
        mensajeNoVinculado = findViewById(R.id.mensajeNoVinculado);
        calendarView = findViewById(R.id.calendarView);
        timePicker = findViewById(R.id.timePicker);
        btnAgendar = findViewById(R.id.btnAgendar);
        ivHome = findViewById(R.id.ivHome);
        ivLupa = findViewById(R.id.ivLupa);
        ivChef = findViewById(R.id.ivChef);
        ivMensaje = findViewById(R.id.ivMensaje);
        ivCarta = findViewById(R.id.ivCarta);
        ivCalendario = findViewById(R.id.ivCalendario);
        cardEstadoCita = findViewById(R.id.cardEstadoCita);
        tvFechaHoraCita = findViewById(R.id.tvFechaHoraCita);
        tvEstadoCita = findViewById(R.id.tvEstadoCita);
    }

    private void verificarVinculacionNutriologo() {
        db.collection("pacientes")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("nutriologoId")) {
                        nutriologoId = documentSnapshot.getString("nutriologoId");
                        if (nutriologoId != null && !nutriologoId.isEmpty()) {
                            mostrarCalendario();
                        } else {
                            mostrarMensajeNoVinculado();
                        }
                    } else {
                        mostrarMensajeNoVinculado();
                    }
                })
                .addOnFailureListener(e -> {
                    mostrarMensajeNoVinculado();
                });
    }

    private void verificarCitaExistente() {
        db.collection("citas")
                .whereEqualTo("pacienteId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("CalendarioActivity", "Error al escuchar cambios en la cita", error);
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        DocumentSnapshot doc = value.getDocuments().get(0);
                        if (doc.exists()) {
                            mostrarEstadoCita(doc);
                            deshabilitarControles();
                        }
                    } else {
                        habilitarControles();
                        cardEstadoCita.setVisibility(View.GONE);
                    }
                });
    }

    private void mostrarEstadoCita(DocumentSnapshot doc) {
        try {
            Timestamp timestamp = doc.getTimestamp("fecha");
            String hora = doc.getString("hora");
            String estado = doc.getString("estado");

            if (timestamp == null || hora == null || estado == null) {
                Log.e("CalendarioActivity", "Datos de cita incompletos");
                return;
            }

            runOnUiThread(() -> {
                cardEstadoCita.setVisibility(View.VISIBLE);

                Date fecha = timestamp.toDate();
                String fechaFormateada = formatearFecha(fecha);
                tvFechaHoraCita.setText("Fecha: " + fechaFormateada + "\nHora: " + hora);

                // Verificar si la cita ya pasó
                if (estado.equals("confirmada") && hayCitaPasada(fecha, hora)) {
                    eliminarCitaPasada(doc.getId());
                    return;
                }

                String mensaje;
                int colorFondo;

                switch (estado.toLowerCase()) {
                    case "confirmada":
                        mensaje = "¡Tu cita está confirmada!\nFecha: " + fechaFormateada +
                                "\nHora: " + hora +
                                "\n¿Necesitas cancelarla?";
                        colorFondo = ContextCompat.getColor(this, R.color.green);
                        mostrarOpcionCancelar();
                        deshabilitarControles();
                        break;
                    case "rechazada":
                        mensaje = "Tu cita ha sido rechazada\nPuedes agendar una nueva cita";
                        colorFondo = ContextCompat.getColor(this, R.color.red);
                        habilitarControlesParaNuevaCita();
                        new Handler().postDelayed(() -> eliminarCitaRechazada(doc.getId()), 5000);
                        break;
                    default:
                        mensaje = "Tu cita está pendiente de confirmación";
                        colorFondo = ContextCompat.getColor(this, R.color.orange);
                        deshabilitarControles();
                        break;
                }

                cardEstadoCita.setCardBackgroundColor(colorFondo);
                tvEstadoCita.setText(mensaje);
                tvEstadoCita.setTextColor(Color.WHITE);
            });

        } catch (Exception e) {
            Log.e("CalendarioActivity", "Error al mostrar estado de cita", e);
        }
    }

    private boolean hayCitaPasada(Date fecha, String hora) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date horaDate = sdf.parse(hora);
            Calendar calendarCita = Calendar.getInstance();
            Calendar calendarActual = Calendar.getInstance();

            calendarCita.setTime(fecha);
            calendarCita.set(Calendar.HOUR_OF_DAY, horaDate.getHours());
            calendarCita.set(Calendar.MINUTE, horaDate.getMinutes());

            return calendarCita.before(calendarActual);
        } catch (ParseException e) {
            Log.e("CalendarioActivity", "Error al parsear hora", e);
            return false;
        }
    }

    private void mostrarOpcionCancelar() {
        Button btnCancelar = new Button(this);
        btnCancelar.setText("Cancelar Cita");
        btnCancelar.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.red)));
        btnCancelar.setTextColor(Color.WHITE);

        LinearLayout contenedor = findViewById(R.id.calendarioLayout);
        contenedor.addView(btnCancelar);

        btnCancelar.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Cancelar Cita")
                    .setMessage("¿Estás seguro de que deseas cancelar tu cita? Esta acción no se puede deshacer.")
                    .setPositiveButton("Sí, cancelar", (dialog, which) -> cancelarCita())
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    private void cancelarCita() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("citas")
                .whereEqualTo("pacienteId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        document.getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Cita cancelada exitosamente",
                                            Toast.LENGTH_SHORT).show();
                                    cardEstadoCita.setVisibility(View.GONE);
                                    btnCancelarCita.setVisibility(View.GONE);
                                    habilitarControlesParaNuevaCita();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Error al cancelar la cita",
                                                Toast.LENGTH_SHORT).show()
                                );
                    }
                });
    }

    private void eliminarCitaPasada(String citaId) {
        db.collection("citas")
                .document(citaId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    cardEstadoCita.setVisibility(View.GONE);
                    habilitarControlesParaNuevaCita();
                    Toast.makeText(this, "Tu cita anterior ha finalizado. Puedes agendar una nueva.",
                            Toast.LENGTH_LONG).show();
                });
    }

    private void habilitarControlesParaNuevaCita() {
        calendarView.setEnabled(true);
        timePicker.setEnabled(true);
        btnAgendar.setEnabled(true);
        btnAgendar.setText("Agendar Nueva Cita");
    }

    private void eliminarCitaRechazada(String citaId) {
        db.collection("citas")
                .document(citaId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    cardEstadoCita.setVisibility(View.GONE);
                    Log.d("CalendarioActivity", "Cita rechazada eliminada correctamente");
                })
                .addOnFailureListener(e -> Log.e("CalendarioActivity", "Error al eliminar cita rechazada", e));
    }

    private void deshabilitarControles() {
        calendarView.setEnabled(false);
        timePicker.setEnabled(false);
        btnAgendar.setEnabled(false);
        btnAgendar.setText("Ya tienes una cita programada");
    }

    private void mostrarDialogoCancelar() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cancelar Cita")
                .setMessage("¿Estás seguro de que deseas cancelar tu cita? Esta acción no se puede deshacer.")
                .setPositiveButton("Sí, cancelar", (dialog, which) -> cancelarCita())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void habilitarControles() {
        // Habilitar controles
        calendarView.setEnabled(true);
        timePicker.setEnabled(true);
        btnAgendar.setEnabled(true);
        btnAgendar.setAlpha(1.0f);
        btnAgendar.setText("Agendar Cita");
    }

    private void agendarCita(String nutriologoId, Date fechaSeleccionada, String horaSeleccionada) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String pacienteId = auth.getCurrentUser().getUid();

        db.collection("users").document(pacienteId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String pacienteNombre = documentSnapshot.getString("nombre");
                    if (pacienteNombre == null) pacienteNombre = "Paciente";

                    Map<String, Object> cita = new HashMap<>();
                    cita.put("nutriologoId", nutriologoId);
                    cita.put("pacienteId", pacienteId);
                    cita.put("pacienteNombre", pacienteNombre);
                    cita.put("fecha", fechaSeleccionada);
                    cita.put("hora", horaSeleccionada);
                    cita.put("estado", "pendiente");  // Aseguramos que sea "pendiente"

                    db.collection("citas")
                            .add(cita)
                            .addOnSuccessListener(documentReference -> {
                                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                                builder.setTitle("Cita Agendada")
                                        .setMessage("Tu solicitud de cita ha sido enviada al nutriólogo. " +
                                                "Podrás ver el estado de tu cita en esta pantalla.")
                                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                                        .show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error al agendar la cita: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                });
    }

    private void mostrarCalendario() {
        mensajeNoVinculado.setVisibility(View.GONE);
        calendarioLayout.setVisibility(View.VISIBLE);
    }

    private void mostrarMensajeNoVinculado() {
        mensajeNoVinculado.setVisibility(View.VISIBLE);
        calendarioLayout.setVisibility(View.GONE);
    }

    private void setupNavigationListeners() {
        ivHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, PacienteActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });

        ivLupa.setOnClickListener(v -> {
            Intent intent = new Intent(this, ListadeAlimentosActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });

        ivChef.setOnClickListener(v -> {
            Intent intent = new Intent(this, RecetasActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });

        ivMensaje.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });

        ivCarta.setOnClickListener(v -> {
            Intent intent = new Intent(this, BuzonQuejasPaciente.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });

        ivCalendario.setImageResource(R.drawable.ic_calendar);
        ivCalendario.setColorFilter(getResources().getColor(R.color.pink_strong));
    }
}
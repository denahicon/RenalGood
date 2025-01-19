package com.example.renalgood.agendarcitap;

import android.content.Intent;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
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
import java.util.*;
import androidx.appcompat.app.AlertDialog;

public class CalendarioActivity extends AppCompatActivity {
    private static final String TAG = "CalendarioActivity";
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;
    private String nutriologoId;
    private LinearLayout calendarioLayout;
    private TextView mensajeNoVinculado;
    private CalendarView calendarView;
    private TimePicker timePicker;
    private Button btnAgendar, btnCancelarCita;
    private ImageView ivHome, ivLupa, ivChef, ivMensaje, ivCarta, ivCalendario;
    private CardView cardEstadoCita;
    private TextView tvFechaHoraCita, tvEstadoCita;
    private Date selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendario);

        initializeFirebase();
        initializeViews();
        setupInitialState();
        setupListeners();
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();
    }

    private void initializeViews() {
        calendarioLayout = findViewById(R.id.calendarioLayout);
        mensajeNoVinculado = findViewById(R.id.mensajeNoVinculado);
        calendarView = findViewById(R.id.calendarView);
        timePicker = findViewById(R.id.timePicker);
        btnAgendar = findViewById(R.id.btnAgendar);
        btnCancelarCita = findViewById(R.id.btnCancelarCita);
        cardEstadoCita = findViewById(R.id.cardEstadoCita);
        tvFechaHoraCita = findViewById(R.id.tvFechaHoraCita);
        tvEstadoCita = findViewById(R.id.tvEstadoCita);
        ivHome = findViewById(R.id.ivHome);
        ivLupa = findViewById(R.id.ivLupa);
        ivChef = findViewById(R.id.ivChef);
        ivMensaje = findViewById(R.id.ivMensaje);
        ivCarta = findViewById(R.id.ivCarta);
        ivCalendario = findViewById(R.id.ivCalendario);
        btnAgendar.setOnClickListener(v -> agendarCita());
        btnCancelarCita.setOnClickListener(v -> mostrarDialogoCancelar());
    }

    private void setupInitialState() {
        timePicker.setIs24HourView(true);
        AppointmentValidations.startPeriodicCleanup(this, db);
        setupNavigationListeners();
        verificarVinculacionNutriologo();
        verificarCitasPendientes();

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            selectedDate = calendar.getTime();
        });
    }

    private void setupListeners() {
        btnAgendar.setOnClickListener(v -> agendarCita());
        btnCancelarCita.setOnClickListener(v -> mostrarDialogoCancelar());
        setupNavigationListeners();
    }

    private void agendarCita() {
        if (nutriologoId == null || nutriologoId.isEmpty()) {
            Toast.makeText(this, "Error: No hay nutriólogo vinculado", Toast.LENGTH_LONG).show();
            return;
        }

        if (selectedDate == null) {
            Toast.makeText(this, "Por favor seleccione una fecha", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener la hora seleccionada del TimePicker
        Calendar selectedCalendar = Calendar.getInstance();
        selectedCalendar.setTime(selectedDate);
        selectedCalendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
        selectedCalendar.set(Calendar.MINUTE, timePicker.getMinute());
        selectedCalendar.set(Calendar.SECOND, 0);
        selectedCalendar.set(Calendar.MILLISECOND, 0);

        // Log para debugging
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        Log.d("AgendarCita", "Fecha seleccionada en calendar: " + sdf.format(selectedCalendar.getTime()));

        // Validaciones
        if (!validarFechaHora(selectedCalendar)) return;

        String horaSeleccionada = String.format(Locale.getDefault(), "%02d:%02d",
                timePicker.getHour(), timePicker.getMinute());
        final Date fechaSeleccionada = selectedCalendar.getTime();

        // Verificar disponibilidad
        verificarDisponibilidadYAgendar(fechaSeleccionada, horaSeleccionada);
    }

    private boolean validarFechaHora(Calendar selectedCalendar) {
        Calendar currentCalendar = Calendar.getInstance();

        // Resetear segundos y milisegundos
        selectedCalendar.set(Calendar.SECOND, 0);
        selectedCalendar.set(Calendar.MILLISECOND, 0);
        currentCalendar.set(Calendar.SECOND, 0);
        currentCalendar.set(Calendar.MILLISECOND, 0);

        // Validar fecha pasada
        if (selectedCalendar.before(currentCalendar)) {
            mostrarMensajeError("No se pueden agendar citas para fechas/horas pasadas");
            return false;
        }

        // Validar horario laboral
        int hour = selectedCalendar.get(Calendar.HOUR_OF_DAY);
        int minute = selectedCalendar.get(Calendar.MINUTE);
        if (hour < 9 || hour > 18 || (hour == 18 && minute > 0)) {
            mostrarMensajeError("Por favor seleccione un horario entre 9:00 AM y 6:00 PM");
            return false;
        }

        // Validar fin de semana
        int dayOfWeek = selectedCalendar.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            mostrarMensajeError("No se pueden agendar citas en fin de semana");
            return false;
        }

        return true;
    }

    private void mostrarMensajeError(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }

    private void actualizarEstadoControles(boolean enabled, String buttonText) {
        calendarView.setEnabled(enabled);
        timePicker.setEnabled(enabled);
        btnAgendar.setEnabled(enabled);
        btnAgendar.setText(buttonText);
        btnAgendar.setAlpha(enabled ? 1.0f : 0.5f);
        if (!enabled) {
            btnCancelarCita.setVisibility(View.GONE);
        }
    }

    private void habilitarControles() {
        actualizarEstadoControles(true, "Agendar Cita");
    }

    private void deshabilitarControles() {
        actualizarEstadoControles(false, "Ya tienes una cita programada");
    }

    private void habilitarControlesParaNuevaCita() {
        actualizarEstadoControles(true, "Agendar Nueva Cita");
    }

    private void verificarDisponibilidadYAgendar(Date fechaSeleccionada, String horaSeleccionada) {
        AppointmentValidations.verifyAppointmentAvailability(
                db, fechaSeleccionada, horaSeleccionada, nutriologoId,
                new AppointmentValidations.AppointmentCallback() {
                    @Override
                    public void onSuccess() {
                        mostrarDialogoConfirmacion(fechaSeleccionada, horaSeleccionada);
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(CalendarioActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void crearCita(Date fechaSeleccionada, String horaSeleccionada) {
        Map<String, Object> cita = new HashMap<>();
        cita.put("nutriologoId", nutriologoId);
        cita.put("pacienteId", userId);
        cita.put("fecha", new Timestamp(fechaSeleccionada));  // Asegurarnos que la fecha es correcta
        cita.put("hora", horaSeleccionada);
        cita.put("estado", "pendiente");
        cita.put("fechaCreacion", Timestamp.now());

        Log.d("CrearCita", "Guardando cita para fecha: " +
                new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(fechaSeleccionada));

        db.collection("citas")
                .add(cita)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Cita agendada exitosamente", Toast.LENGTH_SHORT).show();
                    verificarCitasPendientes();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Error al agendar la cita: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void cancelarCita() {
        db.collection("citas")
                .whereEqualTo("pacienteId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Timestamp citaTimestamp = document.getTimestamp("fecha");
                        String horaStr = document.getString("hora");

                        if (citaTimestamp != null && horaStr != null) {
                            if (!AppointmentValidations.canCancelAppointment(citaTimestamp, horaStr)) {
                                Toast.makeText(this,
                                        "No se puede cancelar la cita con menos de 24 horas de anticipación",
                                        Toast.LENGTH_LONG).show();
                                return;
                            }

                            eliminarCita(document.getReference().getId());
                        }
                    }
                });
    }

    private void eliminarCita(String citaId) {
        db.collection("citas")
                .document(citaId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cita cancelada exitosamente", Toast.LENGTH_SHORT).show();
                    cardEstadoCita.setVisibility(View.GONE);
                    habilitarControlesParaNuevaCita();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al cancelar la cita", Toast.LENGTH_SHORT).show());
    }

    private void verificarVinculacionNutriologo() {
        db.collection("vinculaciones")
                .whereEqualTo("pacienteId", userId)
                .whereEqualTo("estado", "activo")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Hay una vinculación activa
                        DocumentSnapshot vinculacion = querySnapshot.getDocuments().get(0);
                        nutriologoId = vinculacion.getString("nutriologoId");
                        if (nutriologoId != null && !nutriologoId.isEmpty()) {
                            mostrarCalendario();
                            return;
                        }
                    }
                    // No hay vinculación activa
                    mostrarMensajeNoVinculado();
                })
                .addOnFailureListener(e -> {
                    Log.e("Calendario", "Error verificando vinculación", e);
                    mostrarMensajeNoVinculado();
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

                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String fechaStr = dateFormat.format(timestamp.toDate());

                tvFechaHoraCita.setText("Fecha: " + fechaStr + "\nHora: " + hora);

                // Si es una cita pendiente, verificar si ha expirado
                if (estado.equals("pendiente") &&
                        AppointmentValidations.isAppointmentConfirmationExpired(timestamp, hora)) {
                    eliminarCitaExpirada(doc.getId());
                    return;
                }

                String mensaje;
                int colorFondo;

                switch (estado.toLowerCase()) {
                    case "confirmada":
                        mensaje = "¡Tu cita está confirmada!\nFecha: " + fechaStr +
                                "\nHora: " + hora;

                        // Mostrar opción de cancelar solo si estamos a más de 24h de la cita
                        if (AppointmentValidations.canCancelAppointment(timestamp, hora)) {
                            mensaje += "\n¿Necesitas cancelarla?";
                            btnCancelarCita.setVisibility(View.VISIBLE);
                        } else {
                            btnCancelarCita.setVisibility(View.GONE);
                        }

                        colorFondo = ContextCompat.getColor(this, R.color.green);
                        break;

                    case "rechazada":
                        mensaje = "Tu cita ha sido rechazada\nPuedes agendar una nueva cita";
                        colorFondo = ContextCompat.getColor(this, R.color.red);
                        btnCancelarCita.setVisibility(View.GONE);
                        break;

                    default: // pendiente
                        mensaje = "Tu cita está pendiente de confirmación";
                        colorFondo = ContextCompat.getColor(this, R.color.orange);
                        btnCancelarCita.setVisibility(View.VISIBLE);
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

    private void mostrarDialogoCancelar() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cancelar Cita")
                .setMessage("¿Estás seguro de que deseas cancelar tu cita? Esta acción no se puede deshacer.")
                .setPositiveButton("Sí, cancelar", (dialog, which) -> cancelarCita())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void verificarCitasPendientes() {
        db.collection("citas")
                .whereEqualTo("pacienteId", userId)
                .whereEqualTo("estado", "pendiente")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Toast.makeText(this,
                                "Ya tienes una cita pendiente",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }else {
                        habilitarControles();
                        cardEstadoCita.setVisibility(View.GONE);
                    }
                });
    }

    private void mostrarDialogoConfirmacion(Date fechaSeleccionada, String horaSeleccionada) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmar Cita");

        String fechaStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                .format(fechaSeleccionada);

        builder.setMessage("¿Deseas agendar una cita para el día " + fechaStr +
                " a las " + horaSeleccionada + "?");

        builder.setPositiveButton("Confirmar", (dialog, which) ->
                crearCita(fechaSeleccionada, horaSeleccionada));

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void mostrarCalendario() {
        if (mensajeNoVinculado != null) {
            mensajeNoVinculado.setVisibility(View.GONE);
        }
        if (calendarioLayout != null) {
            calendarioLayout.setVisibility(View.VISIBLE);
        }
    }

    private void mostrarMensajeNoVinculado() {
        if (mensajeNoVinculado != null) {
            mensajeNoVinculado.setVisibility(View.VISIBLE);
            mensajeNoVinculado.setText("No tienes un nutriólogo asignado. " +
                    "Para agendar una cita, primero debes vincularte con un nutriólogo.");
        }
        if (calendarioLayout != null) {
            calendarioLayout.setVisibility(View.GONE);
        }
    }

    private void eliminarCitaExpirada(String citaId) {
        db.collection("citas")
                .document(citaId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    cardEstadoCita.setVisibility(View.GONE);
                    deshabilitarControles();
                    Toast.makeText(this,
                            "La cita ha expirado. Puedes agendar una nueva.",
                            Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error al eliminar cita expirada", e));
    }
}
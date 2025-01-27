package com.example.renalgood.agendarcitap;

import android.content.DialogInterface;
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
    private Spinner timeSpinner;
    private Button btnAgendar, btnCancelarCita;
    private ImageView ivHome, ivLupa, ivChef, ivMensaje, ivCarta, ivCalendario;
    private CardView cardEstadoCita;
    private TextView tvFechaHoraCita, tvEstadoCita;
    private Date selectedDate;
    private String selectedTimeSlot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendario);

        initializeFirebase();

        if (db != null) {
            AppointmentCleanupService.cleanupPastAppointments(db);
        } else {
            Log.e(TAG, "Error: Firebase no inicializado correctamente");
        }

        initializeViews();
        setupInitialState();
        setupListeners();
    }

    private void initializeFirebase() {
        try {
            db = FirebaseFirestore.getInstance();
            mAuth = FirebaseAuth.getInstance();
            if (mAuth.getCurrentUser() != null) {
                userId = mAuth.getCurrentUser().getUid();
            } else {
                Log.e(TAG, "Error: Usuario no autenticado");
                finish();
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando Firebase", e);
            Toast.makeText(this, "Error al inicializar la aplicación", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        calendarioLayout = findViewById(R.id.calendarioLayout);
        mensajeNoVinculado = findViewById(R.id.mensajeNoVinculado);
        calendarView = findViewById(R.id.calendarView);
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
        btnCancelarCita = findViewById(R.id.btnCancelarCita);
        if (btnCancelarCita != null) {
            btnCancelarCita.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mostrarDialogoCancelar();
                }
            });
            btnCancelarCita.setClickable(true);
            btnCancelarCita.setEnabled(true);
        } else {
            Log.e(TAG, "Error: btnCancelarCita no encontrado");
        }
        timeSpinner = findViewById(R.id.timeSpinner);
    }

    private void setupInitialState() {
        AppointmentTimeSlots.startPeriodicCleanup(this, db);
        setupNavigationListeners();
        verificarVinculacionNutriologo();
        verificarCitasPendientes();
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);

            if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                selectedDate = null;
                Toast.makeText(this, "No se atiende los domingos", Toast.LENGTH_SHORT).show();
                return;
            }

            selectedDate = calendar.getTime();
            updateAvailableTimeSlots(calendar);
        });
    }

    private void verificarDisponibilidadYAgendar(Date fechaSeleccionada, String horaSeleccionada) {
        if (userId == null || nutriologoId == null) {
            Toast.makeText(this, "Error: Información de usuario no disponible", Toast.LENGTH_SHORT).show();
            return;
        }
        if (fechaSeleccionada == null) {
            Toast.makeText(this, "Por favor seleccione una fecha", Toast.LENGTH_SHORT).show();
            return;
        }
        Calendar appointmentCalendar = Calendar.getInstance();
        appointmentCalendar.setTime(fechaSeleccionada);
        String[] horaParts = horaSeleccionada.split(":");
        appointmentCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(horaParts[0]));
        appointmentCalendar.set(Calendar.MINUTE, Integer.parseInt(horaParts[1]));

        if (!AppointmentTimeSlots.isValidAppointmentTime(appointmentCalendar)) {
            Toast.makeText(this, "El horario seleccionado no es válido", Toast.LENGTH_SHORT).show();
            return;
        }

        AppointmentTimeSlots.verifyAppointmentAvailability(
                db,
                fechaSeleccionada,
                horaSeleccionada,
                nutriologoId,
                userId, // Agregamos el userId como nuevo parámetro
                new AppointmentCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> mostrarDialogoConfirmacion(fechaSeleccionada, horaSeleccionada));
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> Toast.makeText(CalendarioActivity.this, message, Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private boolean validarHorarioSeleccionado(Calendar calendar) {
        // Validar hora dentro del rango permitido (9 AM - 6 PM)
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        if (hour < 9 || hour > 18 || (hour == 18 && minute > 0)) {
            Toast.makeText(this, "El horario de atención es de 9:00 AM a 6:00 PM",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validar que no sea fin de semana
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            Toast.makeText(this, "No se pueden agendar citas en fin de semana",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void mostrarDialogoConfirmacion(Date fechaSeleccionada, String horaSeleccionada) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String fechaStr = dateFormat.format(fechaSeleccionada);

        new AlertDialog.Builder(this)
                .setTitle("Confirmar Cita")
                .setMessage("¿Deseas agendar una cita para el día " + fechaStr + " a las " + horaSeleccionada + "?")
                .setPositiveButton("Confirmar", (dialog, which) -> crearCita(fechaSeleccionada, horaSeleccionada))
                .setNegativeButton("Cancelar", null)
                .show();
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

        if (selectedDate == null || selectedTimeSlot == null) {
            Toast.makeText(this, "Por favor seleccione fecha y hora", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar selectedCalendar = Calendar.getInstance();
        selectedCalendar.setTime(selectedDate);

        String[] timeParts = selectedTimeSlot.split(":");
        selectedCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
        selectedCalendar.set(Calendar.MINUTE, 0);

        verificarDisponibilidadYAgendar(selectedCalendar.getTime(), selectedTimeSlot);
    }

    private boolean validarFechaHora(Calendar selectedCalendar) {
        Calendar currentCalendar = Calendar.getInstance();

        // Resetear segundos y milisegundos
        selectedCalendar.set(Calendar.SECOND, 0);
        selectedCalendar.set(Calendar.MILLISECOND, 0);
        currentCalendar.set(Calendar.SECOND, 0);
        currentCalendar.set(Calendar.MILLISECOND, 0);

        Log.d("Validación", "Fecha actual: " + currentCalendar.getTime());
        Log.d("Validación", "Fecha seleccionada: " + selectedCalendar.getTime());
        Log.d("Validación", "Día de la semana: " + selectedCalendar.get(Calendar.DAY_OF_WEEK));
        Log.d("Validación", "Hora seleccionada: " + selectedCalendar.get(Calendar.HOUR_OF_DAY));

        // Validar fecha pasada
        if (selectedCalendar.before(currentCalendar)) {
            Log.d("Validación", "Error: Fecha pasada");
            mostrarMensajeError("No se pueden agendar citas para fechas/horas pasadas");
            return false;
        }

        // Validar horario laboral
        int hour = selectedCalendar.get(Calendar.HOUR_OF_DAY);
        int minute = selectedCalendar.get(Calendar.MINUTE);
        if (hour < 9 || hour > 18 || (hour == 18 && minute > 0)) {
            Log.d("Validación", "Error: Fuera de horario laboral");
            mostrarMensajeError("Por favor seleccione un horario entre 9:00 AM y 6:00 PM");
            return false;
        }

        // Validar fin de semana
        int dayOfWeek = selectedCalendar.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            Log.d("Validación", "Error: Fin de semana");
            mostrarMensajeError("No se pueden agendar citas en fin de semana");
            return false;
        }

        return true;
    }

    private void mostrarMensajeError(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }

    private void updateAvailableTimeSlots(Calendar date) {
        List<String> availableSlots = AppointmentTimeSlots.getAvailableTimeSlots(date);

        if (availableSlots.isEmpty()) {
            Toast.makeText(this, "No hay horarios disponibles para esta fecha", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create array adapter for time slots
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                availableSlots
        );

        // Replace TimePicker with Spinner for time selection
        Spinner timeSpinner = findViewById(R.id.timeSpinner);
        timeSpinner.setAdapter(adapter);
        timeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTimeSlot = availableSlots.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedTimeSlot = null;
            }
        });
    }

    private void actualizarEstadoControles(boolean enabled, String buttonText) {
        calendarView.setEnabled(enabled);
        btnAgendar.setEnabled(enabled);
        btnAgendar.setText(buttonText);
        btnAgendar.setAlpha(enabled ? 1.0f : 0.5f);
    }

    private void habilitarControles() {
        actualizarEstadoControles(true, "Agendar Cita");
        btnCancelarCita.setEnabled(false);
        btnCancelarCita.setAlpha(0.5f);
    }

    private void deshabilitarControles() {
        actualizarEstadoControles(false, "Ya tienes una cita programada");
    }

    private void habilitarControlesParaNuevaCita() {
        actualizarEstadoControles(true, "Agendar Nueva Cita");
        btnCancelarCita.setEnabled(false);
        btnCancelarCita.setAlpha(0.5f);
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
        Log.d(TAG, "Iniciando proceso de cancelación de cita");
        db.collection("citas")
                .whereEqualTo("pacienteId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean citaEncontrada = false;
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Timestamp citaTimestamp = document.getTimestamp("fecha");
                        String horaStr = document.getString("hora");

                        if (citaTimestamp != null && horaStr != null) {
                            Log.d(TAG, "Cita encontrada, verificando si se puede cancelar");
                            if (AppointmentTimeSlots.canCancelAppointment(citaTimestamp, horaStr)) {
                                citaEncontrada = true;
                                eliminarCita(document.getId(), () -> {
                                    // Callback después de eliminar la cita
                                    cardEstadoCita.setVisibility(View.GONE);
                                    habilitarControlesParaNuevaCita();
                                    // Recargar el estado actual
                                    verificarCitasPendientes();
                                });
                                break;
                            } else {
                                Log.d(TAG, "La cita no se puede cancelar - fuera del límite de tiempo");
                                Toast.makeText(this,
                                        "No se puede cancelar la cita con menos de 24 horas de anticipación",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                    if (!citaEncontrada) {
                        Log.d(TAG, "No se encontró la cita para cancelar");
                        // Actualizar UI para reflejar que no hay cita
                        cardEstadoCita.setVisibility(View.GONE);
                        habilitarControlesParaNuevaCita();
                        verificarCitasPendientes();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al buscar la cita para cancelar", e);
                    Toast.makeText(this, "Error al cancelar la cita", Toast.LENGTH_SHORT).show();
                });
    }

    private void eliminarCita(String citaId, Runnable onComplete) {
        db.collection("citas")
                .document(citaId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Cita eliminada exitosamente");
                    Toast.makeText(this, "Cita cancelada exitosamente", Toast.LENGTH_SHORT).show();
                    if (onComplete != null) {
                        onComplete.run();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al eliminar la cita", e);
                    Toast.makeText(this, "Error al cancelar la cita", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        verificarVinculacionNutriologo();
        verificarCitasPendientes();
    }

    private void verificarVinculacionNutriologo() {
        // En el método verificarVinculacionNutriologo()
        Log.d("Vinculación", "Verificando vinculación para paciente: " + userId);
        db.collection("vinculaciones")
                .whereEqualTo("pacienteId", userId)
                .whereEqualTo("estado", "activo")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        nutriologoId = querySnapshot.getDocuments().get(0).getString("nutriologoId");
                        Log.d("Vinculación", "Nutriólogo vinculado: " + nutriologoId);
                        if (nutriologoId != null) {
                            mostrarCalendario();
                            return;
                        }
                    }
                    Log.d("Vinculación", "No hay vinculación activa");
                    mostrarMensajeNoVinculado();
                });
    }

    private void mostrarEstadoCita(DocumentSnapshot doc) {
        try {
            Timestamp timestamp = doc.getTimestamp("fecha");
            String hora = doc.getString("hora");
            String estado = doc.getString("estado");

            if (timestamp == null || hora == null || estado == null) {
                Log.e(TAG, "Datos de cita incompletos");
                return;
            }
            boolean puedeSerCancelada = AppointmentTimeSlots.canCancelAppointment(timestamp, hora) &&
                    estado.equals("confirmada"); // Solo citas confirmadas pueden cancelarse

            runOnUiThread(() -> {
                cardEstadoCita.setVisibility(View.VISIBLE);

                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String fechaStr = dateFormat.format(timestamp.toDate());
                tvFechaHoraCita.setText(String.format("Fecha: %s\nHora: %s", fechaStr, hora));

                String mensaje;
                int colorFondo;

                switch (estado.toLowerCase()) {
                    case "confirmada":
                        mensaje = String.format("¡Tu cita está confirmada!\nFecha: %s\nHora: %s",
                                fechaStr, hora);
                        colorFondo = ContextCompat.getColor(this, R.color.green);
                        btnCancelarCita.setEnabled(puedeSerCancelada);
                        btnCancelarCita.setAlpha(puedeSerCancelada ? 1.0f : 0.5f); // Efecto visual de deshabilitado
                        break;
                    case "rechazada":
                        mensaje = "Tu cita ha sido rechazada\nPuedes agendar una nueva cita";
                        colorFondo = ContextCompat.getColor(this, R.color.red);
                        btnCancelarCita.setEnabled(false);
                        btnCancelarCita.setAlpha(0.5f);
                        break;
                    default: // pendiente
                        mensaje = String.format("Tu cita está pendiente de confirmación\n" +
                                "Fecha: %s\nHora: %s", fechaStr, hora);
                        colorFondo = ContextCompat.getColor(this, R.color.orange);
                        btnCancelarCita.setEnabled(false);
                        btnCancelarCita.setAlpha(0.5f);
                        break;
                }

                LinearLayout estadoContainer = findViewById(R.id.estadoContainer);
                estadoContainer.setBackgroundColor(colorFondo);
                tvEstadoCita.setText(mensaje);
                tvEstadoCita.setTextColor(Color.WHITE);

                Log.d(TAG, "Estado de la cita: " + estado);
                Log.d(TAG, "¿Puede ser cancelada?: " + puedeSerCancelada);
                Log.d(TAG, "Botón habilitado: " + btnCancelarCita.isEnabled());
            });

        } catch (Exception e) {
            Log.e(TAG, "Error al mostrar estado de cita", e);
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
        Log.d(TAG, "Mostrando diálogo de cancelación");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cancelar Cita")
                .setMessage("¿Estás seguro de que deseas cancelar tu cita? Esta acción no se puede deshacer.")
                .setPositiveButton("Sí, cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "Usuario confirmó cancelación");
                        cancelarCita();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "Usuario canceló la acción");
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void verificarCitasPendientes() {
        Calendar currentTime = Calendar.getInstance();

        db.collection("citas")
                .whereEqualTo("pacienteId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean hasFutureCita = false;

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Timestamp citaTimestamp = doc.getTimestamp("fecha");
                        String hora = doc.getString("hora");
                        String estado = doc.getString("estado");

                        if (citaTimestamp != null && hora != null && estado != null) {
                            Calendar citaCalendar = Calendar.getInstance();
                            citaCalendar.setTime(citaTimestamp.toDate());

                            try {
                                String[] horaParts = hora.split(":");
                                citaCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(horaParts[0]));
                                citaCalendar.set(Calendar.MINUTE, Integer.parseInt(horaParts[1]));

                                if (estado.equals("pendiente")) {
                                    // Si la cita pendiente ya pasó, eliminarla
                                    if (citaCalendar.before(currentTime)) {
                                        eliminarCitaExpirada(doc.getId(), doc.getString("nutriologoId"));
                                    } else {
                                        hasFutureCita = true;
                                        mostrarEstadoCita(doc);
                                        deshabilitarControles();
                                    }
                                } else if (estado.equals("confirmada")) {
                                    if (citaCalendar.after(currentTime)) {
                                        hasFutureCita = true;
                                        mostrarEstadoCita(doc);
                                        deshabilitarControles();
                                    } else {
                                        eliminarCitaPasada(doc.getId());
                                    }
                                } else if (estado.equals("rechazada")) {
                                    habilitarControlesParaNuevaCita();
                                    mostrarEstadoCita(doc);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing hora: " + hora, e);
                            }
                        }
                    }

                    if (!hasFutureCita) {
                        habilitarControles();
                        cardEstadoCita.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error verificando citas pendientes", e));
    }

    private void eliminarCitaExpirada(String citaId, String nutriologoId) {
        if (nutriologoId == null) {
            eliminarCitaPasada(citaId);
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Date fecha = new Date();
        String fechaStr = dateFormat.format(fecha);

        db.collection("citas")
                .document(citaId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String hora = documentSnapshot.getString("hora");
                    if (hora != null) {
                        // Enviar notificación antes de eliminar
                        NotificationService.sendNotificationToUser(
                                userId,
                                "patients",
                                "appointment_expired",
                                "Cita Expirada",
                                "Tu cita del " + fechaStr + " a las " + hora +
                                        " ha expirado porque el nutriólogo no respondió a tiempo",
                                citaId
                        );
                    }
                    // Eliminar la cita
                    documentSnapshot.getReference().delete();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error al eliminar cita expirada", e));
    }

    private void eliminarCitaPasada(String citaId) {
        db.collection("citas")
                .document(citaId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Cita pasada eliminada exitosamente: " + citaId);
                    // No es necesario mostrar un Toast ya que es una limpieza automática
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error al eliminar cita pasada", e));
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
}
package com.example.renalgood.agendarcitap;

import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.renalgood.Chat.ChatActivity;
import com.example.renalgood.ListadeAlimentos.ListadeAlimentosActivity;
import com.example.renalgood.Paciente.BuzonQuejasPaciente;
import com.example.renalgood.Paciente.PacienteActivity;
import com.example.renalgood.R;
import com.example.renalgood.recetas.RecetasActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
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

    private void mostrarCalendario() {
        mensajeNoVinculado.setVisibility(View.GONE);
        calendarioLayout.setVisibility(View.VISIBLE);
    }

    private void mostrarMensajeNoVinculado() {
        mensajeNoVinculado.setVisibility(View.VISIBLE);
        calendarioLayout.setVisibility(View.GONE);
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
                    cita.put("estado", "pendiente");

                    db.collection("citas")
                            .add(cita)
                            .addOnSuccessListener(documentReference -> {
                                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                                builder.setTitle("Cita Agendada")
                                        .setMessage("Tu solicitud de cita ha sido enviada al nutriólogo. " +
                                                "Recibirás una notificación cuando sea confirmada.")
                                        .setPositiveButton("OK", (dialog, which) -> {
                                            dialog.dismiss();
                                            finish();
                                        })
                                        .show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error al agendar la cita: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al obtener datos del paciente: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
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
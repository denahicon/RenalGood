package com.example.renalgood.agendarcitap;

import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Button;
import android.widget.LinearLayout;
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
import android.content.Intent;

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

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();

        // Inicializar vistas
        initViews();

        // Configurar TimePicker para formato 24 horas
        timePicker.setIs24HourView(true);

        verificarVinculacionNutriologo();

        // Configurar listener del botón agendar
        btnAgendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                agendarCita();
            }
        });

        // Configurar navegación
        setupNavigationListeners();
    }

    private void initViews() {
        calendarioLayout = findViewById(R.id.calendarioLayout);
        mensajeNoVinculado = findViewById(R.id.mensajeNoVinculado);
        calendarView = findViewById(R.id.calendarView);
        timePicker = findViewById(R.id.timePicker);
        btnAgendar = findViewById(R.id.btnAgendar);

        // Inicializar vistas de navegación
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
                            // Paciente vinculado - mostrar calendario
                            mostrarCalendario();
                        } else {
                            // Paciente no vinculado - mostrar mensaje
                            mostrarMensajeNoVinculado();
                        }
                    } else {
                        mostrarMensajeNoVinculado();
                    }
                })
                .addOnFailureListener(e -> {
                    // Manejar error
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

    private void agendarCita() {
        // Obtener fecha seleccionada del CalendarView
        long fechaSeleccionada = calendarView.getDate();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(fechaSeleccionada);

        // Obtener hora seleccionada del TimePicker
        int hora = timePicker.getHour();
        int minuto = timePicker.getMinute();

        // Establecer la hora en el calendario
        calendar.set(Calendar.HOUR_OF_DAY, hora);
        calendar.set(Calendar.MINUTE, minuto);

        // Crear objeto de cita
        CitaModel cita = new CitaModel(
                userId,
                nutriologoId,
                calendar.getTimeInMillis(),
                "pendiente" // Estado inicial de la cita
        );

        // Guardar cita en Firestore
        db.collection("citas")
                .add(cita)
                .addOnSuccessListener(documentReference -> {
                    // Programar notificación 2 horas antes
                    programarNotificacion(calendar.getTimeInMillis());
                    // Mostrar mensaje de éxito
                    mostrarMensajeExito();
                })
                .addOnFailureListener(e -> {
                    // Manejar error
                    mostrarMensajeError();
                });
    }

    private void programarNotificacion(long timestampCita) {
        // Aquí implementarías la lógica para programar la notificación
        // 2 horas antes de la cita usando WorkManager o AlarmManager
    }

    private void mostrarMensajeExito() {
        // Implementar mostrar mensaje de éxito
    }

    private void mostrarMensajeError() {
        // Implementar mostrar mensaje de error
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
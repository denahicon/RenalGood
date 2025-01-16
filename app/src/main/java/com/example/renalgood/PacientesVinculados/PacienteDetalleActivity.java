package com.example.renalgood.PacientesVinculados;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.renalgood.Paciente.PatientData;
import com.example.renalgood.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class PacienteDetalleActivity extends AppCompatActivity {
    private TextView tvNombre, tvEdad, tvSituacionClinica, tvPeso, tvEstatura;
    private RecyclerView rvHistorialAlimenticio;
    private HistorialAlimenticioAdapter adapter;
    private FirebaseFirestore db;
    private String pacienteId;
    private List<HistorialAlimenticio> historialList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paciente_detalle);

        // Obtener el ID del paciente del Intent
        pacienteId = getIntent().getStringExtra("pacienteId");
        if (pacienteId == null) {
            Toast.makeText(this, "Error: ID del paciente no encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupToolbar();
        setupHistorialRecyclerView();
        loadPacienteInfo(); // Cargar datos del paciente
        loadHistorialAlimenticio(); // Cargar historial
    }

    private void loadPacienteInfo() {
        db.collection("pacientes")
                .document(pacienteId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Obtener los datos directamente del documento
                        String nombre = documentSnapshot.getString("name");
                        Long edad = documentSnapshot.getLong("age");
                        String situacionClinica = documentSnapshot.getString("situacionClinica");
                        Double peso = documentSnapshot.getDouble("peso");
                        Double estatura = documentSnapshot.getDouble("estatura");

                        // Actualizar la UI con los datos
                        if (nombre != null) tvNombre.setText("Nombre: " + nombre);
                        if (edad != null) tvEdad.setText("Edad: " + edad + " años");
                        if (situacionClinica != null) tvSituacionClinica.setText("Situación Clínica: " + situacionClinica);
                        if (peso != null) tvPeso.setText("Peso: " + String.format("%.1f", peso) + " kg");
                        if (estatura != null) tvEstatura.setText("Estatura: " + String.format("%.1f", estatura) + " cm");
                    } else {
                        Toast.makeText(this, "No se encontró información del paciente", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PacienteDetalle", "Error al cargar datos del paciente: " + e.getMessage());
                    Toast.makeText(this, "Error al cargar la información del paciente", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadHistorialAlimenticio() {
        historialList.clear();
        adapter.notifyDataSetChanged();

        db.collection("historial_alimenticio")
                .whereEqualTo("pacienteId", pacienteId)
                .get() // Primero obtener todos los registros sin ordenar
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        HistorialAlimenticio historial = document.toObject(HistorialAlimenticio.class);
                        if (historial != null) {
                            historial.setId(document.getId());
                            historialList.add(historial);
                        }
                    }
                    // Ordenar la lista localmente
                    Collections.sort(historialList, (h1, h2) -> h2.getFecha().compareTo(h1.getFecha()));
                    adapter.notifyDataSetChanged();

                    if (historialList.isEmpty()) {
                        Toast.makeText(PacienteDetalleActivity.this,
                                "No hay registros de historial alimenticio", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PacienteDetalle", "Error al cargar historial: " + e.getMessage());
                    Toast.makeText(PacienteDetalleActivity.this,
                            "Error al cargar el historial alimenticio", Toast.LENGTH_SHORT).show();
                });
    }

    private void initializeViews() {
        // Inicializar vistas
        tvNombre = findViewById(R.id.tvNombre);
        tvEdad = findViewById(R.id.tvEdad);
        tvSituacionClinica = findViewById(R.id.tvSituacionClinica);
        tvPeso = findViewById(R.id.tvPeso);
        tvEstatura = findViewById(R.id.tvEstatura);
        rvHistorialAlimenticio = findViewById(R.id.rvHistorialAlimenticio);

        // Inicializar Firebase y lista
        db = FirebaseFirestore.getInstance();
        historialList = new ArrayList<>();
    }

    private void setupHistorialRecyclerView() {
        adapter = new HistorialAlimenticioAdapter(historialList);
        rvHistorialAlimenticio.setLayoutManager(new LinearLayoutManager(this));
        rvHistorialAlimenticio.setAdapter(adapter);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Detalle del Paciente");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
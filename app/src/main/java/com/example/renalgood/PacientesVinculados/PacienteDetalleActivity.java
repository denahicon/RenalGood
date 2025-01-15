package com.example.renalgood.PacientesVinculados;

import android.os.Bundle;
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

        pacienteId = getIntent().getStringExtra("pacienteId");
        if (pacienteId == null) {
            Toast.makeText(this, "Error al cargar los datos del paciente", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupToolbar();
        loadPacienteInfo();
        setupHistorialRecyclerView();
        loadHistorialAlimenticio();
    }

    private void initializeViews() {
        tvNombre = findViewById(R.id.tvNombre);
        tvEdad = findViewById(R.id.tvEdad);
        tvSituacionClinica = findViewById(R.id.tvSituacionClinica);
        tvPeso = findViewById(R.id.tvPeso);
        tvEstatura = findViewById(R.id.tvEstatura);
        rvHistorialAlimenticio = findViewById(R.id.rvHistorialAlimenticio);
        db = FirebaseFirestore.getInstance();
        historialList = new ArrayList<>();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void setupHistorialRecyclerView() {
        adapter = new HistorialAlimenticioAdapter(historialList);
        rvHistorialAlimenticio.setLayoutManager(new LinearLayoutManager(this));
        rvHistorialAlimenticio.setAdapter(adapter);
    }

    private void loadPacienteInfo() {
        db.collection("pacientes")
                .document(pacienteId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        PatientData paciente = documentSnapshot.toObject(PatientData.class);
                        if (paciente != null) {
                            updateUI(paciente);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al cargar la información del paciente",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUI(PatientData paciente) {
        tvNombre.setText("Nombre: " + paciente.getName());
        tvEdad.setText("Edad: " + paciente.getAge() + " años");
        tvSituacionClinica.setText("Situación Clínica: " + paciente.getClinicalSituation());
        tvPeso.setText("Peso: " + String.format("%.1f", paciente.getWeight()) + " kg");
        tvEstatura.setText("Estatura: " + paciente.getHeight() + " cm");
    }

    private void loadHistorialAlimenticio() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -30);
        Date treintaDiasAtras = calendar.getTime();

        db.collection("historial_alimenticio")
                .whereEqualTo("pacienteId", pacienteId)
                .whereGreaterThan("fecha", treintaDiasAtras)
                .orderBy("fecha", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error al cargar el historial",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    historialList.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            HistorialAlimenticio historial = doc.toObject(HistorialAlimenticio.class);
                            if (historial != null) {
                                historialList.add(historial);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
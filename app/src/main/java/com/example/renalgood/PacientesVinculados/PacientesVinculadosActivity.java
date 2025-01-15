package com.example.renalgood.PacientesVinculados;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.renalgood.Nutriologo.NavigationHelper;
import com.example.renalgood.Nutriologo.PacientesAdapter;
import com.example.renalgood.Paciente.PatientData;
import com.example.renalgood.R;
import com.example.renalgood.mensaje.MensajeDetalleActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import android.content.Intent;
import androidx.recyclerview.widget.RecyclerView;

public class PacientesVinculadosActivity extends AppCompatActivity {
    private RecyclerView rvPacientes;
    private PacientesAdapter adapter;
    private List<PatientData> pacientesList;
    private FirebaseFirestore db;
    private String nutriologoId;
    private ImageView ivHome, ivMensaje, ivCalendario, ivPacientesVinculados, ivCarta;
    private NavigationHelper navigationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pacientes_vinculados);

        initializeViews();
        setupNavigationListeners();
        setupFirestore();
    }

    private void initializeViews() {
        rvPacientes = findViewById(R.id.rvPacientes);
        ivHome = findViewById(R.id.ivHome);
        ivMensaje = findViewById(R.id.ivMensaje);
        ivCalendario = findViewById(R.id.ivCalendario);
        ivPacientesVinculados = findViewById(R.id.group_2811039);
        ivCarta = findViewById(R.id.ivCarta);

        pacientesList = new ArrayList<>();
        adapter = new PacientesAdapter(pacientesList);

        adapter.setOnPacienteClickListener(paciente -> {
            Intent intent = new Intent(this, com.example.renalgood.PacientesVinculados.PacienteDetalleActivity.class);
            intent.putExtra("pacienteId", paciente.getId());
            startActivity(intent);
        });

        rvPacientes.setLayoutManager(new LinearLayoutManager(this));
        rvPacientes.setAdapter(adapter);
    }

    private void setupNavigationListeners() {
        navigationHelper = new NavigationHelper(
                this, ivHome, ivMensaje, ivCalendario, ivPacientesVinculados, ivCarta
        );
        navigationHelper.setupNavigation("pacientes");
    }

    private void setupFirestore() {
        db = FirebaseFirestore.getInstance();
        nutriologoId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    private void loadPacienteInfo(String pacienteId) {
        db.collection("pacientes")
                .document(pacienteId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        PatientData paciente = documentSnapshot.toObject(PatientData.class);
                        if (paciente != null) {
                            // Agrega log para debug
                            Log.d("PacientesVinculados", "Paciente cargado: " + paciente.getName());
                            paciente.setId(documentSnapshot.getId());
                            pacientesList.add(paciente);
                            adapter.notifyDataSetChanged();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PacientesVinculados", "Error al cargar paciente: " + e.getMessage());
                    Toast.makeText(this, "Error al cargar la información del paciente",
                            Toast.LENGTH_SHORT).show();
                });
    }
}
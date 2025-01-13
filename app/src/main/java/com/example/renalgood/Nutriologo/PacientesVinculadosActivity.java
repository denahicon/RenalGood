package com.example.renalgood.Nutriologo;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pacientes_vinculados);

        initializeViews();
        setupFirestore();
        loadPacientesVinculados();
    }

    private void initializeViews() {
        rvPacientes = findViewById(R.id.rvPacientes);
        pacientesList = new ArrayList<>();
        adapter = new PacientesAdapter(pacientesList);

        adapter.setOnPacienteClickListener(paciente -> {
            // Aquí puedes manejar el click en un paciente
            // Por ejemplo, abrir el chat o ver detalles
            Intent intent = new Intent(this, MensajeDetalleActivity.class);
            intent.putExtra("pacienteId", paciente.getId());
            intent.putExtra("nombrePaciente", paciente.getName());
            startActivity(intent);
        });

        rvPacientes.setLayoutManager(new LinearLayoutManager(this));
        rvPacientes.setAdapter(adapter);
    }

    private void setupFirestore() {
        db = FirebaseFirestore.getInstance();
        nutriologoId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    private void loadPacientesVinculados() {
        db.collection("vinculaciones")
                .whereEqualTo("nutriologoId", nutriologoId)
                .whereEqualTo("estado", "activo")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error al cargar pacientes", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    pacientesList.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String pacienteId = doc.getString("pacienteId");
                            if (pacienteId != null) {
                                loadPacienteInfo(pacienteId);
                            }
                        }
                    }
                });
    }

    private void loadPacienteInfo(String pacienteId) {
        db.collection("pacientes")
                .document(pacienteId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        PatientData paciente = documentSnapshot.toObject(PatientData.class);
                        if (paciente != null) {
                            paciente.setId(documentSnapshot.getId());
                            pacientesList.add(paciente);
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
    }
}
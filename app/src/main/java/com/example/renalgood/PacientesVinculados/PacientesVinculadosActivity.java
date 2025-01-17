package com.example.renalgood.PacientesVinculados;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.renalgood.Nutriologo.NavigationHelper;
import com.example.renalgood.Nutriologo.PacientesAdapter;
import com.example.renalgood.Paciente.PatientData;
import com.example.renalgood.R;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.Arrays;
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
        loadPacientesVinculados();
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

    @Override
    protected void onResume() {
        super.onResume();
        loadPacientesVinculados();
    }

    private void loadPacienteInfo(String pacienteId) {
        Log.d("PacientesVinculados", "Iniciando carga de paciente: " + pacienteId);

        // Crear las tareas para obtener datos del paciente y su vinculación
        Task<DocumentSnapshot> patientTask = db.collection("patients")
                .document(pacienteId)
                .get();

        Task<QuerySnapshot> vinculacionTask = db.collection("vinculaciones")
                .whereEqualTo("pacienteId", pacienteId)
                .whereEqualTo("estado", "activo")
                .limit(1)
                .get();

        // Ejecutar ambas tareas en paralelo
        Tasks.whenAllSuccess(patientTask, vinculacionTask)
                .addOnSuccessListener(results -> {
                    DocumentSnapshot patientDoc = (DocumentSnapshot) results.get(0);
                    QuerySnapshot vinculacionDocs = (QuerySnapshot) results.get(1);

                    if (!patientDoc.exists()) {
                        Log.e("PacientesVinculados", "Documento del paciente no encontrado");
                        return;
                    }

                    try {
                        PatientData paciente = new PatientData();
                        paciente.setId(patientDoc.getId());

                        // Obtener nombre usando las alternativas
                        String nombre = null;
                        for (String field : Arrays.asList("name", "nombre", "nombre_completo")) {
                            nombre = patientDoc.getString(field);
                            if (nombre != null) break;
                        }

                        if (nombre == null) {
                            Log.e("PacientesVinculados", "Nombre no encontrado en el documento");
                            return;
                        }
                        paciente.setName(nombre);

                        // Obtener edad
                        Long edad = null;
                        for (String field : Arrays.asList("age", "edad")) {
                            edad = patientDoc.getLong(field);
                            if (edad != null) break;
                        }
                        if (edad != null) {
                            paciente.setAge(edad.intValue());
                        }

                        // Obtener situación clínica
                        String situacion = null;
                        for (String field : Arrays.asList("clinicalSituation", "situacion_clinica", "situacionClinica")) {
                            situacion = patientDoc.getString(field);
                            if (situacion != null) break;
                        }
                        paciente.setSituacionClinica(situacion);

                        // Verificar vinculación activa
                        if (!vinculacionDocs.isEmpty()) {
                            pacientesList.add(paciente);
                            adapter.notifyDataSetChanged();
                            Log.d("PacientesVinculados",
                                    String.format("Paciente agregado: %s, ID: %s", nombre, pacienteId));
                        }

                    } catch (Exception e) {
                        Log.e("PacientesVinculados", "Error procesando datos: " + e.getMessage(), e);
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("PacientesVinculados", "Error en carga de datos: " + e.getMessage(), e));
    }

    private void loadPacientesVinculados() {
        Log.d("PacientesVinculados", "Iniciando carga de pacientes vinculados");
        pacientesList.clear();
        adapter.notifyDataSetChanged();

        db.collection("vinculaciones")
                .whereEqualTo("nutriologoId", nutriologoId)
                .whereEqualTo("estado", "activo")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d("PacientesVinculados", "Documentos encontrados: " + querySnapshot.size());
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        String pacienteId = document.getString("pacienteId");
                        Log.d("PacientesVinculados", "ID de paciente encontrado: " + pacienteId);
                        if (pacienteId != null) {
                            loadPacienteInfo(pacienteId);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PacientesVinculados", "Error al cargar vinculaciones: " + e.getMessage());
                    e.printStackTrace();
                });
    }
    }

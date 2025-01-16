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
import java.util.Map;

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
        Log.d("PacientesVinculados", "Intentando cargar paciente con ID: " + pacienteId);

        db.collection("patients")
                .document(pacienteId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d("PacientesVinculados", "Documento encontrado: " + documentSnapshot.exists());
                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        Log.d("PacientesVinculados", "Datos completos del documento: " + data);

                        try {
                            PatientData paciente = new PatientData();
                            paciente.setId(documentSnapshot.getId());

                            // Intentamos todos los posibles nombres de campos
                            String nombre = (String) data.get("nombre");
                            if (nombre == null) nombre = (String) data.get("name");
                            if (nombre == null) nombre = (String) data.get("nombre_completo");
                            Log.d("PacientesVinculados", "Nombre encontrado: " + nombre);
                            paciente.setName(nombre);

                            // Para la edad
                            Object edadObj = data.get("edad");
                            if (edadObj == null) edadObj = data.get("age");
                            if (edadObj instanceof Long) {
                                paciente.setAge(((Long) edadObj).intValue());
                            }

                            // Para la situación clínica
                            String situacion = (String) data.get("situacionClinica");
                            if (situacion == null) situacion = (String) data.get("situacion_clinica");
                            if (situacion == null) situacion = (String) data.get("clinical_situation");
                            paciente.setSituacionClinica(situacion);

                            if (nombre != null) {
                                pacientesList.add(paciente);
                                adapter.notifyDataSetChanged();
                                Log.d("PacientesVinculados", "Paciente agregado exitosamente: " + nombre);
                            } else {
                                Log.e("PacientesVinculados", "No se pudo agregar el paciente - nombre es null");
                            }

                        } catch (Exception e) {
                            Log.e("PacientesVinculados", "Error procesando datos: " + e.getMessage());
                            e.printStackTrace();
                        }
                    } else {
                        Log.e("PacientesVinculados", "Documento no encontrado para ID: " + pacienteId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PacientesVinculados", "Error cargando paciente: " + e.getMessage());
                });
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

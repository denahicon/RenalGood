package com.example.renalgood.CitasNutriologo;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.renalgood.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class CitasPendientesFragment extends Fragment implements CitasAdapter.CitaClickListener {
    private static final String TAG = "CitasPendientesFragment";
    private RecyclerView recyclerView;
    private CitasAdapter citasAdapter;
    private FirebaseFirestore db;
    private TextView tvEmpty;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_citas_pendientes, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewCitas);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        setupRecyclerView();
        cargarCitasPendientes();
    }

    private void setupRecyclerView() {
        citasAdapter = new CitasAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(citasAdapter);
    }

    @Override
    public void onAceptarClick(CitaModel cita) {
        db.collection("citas").document(cita.getId())
                .update("estado", "confirmada")
                .addOnSuccessListener(aVoid -> {
                    // Notificar al paciente
                    notificarPaciente(cita.getPacienteId(),
                            "Cita Confirmada",
                            "Tu cita ha sido confirmada por el nutriólogo");
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error al aceptar cita", e));
    }

    @Override
    public void onRechazarClick(CitaModel cita) {
        db.collection("citas").document(cita.getId())
                .update("estado", "rechazada")
                .addOnSuccessListener(aVoid -> {
                    // Notificar al paciente
                    notificarPaciente(cita.getPacienteId(),
                            "Cita Rechazada",
                            "Tu cita ha sido rechazada por el nutriólogo");
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error al rechazar cita", e));
    }

    @Override
    public void onCancelarClick(CitaModel cita) {
        // No se necesita implementación para citas pendientes
        Log.d(TAG, "onCancelarClick: No aplicable para citas pendientes");
    }

    private void notificarPaciente(String pacienteId, String titulo, String mensaje) {
        // Aquí implementa la lógica de notificación usando NotificationService
    }

    private void updateEmptyView(boolean isEmpty) {
        if (tvEmpty != null && recyclerView != null) {
            tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }

    private void cargarCitasPendientes() {
        String nutriologoId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("citas")
                .whereEqualTo("nutriologoId", nutriologoId)
                .whereEqualTo("estado", "pendiente")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error cargando citas", error);
                        return;
                    }

                    List<CitaModel> citas = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            CitaModel cita = new CitaModel();
                            cita.setId(doc.getId());
                            cita.setNutriologoId(doc.getString("nutriologoId"));
                            cita.setPacienteId(doc.getString("pacienteId"));

                            // Manejar la fecha
                            if (doc.getTimestamp("fecha") != null) {
                                cita.setFecha(doc.getTimestamp("fecha").toDate());
                            }

                            cita.setHora(doc.getString("hora"));
                            cita.setEstado(doc.getString("estado"));

                            // Obtener el nombre del paciente desde la colección "patients"
                            String pacienteId = doc.getString("pacienteId");
                            if (pacienteId != null) {
                                db.collection("patients")
                                        .document(pacienteId)
                                        .get()
                                        .addOnSuccessListener(patientDoc -> {
                                            if (patientDoc.exists()) {
                                                String nombrePaciente = patientDoc.getString("name");
                                                cita.setPacienteNombre(nombrePaciente);
                                                citasAdapter.notifyDataSetChanged();
                                            }
                                        })
                                        .addOnFailureListener(e -> Log.e(TAG, "Error al obtener datos del paciente", e));
                            }

                            citas.add(cita);
                        }
                    }

                    citasAdapter.updateList(citas);
                    updateEmptyView(citas.isEmpty());
                });
    }
}
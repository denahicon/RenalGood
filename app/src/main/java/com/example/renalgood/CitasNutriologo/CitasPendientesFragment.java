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

public class CitasPendientesFragment extends Fragment {
    private RecyclerView recyclerView;
    private CitasAdapter citasAdapter;
    private FirebaseFirestore db;
    private TextView tvEmpty;

    @Nullable
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
        citasAdapter = new CitasAdapter(new ArrayList<>(), new CitasAdapter.CitaClickListener() {
            @Override
            public void onAceptarClick(Cita cita) {
                aceptarCita(cita);
            }

            @Override
            public void onRechazarClick(Cita cita) {
                rechazarCita(cita);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(citasAdapter);
    }

    private void cargarCitasPendientes() {
        String nutriologoId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("citas")
                .whereEqualTo("nutriologoId", nutriologoId)
                .whereEqualTo("estado", "pendiente")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("CitasPendientes", "Error cargando citas", error);
                        return;
                    }

                    List<Cita> citas = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        Cita cita = doc.toObject(Cita.class);
                        citas.add(cita);
                    }

                    citasAdapter.updateCitas(citas);
                    updateEmptyView(citas.isEmpty());
                });
    }

    private void updateEmptyView(boolean isEmpty) {
        tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void aceptarCita(Cita cita) {
        db.collection("citas").document(cita.getId())
                .update("estado", "confirmada")
                .addOnSuccessListener(aVoid -> {
                    // Enviar notificación al paciente
                    notificarPaciente(cita.getPacienteId(),
                            "Cita Confirmada",
                            "Tu cita ha sido confirmada por el nutriólogo");
                })
                .addOnFailureListener(e ->
                        Log.e("CitasPendientes", "Error al aceptar cita", e));
    }

    private void rechazarCita(Cita cita) {
        db.collection("citas").document(cita.getId())
                .update("estado", "rechazada")
                .addOnSuccessListener(aVoid -> {
                    // Enviar notificación al paciente
                    notificarPaciente(cita.getPacienteId(),
                            "Cita Rechazada",
                            "Tu cita ha sido rechazada por el nutriólogo");
                })
                .addOnFailureListener(e ->
                        Log.e("CitasPendientes", "Error al rechazar cita", e));
    }

    private void notificarPaciente(String pacienteId, String titulo, String mensaje) {
        // Implementar lógica de notificaciones usando FCM
        db.collection("users").document(pacienteId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    // Aquí implementarías el envío de notificación usando FCM
                });
    }
}
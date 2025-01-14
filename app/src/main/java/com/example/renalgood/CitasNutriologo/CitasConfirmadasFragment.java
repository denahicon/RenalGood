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

public class CitasConfirmadasFragment extends Fragment implements CitasAdapter.CitaClickListener {
    private RecyclerView recyclerView;
    private CitasAdapter citasAdapter;
    private FirebaseFirestore db;
    private TextView tvEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_citas_confirmadas, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewCitas);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        setupRecyclerView();
        cargarCitasConfirmadas();
    }

    private void setupRecyclerView() {
        // Pasamos this como el CitaClickListener, aunque no usaremos los clicks en citas confirmadas
        citasAdapter = new CitasAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(citasAdapter);
    }

    private void cargarCitasConfirmadas() {
        String nutriologoId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("citas")
                .whereEqualTo("nutriologoId", nutriologoId)
                .whereEqualTo("estado", "confirmada")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("CitasConfirmadas", "Error cargando citas", error);
                        return;
                    }

                    List<CitaModel> citas = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            CitaModel cita = new CitaModel();
                            cita.setId(doc.getId());
                            cita.setNutriologoId(doc.getString("nutriologoId"));
                            cita.setPacienteId(doc.getString("pacienteId"));
                            cita.setPacienteNombre(doc.getString("pacienteNombre"));
                            cita.setHora(doc.getString("hora"));
                            cita.setEstado(doc.getString("estado"));

                            // Manejo seguro de la fecha
                            if (doc.getTimestamp("fecha") != null) {
                                cita.setFecha(doc.getTimestamp("fecha").toDate());
                            }

                            citas.add(cita);
                        }
                    }

                    citasAdapter.updateList(citas);
                    updateEmptyView(citas.isEmpty());
                });
    }

    private void updateEmptyView(boolean isEmpty) {
        tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    // Implementamos los métodos requeridos del CitaClickListener, aunque no los usaremos
    @Override
    public void onAceptarClick(CitaModel cita) {
        // No se necesita implementación para citas confirmadas
    }

    @Override
    public void onRechazarClick(CitaModel cita) {
        // No se necesita implementación para citas confirmadas
    }
}
package com.example.renalgood.CitasNutriologo;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.renalgood.R;
import com.example.renalgood.agendarcitap.AppointmentTimeSlots;
import com.example.renalgood.agendarcitap.NotificationService;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CitasConfirmadasFragment extends Fragment implements CitasAdapter.CitaClickListener {
    private static final String TAG = "CitasConfirmadasFragment";
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
        citasAdapter = new CitasAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(citasAdapter);
    }

    private void updateEmptyView(boolean isEmpty) {
        if (tvEmpty != null && recyclerView != null) {
            tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onAceptarClick(CitaModel cita) {
        Log.d(TAG, "onAceptarClick: No aplicable para citas confirmadas");
    }

    @Override
    public void onRechazarClick(CitaModel cita) {
        Log.d(TAG, "onRechazarClick: No aplicable para citas confirmadas");
    }

    @Override
    public void onCancelarClick(CitaModel cita) {
        // Verificar si la cita puede ser cancelada (24 horas antes)
        if (cita.getFecha() != null) {
            Calendar citaCalendar = Calendar.getInstance();
            citaCalendar.setTime(cita.getFecha());

            // Obtener la hora de la cita
            String[] horaPartes = cita.getHora().split(":");
            citaCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(horaPartes[0]));
            citaCalendar.set(Calendar.MINUTE, Integer.parseInt(horaPartes[1]));

            // Verificar si estamos dentro del límite de 24 horas
            Calendar limiteCancelacion = Calendar.getInstance();
            limiteCancelacion.add(Calendar.HOUR_OF_DAY, 24);

            if (citaCalendar.after(limiteCancelacion)) {
                mostrarDialogoCancelacion(cita);
            } else {
                // Menos de 24 horas para la cita
                Toast.makeText(getContext(),
                        "No se puede cancelar la cita con menos de 24 horas de anticipación",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void mostrarDialogoCancelacion(CitaModel cita) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cancelar Cita")
                .setMessage("¿Estás seguro de que deseas cancelar esta cita? " +
                        "Se notificará al paciente de la cancelación.")
                .setPositiveButton("Sí, Cancelar", (dialog, which) -> {
                    procesarCancelacion(cita);
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void procesarCancelacion(CitaModel cita) {
        // Actualizar el estado de la cita a "cancelada"
        db.collection("citas")
                .document(cita.getId())
                .update("estado", "cancelada")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(),
                            "Cita cancelada exitosamente",
                            Toast.LENGTH_SHORT).show();

                    // Notificar al paciente
                    notificarPacienteCancelacion(cita);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(),
                            "Error al cancelar la cita",
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error al cancelar cita", e);
                });
    }

    private void notificarPacienteCancelacion(CitaModel cita) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String fechaStr = dateFormat.format(cita.getFecha());

        NotificationService.sendNotificationToUser(
                cita.getPacienteId(),
                "patients",
                "appointment_cancellation",
                "Cita Cancelada",
                "Tu cita del " + fechaStr + " a las " + cita.getHora() +
                        " ha sido cancelada por el nutriólogo.",
                cita.getId()
        );
    }

    private void cancelarCita(CitaModel cita) {
        if (cita.getFecha() != null) {
            Timestamp citaTimestamp = new Timestamp(cita.getFecha());

            if (AppointmentTimeSlots.canCancelAppointment(citaTimestamp, cita.getHora())) {
                db.collection("citas")
                        .document(cita.getId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            if (getContext() != null) {
                                Toast.makeText(getContext(),
                                        "Cita cancelada exitosamente",
                                        Toast.LENGTH_SHORT).show();
                            }
                            notificarPacienteCancelacion(cita);
                        })
                        .addOnFailureListener(e -> {
                            if (getContext() != null) {
                                Toast.makeText(getContext(),
                                        "Error al cancelar la cita",
                                        Toast.LENGTH_SHORT).show();
                            }
                            Log.e(TAG, "Error al cancelar cita", e);
                        });
            } else {
                if (getContext() != null) {
                    Toast.makeText(getContext(),
                            "No se puede cancelar la cita con menos de 24 horas de anticipación",
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void cargarCitasConfirmadas() {
        String nutriologoId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("citas")
                .whereEqualTo("nutriologoId", nutriologoId)
                .whereEqualTo("estado", "confirmada")
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
                            cita.setPacienteId(doc.getString("pacienteId"));

                            // Manejar la fecha
                            Timestamp timestamp = doc.getTimestamp("fecha");
                            if (timestamp != null) {
                                cita.setFecha(timestamp.toDate());
                            }

                            cita.setHora(doc.getString("hora"));
                            cita.setEstado(doc.getString("estado"));

                            // Obtener el nombre del paciente
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
                                        .addOnFailureListener(e ->
                                                Log.e(TAG, "Error al obtener datos del paciente", e));
                            }

                            citas.add(cita);
                        }
                    }

                    citasAdapter.updateList(citas);
                    updateEmptyView(citas.isEmpty());
                });
    }
}
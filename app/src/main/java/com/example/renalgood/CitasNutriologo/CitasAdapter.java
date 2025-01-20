package com.example.renalgood.CitasNutriologo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.renalgood.R;
import com.example.renalgood.agendarcitap.AppointmentValidations;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CitasAdapter extends RecyclerView.Adapter<CitasAdapter.CitaViewHolder> {
    private List<CitaModel> citasList;
    private final CitaClickListener listener;
    private final SimpleDateFormat dateFormat;

    public interface CitaClickListener {
        void onAceptarClick(CitaModel cita);
        void onRechazarClick(CitaModel cita);
        void onCancelarClick(CitaModel cita); // Nuevo método
    }


    public CitasAdapter(CitaClickListener listener) {
        this.citasList = new ArrayList<>();
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public CitaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cita, parent, false);
        return new CitaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CitaViewHolder holder, int position) {
        CitaModel cita = citasList.get(position);
        holder.bind(cita);
    }

    @Override
    public int getItemCount() {
        return citasList.size();
    }

    public void updateList(List<CitaModel> newList) {
        this.citasList = newList;
        notifyDataSetChanged();
    }

    class CitaViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvNombrePaciente;
        private final TextView tvFechaCita;
        private final TextView tvHoraCita;
        private final Button btnAceptar;
        private final Button btnRechazar;
        private final Button btnCancelar; // Nuevo botón

        CitaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombrePaciente = itemView.findViewById(R.id.tvNombrePaciente);
            tvFechaCita = itemView.findViewById(R.id.tvFechaCita);
            tvHoraCita = itemView.findViewById(R.id.tvHoraCita);
            btnAceptar = itemView.findViewById(R.id.btnAceptar);
            btnRechazar = itemView.findViewById(R.id.btnRechazar);
            btnCancelar = itemView.findViewById(R.id.btnCancelar);
        }

        void bind(final CitaModel cita) {
            tvNombrePaciente.setText(String.format("Paciente: %s", cita.getPacienteNombre()));
            tvFechaCita.setText(String.format("Fecha: %s", dateFormat.format(cita.getFecha())));
            tvHoraCita.setText(String.format("Hora: %s", cita.getHora()));

            // Verificar si la cita puede ser cancelada
            boolean puedeSerCancelada = false;
            if (cita.getFecha() != null) {
                Timestamp citaTimestamp = new Timestamp(cita.getFecha());
                puedeSerCancelada = AppointmentValidations.canCancelAppointment(citaTimestamp, cita.getHora());
            }

            if ("confirmada".equals(cita.getEstado()) && puedeSerCancelada) {
                btnCancelar.setVisibility(View.VISIBLE);
                btnCancelar.setOnClickListener(v -> {
                    if (listener != null) listener.onCancelarClick(cita);
                });
            } else {
                btnCancelar.setVisibility(View.GONE);
            }

            // Solo mostrar botones de aceptar/rechazar si la cita está pendiente
            if ("pendiente".equals(cita.getEstado())) {
                btnAceptar.setVisibility(View.VISIBLE);
                btnRechazar.setVisibility(View.VISIBLE);

                btnAceptar.setOnClickListener(v -> {
                    if (listener != null) listener.onAceptarClick(cita);
                });
                btnRechazar.setOnClickListener(v -> {
                    if (listener != null) listener.onRechazarClick(cita);
                });
            } else {
                btnAceptar.setVisibility(View.GONE);
                btnRechazar.setVisibility(View.GONE);
            }
        }
    }
}
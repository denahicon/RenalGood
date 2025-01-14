package com.example.renalgood.CitasNutriologo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.renalgood.R;
import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CitasAdapter extends RecyclerView.Adapter<CitasAdapter.CitaViewHolder> {
    private List<CitaModel> citasList;
    private final CitaClickListener listener;
    private final SimpleDateFormat dateFormat;

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

        CitaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombrePaciente = itemView.findViewById(R.id.tvNombrePaciente);
            tvFechaCita = itemView.findViewById(R.id.tvFechaCita);
            tvHoraCita = itemView.findViewById(R.id.tvHoraCita);
            btnAceptar = itemView.findViewById(R.id.btnAceptar);
            btnRechazar = itemView.findViewById(R.id.btnRechazar);
        }

        void bind(final CitaModel cita) {
            tvNombrePaciente.setText(cita.getPacienteNombre());

            // Manejo seguro de la fecha
            if (cita.getFecha() != null) {
                tvFechaCita.setText(dateFormat.format(cita.getFecha()));
            } else {
                tvFechaCita.setText("Fecha no disponible");
            }

            tvHoraCita.setText(cita.getHora());

            if ("pendiente".equals(cita.getEstado())) {
                btnAceptar.setVisibility(View.VISIBLE);
                btnRechazar.setVisibility(View.VISIBLE);

                btnAceptar.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onAceptarClick(cita);
                    }
                });

                btnRechazar.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onRechazarClick(cita);
                    }
                });
            } else {
                btnAceptar.setVisibility(View.GONE);
                btnRechazar.setVisibility(View.GONE);
            }
        }
    }

    public interface CitaClickListener {
        void onAceptarClick(CitaModel cita);
        void onRechazarClick(CitaModel cita);
    }
}
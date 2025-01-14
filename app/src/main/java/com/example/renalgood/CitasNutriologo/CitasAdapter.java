package com.example.renalgood.CitasNutriologo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renalgood.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
public class CitasAdapter extends RecyclerView.Adapter<CitasAdapter.CitaViewHolder> {
    private List<Cita> citas;
    private CitaClickListener listener;
    private SimpleDateFormat dateFormat;

    public interface CitaClickListener {
        void onAceptarClick(Cita cita);
        void onRechazarClick(Cita cita);
    }

    public CitasAdapter(List<Cita> citas, CitaClickListener listener) {
        this.citas = citas;
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
        Cita cita = citas.get(position);
        holder.bind(cita);
    }

    @Override
    public int getItemCount() {
        return citas.size();
    }

    public void updateCitas(List<Cita> newCitas) {
        this.citas = newCitas;
        notifyDataSetChanged();
    }

    class CitaViewHolder extends RecyclerView.ViewHolder {
        private TextView tvNombrePaciente;
        private TextView tvFechaCita;
        private TextView tvHoraCita;
        private Button btnAceptar;
        private Button btnRechazar;

        CitaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombrePaciente = itemView.findViewById(R.id.tvNombrePaciente);
            tvFechaCita = itemView.findViewById(R.id.tvFechaCita);
            tvHoraCita = itemView.findViewById(R.id.tvHoraCita);
            btnAceptar = itemView.findViewById(R.id.btnAceptar);
            btnRechazar = itemView.findViewById(R.id.btnRechazar);
        }

        void bind(final Cita cita) {
            tvNombrePaciente.setText(cita.getPacienteNombre());
            tvFechaCita.setText(dateFormat.format(cita.getFecha()));
            tvHoraCita.setText(cita.getHora());

            // Configuración de visibilidad y clicks de botones según estado
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
}
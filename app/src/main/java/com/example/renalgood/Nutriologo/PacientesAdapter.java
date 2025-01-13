package com.example.renalgood.Nutriologo;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.renalgood.Paciente.PatientData;
import com.example.renalgood.R;
import java.util.List;
import android.view.LayoutInflater;

public class PacientesAdapter extends RecyclerView.Adapter<PacientesAdapter.PacienteViewHolder> {
    private List<PatientData> pacientesList;
    private OnPacienteClickListener listener;

    public interface OnPacienteClickListener {
        void onPacienteClick(PatientData paciente);
    }

    public PacientesAdapter(List<PatientData> pacientesList) {
        this.pacientesList = pacientesList;
    }

    public void setOnPacienteClickListener(OnPacienteClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public PacienteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_paciente, parent, false);
        return new PacienteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PacienteViewHolder holder, int position) {
        PatientData paciente = pacientesList.get(position);
        holder.bind(paciente, listener);
    }

    @Override
    public int getItemCount() {
        return pacientesList.size();
    }

    static class PacienteViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvNombre;
        private final TextView tvEdad;
        private final TextView tvSituacionClinica;

        public PacienteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvEdad = itemView.findViewById(R.id.tvEdad);
            tvSituacionClinica = itemView.findViewById(R.id.tvSituacionClinica);
        }

        public void bind(final PatientData paciente, final OnPacienteClickListener listener) {
            // Establecer nombre
            tvNombre.setText(paciente.getName());

            // Establecer edad
            tvEdad.setText(String.format("Edad: %d años", paciente.getAge()));

            // Establecer situación clínica
            tvSituacionClinica.setText(paciente.getClinicalSituation());

            // Configurar click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPacienteClick(paciente);
                }
            });
        }
    }

    public void updatePacientes(List<PatientData> newPacientes) {
        pacientesList.clear();
        pacientesList.addAll(newPacientes);
        notifyDataSetChanged();
    }
}
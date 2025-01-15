package com.example.renalgood.PacientesVinculados;

import android.icu.text.SimpleDateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.renalgood.R;
import java.util.List;
import java.util.Locale;

public class HistorialAlimenticioAdapter extends RecyclerView.Adapter<HistorialAlimenticioAdapter.ViewHolder> {
    private List<HistorialAlimenticio> historialList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public HistorialAlimenticioAdapter(List<HistorialAlimenticio> historialList) {
        this.historialList = historialList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_historial_alimenticio, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistorialAlimenticio historial = historialList.get(position);

        holder.tvFecha.setText(dateFormat.format(historial.getFecha()));
        holder.tvComida.setText(historial.getNombreComida());
        holder.tvCalorias.setText(String.format("Calorías: %.1f", historial.getCalorias()));
        holder.tvProteinas.setText(String.format("Proteínas: %.1fg", historial.getProteinas()));
        holder.tvLipidos.setText(String.format("Lípidos: %.1fg", historial.getLipidos()));
        holder.tvCarbohidratos.setText(String.format("Carbohidratos: %.1fg",
                historial.getCarbohidratos()));
    }

    @Override
    public int getItemCount() {
        return historialList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFecha, tvComida, tvCalorias, tvProteinas, tvLipidos, tvCarbohidratos;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvComida = itemView.findViewById(R.id.tvComida);
            tvCalorias = itemView.findViewById(R.id.tvCalorias);
            tvProteinas = itemView.findViewById(R.id.tvProteinas);
            tvLipidos = itemView.findViewById(R.id.tvLipidos);
            tvCarbohidratos = itemView.findViewById(R.id.tvCarbohidratos);
        }
    }
}
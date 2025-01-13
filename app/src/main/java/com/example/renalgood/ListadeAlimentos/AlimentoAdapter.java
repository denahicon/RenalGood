package com.example.renalgood.ListadeAlimentos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renalgood.R;

import java.util.ArrayList;
import java.util.List;

public class AlimentoAdapter extends RecyclerView.Adapter<AlimentoAdapter.AlimentoViewHolder> {
    private static final String TAG = "AlimentoAdapter";
    private List<Alimento> alimentos = new ArrayList<>();
    private final OnAlimentoClickListener listener;

    public interface OnAlimentoClickListener {
        void onAlimentoClick(Alimento alimento);
    }

    public AlimentoAdapter(List<Alimento> alimentos, OnAlimentoClickListener listener) {
        this.alimentos = alimentos != null ? alimentos : new ArrayList<>();
        this.listener = listener;
    }

    public void setAlimentos(List<Alimento> alimentos) {
        this.alimentos = alimentos != null ? alimentos : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AlimentoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alimento, parent, false);
        return new AlimentoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlimentoViewHolder holder, int position) {
        Alimento alimento = alimentos.get(position);
        holder.bind(alimento, listener);
    }

    @Override
    public int getItemCount() {
        return alimentos != null ? alimentos.size() : 0;
    }

    static class AlimentoViewHolder extends RecyclerView.ViewHolder {
        private final TextView nombreTextView;
        private final TextView detallesTextView;

        public AlimentoViewHolder(@NonNull View itemView) {
            super(itemView);
            nombreTextView = itemView.findViewById(R.id.nombreAlimentoTextView);
            detallesTextView = itemView.findViewById(R.id.energiaAlimentoTextView);
        }

        public void bind(final Alimento alimento, final OnAlimentoClickListener listener) {
            // Mostrar el nombre del alimento (que está en el campo ALIMENTOS)
            nombreTextView.setText(alimento.getNombre());

            // Mostrar detalles relevantes
            String detalles = String.format("%d Kcal - %s %s",
                    (int) alimento.getEnergia(),
                    alimento.getCantidadSugerida(),
                    alimento.getUnidad());
            detallesTextView.setText(detalles);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAlimentoClick(alimento);
                }
            });
        }
    }
}
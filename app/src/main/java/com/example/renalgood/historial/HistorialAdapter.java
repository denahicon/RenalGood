package com.example.renalgood.historial;

import android.icu.text.SimpleDateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.renalgood.R;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistorialAdapter extends ListAdapter<DailyMealHistory, HistorialAdapter.HistorialViewHolder> {

    protected HistorialAdapter() {
        super(new DiffCallback());
    }

    @NonNull
    @Override
    public HistorialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_historial_dia, parent, false);
        return new HistorialViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistorialViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class HistorialViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvFecha;
        private final TextView tvCaloriasMeta;
        private final TextView tvCaloriasConsumidas;
        private final RecyclerView rvComidas;

        public HistorialViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvCaloriasMeta = itemView.findViewById(R.id.tvCaloriasMeta);
            tvCaloriasConsumidas = itemView.findViewById(R.id.tvCaloriasConsumidas);
            rvComidas = itemView.findViewById(R.id.rvComidas);
        }

        void bind(DailyMealHistory history) {
            tvFecha.setText(formatDate(history.getCreatedAt()));
            tvCaloriasMeta.setText(String.format("Meta: %.0f kcal", history.getTargetCalories()));
            tvCaloriasConsumidas.setText(String.format("Consumido: %d kcal", history.getCaloriasDiarias()));

            // Configurar el RecyclerView para las comidas del día
            List<MealRecord> mealsList = new ArrayList<>(history.getMeals().values());
            ComidasAdapter comidasAdapter = new ComidasAdapter(mealsList);
            rvComidas.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            rvComidas.setAdapter(comidasAdapter);
        }

        private String formatDate(Date date) {
            return new SimpleDateFormat("EEEE dd/MM", Locale.getDefault()).format(date);
        }
    }

    static class DiffCallback extends DiffUtil.ItemCallback<DailyMealHistory> {
        @Override
        public boolean areItemsTheSame(@NonNull DailyMealHistory oldItem, @NonNull DailyMealHistory newItem) {
            return oldItem.getCreatedAt().equals(newItem.getCreatedAt());
        }

        @Override
        public boolean areContentsTheSame(@NonNull DailyMealHistory oldItem, @NonNull DailyMealHistory newItem) {
            return oldItem.equals(newItem);
        }
    }
}
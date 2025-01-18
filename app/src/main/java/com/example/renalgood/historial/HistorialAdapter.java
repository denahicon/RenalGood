package com.example.renalgood.historial;

import android.icu.text.SimpleDateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.renalgood.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.HistorialViewHolder> {
    private List<DailyMealHistory> historialList;
    private final SimpleDateFormat dateFormat;

    public HistorialAdapter(List<DailyMealHistory> historialList) {
        this.historialList = historialList;
        this.dateFormat = new SimpleDateFormat("EEEE dd/MM", Locale.getDefault());
    }

    public void submitList(List<DailyMealHistory> newList) {
        this.historialList = newList;
        notifyDataSetChanged();
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
        DailyMealHistory daily = historialList.get(position);
        holder.bind(daily);
    }

    @Override
    public int getItemCount() {
        return historialList.size();
    }

    class HistorialViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvFecha;
        private final TextView tvMeta;
        private final TextView tvConsumido;
        private final RecyclerView rvComidas;
        private final ProgressBar pbCalorias;

        HistorialViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvConsumido = itemView.findViewById(R.id.tvConsumido);
            rvComidas = itemView.findViewById(R.id.rvComidas);
            pbCalorias = itemView.findViewById(R.id.pbCalorias);

            rvComidas.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
        }

        void bind(DailyMealHistory daily) {
            tvFecha.setText(dateFormat.format(daily.getDate()));
            tvMeta.setText(String.format("Meta: %.0f kcal", daily.getTargetCalories()));
            tvConsumido.setText(String.format("Consumido: %.0f kcal", (double)daily.getCaloriasDiarias()));

            double progress = (daily.getCaloriasDiarias() * 100.0) / daily.getTargetCalories();
            pbCalorias.setProgress((int)progress);

            List<MealRecord> mealsList = new ArrayList<>(daily.getMeals().values());
            MealRecordAdapter mealAdapter = new MealRecordAdapter(mealsList);
            rvComidas.setAdapter(mealAdapter);
        }
    }
}
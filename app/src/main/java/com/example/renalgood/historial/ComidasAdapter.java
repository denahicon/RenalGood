package com.example.renalgood.historial;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.renalgood.R;
import java.util.List;

public class ComidasAdapter extends RecyclerView.Adapter<ComidasAdapter.ComidaViewHolder> {
    private List<MealRecord> mealsList;

    public ComidasAdapter(List<MealRecord> mealsList) {
        this.mealsList = mealsList;
    }

    @NonNull
    @Override
    public ComidaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comida, parent, false);
        return new ComidaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ComidaViewHolder holder, int position) {
        holder.bind(mealsList.get(position));
    }

    @Override
    public int getItemCount() {
        return mealsList.size();
    }

    static class ComidaViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTipoComida;
        private final TextView tvNombreReceta;
        private final TextView tvCalorias;

        public ComidaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTipoComida = itemView.findViewById(R.id.tvTipoComida);
            tvNombreReceta = itemView.findViewById(R.id.tvNombreReceta);
            tvCalorias = itemView.findViewById(R.id.tvCalorias);
        }

        void bind(MealRecord meal) {
            tvTipoComida.setText(meal.getMealType());
            tvNombreReceta.setText(meal.getRecipeName());
            tvCalorias.setText(String.format("%.0f kcal", meal.getCalories()));
        }
    }
}
package com.example.renalgood.historial;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.renalgood.R;
import java.util.List;

public class MealRecordAdapter extends RecyclerView.Adapter<MealRecordAdapter.MealViewHolder> {
    private List<MealRecord> meals;

    public MealRecordAdapter(List<MealRecord> meals) {
        this.meals = meals;
    }

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meal_record, parent, false);
        return new MealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        MealRecord meal = meals.get(position);
        holder.bind(meal);
    }

    @Override
    public int getItemCount() {
        return meals.size();
    }

    static class MealViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMealType;
        private final TextView tvRecipeName;
        private final TextView tvCalories;

        public MealViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMealType = itemView.findViewById(R.id.tvMealType);
            tvRecipeName = itemView.findViewById(R.id.tvRecipeName);
            tvCalories = itemView.findViewById(R.id.tvCalories);
        }

        void bind(MealRecord meal) {
            tvMealType.setText(meal.getMealType());
            tvRecipeName.setText(meal.getRecipeName());
            // Cambiar el formato para usar %.0f en lugar de %d
            tvCalories.setText(String.format("%.0f kcal", meal.getCalories()));
        }
    }
}
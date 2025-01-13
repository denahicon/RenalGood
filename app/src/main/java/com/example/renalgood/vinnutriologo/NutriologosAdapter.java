package com.example.renalgood.vinnutriologo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.renalgood.Nutriologo.Nutriologo;
import com.example.renalgood.R;

import java.util.ArrayList;
import java.util.List;

public class NutriologosAdapter extends RecyclerView.Adapter<NutriologosAdapter.NutriologoViewHolder> {
    private List<Nutriologo> nutriologos = new ArrayList<>();
    private OnNutriologoClickListener listener;

    public interface OnNutriologoClickListener {
        void onNutriologoClick(Nutriologo nutriologo);
    }

    public NutriologosAdapter(OnNutriologoClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public NutriologoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nutriologo, parent, false);
        return new NutriologoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NutriologoViewHolder holder, int position) {
        Nutriologo nutriologo = nutriologos.get(position);

        // Configurar los datos en las vistas
        holder.nombreTextView.setText(nutriologo.getNombre());
        holder.especialidadTextView.setText(nutriologo.getAreaEspecializacion());
        holder.experienciaTextView.setText("Experiencia: " + nutriologo.getAnosExperiencia() + " años");
        holder.direccionTextView.setText(nutriologo.getDireccionClinica());

        // Cargar la imagen del nutriólogo si existe
        if (nutriologo.getPhotoUrl() != null && !nutriologo.getPhotoUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(nutriologo.getPhotoUrl())
                    .circleCrop()
                    .placeholder(android.R.drawable.ic_menu_camera)
                    .into(holder.profileImageView);
        } else {
            holder.profileImageView.setImageResource(android.R.drawable.ic_menu_camera);
        }

        // Configurar el clic en el item para abrir NutriologoProfileActivity
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNutriologoClick(nutriologo);
            }
        });
    }

    @Override
    public int getItemCount() {
        return nutriologos.size();
    }

    public void setNutriologos(List<Nutriologo> nutriologos) {
        this.nutriologos = nutriologos;
        notifyDataSetChanged();
    }

    static class NutriologoViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImageView;
        TextView nombreTextView;
        TextView especialidadTextView;
        TextView experienciaTextView;
        TextView direccionTextView;

        NutriologoViewHolder(View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.profileImageView);
            nombreTextView = itemView.findViewById(R.id.nombreTextView);
            especialidadTextView = itemView.findViewById(R.id.especialidadTextView);
            experienciaTextView = itemView.findViewById(R.id.experienciaTextView);
            direccionTextView = itemView.findViewById(R.id.direccionTextView);
        }
    }
}
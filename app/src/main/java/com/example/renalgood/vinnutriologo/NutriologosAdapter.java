package com.example.renalgood.vinnutriologo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.renalgood.Nutriologo.Nutriologo;
import com.example.renalgood.R;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;

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

        holder.nombreTextView.setText(nutriologo.getNombre());
        holder.especialidadChip.setText(nutriologo.getAreaEspecializacion());
        holder.experienciaTextView.setText("Experiencia: " + nutriologo.getAnosExperiencia() + " años");
        holder.direccionTextView.setText(nutriologo.getDireccionClinica());

        if (nutriologo.getSelfieUrl() != null && !nutriologo.getSelfieUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(nutriologo.getSelfieUrl())
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(holder.profileImageView);
        } else {
            holder.profileImageView.setImageResource(R.drawable.default_profile);
        }

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
        CircleImageView profileImageView;
        TextView nombreTextView;
        Chip especialidadChip;
        TextView experienciaTextView;
        TextView direccionTextView;

        NutriologoViewHolder(View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.profileImageView);
            nombreTextView = itemView.findViewById(R.id.nombreTextView);
            especialidadChip = itemView.findViewById(R.id.especialidadChip);
            experienciaTextView = itemView.findViewById(R.id.experienciaTextView);
            direccionTextView = itemView.findViewById(R.id.direccionTextView);
        }
    }
}
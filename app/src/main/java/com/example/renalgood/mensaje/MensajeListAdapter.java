package com.example.renalgood.mensaje;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.renalgood.R;
import java.util.List;

public class MensajeListAdapter extends RecyclerView.Adapter<MensajeListAdapter.ViewHolder> {
    private List<MensajeList> mensajeList;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(MensajeList mensaje);
    }

    public MensajeListAdapter(List<MensajeList> mensajeList, OnItemClickListener listener) {
        this.mensajeList = mensajeList;
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mensaje, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(mensajeList.get(position), onItemClickListener);
    }

    @Override
    public int getItemCount() {
        return mensajeList.size();
    }

    public void updateList(List<MensajeList> newList) {
        this.mensajeList = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView nombreTextView;
        private final TextView mensajeTextView;
        private final TextView horaTextView;
        private final ImageView profileImageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nombreTextView = itemView.findViewById(R.id.tvNombre);
            mensajeTextView = itemView.findViewById(R.id.tvMensaje);
            horaTextView = itemView.findViewById(R.id.tvHora);
            profileImageView = itemView.findViewById(R.id.ivFotoPerfil);
        }

        public void bind(final MensajeList mensaje, final OnItemClickListener listener) {
            if (nombreTextView != null) nombreTextView.setText(mensaje.getNombre());
            if (mensajeTextView != null) mensajeTextView.setText(mensaje.getUltimoMensaje());
            if (horaTextView != null) horaTextView.setText(mensaje.getHora());

            if (profileImageView != null && mensaje.getProfilePic() != null && !mensaje.getProfilePic().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(mensaje.getProfilePic())
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .circleCrop()
                        .into(profileImageView);
            } else if (profileImageView != null) {
                profileImageView.setImageResource(R.drawable.default_profile);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(mensaje);
                }
            });
        }
    }
}
package com.example.renalgood.mensaje;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.renalgood.R;
import java.util.ArrayList;
import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;

public class MensajeListAdapter extends RecyclerView.Adapter<MensajeListAdapter.ViewHolder> {
    private List<MensajeList> mensajeList;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(MensajeList mensaje);
    }

    public MensajeListAdapter(List<MensajeList> mensajeList, OnItemClickListener onItemClickListener) {
        this.mensajeList = mensajeList;
        this.onItemClickListener = onItemClickListener;
    }

    public void updateList(List<MensajeList> newMensajeList) {
        this.mensajeList = newMensajeList;
        notifyDataSetChanged();
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
        MensajeList mensaje = mensajeList.get(position);
        holder.bind(mensaje, onItemClickListener);
    }

    @Override
    public int getItemCount() {
        return mensajeList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView nombreTextView;
        public TextView ultimoMensajeTextView;
        public TextView horaTextView;
        public ImageView profileImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            nombreTextView = itemView.findViewById(R.id.tvContactName);
            ultimoMensajeTextView = itemView.findViewById(R.id.tvLastMessage);
            horaTextView = itemView.findViewById(R.id.tvTimestamp);
            profileImageView = itemView.findViewById(R.id.ivProfilePic);
        }

        public void bind(final MensajeList mensaje, final OnItemClickListener listener) {
            nombreTextView.setText(mensaje.getNombre());
            ultimoMensajeTextView.setText(mensaje.getUltimoMensaje());
            horaTextView.setText(mensaje.getHora());

            if (mensaje.getProfilePic() != null && !mensaje.getProfilePic().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(mensaje.getProfilePic())
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(profileImageView);
            } else {
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
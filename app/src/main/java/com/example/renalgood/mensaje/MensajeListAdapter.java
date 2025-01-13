package com.example.renalgood.mensaje;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.renalgood.R;
import java.util.ArrayList;
import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;

public class MensajeListAdapter extends RecyclerView.Adapter<MensajeListAdapter.ViewHolder> {
    private List<MensajeList> mensajes;
    private final OnMensajeClickListener listener;

    public interface OnMensajeClickListener {
        void onMensajeClick(MensajeList mensaje);
    }

    public MensajeListAdapter(List<MensajeList> mensajes, OnMensajeClickListener listener) {
        this.mensajes = new ArrayList<>(mensajes);
        this.listener = listener;
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
        MensajeList mensaje = mensajes.get(position);
        holder.bind(mensaje, listener);
    }

    @Override
    public int getItemCount() {
        return mensajes.size();
    }

    public void updateList(List<MensajeList> newMensajes) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new MensajeDiffCallback(mensajes, newMensajes));
        mensajes = new ArrayList<>(newMensajes);
        diffResult.dispatchUpdatesTo(this);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView ivProfilePic;
        private final TextView tvNombre;
        private final TextView tvUltimoMensaje;
        private final TextView tvHora;

        ViewHolder(View itemView) {
            super(itemView);
            ivProfilePic = itemView.findViewById(R.id.ivProfilePic);
            tvNombre = itemView.findViewById(R.id.tvName);
            tvUltimoMensaje = itemView.findViewById(R.id.tvLastMessage);
            tvHora = itemView.findViewById(R.id.tvTime);
        }

        void bind(final MensajeList mensaje, final OnMensajeClickListener listener) {
            tvNombre.setText(mensaje.getNombre());
            tvUltimoMensaje.setText(mensaje.getUltimoMensaje());
            tvHora.setText(mensaje.getHora());

            if (mensaje.getProfilePic() != null && !mensaje.getProfilePic().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(mensaje.getProfilePic())
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(ivProfilePic);
            } else {
                ivProfilePic.setImageResource(R.drawable.default_profile);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMensajeClick(mensaje);
                }
            });
        }
    }

    private static class MensajeDiffCallback extends DiffUtil.Callback {
        private final List<MensajeList> oldList;
        private final List<MensajeList> newList;

        MensajeDiffCallback(List<MensajeList> oldList, List<MensajeList> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getPacienteId()
                    .equals(newList.get(newItemPosition).getPacienteId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            MensajeList oldItem = oldList.get(oldItemPosition);
            MensajeList newItem = newList.get(newItemPosition);
            return oldItem.getUltimoMensaje().equals(newItem.getUltimoMensaje())
                    && oldItem.getHora().equals(newItem.getHora());
        }
    }
}
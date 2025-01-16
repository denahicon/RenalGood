package com.example.renalgood.mensaje;

import android.icu.text.SimpleDateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.renalgood.R;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MensajeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TIPO_ENVIADO = 1;
    private static final int TIPO_RECIBIDO = 2;

    private List<Mensaje> mensajes;
    private String idUsuarioActual;

    public MensajeAdapter(List<Mensaje> mensajes, String idUsuarioActual) {
        this.mensajes = mensajes;
        this.idUsuarioActual = idUsuarioActual;
    }

    @Override
    public int getItemViewType(int position) {
        Mensaje mensaje = mensajes.get(position);
        String emisorId = mensaje.getEmisorId();
        // Protección contra nulls
        return (emisorId != null && emisorId.equals(idUsuarioActual)) ? TIPO_ENVIADO : TIPO_RECIBIDO;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TIPO_ENVIADO) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new MensajeEnviadoViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new MensajeRecibidoViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Mensaje mensaje = mensajes.get(position);
        if (getItemViewType(position) == TIPO_ENVIADO) {
            ((MensajeEnviadoViewHolder) holder).bind(mensaje);
        } else {
            ((MensajeRecibidoViewHolder) holder).bind(mensaje);
        }
    }

    @Override
    public int getItemCount() {
        return mensajes.size();
    }

    static class MensajeEnviadoViewHolder extends RecyclerView.ViewHolder {
        TextView textoMensaje, textoHora;

        MensajeEnviadoViewHolder(View itemView) {
            super(itemView);
            textoMensaje = itemView.findViewById(R.id.texto_mensaje_enviado);
            textoHora = itemView.findViewById(R.id.texto_hora_enviado);
        }

        void bind(Mensaje mensaje) {
            if (textoMensaje != null) {
                textoMensaje.setText(mensaje.getMensaje());
            }
            if (textoHora != null && mensaje.getTimestamp() != 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                textoHora.setText(sdf.format(new Date(mensaje.getTimestamp())));
            }
        }
    }

    static class MensajeRecibidoViewHolder extends RecyclerView.ViewHolder {
        TextView textoMensaje, textoHora;

        MensajeRecibidoViewHolder(View itemView) {
            super(itemView);
            textoMensaje = itemView.findViewById(R.id.texto_mensaje_recibido);
            textoHora = itemView.findViewById(R.id.texto_hora_recibido);
        }

        void bind(Mensaje mensaje) {
            if (textoMensaje != null) {
                textoMensaje.setText(mensaje.getMensaje());
            }
            if (textoHora != null && mensaje.getTimestamp() != 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                textoHora.setText(sdf.format(new Date(mensaje.getTimestamp())));
            }
        }
    }
}
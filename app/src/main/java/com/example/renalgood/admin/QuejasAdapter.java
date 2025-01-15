package com.example.renalgood.admin;

import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.renalgood.R;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QuejasAdapter extends RecyclerView.Adapter<QuejasAdapter.QuejaViewHolder> {
    private List<Queja> quejas;
    private FirebaseFirestore db;
    private Context context;

    public QuejasAdapter() {
        this.quejas = new ArrayList<>();
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public QuejaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext(); // Guardamos el context
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_queja, parent, false);
        return new QuejaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuejaViewHolder holder, int position) {
        Queja queja = quejas.get(position);

        holder.tvTipo.setText(queja.getTipo().toUpperCase());
        holder.tvEmail.setText(queja.getEmail());
        holder.tvTexto.setText(queja.getTexto());
        holder.tvFecha.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm",
                Locale.getDefault()).format(new Date(queja.getFecha())));

        if ("pendiente".equals(queja.getEstado())) {
            holder.btnMarcarAtendido.setVisibility(View.VISIBLE);
        } else {
            holder.btnMarcarAtendido.setVisibility(View.GONE);
        }

        holder.btnMarcarAtendido.setOnClickListener(v -> marcarComoAtendido(queja));
    }

    @Override
    public int getItemCount() {
        return quejas.size();
    }

    public void actualizarQuejas(List<Queja> nuevasQuejas) {
        Log.d("QuejasAdapter", "Actualizando quejas: " + nuevasQuejas.size());
        this.quejas = nuevasQuejas;
        notifyDataSetChanged();
    }

    private void marcarComoAtendido(Queja queja) {
        String coleccion = queja.getTipoUsuario().equals("paciente") ?
                "comentariosPacientes" : "comentariosNutriologos";

        // En lugar de actualizar, eliminamos el documento
        db.collection(coleccion)
                .document(queja.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Encontrar el índice de la queja eliminada
                    int position = quejas.indexOf(queja);
                    if (position != -1) {
                        quejas.remove(position);
                        notifyItemRemoved(position);
                    }

                    // Enviar notificación al usuario
                    enviarNotificacionUsuario(queja);

                    if (context != null) {
                        Toast.makeText(context, "Queja atendida y eliminada", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (context != null) {
                        Toast.makeText(context, "Error al eliminar la queja", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void enviarNotificacionUsuario(Queja queja) {
        // Crear una nueva colección para notificaciones
        Map<String, Object> notificacion = new HashMap<>();
        notificacion.put("userId", queja.getUserId());
        notificacion.put("mensaje", "Tu " + queja.getTipo() + " ha sido atendida por el administrador");
        notificacion.put("fecha", System.currentTimeMillis());
        notificacion.put("leida", false);

        db.collection("notificaciones")
                .add(notificacion)
                .addOnSuccessListener(documentReference ->
                        Log.d("QuejasAdapter", "Notificación enviada al usuario"))
                .addOnFailureListener(e ->
                        Log.e("QuejasAdapter", "Error enviando notificación", e));
    }

    static class QuejaViewHolder extends RecyclerView.ViewHolder {
        TextView tvTipo, tvEmail, tvTexto, tvFecha;
        Button btnMarcarAtendido;

        QuejaViewHolder(View itemView) {
            super(itemView);
            tvTipo = itemView.findViewById(R.id.tvTipo);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvTexto = itemView.findViewById(R.id.tvTexto);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            btnMarcarAtendido = itemView.findViewById(R.id.btnMarcarAtendido);
        }
    }
}
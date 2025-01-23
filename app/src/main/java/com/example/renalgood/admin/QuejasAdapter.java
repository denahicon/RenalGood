package com.example.renalgood.admin;

import android.content.Context;
import android.icu.text.SimpleDateFormat;
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

public class QuejasAdapter extends RecyclerView.Adapter<QuejasAdapter.ViewHolder> {
    private List<Queja> quejas = new ArrayList<>();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_queja, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Queja queja = quejas.get(position);
        Context context = holder.itemView.getContext();

        holder.tvTipo.setText(queja.getTipo().substring(0, 1).toUpperCase() + queja.getTipo().substring(1));
        holder.tvEmail.setText(queja.getEmail());
        holder.tvTexto.setText(queja.getTexto());

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String fechaFormateada = sdf.format(new Date(queja.getFecha()));
        holder.tvFecha.setText(fechaFormateada);

        if ("atendido".equals(queja.getEstado())) {
            holder.btnMarcarAtendido.setVisibility(View.GONE);
        } else {
            holder.btnMarcarAtendido.setVisibility(View.VISIBLE);
            holder.btnMarcarAtendido.setOnClickListener(v -> atenderYEliminarQueja(queja, context));
        }
    }

    private void atenderYEliminarQueja(Queja queja, Context context) {
        String coleccion = queja.getTipoUsuario() != null && queja.getTipoUsuario().equals("nutriologo")
                ? "comentariosNutriologos"
                : "comentariosPacientes";

        Map<String, Object> notificacion = new HashMap<>();
        notificacion.put("userId", queja.getUserId());
        notificacion.put("mensaje", "Tu " + queja.getTipo() + " ha sido atendida por el administrador");
        notificacion.put("leida", false);
        notificacion.put("tipoUsuario", queja.getTipoUsuario() != null ? queja.getTipoUsuario() : "paciente");
        notificacion.put("fecha", System.currentTimeMillis());

        // Actualizar el estado primero
        db.collection(coleccion)
                .document(queja.getId())
                .update("estado", "atendido")
                .addOnSuccessListener(aVoid -> {
                    // Después crear la notificación
                    db.collection("notificaciones")
                            .add(notificacion)
                            .addOnSuccessListener(notifRef -> {
                                // Actualizar la UI
                                quejas.remove(queja);
                                notifyDataSetChanged();
                                Toast.makeText(context,
                                        queja.getTipo() + " atendida correctamente",
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context,
                            "Error al procesar la " + queja.getTipo(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return quejas.size();
    }

    public void actualizarQuejas(List<Queja> nuevasQuejas) {
        this.quejas = nuevasQuejas;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTipo, tvEmail, tvTexto, tvFecha;
        Button btnMarcarAtendido;

        ViewHolder(View itemView) {
            super(itemView);
            tvTipo = itemView.findViewById(R.id.tvTipo);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvTexto = itemView.findViewById(R.id.tvTexto);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            btnMarcarAtendido = itemView.findViewById(R.id.btnMarcarAtendido);
        }
    }
}
package com.example.renalgood.admin;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.renalgood.R;
import java.util.List;
import java.util.Locale;

public class SolicitudesAdapter extends RecyclerView.Adapter<SolicitudesAdapter.SolicitudViewHolder> {
    private List<NotificacionAdmin> solicitudes;
    private OnSolicitudClickListener listener;
    private Context context;

    public interface OnSolicitudClickListener {
        void onVerDetallesClick(NotificacionAdmin solicitud);
        void onAprobarClick(NotificacionAdmin solicitud);
        void onRechazarClick(NotificacionAdmin solicitud);
    }

    public SolicitudesAdapter(Context context, List<NotificacionAdmin> solicitudes, OnSolicitudClickListener listener) {
        this.context = context;
        this.solicitudes = solicitudes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SolicitudViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_solicitud, parent, false);
        return new SolicitudViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SolicitudViewHolder holder, int position) {
        NotificacionAdmin solicitud = solicitudes.get(position);
        holder.bind(solicitud, listener);

        try {
            if (solicitud.getId() != null) {
                // Primero intentar con la URL directa
                if (solicitud.getIdentificacionUrl() != null && !solicitud.getIdentificacionUrl().isEmpty()) {
                    ImageLoadUtils.cargarImagen(context,
                            solicitud.getIdentificacionUrl(),
                            holder.ivIdentificacion,
                            R.drawable.ic_add_photo);
                } else {
                    // Si no hay URL directa, intentar cargar desde el path en Storage
                    String basePath = "solicitudes/" + solicitud.getId() + "/";
                    ImageLoadUtils.cargarImagenDesdeStorage(context,
                            basePath + "identificacion.jpg",
                            holder.ivIdentificacion,
                            R.drawable.ic_add_photo);
                }
            } else {
                // Si no hay ID, mostrar placeholder
                if (holder.ivIdentificacion != null) {
                    holder.ivIdentificacion.setImageResource(R.drawable.ic_add_photo);
                }
            }
        } catch (Exception e) {
            Log.e("SolicitudesAdapter", "Error cargando imagen: " + e.getMessage());
            if (holder.ivIdentificacion != null) {
                holder.ivIdentificacion.setImageResource(R.drawable.ic_add_photo);
            }
        }
    }

    @Override
    public int getItemCount() {
        return solicitudes.size();
    }

    static class SolicitudViewHolder extends RecyclerView.ViewHolder {
        private TextView tvNombre;
        private TextView tvCedula;
        private TextView tvFecha;
        private ImageView ivIdentificacion;
        private Button btnVerDetalles;
        private Button btnAprobar;
        private Button btnRechazar;

        public SolicitudViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tv_nombre);
            tvCedula = itemView.findViewById(R.id.tv_cedula);
            tvFecha = itemView.findViewById(R.id.tv_fecha);
            ivIdentificacion = itemView.findViewById(R.id.iv_identificacion);
            btnVerDetalles = itemView.findViewById(R.id.btn_ver_detalles);
            btnAprobar = itemView.findViewById(R.id.btn_aprobar);
            btnRechazar = itemView.findViewById(R.id.btn_rechazar);
        }

        void bind(NotificacionAdmin solicitud, OnSolicitudClickListener listener) {
            tvNombre.setText(solicitud.getNombre());
            tvCedula.setText("Cédula: " + solicitud.getNumeroCedula());
            if (solicitud.getFecha() != null) {
                tvFecha.setText(new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        .format(solicitud.getFecha()));
            }

            btnVerDetalles.setOnClickListener(v -> listener.onVerDetallesClick(solicitud));
            btnAprobar.setOnClickListener(v -> listener.onAprobarClick(solicitud));
            btnRechazar.setOnClickListener(v -> listener.onRechazarClick(solicitud));
        }
    }
}
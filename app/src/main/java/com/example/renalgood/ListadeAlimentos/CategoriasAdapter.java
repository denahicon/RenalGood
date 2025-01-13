package com.example.renalgood.ListadeAlimentos;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.RelativeLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.renalgood.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoriasAdapter extends RecyclerView.Adapter<CategoriasAdapter.CategoriaViewHolder> {

    private static final String TAG = "CategoriasAdapter";
    private final Context context;
    private final String[] categorias;
    private final Map<String, List<Alimento>> alimentosPorCategoria;
    private final Map<String, Boolean> expansionEstados;
    private final Map<String, AlimentoAdapter> alimentoAdapters;

    public CategoriasAdapter(Context context, String[] categorias) {
        this.context = context;
        this.categorias = categorias;
        this.alimentosPorCategoria = new HashMap<>();
        this.expansionEstados = new HashMap<>();
        this.alimentoAdapters = new HashMap<>();

        // Inicializar estados
        for (String categoria : categorias) {
            expansionEstados.put(categoria, false);
            alimentosPorCategoria.put(categoria, new ArrayList<>());
        }
    }

    @Override
    public CategoriaViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_categoria, parent, false);
        return new CategoriaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CategoriaViewHolder holder, int position) {
        String categoria = categorias[position];
        holder.bind(categoria);
    }

    @Override
    public int getItemCount() {
        return categorias.length;
    }

    public void actualizarAlimentos(String categoria, List<Alimento> alimentos) {
        try {
            if (alimentos == null) {
                Log.e(TAG, "Lista de alimentos nula para categoría: " + categoria);
                return;
            }

            Log.d(TAG, "Actualizando " + categoria + " con " + alimentos.size() + " alimentos");
            alimentosPorCategoria.put(categoria, new ArrayList<>(alimentos));

            AlimentoAdapter adapter = alimentoAdapters.get(categoria);
            if (adapter != null) {
                adapter.setAlimentos(alimentos);
                adapter.notifyDataSetChanged();
            }

            notifyDataSetChanged();
        } catch (Exception e) {
            Log.e(TAG, "Error actualizando alimentos: " + e.getMessage());
        }
    }

    class CategoriaViewHolder extends RecyclerView.ViewHolder {
        private final TextView categoriaTextView;
        private final ImageView flechaImageView;
        private final RecyclerView alimentosRecyclerView;
        private final RelativeLayout headerLayout;

        public CategoriaViewHolder(View itemView) {
            super(itemView);
            categoriaTextView = itemView.findViewById(R.id.categoriaTextView);
            flechaImageView = itemView.findViewById(R.id.flechaImageView);
            alimentosRecyclerView = itemView.findViewById(R.id.alimentosRecyclerView);
            headerLayout = itemView.findViewById(R.id.headerLayout);
        }

        public void bind(final String categoria) {
            try {
                categoriaTextView.setText(categoria);
                boolean expandido = expansionEstados.getOrDefault(categoria, false);
                flechaImageView.setRotation(expandido ? 180 : 0);
                alimentosRecyclerView.setVisibility(expandido ? View.VISIBLE : View.GONE);

                // Configurar RecyclerView si aún no está configurado
                if (!alimentoAdapters.containsKey(categoria)) {
                    alimentosRecyclerView.setLayoutManager(new LinearLayoutManager(context));
                    AlimentoAdapter adapter = new AlimentoAdapter(
                            alimentosPorCategoria.getOrDefault(categoria, new ArrayList<>()),
                            alimento -> {
                                try {
                                    Intent intent = new Intent(context, DetalleAlimentoActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                    Bundle bundle = new Bundle();
                                    // Datos básicos
                                    bundle.putString("nombre", alimento.getNombre());
                                    bundle.putString("cantidadSugerida", alimento.getCantidadSugerida());
                                    bundle.putString("unidad", alimento.getUnidad());

                                    // Datos nutricionales principales
                                    bundle.putDouble("energia", alimento.getEnergia());
                                    bundle.putString("proteina", alimento.getProteina());
                                    bundle.putDouble("lipidos", alimento.getLipidos());
                                    bundle.putDouble("hidratosCarbono", alimento.getHidratosCarbono());
                                    bundle.putString("fibra", alimento.getFibra());

                                    // Vitaminas y minerales
                                    bundle.putDouble("acidoAscorbico", alimento.getAcidoAscorbico());
                                    bundle.putString("acidoFolico", alimento.getAcidoFolico());
                                    bundle.putString("vitaminaA", alimento.getVitaminaA());
                                    bundle.putDouble("hierroNoHem", alimento.getHierroNoHem());
                                    bundle.putDouble("potasio", alimento.getPotasio());

                                    // Índices y medidas
                                    bundle.putDouble("pesoBrutoRedondeado", alimento.getPesoBrutoRedondeado());
                                    bundle.putDouble("pesoNeto", alimento.getPesoNeto());
                                    bundle.putString("indiceGlicemico", alimento.getIndiceGlicemico());
                                    bundle.putString("cargaGlicemica", alimento.getCargaGlicemica());
                                    bundle.putDouble("azucarEquivalente", alimento.getAzucarEquivalente());

                                    // Campos específicos de leguminosas
                                    if (categoria.equals("Leguminosas")) {
                                        bundle.putString("selenio", alimento.getSelenio());
                                        bundle.putDouble("sodio", alimento.getSodio());
                                        bundle.putString("fosforo", alimento.getFosforo());
                                    }

                                    intent.putExtras(bundle);
                                    context.startActivity(intent);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error al abrir DetalleAlimentoActivity: " + e.getMessage());
                                }
                            }
                    );
                    alimentosRecyclerView.setAdapter(adapter);
                    alimentoAdapters.put(categoria, adapter);
                }

                headerLayout.setOnClickListener(v -> {
                    boolean nuevoEstado = !expansionEstados.getOrDefault(categoria, false);
                    expansionEstados.put(categoria, nuevoEstado);
                    alimentosRecyclerView.setVisibility(nuevoEstado ? View.VISIBLE : View.GONE);
                    flechaImageView.animate()
                            .rotation(nuevoEstado ? 180 : 0)
                            .setDuration(200)
                            .start();

                    if (nuevoEstado) {
                        AlimentoAdapter adapter = alimentoAdapters.get(categoria);
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error en bind: " + e.getMessage());
            }
        }
    }
}
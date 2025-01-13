package com.example.renalgood.ListadeAlimentos;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.renalgood.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import com.example.renalgood.Chat.ChatActivity;
import com.example.renalgood.Paciente.BuzonQuejasPaciente;
import com.example.renalgood.agendarcitap.CalendarioActivity;
import com.example.renalgood.Paciente.PacienteActivity;
import com.example.renalgood.recetas.RecetasActivity;

public class ListadeAlimentosActivity extends AppCompatActivity {
    private static final String TAG = "ListadeAlimentos";
    private RecyclerView recyclerView;
    private CategoriasAdapter categoriasAdapter; // Cambiaremos a un nuevo adapter
    private SearchView searchView;
    private FirebaseFirestore db;
    private ImageView ivHome;
    private ImageView ivLupa;
    private ImageView ivChef;
    private ImageView ivMensaje;
    private ImageView ivCarta;
    private ImageView ivCalendario;

    // Mapa para almacenar los alimentos por categoría
    private Map<String, List<Alimento>> alimentosPorCategoria = new HashMap<>();
    private Map<String, List<Alimento>> alimentosCompletoPorCategoria = new HashMap<>();

    // Lista de categorías
    private final String[] CATEGORIAS = {
            "Frutas",
            "Verduras",
            "Leguminosas",
            "Cereales con grasa",
            "Leche entera"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_listade_alimentos);

        inicializarVistas();
        configurarRecyclerView();
        configurarSearchView();
        setupNavigationListeners();

        // Cargar todas las categorías
        for (String categoria : CATEGORIAS) {
            cargarAlimentosPorCategoria(categoria);
        }
    }

    private void setupNavigationListeners() {
        try {
            ivHome.setOnClickListener(v -> {
                Intent intent = new Intent(this, PacienteActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
            });

            ivLupa.setImageResource(R.drawable.ic_search);
            ivLupa.setColorFilter(getResources().getColor(R.color.pink_strong));

            ivChef.setOnClickListener(v -> {
                Intent intent = new Intent(this, RecetasActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
            });

            ivMensaje.setOnClickListener(v -> {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
            });

            ivCarta.setOnClickListener(v -> {
                Intent intent = new Intent(this, BuzonQuejasPaciente.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
            });

            ivCalendario.setOnClickListener(v -> {
                Intent intent = new Intent(this, CalendarioActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
            });
        } catch (Exception e) {
            Log.e(TAG, "Error configurando navegación: " + e.getMessage());
        }
    }

    private void inicializarVistas() {
        try {
            db = FirebaseFirestore.getInstance();
            recyclerView = findViewById(R.id.recyclerViewAlimentos);
            searchView = findViewById(R.id.searchViewAlimentos);

            // Referencias de navegación
            ivHome = findViewById(R.id.ivHome);
            ivLupa = findViewById(R.id.ivLupa);
            ivChef = findViewById(R.id.ivChef);
            ivMensaje = findViewById(R.id.ivMensaje);
            ivCarta = findViewById(R.id.ivCarta);
            ivCalendario = findViewById(R.id.ivCalendario);
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando vistas: " + e.getMessage());
            Toast.makeText(this, "Error iniciando la aplicación", Toast.LENGTH_SHORT).show();
        }
    }

    private void configurarRecyclerView() {
        try {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            categoriasAdapter = new CategoriasAdapter(this, CATEGORIAS);
            recyclerView.setAdapter(categoriasAdapter);
        } catch (Exception e) {
            Log.e(TAG, "Error configurando RecyclerView: " + e.getMessage());
        }
    }

    private void configurarSearchView() {
        try {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    filtrarAlimentos(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    filtrarAlimentos(newText);
                    return true;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error configurando SearchView: " + e.getMessage());
        }
    }

    private void cargarAlimentosPorCategoria(String categoria) {
        try {
            Log.d(TAG, "Iniciando carga de categoría: " + categoria);
            db.collection(categoria)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<Alimento> listaAlimentos = new ArrayList<>();
                        Log.d(TAG, "Documentos encontrados en " + categoria + ": " + queryDocumentSnapshots.size());

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            try {
                                Alimento alimento = document.toObject(Alimento.class);
                                if (alimento != null) {
                                    listaAlimentos.add(alimento);
                                    Log.d(TAG, "Alimento añadido: " + alimento.getNombre());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error al convertir documento a Alimento: " + e.getMessage());
                            }
                        }

                        alimentosPorCategoria.put(categoria, listaAlimentos);
                        alimentosCompletoPorCategoria.put(categoria, new ArrayList<>(listaAlimentos));

                        Log.d(TAG, "Actualizando adapter para " + categoria + " con " + listaAlimentos.size() + " elementos");
                        categoriasAdapter.actualizarAlimentos(categoria, listaAlimentos);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error cargando alimentos de " + categoria, e);
                        Toast.makeText(this, "Error al cargar " + categoria + ": " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error en cargarAlimentos: " + e.getMessage());
        }
    }

    private void filtrarAlimentos(String texto) {
        try {
            if (texto == null || texto.isEmpty()) {
                // Restaurar listas originales
                for (String categoria : CATEGORIAS) {
                    List<Alimento> listaOriginal = alimentosCompletoPorCategoria.get(categoria);
                    if (listaOriginal != null) {
                        categoriasAdapter.actualizarAlimentos(categoria, listaOriginal);
                    }
                }
                return;
            }

            String textoLower = texto.toLowerCase().trim();

            // Filtrar cada categoría
            for (String categoria : CATEGORIAS) {
                List<Alimento> listaCompleta = alimentosCompletoPorCategoria.get(categoria);
                if (listaCompleta != null) {
                    List<Alimento> listaFiltrada = new ArrayList<>();

                    for (Alimento alimento : listaCompleta) {
                        if (alimento != null && alimento.getNombre() != null &&
                                alimento.getNombre().toLowerCase().contains(textoLower)) {
                            listaFiltrada.add(alimento);
                        }
                    }

                    categoriasAdapter.actualizarAlimentos(categoria, listaFiltrada);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error filtrando alimentos: " + e.getMessage());
        }
    }
}
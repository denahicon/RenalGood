package com.example.renalgood.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renalgood.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class BuzonAdminActivity extends AppCompatActivity {
    private ImageView ivHome, ivCedulas, ivEmail, ivAddRecipe;
    private RecyclerView recyclerViewPacientes, recyclerViewNutriologos;
    private QuejasAdapter adapterPacientes, adapterNutriologos;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_buzon_admin);

        initViews();
        setupRecyclerViews();
        setupClickListeners();
        cargarQuejas();
    }

    private void initViews() {
        ivHome = findViewById(R.id.ivHome);
        ivCedulas = findViewById(R.id.ivCedulas);
        ivEmail = findViewById(R.id.ivEmail);
        ivAddRecipe = findViewById(R.id.ivAddRecipe);
        recyclerViewPacientes = findViewById(R.id.recyclerViewPacientes);
        recyclerViewNutriologos = findViewById(R.id.recyclerViewNutriologos);
        db = FirebaseFirestore.getInstance();
    }

    private void setupRecyclerViews() {
        // Configurar RecyclerView para pacientes
        recyclerViewPacientes.setLayoutManager(new LinearLayoutManager(this));
        adapterPacientes = new QuejasAdapter();
        recyclerViewPacientes.setAdapter(adapterPacientes);

        // Configurar RecyclerView para nutriólogos
        recyclerViewNutriologos.setLayoutManager(new LinearLayoutManager(this));
        adapterNutriologos = new QuejasAdapter();
        recyclerViewNutriologos.setAdapter(adapterNutriologos);
    }

    private void setupClickListeners() {
        ivHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminActivity.class);
            startActivity(intent);
            finish();
        });

        ivCedulas.setOnClickListener(v -> {
            Intent intent = new Intent(this, SolicitudesNutriologosActivity.class);
            startActivity(intent);
            finish();
        });

        ivEmail.setImageResource(R.drawable.ic_email);
        ivEmail.setColorFilter(getResources().getColor(R.color.red));

        ivAddRecipe.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminRecipeActivity.class);
            startActivity(intent);
        });
    }

    private void cargarQuejas() {
        // Cargar quejas de pacientes
        db.collection("comentariosPacientes")
                .orderBy("fecha", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("BuzonAdmin", "Error cargando quejas pacientes: " + error.getMessage());
                        Toast.makeText(this, "Error al cargar quejas de pacientes", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        List<Queja> quejas = new ArrayList<>();
                        for (DocumentSnapshot document : value.getDocuments()) {
                            Log.d("BuzonAdmin", "Documento paciente: " + document.getData());
                            Queja queja = document.toObject(Queja.class);
                            if (queja != null) {
                                queja.setId(document.getId());
                                quejas.add(queja);
                            }
                        }
                        Log.d("BuzonAdmin", "Quejas pacientes cargadas: " + quejas.size());
                        adapterPacientes.actualizarQuejas(quejas);
                    }
                });

        // Cargar quejas de nutriólogos
        db.collection("comentariosNutriologos")
                .orderBy("fecha", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error al cargar quejas de nutriólogos", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        List<Queja> quejas = new ArrayList<>();
                        for (DocumentSnapshot document : value.getDocuments()) {
                            Queja queja = document.toObject(Queja.class);
                            if (queja != null) {
                                queja.setId(document.getId());
                                quejas.add(queja);
                            }
                        }
                        adapterNutriologos.actualizarQuejas(quejas);
                    }
                });
    }
}
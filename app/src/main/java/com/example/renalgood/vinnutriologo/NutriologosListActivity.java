package com.example.renalgood.vinnutriologo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renalgood.Chat.ChatActivity;
import com.example.renalgood.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.renalgood.Nutriologo.Nutriologo;
import java.util.ArrayList;
import java.util.List;

public class NutriologosListActivity extends AppCompatActivity implements NutriologosAdapter.OnNutriologoClickListener {
    private static final String TAG = "NutriologosListActivity";
    private RecyclerView recyclerNutriologos;
    private FirebaseFirestore db;
    private NutriologosAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nutriologos_list);

        recyclerNutriologos = findViewById(R.id.recyclerNutriologos);
        recyclerNutriologos.setLayoutManager(new LinearLayoutManager(this));

        mAdapter = new NutriologosAdapter(this);
        recyclerNutriologos.setAdapter(mAdapter);

        db = FirebaseFirestore.getInstance();
        loadNutriologos();
    }

    private void loadNutriologos() {
        db.collection("nutriologos")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Nutriologo> nutriologos = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Nutriologo nutriologo = document.toObject(Nutriologo.class);
                        nutriologo.setId(document.getId());
                        nutriologos.add(nutriologo);
                    }
                    mAdapter.setNutriologos(nutriologos);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar nutriólogos: ", e);
                    Toast.makeText(this,
                            "Error al cargar nutriólogos: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onNutriologoClick(Nutriologo nutriologo) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Agregar logs para depuración
        Log.d("NutriologosListActivity", "Vinculando paciente: " + userId + " con nutriólogo: " + nutriologo.getId());

        VinculacionManager.vincularConNutriologo(this, userId, nutriologo.getId(), new VinculacionManager.OnVinculacionListener() {
            @Override
            public void onSuccess() {
                Log.d("NutriologosListActivity", "Vinculación exitosa");
                Intent intent = new Intent(NutriologosListActivity.this, ChatActivity.class);
                intent.putExtra("nutriologoId", nutriologo.getId());
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(Exception e) {
                Log.e("NutriologosListActivity", "Error en vinculación: " + e.getMessage());
                Toast.makeText(NutriologosListActivity.this,
                        "Error al vincular: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
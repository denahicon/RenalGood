package com.example.renalgood.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.renalgood.MainActivity;
import com.example.renalgood.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminActivity extends AppCompatActivity {

    private TextView tvNombreAdmin;
    private ImageView ivHome, ivCedulas, ivEmail, ivAddRecipe;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        initializeViews();
        setupFirebase();
        setupClickListeners();

        if (firebaseAuth.getCurrentUser() != null) {
            getCurrentAdminName();
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void initializeViews() {
        tvNombreAdmin = findViewById(R.id.tvNombreAdmin);
        ivHome = findViewById(R.id.ivHome);
        ivCedulas = findViewById(R.id.ivCedulas);
        ivEmail = findViewById(R.id.ivEmail);
        ivAddRecipe = findViewById(R.id.ivAddRecipe);
    }

    private void setupFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    private void setupClickListeners() {
        ivHome.setImageResource(R.drawable.ic_home);
        ivHome.setColorFilter(getResources().getColor(R.color.prueba));

        ivCedulas.setOnClickListener(v -> {
            Intent intent = new Intent(this, SolicitudesNutriologosActivity.class);
            startActivity(intent);
            finish();
        });

        ivEmail.setOnClickListener(v -> {
            Intent intent = new Intent(this, BuzonAdminActivity.class);
            startActivity(intent);
            finish();
        });

        ivAddRecipe.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminRecipeActivity.class);
            startActivity(intent);
        });
    }

    private void getCurrentAdminName() {
        String currentUserUID = firebaseAuth.getCurrentUser().getUid();

        firestore.collection("admins")
                .document(currentUserUID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String adminName = documentSnapshot.getString("nombre");
                        tvNombreAdmin.setText(adminName);
                    }
                })
                .addOnFailureListener(e -> {
                    // Manejar el error si es necesario
                });
    }

}
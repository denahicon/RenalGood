package com.example.renalgood.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.renalgood.R;

public class BuzonAdminActivity extends AppCompatActivity {

    private ImageView ivHome;
    private ImageView ivCedulas;
    private ImageView ivEmail;
    private ImageView ivAddRecipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_buzon_admin);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        ivHome = findViewById(R.id.ivHome);
        ivCedulas = findViewById(R.id.ivCedulas);
        ivEmail = findViewById(R.id.ivEmail);
        ivAddRecipe = findViewById(R.id.ivAddRecipe);
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
}
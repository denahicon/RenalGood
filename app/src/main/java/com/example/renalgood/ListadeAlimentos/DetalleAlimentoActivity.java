package com.example.renalgood.ListadeAlimentos;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

import com.example.renalgood.R;

public class DetalleAlimentoActivity extends AppCompatActivity {
    private static final String TAG = "DetalleAlimento";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_alimento);

        try {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                initializeViews(extras);
            } else {
                Log.e(TAG, "No se recibieron datos");
                showError("No se recibieron los datos del alimento");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en onCreate: " + e.getMessage(), e);
            showError("Error al mostrar los detalles");
        }
    }

    private void initializeViews(Bundle extras) {
        try {
            // Obtener las referencias a las vistas
            TextView nombreTextView = findViewById(R.id.nombreAlimentoTextView);
            TextView caloriasTextView = findViewById(R.id.caloriasTextView);
            TextView proteinasTextView = findViewById(R.id.proteinasTextView);
            TextView carbohidratosTextView = findViewById(R.id.carbohidratosTextView);
            TextView grasasTextView = findViewById(R.id.grasasTextView);
            TextView cantidadTextView = findViewById(R.id.cantidadTextView);
            ImageView imagenImageView = findViewById(R.id.imagenAlimentoImageView);

            // Establecer los valores desde el Bundle
            String nombre = extras.getString("nombre", "");
            double energia = extras.getDouble("energia", 0.0);
            String proteina = extras.getString("proteina", "0");
            double hidratosCarbono = extras.getDouble("hidratosCarbono", 0.0);
            double lipidos = extras.getDouble("lipidos", 0.0);
            String cantidadSugerida = extras.getString("cantidadSugerida", "");
            String unidad = extras.getString("unidad", "");

            // Mostrar los valores en las vistas
            nombreTextView.setText(nombre);
            caloriasTextView.setText(String.format("%.1f Kcal", energia));
            proteinasTextView.setText(proteina + " g");
            carbohidratosTextView.setText(String.format("%.1f g", hidratosCarbono));
            grasasTextView.setText(String.format("%.1f g", lipidos));
            cantidadTextView.setText(String.format("%s %s", cantidadSugerida, unidad));

            // Establecer imagen placeholder
            imagenImageView.setImageResource(R.drawable.placeholder_fruta);

        } catch (Exception e) {
            Log.e(TAG, "Error al inicializar vistas: " + e.getMessage(), e);
            showError("Error al mostrar los detalles del alimento");
        }
    }

    private void showError(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
        finish();
    }
}
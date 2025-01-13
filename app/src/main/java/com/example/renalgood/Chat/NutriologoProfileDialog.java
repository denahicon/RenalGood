package com.example.renalgood.Chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.DialogFragment;
import com.example.renalgood.R;
import com.google.firebase.firestore.FirebaseFirestore;

public class NutriologoProfileDialog extends DialogFragment {
    private String nutriologoId;

    public static NutriologoProfileDialog newInstance(String nutriologoId) {
        NutriologoProfileDialog dialog = new NutriologoProfileDialog();
        Bundle args = new Bundle();
        args.putString("nutriologoId", nutriologoId);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_nutriologo_profile, container, false);
        nutriologoId = getArguments().getString("nutriologoId");

        // Cargar datos del nutriólogo
        FirebaseFirestore.getInstance().collection("nutriologos")
                .document(nutriologoId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        ((TextView) view.findViewById(R.id.tvNombreNutriologo)).setText(document.getString("nombre"));
                        ((TextView) view.findViewById(R.id.tvAreaEspecializacion)).setText(document.getString("areaEspecializacion"));
                        ((TextView) view.findViewById(R.id.tvAnosExperiencia)).setText(document.getString("anosExperiencia") + " años de experiencia");
                        ((TextView) view.findViewById(R.id.tvDireccionClinica)).setText(document.getString("direccionClinica"));
                    }
                });

        return view;
    }
}
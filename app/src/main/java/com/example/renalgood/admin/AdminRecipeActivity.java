package com.example.renalgood.admin;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.renalgood.R;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.*;


public class AdminRecipeActivity extends AppCompatActivity {
    private ImageView recipeImageView;
    private Button btnSelectImage, btnAddIngredient, btnAddStep, btnSaveRecipe;
    private TextInputEditText edtRecipeName, edtCalories, edtProtein, edtCarbs;
    private AutoCompleteTextView spinnerCategory;
    private LinearLayout ingredientsContainer, stepsContainer;
    private ImageView ivHome, ivCedulas, ivEmail, ivAddRecipe;
    private FirebaseStorage storage;
    private FirebaseFirestore db;
    private Uri selectedImageUri;
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_recipe);

        initializeViews();
        setupFirebase();
        setupNavigation();
        setupCategorySpinner();
        setupListeners();
    }

    private void initializeViews() {
        recipeImageView = findViewById(R.id.recipeImageView);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnAddIngredient = findViewById(R.id.btnAddIngredient);
        btnAddStep = findViewById(R.id.btnAddStep);
        btnSaveRecipe = findViewById(R.id.btnSaveRecipe);
        edtRecipeName = findViewById(R.id.edtRecipeName);
        edtCalories = findViewById(R.id.edtCalories);
        edtProtein = findViewById(R.id.edtProtein);
        edtCarbs = findViewById(R.id.edtCarbs);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        ingredientsContainer = findViewById(R.id.ingredientsContainer);
        stepsContainer = findViewById(R.id.stepsContainer);
        ivHome = findViewById(R.id.ivHome);
        ivCedulas = findViewById(R.id.ivCedulas);
        ivEmail = findViewById(R.id.ivEmail);
        ivAddRecipe = findViewById(R.id.ivAddRecipe);
    }

    private void setupFirebase() {
        storage = FirebaseStorage.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    private void setupNavigation() {
        ivHome.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminActivity.class));
            finish();
        });

        ivCedulas.setOnClickListener(v -> {
            startActivity(new Intent(this, SolicitudesNutriologosActivity.class));
            finish();
        });

        ivEmail.setOnClickListener(v -> {
            startActivity(new Intent(this, BuzonAdminActivity.class));
            finish();
        });

        ivAddRecipe.setImageResource(R.drawable.ic_menu);
        ivAddRecipe.setColorFilter(getResources().getColor(R.color.red));
    }

    private void setupCategorySpinner() {
        String[] categories = new String[]{"Desayuno", "Comida", "Cena", "Merienda"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.dropdown_item, categories);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupListeners() {
        btnSelectImage.setOnClickListener(v -> openImageChooser());
        btnAddIngredient.setOnClickListener(v -> addIngredientView());
        btnAddStep.setOnClickListener(v -> addStepView());
        btnSaveRecipe.setOnClickListener(v -> {
            if (validateInputs()) {
                uploadRecipe();
            }
        });
    }

    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Seleccionar Imagen"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            recipeImageView.setImageURI(selectedImageUri);
            btnSelectImage.setVisibility(View.GONE);
        }
    }

    private void addIngredientView() {
        View ingredientView = getLayoutInflater().inflate(R.layout.item_ingredient, null);
        ImageView ivDelete = ingredientView.findViewById(R.id.ivDeleteIngredient);
        ivDelete.setOnClickListener(v -> ingredientsContainer.removeView(ingredientView));
        ingredientsContainer.addView(ingredientView);
    }

    private void addStepView() {
        View stepView = getLayoutInflater().inflate(R.layout.item_step, null);
        TextView txtStepNumber = stepView.findViewById(R.id.txtStepNumber);
        txtStepNumber.setText("Paso " + (stepsContainer.getChildCount() + 1));

        ImageButton btnDelete = stepView.findViewById(R.id.btnDeleteStep);
        btnDelete.setOnClickListener(v -> {
            stepsContainer.removeView(stepView);
            updateStepNumbers();
        });

        stepsContainer.addView(stepView);
    }

    private void updateStepNumbers() {
        for (int i = 0; i < stepsContainer.getChildCount(); i++) {
            View stepView = stepsContainer.getChildAt(i);
            TextView txtStepNumber = stepView.findViewById(R.id.txtStepNumber);
            txtStepNumber.setText("Paso " + (i + 1));
        }
    }

    private boolean validateInputs() {
        if (selectedImageUri == null) {
            showToast("Por favor selecciona una imagen");
            return false;
        }
        if (TextUtils.isEmpty(edtRecipeName.getText())) {
            edtRecipeName.setError("Campo requerido");
            return false;
        }
        // Añade más validaciones según necesites
        return true;
    }

    private void uploadRecipe() {
        showLoading();

        String imageFileName = "recipe-images/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference imageRef = storage.getReference().child(imageFileName);

        imageRef.putFile(selectedImageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return imageRef.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUrl -> {
                    Map<String, Object> recipe = new HashMap<>();
                    recipe.put("name", edtRecipeName.getText().toString());
                    recipe.put("category", spinnerCategory.getText().toString());
                    recipe.put("calories", Integer.parseInt(edtCalories.getText().toString()));
                    recipe.put("nutrients", getNutrientsMap());
                    recipe.put("ingredients", getIngredientsList());
                    recipe.put("instructions", getStepsList());
                    recipe.put("imageUrl", downloadUrl.toString());
                    recipe.put("timestamp", FieldValue.serverTimestamp());

                    db.collection("recipes")
                            .add(recipe)
                            .addOnSuccessListener(documentReference -> {
                                hideLoading();
                                showToast("Receta guardada exitosamente");
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                hideLoading();
                                showToast("Error al guardar la receta: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    showToast("Error al subir la imagen: " + e.getMessage());
                });
    }

    private Map<String, Double> getNutrientsMap() {
        Map<String, Double> nutrients = new HashMap<>();
        nutrients.put("protein", Double.parseDouble(edtProtein.getText().toString()));
        nutrients.put("carbs", Double.parseDouble(edtCarbs.getText().toString()));
        return nutrients;
    }

    private List<Map<String, Object>> getIngredientsList() {
        List<Map<String, Object>> ingredients = new ArrayList<>();
        for (int i = 0; i < ingredientsContainer.getChildCount(); i++) {
            View view = ingredientsContainer.getChildAt(i);
            EditText etName = view.findViewById(R.id.etIngredientName);
            EditText etAmount = view.findViewById(R.id.etAmount);
            EditText etUnit = view.findViewById(R.id.etUnit);

            String name = etName.getText().toString();
            String amount = etAmount.getText().toString();
            String unit = etUnit.getText().toString();

            if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(amount) && !TextUtils.isEmpty(unit)) {
                Map<String, Object> ingredient = new HashMap<>();
                ingredient.put("name", name);
                ingredient.put("amount", Double.parseDouble(amount));
                ingredient.put("unit", unit);
                ingredients.add(ingredient);
            }
        }
        return ingredients;
    }

    private List<String> getStepsList() {
        List<String> steps = new ArrayList<>();
        for (int i = 0; i < stepsContainer.getChildCount(); i++) {
            View view = stepsContainer.getChildAt(i);
            EditText etDescription = view.findViewById(R.id.etStepDescription);
            String description = etDescription.getText().toString();
            if (!TextUtils.isEmpty(description)) {
                steps.add(description);
            }
        }
        return steps;
    }

    private void showLoading() {
    }

    private void hideLoading() {
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
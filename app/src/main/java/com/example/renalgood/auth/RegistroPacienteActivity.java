package com.example.renalgood.auth;

import static android.content.ContentValues.TAG;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.renalgood.Paciente.PacienteActivity;
import com.example.renalgood.Paciente.PatientData;
import com.example.renalgood.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RegistroPacienteActivity extends AppCompatActivity {
    private TextView tvPregunta;
    private EditText etDinamico;
    private Spinner spDinamico;
    private Button btnSiguiente, btnVolver;
    private ProgressBar progressBar;
    private ProgressBar progressBarLoading;
    private int currentStep = 0;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private PatientData patientData = new PatientData();

    private enum FormInputType {
        TEXT_NORMAL,
        TEXT_EMAIL,
        TEXT_PASSWORD,
        NUMBER_NORMAL,
        NUMBER_DECIMAL,
        SPINNER
    }

    private static class FormStep {
        String question;
        FormInputType inputType;
        int spinnerArrayId; // Para los spinners

        FormStep(String question, FormInputType inputType) {
            this.question = question;
            this.inputType = inputType;
            this.spinnerArrayId = 0;
        }

        FormStep(String question, FormInputType inputType, int spinnerArrayId) {
            this.question = question;
            this.inputType = inputType;
            this.spinnerArrayId = spinnerArrayId;
        }
    }

    private final FormStep[] formSteps = {
            new FormStep("¿Cuál es tu nombre completo?", FormInputType.TEXT_NORMAL),
            new FormStep("¿Cuál es tu correo electrónico?", FormInputType.TEXT_EMAIL),
            new FormStep("Establece una contraseña", FormInputType.TEXT_PASSWORD),
            new FormStep("¿Cuál es tu edad?", FormInputType.NUMBER_NORMAL),
            new FormStep("¿Cuál es tu peso en kilogramos?", FormInputType.NUMBER_DECIMAL),
            new FormStep("¿Cuál es tu altura en centímetros?", FormInputType.NUMBER_NORMAL),
            new FormStep("Nivel de creatinina:", FormInputType.SPINNER, R.array.nivel_creatinina),
            new FormStep("Situación clínica:", FormInputType.SPINNER, R.array.situacion_clinica),
            new FormStep("¿Realizas actividad física?", FormInputType.SPINNER, R.array.actividad_fisica),
            new FormStep("¿Cuántos días por semana?", FormInputType.SPINNER, R.array.dias_semana),
            new FormStep("Sexo:", FormInputType.SPINNER, R.array.sexo)
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro_paciente);

        initializeViews();
        initializeFirebase();
        setupListeners();
        updateUI(currentStep);
    }

    private void initializeViews() {
        tvPregunta = findViewById(R.id.tv_pregunta);
        etDinamico = findViewById(R.id.et_dinamico);
        spDinamico = findViewById(R.id.sp_dinamico);
        btnSiguiente = findViewById(R.id.btn_siguiente);
        btnVolver = findViewById(R.id.btn_volver);
        progressBar = findViewById(R.id.progressBar);
        progressBarLoading = findViewById(R.id.progressBarLoading);
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    private void setupListeners() {
        btnSiguiente.setOnClickListener(v -> handleNextStep());
        btnVolver.setOnClickListener(v -> handlePreviousStep());

        spDinamico.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = parent.getItemAtPosition(position).toString();

                // Habilitar el botón siguiente para todos los spinners excepto cuando
                // es la primera opción (que suele ser "Seleccione una opción")
                btnSiguiente.setEnabled(position != 0);

                if (currentStep == 8 && selection.equals("No")) { // Paso de actividad física
                    // Si selecciona "No", guardamos los datos actuales
                    saveCurrentStepData();
                    patientData.setDaysPerWeek("N/A");
                    // Saltamos al paso de género
                    currentStep = 10;
                    updateUI(currentStep);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                btnSiguiente.setEnabled(false);
            }
        });
    }

    private void updateUI(int step) {
        if (step >= formSteps.length) {
            Log.d(TAG, "updateUI: Llegamos al final del formulario, iniciando registro");
            registerPatient();
            return;
        }

        FormStep currentStep = formSteps[step];
        tvPregunta.setText(currentStep.question);

        // Actualizar progress bar
        int progress = (step * 100) / formSteps.length;
        progressBar.setProgress(progress);

        // Configurar input
        if (currentStep.inputType == FormInputType.SPINNER) {
            etDinamico.setVisibility(View.GONE);
            spDinamico.setVisibility(View.VISIBLE);
            setupSpinner(currentStep.spinnerArrayId);
            // Solo deshabilitar el botón si es la primera opción
            btnSiguiente.setEnabled(spDinamico.getSelectedItemPosition() != 0);
        } else {
            etDinamico.setVisibility(View.VISIBLE);
            spDinamico.setVisibility(View.GONE);
            configureEditText(currentStep.inputType);
            btnSiguiente.setEnabled(true);
        }

        btnVolver.setVisibility(step > 0 ? View.VISIBLE : View.GONE);
    }

    private void configureEditText(FormInputType inputType) {
        switch (inputType) {
            case TEXT_NORMAL:
                etDinamico.setInputType(InputType.TYPE_CLASS_TEXT);
                break;
            case TEXT_EMAIL:
                etDinamico.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                break;
            case TEXT_PASSWORD:
                etDinamico.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                break;
            case NUMBER_NORMAL:
                etDinamico.setInputType(InputType.TYPE_CLASS_NUMBER);
                break;
            case NUMBER_DECIMAL:
                etDinamico.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                break;
        }
        etDinamico.setText("");
        etDinamico.setHint(formSteps[currentStep].question);
    }

    private void setupSpinner(int arrayResourceId) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                arrayResourceId,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDinamico.setAdapter(adapter);
    }

    private void handleNextStep() {
        Log.d(TAG, "handleNextStep: Paso actual=" + currentStep + ", Total pasos=" + formSteps.length);

        if (validateCurrentStep()) {
            saveCurrentStepData();

            if (currentStep >= formSteps.length - 1) {
                Log.d(TAG, "handleNextStep: Último paso completado, iniciando registro");
                registerPatient();
            } else {
                currentStep++;
                updateUI(currentStep);
            }
        }
    }

    private void handlePreviousStep() {
        if (currentStep > 0) {
            currentStep--;
            updateUI(currentStep);
        }
    }

    private boolean validateCurrentStep() {
        FormStep currentFormStep = formSteps[currentStep];

        if (currentFormStep.inputType == FormInputType.SPINNER) {
            return spDinamico.getSelectedItemPosition() != 0;
        }

        String value = etDinamico.getText().toString().trim();
        if (value.isEmpty()) {
            etDinamico.setError("Este campo es requerido");
            return false;
        }

        // Validaciones específicas según el tipo
        switch (currentFormStep.inputType) {
            case TEXT_EMAIL:
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
                    etDinamico.setError("Email inválido");
                    return false;
                }
                break;
            case TEXT_PASSWORD:
                if (value.length() < 6) {
                    etDinamico.setError("La contraseña debe tener al menos 6 caracteres");
                    return false;
                }
                break;
            case NUMBER_NORMAL:
                try {
                    int num = Integer.parseInt(value);
                    if (currentStep == 3 && (num < 18 || num > 120)) { // Edad
                        etDinamico.setError("Edad inválida (18-120)");
                        return false;
                    } else if (currentStep == 5 && (num < 100 || num > 250)) { // Altura
                        etDinamico.setError("Altura inválida (100-250 cm)");
                        return false;
                    }
                } catch (NumberFormatException e) {
                    etDinamico.setError("Número inválido");
                    return false;
                }
                break;
            case NUMBER_DECIMAL:
                try {
                    double num = Double.parseDouble(value);
                    if (currentStep == 4 && (num < 30 || num > 300)) { // Peso
                        etDinamico.setError("Peso inválido (30-300 kg)");
                        return false;
                    }
                } catch (NumberFormatException e) {
                    etDinamico.setError("Número decimal inválido");
                    return false;
                }
                break;
        }

        return true;
    }

    private void saveCurrentStepData() {
        FormStep currentFormStep = formSteps[currentStep];
        String value;

        if (currentFormStep.inputType == FormInputType.SPINNER) {
            value = spDinamico.getSelectedItem().toString();
        } else {
            value = etDinamico.getText().toString().trim();
        }

        Log.d(TAG, "saveCurrentStepData: Paso=" + currentStep + ", Valor=" + value);

        switch (currentStep) {
            case 0: patientData.setName(value); break;
            case 1: patientData.setEmail(value); break;
            case 2: patientData.setPassword(value); break;
            case 3: patientData.setAge(Integer.parseInt(value)); break;
            case 4: patientData.setWeight(Double.parseDouble(value)); break;
            case 5: patientData.setHeight(Integer.parseInt(value)); break;
            case 6: patientData.setCreatinine(value); break;
            case 7: patientData.setClinicalSituation(value); break;
            case 8:
                patientData.setPhysicalActivity(value);
                if (value.equals("No")) {
                    patientData.setDaysPerWeek("N/A");
                }
                break;
            case 9: patientData.setDaysPerWeek(value); break;
            case 10: patientData.setGender(value); break;
        }
    }

    private void registerPatient() {
        Log.d(TAG, "registerPatient: Iniciando registro con email=" + patientData.getEmail());
        progressBarLoading.setVisibility(View.VISIBLE);
        btnSiguiente.setEnabled(false);
        btnVolver.setEnabled(false);

        // Verificar que tenemos todos los datos necesarios
        if (patientData.getEmail() == null || patientData.getPassword() == null) {
            Log.e(TAG, "registerPatient: Datos faltantes email=" + patientData.getEmail());
            Toast.makeText(this, "Error: Faltan datos necesarios", Toast.LENGTH_LONG).show();
            progressBarLoading.setVisibility(View.GONE);
            return;
        }

        mAuth.createUserWithEmailAndPassword(patientData.getEmail(), patientData.getPassword())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = task.getResult().getUser().getUid();
                        Log.d(TAG, "registerPatient: Usuario creado con ID=" + userId);
                        savePatientToFirestore(userId);
                    } else {
                        Log.e(TAG, "registerPatient: Error en autenticación", task.getException());
                        progressBarLoading.setVisibility(View.GONE);
                        btnSiguiente.setEnabled(true);
                        btnVolver.setEnabled(true);
                        Toast.makeText(this, "Error al registrar: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void savePatientToFirestore(String userId) {
        Log.d(TAG, "savePatientToFirestore: Guardando datos para userId=" + userId);

        Map<String, Object> patientMap = new HashMap<>();
        patientMap.put("name", patientData.getName());
        patientMap.put("email", patientData.getEmail());
        patientMap.put("age", patientData.getAge());
        patientMap.put("weight", patientData.getWeight());
        patientMap.put("height", patientData.getHeight());
        patientMap.put("creatinine", patientData.getCreatinine());
        patientMap.put("clinicalSituation", patientData.getClinicalSituation());
        patientMap.put("physicalActivity", patientData.getPhysicalActivity());
        patientMap.put("daysPerWeek", patientData.getDaysPerWeek());
        patientMap.put("gender", patientData.getGender());

        Log.d(TAG, "savePatientToFirestore: Datos a guardar=" + patientMap.toString());

        db.collection("patients")
                .document(userId)
                .set(patientMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "savePatientToFirestore: Datos guardados exitosamente");
                    progressBarLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(this, PacienteActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "savePatientToFirestore: Error al guardar datos", e);
                    progressBarLoading.setVisibility(View.GONE);
                    btnSiguiente.setEnabled(true);
                    btnVolver.setEnabled(true);
                    Toast.makeText(this, "Error al guardar datos: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
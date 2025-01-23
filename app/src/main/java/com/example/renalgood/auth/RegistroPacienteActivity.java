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
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

import java.util.Date;
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
    private FirebaseManager firebaseManager;
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

        firebaseManager = FirebaseManager.getInstance();
        initializeViews();
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

    private void handleRegistrationError(Exception e) {
        Log.e(TAG, "Error en el registro", e);
        progressBarLoading.setVisibility(View.GONE);
        btnSiguiente.setEnabled(true);
        btnVolver.setEnabled(true);
        DialogUtils.showErrorDialog(this,
                "Error",
                "Error al guardar datos: " + e.getMessage());
    }

    private void setupListeners() {
        btnSiguiente.setOnClickListener(v -> handleNextStep());
        btnVolver.setOnClickListener(v -> handlePreviousStep());

        spDinamico.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = parent.getItemAtPosition(position).toString();

                // Habilitar el botón para cualquier selección excepto la primera opción
                btnSiguiente.setEnabled(position != 0);

                if (currentStep == 8) { // Paso de actividad física
                    if (selection.equals("No")) {
                        patientData.setPhysicalActivity("No");
                        patientData.setDaysPerWeek("0");
                        currentStep = 10; // Saltar a género
                        updateUI(currentStep);
                    }
                }
                else if (currentStep == 10) {
                    // Habilitar el botón para cualquier selección válida
                    btnSiguiente.setEnabled(position != 0);

                    // Guardar la selección
                    if (position != 0) {
                        patientData.setGender(selection);
                        Log.d(TAG, "Género seleccionado: " + selection); // Para debugging
                    }
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
            registerPatient();
            return;
        }

        FormStep currentStep = formSteps[step];
        tvPregunta.setText(currentStep.question);

        // Actualizar progress bar
        int progress = (step * 100) / formSteps.length;
        progressBar.setProgress(progress);

        // Restablecer el estado del botón
        btnSiguiente.setEnabled(true);

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

        // Mostrar/ocultar botón volver
        btnVolver.setVisibility(step > 0 ? View.VISIBLE : View.GONE);

        // Restablecer el estado visual del botón
        btnSiguiente.setPressed(false);
        btnSiguiente.invalidate();
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
        if (!validateCurrentStep()) {
            return;
        }

        saveCurrentStepData();

        if (currentStep == 10) {
            // Si estamos en el paso de género y la validación fue exitosa
            Log.d(TAG, "Género seleccionado: " + patientData.getGender());
            registerPatient();
            return;
        }

        if (currentStep >= formSteps.length - 1) {
            registerPatient();
            return;
        }

        // Lógica para saltar la pregunta de días por semana si la actividad física es "No"
        if (currentStep == 8 && spDinamico.getSelectedItem().toString().equals("No")) {
            currentStep = 10;
        } else {
            currentStep++;
        }

        updateUI(currentStep);
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
            int position = spDinamico.getSelectedItemPosition();
            if (position == 0) {
                Toast.makeText(this, "Por favor seleccione una opción", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (currentStep == 10) {
                String selection = spDinamico.getSelectedItem().toString();
                if (!selection.equals("Hombre") && !selection.equals("Mujer")) {
                    Toast.makeText(this, "Por favor seleccione un género válido", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }

            return true;
        }
        String value = etDinamico.getText().toString().trim();
        if (value.isEmpty()) {
            etDinamico.setError("Este campo es requerido");
            return false;
        }
        switch (currentFormStep.inputType) {
            case TEXT_EMAIL:
                if (!ValidationUtils.validateEmail(this, etDinamico)) {
                    return false;
                }
                break;

            case TEXT_PASSWORD:
                if (!ValidationUtils.validatePassword(this, etDinamico)) {
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
                    patientData.setDaysPerWeek("0");
                }
                break;
            case 9: patientData.setDaysPerWeek(value); break;
            case 10:
                patientData.setGender(value);
                Log.d(TAG, "Género guardado: " + value);
                break;
        }
    }

    private void registerPatient() {
        Log.d(TAG, "registerPatient: Iniciando registro con email=" + patientData.getEmail());
        progressBarLoading.setVisibility(View.VISIBLE);
        btnSiguiente.setEnabled(false);
        btnVolver.setEnabled(false);

        // Configura el idioma de Firebase
        firebaseManager.getAuth().setLanguageCode("es");

        firebaseManager.getAuth().createUserWithEmailAndPassword(patientData.getEmail(), patientData.getPassword())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = task.getResult().getUser().getUid();
                        Log.d(TAG, "registerPatient: Usuario creado con ID=" + userId);
                        savePatientToFirestore(userId);
                    } else {
                        progressBarLoading.setVisibility(View.GONE);
                        btnSiguiente.setEnabled(true);
                        btnVolver.setEnabled(true);

                        String errorMessage;
                        Exception e = task.getException();
                        if (e instanceof FirebaseAuthUserCollisionException) {
                            errorMessage = "Este correo ya está registrado en RenalGood";
                        } else if (e instanceof FirebaseAuthWeakPasswordException) {
                            errorMessage = "La contraseña debe tener al menos 6 caracteres";
                        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            errorMessage = "El formato del correo electrónico no es válido";
                        } else {
                            errorMessage = "Error al registrar: " + e.getMessage();
                        }
                        DialogUtils.showErrorDialog(this, "Error", errorMessage);
                    }
                });
    }

    private void savePatientToFirestore(String userId) {
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
        patientMap.put("caloriasDiarias", 0);
        patientMap.put("lastUpdate", new Timestamp(new Date()));

        // También crear documento en la colección "usuarios"
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("caloriasDiarias", 0);
        userMap.put("lastUpdate", new Timestamp(new Date()));
        userMap.put("tipo", "paciente");

        firebaseManager.getDb().collection("patients")
                .document(userId)
                .set(patientMap)
                .addOnSuccessListener(aVoid -> {
                    // Crear documento en usuarios después de crear el paciente
                    firebaseManager.getDb().collection("usuarios")
                            .document(userId)
                            .set(userMap)
                            .addOnSuccessListener(aVoid2 -> {
                                progressBarLoading.setVisibility(View.GONE);
                                DialogUtils.showSuccessDialog(this,
                                        "Éxito",
                                        "Registro exitoso",
                                        (dialog, which) -> {
                                            Intent intent = new Intent(this, PacienteActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            finish();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error al crear documento de usuario", e);
                                handleRegistrationError(e);
                            });
                })
                .addOnFailureListener(this::handleRegistrationError);
    }
}
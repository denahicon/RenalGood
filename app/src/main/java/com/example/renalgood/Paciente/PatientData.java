package com.example.renalgood.Paciente;

import com.google.firebase.Timestamp;
import java.util.Date;

public class PatientData {
    private String id;          // ID del paciente en Firebase
    private String name;
    private String email;
    private String password;
    private int age;
    private double weight;
    private int height;
    private String creatinine;
    private String clinicalSituation;
    private String physicalActivity;
    private String daysPerWeek;
    private String gender;
    private Timestamp registrationDate;  // Fecha de registro
    private double gfr;         // Tasa de filtración glomerular
    private String ckdStage;    // Etapa de la enfermedad renal

    // Constructor vacío necesario para Firebase
    public PatientData() {
        this.registrationDate = new Timestamp(new Date());
    }

    // Constructor con parámetros básicos
    public PatientData(String name, String email) {
        this();
        this.name = name;
        this.email = email;
    }

    // Getters y Setters existentes
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public String getCreatinine() { return creatinine; }
    public void setCreatinine(String creatinine) { this.creatinine = creatinine; }

    public String getClinicalSituation() { return clinicalSituation; }
    public void setClinicalSituation(String clinicalSituation) {
        this.clinicalSituation = clinicalSituation;
    }

    public String getPhysicalActivity() { return physicalActivity; }
    public void setPhysicalActivity(String physicalActivity) {
        this.physicalActivity = physicalActivity;
    }

    public String getDaysPerWeek() { return daysPerWeek; }
    public void setDaysPerWeek(String daysPerWeek) { this.daysPerWeek = daysPerWeek; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Timestamp getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(Timestamp registrationDate) {
        this.registrationDate = registrationDate;
    }

    public double getGfr() { return gfr; }
    public void setGfr(double gfr) { this.gfr = gfr; }

    public String getCkdStage() { return ckdStage; }
    public void setCkdStage(String ckdStage) { this.ckdStage = ckdStage; }

    // Método para convertir creatinina de String a double
    public double getCreatinineValue() {
        try {
            return Double.parseDouble(creatinine.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }

    // Método para calcular el IMC
    public double getBMI() {
        if (height <= 0) return 0;
        double heightInMeters = height / 100.0;
        return weight / (heightInMeters * heightInMeters);
    }

    // Método para obtener la categoría de IMC
    public String getBMICategory() {
        double bmi = getBMI();
        if (bmi < 18.5) return "Bajo peso";
        if (bmi < 25) return "Normal";
        if (bmi < 30) return "Sobrepeso";
        return "Obesidad";
    }

    // Método para verificar si los datos esenciales están completos
    public boolean isComplete() {
        return name != null && !name.isEmpty() &&
                email != null && !email.isEmpty() &&
                age > 0 &&
                weight > 0 &&
                height > 0 &&
                creatinine != null &&
                clinicalSituation != null &&
                physicalActivity != null &&
                gender != null;
    }

    @Override
    public String toString() {
        return "Paciente: " + name +
                "\nEdad: " + age +
                "\nSituación Clínica: " + clinicalSituation +
                "\nGFR: " + gfr +
                "\nEtapa ERC: " + ckdStage;
    }
}
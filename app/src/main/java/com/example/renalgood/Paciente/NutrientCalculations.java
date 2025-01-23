package com.example.renalgood.Paciente;

public class NutrientCalculations {
    // Factores de actividad física
    private static final double SEDENTARY = 1.2;
    private static final double LIGHTLY_ACTIVE = 1.375;
    private static final double MODERATELY_ACTIVE = 1.55;
    private static final double VERY_ACTIVE = 1.725;
    private static final double EXTRA_ACTIVE = 1.9;

    // Requerimientos de proteínas según la condición clínica
    private static final double PROTEIN_GENERAL = 0.8;
    private static final double PROTEIN_HEMODIALYSIS = 1.2;
    private static final double PROTEIN_PERITONEAL = 1.3;
    private static final double PROTEIN_TRANSPLANT = 1.0;

    // Límites de potasio diarios según la condición clínica
    private static final int POTASSIUM_GENERAL = 2000;
    private static final int POTASSIUM_HEMODIALYSIS = 3000;
    private static final int POTASSIUM_PERITONEAL = 4000;

    // Constantes para el cálculo del GFR (Tasa de Filtración Glomerular)
    private static final double KAPPA_FEMALE = 0.7;
    private static final double KAPPA_MALE = 0.9;
    private static final double ALPHA_FEMALE = -0.329;
    private static final double ALPHA_MALE = -0.411;

    // Calcula el GET (Gasto Energético Total) basado en el sexo, peso, altura, edad y nivel de actividad
    public double calculateGET(boolean isMale, double weight, double height, int age, String activityLevel) {
        double ger; // Gasto Energético en Reposo
        if (isMale) {
            ger = (10 * weight) + (6.25 * height) - (5 * age) + 5;
        } else {
            ger = (10 * weight) + (6.25 * height) - (5 * age) - 161;
        }

        // Multiplica por el factor de actividad física
        double activityFactor = getActivityFactor(activityLevel);
        return ger * activityFactor;
    }

    // Obtiene el factor de actividad física basado en la descripción
    private double getActivityFactor(String activityLevel) {
        switch (activityLevel) {
            case "1-3 días":
                return LIGHTLY_ACTIVE;
            case "3-5 días a la semana":
                return MODERATELY_ACTIVE;
            case "6-7 días a la semana":
                return VERY_ACTIVE;
            case "2 veces al día":
                return EXTRA_ACTIVE;
            default:
                return SEDENTARY;
        }
    }

    // Calcula el GFR (Tasa de Filtración Glomerular) basado en parámetros específicos
    public double calculateGFR(boolean isMale, int age, double creatinine, boolean isBlack) {
        double kappa = isMale ? KAPPA_MALE : KAPPA_FEMALE;
        double alpha = isMale ? ALPHA_MALE : ALPHA_FEMALE;
        double raceCoef = isBlack ? 1.159 : 1.0;
        double sexCoef = isMale ? 1.0 : 1.018;

        double creatRatio = creatinine / kappa;
        double minCratRatio = Math.min(creatRatio, 1);
        double maxCratRatio = Math.max(creatRatio, 1);

        double term1 = Math.pow(minCratRatio, alpha);
        double term2 = Math.pow(maxCratRatio, -1.209);

        double gfr = 141 *
                term1 *
                term2 *
                Math.pow(0.993, age) *
                sexCoef *
                raceCoef;

        // Redondea el GFR a dos decimales
        return Math.round(gfr * 100.0) / 100.0;
    }

    // Calcula la cantidad diaria de proteínas según la situación clínica
    public double calculateDailyProtein(String clinicalSituation, double weight) {
        switch (clinicalSituation) {
            case "Hemodiálisis":
                return PROTEIN_HEMODIALYSIS * weight;
            case "Diálisis peritoneal":
                return PROTEIN_PERITONEAL * weight;
            case "Trasplante":
                return PROTEIN_TRANSPLANT * weight;
            default: // ERCA (Enfermedad Renal Crónica Avanzada)
                return PROTEIN_GENERAL * weight;
        }
    }

    // Calcula la cantidad diaria de lípidos basada en el GET
    public double calculateDailyLipids(double get) {
        double lipidsKcal = get * 0.25; // 25% de las calorías son de lípidos
        return lipidsKcal / 9; // Cada gramo de grasa aporta 9 kcal
    }

    // Calcula la cantidad diaria de carbohidratos basada en el GET
    public double calculateDailyCarbs(double get) {
        double carbsKcal = get * 0.50; // 50% de las calorías son de carbohidratos
        return carbsKcal / 4; // Cada gramo de carbohidrato aporta 4 kcal
    }

    // Calcula el potasio diario según la situación clínica
    public int calculateDailyPotassium(String clinicalSituation) {
        switch (clinicalSituation) {
            case "Hemodiálisis":
                return POTASSIUM_HEMODIALYSIS;
            case "Diálisis peritoneal":
                return POTASSIUM_PERITONEAL;
            default: // ERCA o Trasplante
                return POTASSIUM_GENERAL;
        }
    }

    // Calcula la cantidad diaria de fósforo basada en parámetros específicos
    public double calculateDailyPhosphorus(boolean isMale, double gfr, String clinicalSituation) {
        double baseCalculation;
        switch (clinicalSituation) {
            case "Hemodiálisis":
                baseCalculation = 0.3 * (0.6 * gfr + 10);
                break;
            case "Diálisis peritoneal":
                baseCalculation = 0.4 * (0.6 * gfr + 10);
                break;
            default: // ERCA o Trasplante
                baseCalculation = 0.6 * (0.6 * gfr + 10);
                break;
        }

        // Ajuste según el sexo
        return isMale ? (0.8 * baseCalculation + 5) : (0.8 * baseCalculation - 161);
    }

    // Enum para las etapas de la enfermedad renal crónica
    public enum CKDStage {
        STAGE_1(">90", "Daño renal con GFR normal"),
        STAGE_2("60-89", "Daño renal con GFR levemente disminuido"),
        STAGE_3A("45-59", "Disminución moderada del GFR"),
        STAGE_3B("30-44", "Disminución moderada-severa del GFR"),
        STAGE_4("15-29", "Disminución severa del GFR"),
        STAGE_5("<15", "Fallo renal");

        private final String range; // Rango de valores de GFR
        private final String description; // Descripción de la etapa

        CKDStage(String range, String description) {
            this.range = range;
            this.description = description;
        }

        public String getRange() { return range; }
        public String getDescription() { return description; }
    }

    // Determina la etapa de enfermedad renal crónica basada en el GFR
    public CKDStage getCKDStage(double gfr) {
        if (gfr >= 90) return CKDStage.STAGE_1;
        if (gfr >= 60) return CKDStage.STAGE_2;
        if (gfr >= 45) return CKDStage.STAGE_3A;
        if (gfr >= 30) return CKDStage.STAGE_3B;
        if (gfr >= 15) return CKDStage.STAGE_4;
        return CKDStage.STAGE_5;
    }

    // Calcula la distribución calórica basada en el GET
    public CalorieDistribution calculateCalorieDistribution(double get) {
        return new CalorieDistribution(
                get * 0.25, // Calorías de lípidos
                get * 0.50, // Calorías de carbohidratos
                get * 0.25  // Calorías de proteínas
        );
    }

    // Clase interna para representar la distribución calórica
    public static class CalorieDistribution {
        public final double lipids; // Gramos de lípidos
        public final double carbs;  // Gramos de carbohidratos
        public final double proteins; // Gramos de proteínas

        public CalorieDistribution(double lipids, double carbs, double proteins) {
            this.lipids = lipids;
            this.carbs = carbs;
            this.proteins = proteins;
        }
    }
}

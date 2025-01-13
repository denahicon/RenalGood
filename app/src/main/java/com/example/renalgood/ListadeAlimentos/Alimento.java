package com.example.renalgood.ListadeAlimentos;

import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;

public class Alimento implements Serializable {
    @PropertyName("ALIMENTOS")
    private String nombre;

    @PropertyName("Cantidad sugerida")
    private String cantidadSugerida;

    @PropertyName("Unidad")
    private String unidad;

    @PropertyName("Peso bruto redondeado (g)")
    private double pesoBrutoRedondeado;

    @PropertyName("Peso neto (g)")
    private double pesoNeto;

    @PropertyName("Energia (Kcal)")
    private double energia;

    @PropertyName("Proteina (g)")
    private String proteina;

    @PropertyName("Lípidos (g)")
    private double lipidos;

    @PropertyName("Hidratos de carbono (g)")
    private double hidratosCarbono;

    @PropertyName("Fibra (g)")
    private String fibra;

    @PropertyName("Vitamina A (μg RE)")
    private String vitaminaA;

    @PropertyName("Acido Ascórbico (mg)")
    private double acidoAscorbico;

    @PropertyName("Acido Fólico (μg)")
    private String acidoFolico;

    @PropertyName("Hierro NO HEM (mg)")
    private double hierroNoHem;

    @PropertyName("Potasio (mg)")
    private double potasio;

    @PropertyName("Azúcar por equivalente (g)")
    private double azucarEquivalente;

    @PropertyName("Indice glicémico")
    private String indiceGlicemico;

    @PropertyName("Carga glicémica")
    private String cargaGlicemica;

    // Campos adicionales para leguminosas
    @PropertyName("Selenio (μg)")
    private String selenio;

    @PropertyName("Sodio (mg)")
    private double sodio;

    @PropertyName("Fósforo (mg)")
    private String fosforo;

    // Constructor vacío requerido para Firestore
    public Alimento() {}

    // Getters y Setters
    @PropertyName("ALIMENTOS")
    public String getNombre() {
        return nombre != null ? nombre : "";
    }

    @PropertyName("ALIMENTOS")
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    @PropertyName("Cantidad sugerida")
    public String getCantidadSugerida() {
        return cantidadSugerida;
    }

    @PropertyName("Cantidad sugerida")
    public void setCantidadSugerida(String cantidadSugerida) {
        this.cantidadSugerida = cantidadSugerida;
    }

    @PropertyName("Unidad")
    public String getUnidad() {
        return unidad;
    }

    @PropertyName("Unidad")
    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    @PropertyName("Peso bruto redondeado (g)")
    public double getPesoBrutoRedondeado() {
        return pesoBrutoRedondeado;
    }

    @PropertyName("Peso bruto redondeado (g)")
    public void setPesoBrutoRedondeado(double pesoBrutoRedondeado) {
        this.pesoBrutoRedondeado = pesoBrutoRedondeado;
    }

    @PropertyName("Peso neto (g)")
    public double getPesoNeto() {
        return pesoNeto;
    }

    @PropertyName("Peso neto (g)")
    public void setPesoNeto(double pesoNeto) {
        this.pesoNeto = pesoNeto;
    }

    @PropertyName("Energia (Kcal)")
    public double getEnergia() {
        return energia;
    }

    @PropertyName("Energia (Kcal)")
    public void setEnergia(double energia) {
        this.energia = energia;
    }

    @PropertyName("Proteina (g)")
    public String getProteina() {
        return proteina;
    }

    @PropertyName("Proteina (g)")
    public void setProteina(String proteina) {
        this.proteina = proteina;
    }

    @PropertyName("Lípidos (g)")
    public double getLipidos() {
        return lipidos;
    }

    @PropertyName("Lípidos (g)")
    public void setLipidos(double lipidos) {
        this.lipidos = lipidos;
    }

    @PropertyName("Hidratos de carbono (g)")
    public double getHidratosCarbono() {
        return hidratosCarbono;
    }

    @PropertyName("Hidratos de carbono (g)")
    public void setHidratosCarbono(double hidratosCarbono) {
        this.hidratosCarbono = hidratosCarbono;
    }

    @PropertyName("Fibra (g)")
    public String getFibra() {
        return fibra;
    }

    @PropertyName("Fibra (g)")
    public void setFibra(String fibra) {
        this.fibra = fibra;
    }

    @PropertyName("Vitamina A (μg RE)")
    public String getVitaminaA() {
        return vitaminaA;
    }

    @PropertyName("Vitamina A (μg RE)")
    public void setVitaminaA(String vitaminaA) {
        this.vitaminaA = vitaminaA;
    }

    @PropertyName("Acido Ascórbico (mg)")
    public double getAcidoAscorbico() {
        return acidoAscorbico;
    }

    @PropertyName("Acido Ascórbico (mg)")
    public void setAcidoAscorbico(double acidoAscorbico) {
        this.acidoAscorbico = acidoAscorbico;
    }

    @PropertyName("Acido Fólico (μg)")
    public String getAcidoFolico() {
        return acidoFolico;
    }

    @PropertyName("Acido Fólico (μg)")
    public void setAcidoFolico(String acidoFolico) {
        this.acidoFolico = acidoFolico;
    }

    @PropertyName("Hierro NO HEM (mg)")
    public double getHierroNoHem() {
        return hierroNoHem;
    }

    @PropertyName("Hierro NO HEM (mg)")
    public void setHierroNoHem(double hierroNoHem) {
        this.hierroNoHem = hierroNoHem;
    }

    @PropertyName("Potasio (mg)")
    public double getPotasio() {
        return potasio;
    }

    @PropertyName("Potasio (mg)")
    public void setPotasio(double potasio) {
        this.potasio = potasio;
    }

    @PropertyName("Azúcar por equivalente (g)")
    public double getAzucarEquivalente() {
        return azucarEquivalente;
    }

    @PropertyName("Azúcar por equivalente (g)")
    public void setAzucarEquivalente(double azucarEquivalente) {
        this.azucarEquivalente = azucarEquivalente;
    }

    @PropertyName("Indice glicémico")
    public String getIndiceGlicemico() {
        return indiceGlicemico;
    }

    @PropertyName("Indice glicémico")
    public void setIndiceGlicemico(String indiceGlicemico) {
        this.indiceGlicemico = indiceGlicemico;
    }

    @PropertyName("Carga glicémica")
    public String getCargaGlicemica() {
        return cargaGlicemica;
    }

    @PropertyName("Carga glicémica")
    public void setCargaGlicemica(String cargaGlicemica) {
        this.cargaGlicemica = cargaGlicemica;
    }

    @PropertyName("Selenio (μg)")
    public String getSelenio() {
        return selenio;
    }

    @PropertyName("Selenio (μg)")
    public void setSelenio(String selenio) {
        this.selenio = selenio;
    }

    @PropertyName("Sodio (mg)")
    public double getSodio() {
        return sodio;
    }

    @PropertyName("Sodio (mg)")
    public void setSodio(double sodio) {
        this.sodio = sodio;
    }

    @PropertyName("Fósforo (mg)")
    public String getFosforo() {
        return fosforo;
    }

    @PropertyName("Fósforo (mg)")
    public void setFosforo(String fosforo) {
        this.fosforo = fosforo;
    }

    // Métodos auxiliares para manejar valores nulos o conversiones
    public double getFibraValue() {
        try {
            return fibra != null ? Double.parseDouble(fibra) : 0.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public double getProteinaValue() {
        try {
            return proteina != null ? Double.parseDouble(proteina) : 0.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public double getVitaminaAValue() {
        try {
            return vitaminaA != null ? Double.parseDouble(vitaminaA) : 0.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
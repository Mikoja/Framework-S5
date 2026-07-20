package com.test.entity;

public class Mouvement {

    private Long id;
    private String type;
    private String description;
    private double montant;

    public Mouvement() {
    }

    public Mouvement(Long id, String type, String description, double montant) {
        this.id = id;
        this.type = type;
        this.description = description;
        this.montant = montant;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getMontant() {
        return montant;
    }

    public void setMontant(double montant) {
        this.montant = montant;
    }
}

package com.test.repository;

import com.test.entity.Mouvement;

import java.util.ArrayList;
import java.util.List;

public class MouvementRepository {

    private static final List<Mouvement> mouvements = new ArrayList<>();

    static {
        mouvements.add(new Mouvement(1L, "ENTREE", "Vente produit A", 1500.00));
        mouvements.add(new Mouvement(2L, "SORTIE", "Achat matiere premiere", 800.00));
        mouvements.add(new Mouvement(3L, "ENTREE", "Prestation service", 2200.00));
        mouvements.add(new Mouvement(4L, "SORTIE", "Paiement loyer", 1200.00));
        mouvements.add(new Mouvement(5L, "ENTREE", "Remboursement pret", 500.00));
    }

    public List<Mouvement> findAll() {
        return new ArrayList<>(mouvements);
    }

    public void save(Mouvement mouvement) {
        mouvements.add(mouvement);
    }
}

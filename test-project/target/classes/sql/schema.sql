CREATE DATABASE IF NOT EXISTS framework_db;
USE framework_db;

-- Table mouvement
CREATE TABLE IF NOT EXISTS mouvement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    description VARCHAR(255) NOT NULL,
    montant DOUBLE NOT NULL
);

-- Données de test
INSERT INTO mouvement (type, description, montant) VALUES
('ENTREE', 'Vente produit A', 1500.00),
('SORTIE', 'Achat matiere premiere', 800.00),
('ENTREE', 'Prestation service', 2200.00),
('SORTIE', 'Paiement loyer', 1200.00),
('ENTREE', 'Remboursement pret', 500.00);

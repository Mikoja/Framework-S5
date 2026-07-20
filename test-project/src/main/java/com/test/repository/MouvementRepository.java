package com.test.repository;

import com.test.entity.Mouvement;
import framework.annotations.Repository;
import framework.persistence.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Repository
public class MouvementRepository {

    private static final Logger LOGGER = Logger.getLogger(MouvementRepository.class.getName());

    private final ConnectionFactory connectionFactory;

    public MouvementRepository() {
        this.connectionFactory = null;
    }

    public MouvementRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        // Support pour injection optionnelle
    }

    public List<Mouvement> findAll() {
        List<Mouvement> mouvements = new ArrayList<>();
        String sql = "SELECT id, type, description, montant FROM mouvement";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            if (conn == null) {
                LOGGER.warning("Aucune connexion disponible, retour liste vide");
                return mouvements;
            }
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Mouvement m = new Mouvement();
                m.setId(rs.getLong("id"));
                m.setType(rs.getString("type"));
                m.setDescription(rs.getString("description"));
                m.setMontant(rs.getDouble("montant"));
                mouvements.add(m);
            }
            LOGGER.info(() -> "findAll() : " + mouvements.size() + " mouvements récupérés");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la lecture des mouvements", e);
        } finally {
            closeQuietly(rs);
            closeQuietly(stmt);
            closeQuietly(conn);
        }

        return mouvements;
    }

    public Mouvement findById(Long id) {
        String sql = "SELECT id, type, description, montant FROM mouvement WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            if (conn == null) return null;
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                Mouvement m = new Mouvement();
                m.setId(rs.getLong("id"));
                m.setType(rs.getString("type"));
                m.setDescription(rs.getString("description"));
                m.setMontant(rs.getDouble("montant"));
                return m;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la lecture du mouvement id=" + id, e);
        } finally {
            closeQuietly(rs);
            closeQuietly(stmt);
            closeQuietly(conn);
        }

        return null;
    }

    public void save(Mouvement mouvement) {
        String sql = "INSERT INTO mouvement (type, description, montant) VALUES (?, ?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            if (conn == null) return;
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, mouvement.getType());
            stmt.setString(2, mouvement.getDescription());
            stmt.setDouble(3, mouvement.getMontant());
            stmt.executeUpdate();
            LOGGER.info(() -> "Mouvement sauvegardé : " + mouvement.getType());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la sauvegarde du mouvement", e);
        } finally {
            closeQuietly(stmt);
            closeQuietly(conn);
        }
    }

    private Connection getConnection() {
        try {
            if (connectionFactory != null) {
                return connectionFactory.getConnection();
            }
            // Fallback : chercher dans le ServletContext via ThreadLocal ou autre
            return null;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur de connexion", e);
            return null;
        }
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }
}

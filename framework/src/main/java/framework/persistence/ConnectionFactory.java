package framework.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

public class ConnectionFactory {

    private static final Logger LOGGER = Logger.getLogger(ConnectionFactory.class.getName());

    private final DatabaseConfig config;

    public ConnectionFactory(DatabaseConfig config) {
        this.config = config;
        try {
            Class.forName(config.getDriver());
            LOGGER.info(() -> "Driver JDBC chargé : " + config.getDriver());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Driver JDBC introuvable : " + config.getDriver(), e);
        }
    }

    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(
                config.getUrl(), config.getUser(), config.getPassword());
        LOGGER.fine(() -> "Connexion JDBC établie");
        return conn;
    }

    public DatabaseConfig getConfig() {
        return config;
    }
}

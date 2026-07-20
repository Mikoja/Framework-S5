package framework.persistence;

public class DatabaseConfig {

    private final String url;
    private final String user;
    private final String password;
    private final String driver;

    public DatabaseConfig(String url, String user, String password, String driver) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.driver = driver;
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getDriver() {
        return driver;
    }

    @Override
    public String toString() {
        return "DatabaseConfig{url='" + url + "', user='" + user + "'}";
    }
}

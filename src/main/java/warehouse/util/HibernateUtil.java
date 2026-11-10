package warehouse.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class HibernateUtil {

    private static final String LOCAL_SETTINGS = "db.properties";

    private static SessionFactory sessionFactory;

    private HibernateUtil() {
    }

    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            Configuration configuration = new Configuration().configure();
            applyLocalSettings(configuration);
            sessionFactory = configuration.buildSessionFactory();
        }
        return sessionFactory;
    }

    private static void applyLocalSettings(Configuration configuration) {
        Path file = Path.of(LOCAL_SETTINGS);
        if (!Files.exists(file)) {
            return;
        }
        Properties settings = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            settings.load(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + file.toAbsolutePath(), e);
        }
        setIfPresent(configuration, settings, "db.url", "hibernate.connection.url");
        setIfPresent(configuration, settings, "db.username", "hibernate.connection.username");
        setIfPresent(configuration, settings, "db.password", "hibernate.connection.password");
    }

    private static void setIfPresent(Configuration configuration, Properties settings, String key, String property) {
        String value = settings.getProperty(key);
        if (value != null && !value.isBlank()) {
            configuration.setProperty(property, value.trim());
        }
    }

    public static synchronized void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
            sessionFactory = null;
        }
    }
}

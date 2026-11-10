package warehouse.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class HibernateUtil {

    private static final Logger log = LogManager.getLogger(HibernateUtil.class);
    private static final String LOCAL_DB_FILE = "db.properties";

    private static SessionFactory sessionFactory;

    private HibernateUtil() {
    }

    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            Configuration cfg = new Configuration().configure();

            Properties local = loadLocalSettings();
            if (local.getProperty("db.url") != null) {
                cfg.setProperty("hibernate.connection.url", local.getProperty("db.url").trim());
            }
            if (local.getProperty("db.username") != null) {
                cfg.setProperty("hibernate.connection.username", local.getProperty("db.username").trim());
            }
            if (local.getProperty("db.password") != null) {
                cfg.setProperty("hibernate.connection.password", local.getProperty("db.password").trim());
            }

            sessionFactory = cfg.buildSessionFactory();
            log.info("Connected to database");
        }
        return sessionFactory;
    }

    public static synchronized void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
            sessionFactory = null;
        }
    }

    private static Properties loadLocalSettings() {
        Properties props = new Properties();
        Path path = Paths.get(LOCAL_DB_FILE);
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                props.load(reader);
                log.info("Using local database settings from {}", path.toAbsolutePath());
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read " + LOCAL_DB_FILE, e);
            }
        } else {
            log.info("{} not found, using settings from hibernate.cfg.xml", LOCAL_DB_FILE);
        }
        return props;
    }
}
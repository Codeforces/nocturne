/*
 * Copyright 2009 Mike Mirzayanov
 */

package webmail.database;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.nocturne.main.ApplicationContext;
import org.nocturne.exception.ConfigurationException;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Properties;

/**
 * This class should be excluded from the debug reloading process,
 * because we don't want to have database reconnect on each request.
 *
 * @author Mike Mirzayanov
 */
public class ApplicationDataSourceFactory {
    /** Database connection configuration properties. */
    private static final Properties properties = new Properties();

    /** The only instance. */
    private static DataSource instance;

    /** Deny construct outside class. */
    private ApplicationDataSourceFactory() {
        // No operations.
    }

    public static synchronized DataSource getInstance() {
        if (instance == null) {
            boolean debug = ApplicationContext.getInstance().isDebug();
            ComboPooledDataSource comboPooledDataSource = new ComboPooledDataSource();

            comboPooledDataSource.setJdbcUrl(properties.getProperty("database.url"));
            comboPooledDataSource.setUser(properties.getProperty("database.user"));
            comboPooledDataSource.setPassword(properties.getProperty("database.password"));
            comboPooledDataSource.setCheckoutTimeout(60000);
            comboPooledDataSource.setIdleConnectionTestPeriod(60);
            comboPooledDataSource.setMaxStatementsPerConnection(128);

            if (debug) {
                comboPooledDataSource.setInitialPoolSize(1);
            } else {
                comboPooledDataSource.setInitialPoolSize(8);
            }

            comboPooledDataSource.setMaxPoolSize(32);
            comboPooledDataSource.setPreferredTestQuery("SELECT NOW()");

            instance = comboPooledDataSource;
        }

        return instance;
    }

    static {
        try {
            properties.load(ApplicationDataSourceFactory.class.getResourceAsStream("/database.properties"));
            Class.forName(properties.getProperty("database.driver"));
        } catch (IOException e) {
            throw new ConfigurationException("Can't load /database.properties.", e);
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException("Can't load database driver " + properties.getProperty("database.driver") + ".", e);
        }
    }
}

/*
 * Copyright by Mike Mirzayanov
 */
package bloggy.database;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.apache.log4j.Logger;
import bloggy.exception.ApplicationException;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * The only datasource of the application.
 *
 * @author Mike Mirzayanov
 */
public class ApplicationDataSource {
    private static final Logger logger = Logger.getLogger(Logger.class);
    private static final Properties PROPERTIES = new Properties();

    private ApplicationDataSource() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static DataSource getInstance() {
        return DataSourceHolder.INSTANCE;
    }

    private static class DataSourceHolder {
        @Nonnull
        private static final DataSource INSTANCE;

        static {
            MysqlDataSource instance = new MysqlDataSource();
            instance.setUrl(PROPERTIES.getProperty("database.url"));
            instance.setUser(PROPERTIES.getProperty("database.user"));
            instance.setPassword(PROPERTIES.getProperty("database.password"));

            try (Connection ignored = instance.getConnection()) {
                logger.info("Database has been initialized, test database connection is OK.");

            } catch (SQLException e) {
                throw new ApplicationException("Can't get test database connection.", e);
            }

            INSTANCE = instance;
        }
    }

    static {
        try {
            PROPERTIES.load(ApplicationDataSource.class.getResourceAsStream("/database.properties"));
        } catch (IOException e) {
            throw new ApplicationException("Can't load /database.properties.", e);
        }
    }
}

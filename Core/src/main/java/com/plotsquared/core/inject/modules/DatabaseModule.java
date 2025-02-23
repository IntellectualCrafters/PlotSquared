package com.plotsquared.core.inject.modules;

import com.google.inject.AbstractModule;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.configuration.Storage;
import com.plotsquared.core.inject.annotations.PlotDatabase;
import com.plotsquared.core.util.FileUtils;
import com.plotsquared.core.util.StringMan;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sql.DataSource;
import java.io.File;

public class DatabaseModule extends AbstractModule {

    private static final Logger LOGGER = LogManager.getLogger("PlotSquared/" + PlotSquared.class.getSimpleName());

    @Override
    protected void configure() {
        try {
            if (Storage.MySQL.USE) {
                this.configureMySQL();
            } else if (Storage.SQLite.USE) {
                this.configureSQLite();
            } else {
                LOGGER.error("No storage type is set. Disabling PlotSquared");
                PlotSquared.platform().shutdown();
            }
        } catch (final Exception e) {
            LOGGER.error("Unable to initialize database", e);
            PlotSquared.platform().shutdown();
        }
    }

    private void configureSQLite() throws Exception {
        final File file = FileUtils.getFile(PlotSquared.platform().getDirectory(), Storage.SQLite.DB + ".db");
        if (!file.exists()) {
            file.createNewFile();
        }
        Class.forName("org.sqlite.JDBC");

        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + file);
        config.setDriverClassName("org.sqlite.JDBC");
        final DataSource dataSource = new HikariDataSource();

        binder().bind(DataSource.class).annotatedWith(PlotDatabase.class).toInstance(dataSource);
    }

    private void configureMySQL() {
        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(
                String.format(
                        "jdbc:mysql://%s:%s/%s?%s",
                        Storage.MySQL.HOST,
                        Storage.MySQL.PORT,
                        Storage.MySQL.DATABASE,
                        StringMan.join(Storage.MySQL.PROPERTIES, "&")
                ));
        hikariConfig.setUsername(Storage.MySQL.USER);
        hikariConfig.setPassword(Storage.MySQL.PASSWORD);
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "512");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        final DataSource dataSource = new HikariDataSource(hikariConfig);

        binder().bind(DataSource.class).annotatedWith(PlotDatabase.class).toInstance(dataSource);
    }
}

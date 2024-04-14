/*
 * PlotSquared, a land and world management plugin for Minecraft.
 * Copyright (C) IntellectualSites <https://intellectualsites.com>
 * Copyright (C) IntellectualSites team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.plotsquared.core.database;

import com.plotsquared.core.PlotSquared;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Connects to and uses a SQLite database.
 */
public class SQLite extends Database {

    private static final Logger LOGGER = LogManager.getLogger("PlotSquared/" + SQLite.class.getSimpleName());
    private final HikariDataSource hikariDataSource;

    /**
     * Creates a new SQLite instance
     *
     * @param dbLocation Location of the Database (Must end in .db)
     */
    public SQLite(File dbLocation) {
        if (!PlotSquared.platform().getDirectory().exists()) {
            PlotSquared.platform().getDirectory().mkdirs();
        }
        File file = dbLocation;
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ignored) {
                LOGGER.error("Unable to create database");
            }
        }
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbLocation);
        config.setDriverClassName("org.sqlite.JDBC");
        this.hikariDataSource = new HikariDataSource(config);
    }

    @Override
    public Connection getConnection() {
        try {
            return this.hikariDataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean closeConnection() throws SQLException {
        if (this.hikariDataSource == null) {
            return false;
        }
        this.hikariDataSource.close();
        return true;
    }

    @Override
    public ResultSet querySQL(String query) throws SQLException {
        try (Statement statement = this.getConnection().createStatement()) {
            return statement.executeQuery(query);
        }
    }

    @Override
    public int updateSQL(String query) throws SQLException {
        try (Statement statement = this.getConnection().createStatement()) {
            return statement.executeUpdate(query);
        }
    }

}

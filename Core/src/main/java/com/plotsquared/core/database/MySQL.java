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

import com.plotsquared.core.configuration.Storage;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Connects to and uses a MySQL database
 *
 * @author -_Husky_-
 * @author tips48
 */
public class MySQL extends Database {

    private final HikariDataSource hikariDataSource;

    /**
     * Creates a new MySQL instance.
     *
     * @param hostname Name of the host
     * @param port     Port number
     * @param database Database name
     * @param username Username
     * @param password Password
     */
    public MySQL(String hostname, String port, String database, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + hostname + ':' + port + '/' + database);
        config.setUsername(username);
        config.setPassword(password);
        for (final String property : Storage.MySQL.PROPERTIES) {
            if (property.contains("=")) {
                String[] splittedProperty = property.split("=");
                if (splittedProperty.length != 2) {
                    continue;
                }
                String key = splittedProperty[0];
                String value = splittedProperty[1];
                config.addDataSourceProperty(key,value);
            }
        }
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

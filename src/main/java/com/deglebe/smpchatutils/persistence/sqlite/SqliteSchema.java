package com.deglebe.smpchatutils.persistence.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/* ddl for sqlite file */
final class SqliteSchema {

    private SqliteSchema() {}

    static void apply(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute(
                "CREATE TABLE IF NOT EXISTS namecolors (uuid TEXT PRIMARY KEY NOT NULL, prefix TEXT NOT NULL)"
            );
            // need "CREATE TABLE IF NOT EXISTS ignores (uuid TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL)" for ignores eventually
        }
    }
}

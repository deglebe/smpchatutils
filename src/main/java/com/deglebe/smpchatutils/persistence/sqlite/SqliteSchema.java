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
            st.execute(
                "CREATE TABLE IF NOT EXISTS player_ignores ("
                    + "ignorer_uuid TEXT NOT NULL, "
                    + "ignored_uuid TEXT NOT NULL, "
                    + "PRIMARY KEY (ignorer_uuid, ignored_uuid))"
            );
            st.execute(
                "CREATE INDEX IF NOT EXISTS idx_player_ignores_ignorer ON player_ignores(ignorer_uuid)"
            );
        }
    }
}

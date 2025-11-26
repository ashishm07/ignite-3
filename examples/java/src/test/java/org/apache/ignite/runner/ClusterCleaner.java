package org.apache.ignite.runner;

import org.apache.ignite.client.IgniteClient;

public class ClusterCleaner {

    public static void clean(IgniteClient client) {
        var sql = client.sql();

        // ---- DROP TABLES ----
        try {
            var tables = sql.execute(null, "SHOW TABLES");
            while (tables.hasNext()) {
                var row = tables.next();
                String table = row.stringValue(0);
                sql.execute(null, "DROP TABLE IF EXISTS " + table);
                System.out.println("Dropped TABLE: " + table);
            }
        } catch (Exception ignored) {}

        // ---- DROP ZONES ----
        try {
            var zones = sql.execute(null, "SHOW ZONES");
            while (zones.hasNext()) {
                var row = zones.next();
                String zone = row.stringValue(0);
                sql.execute(null, "DROP ZONE IF EXISTS " + zone);
                System.out.println("Dropped ZONE: " + zone);
            }
        } catch (Exception ignored) {}
    }
}

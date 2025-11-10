package com.hcen.mockdnic.config;

import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

@Singleton
@Startup
@DataSourceDefinition(
        name = "java:app/jdbc/MockDnicDS",
        className = "org.postgresql.xa.PGXADataSource",
        serverName = "179.31.2.178",
        portNumber = 5432,
        databaseName = "postgres",
        user = "webadmin",
        password = "V79FHjyEho"
)
public class MockDnicDataSourceConfig {
    // Datasource configurado para PostgreSQL externo
}
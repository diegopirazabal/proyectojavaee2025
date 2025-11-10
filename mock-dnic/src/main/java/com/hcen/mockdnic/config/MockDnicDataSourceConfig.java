package com.hcen.mockdnic.config;

import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

@Singleton
@Startup
@DataSourceDefinition(
        name = "java:app/jdbc/MockDnicDS",
        className = "org.postgresql.xa.PGXADataSource",
        serverName = "${env.DATABASE_HOST:179.31.2.178}",
        portNumber = 5432,
        databaseName = "${env.DATABASE_NAME:postgres}",
        user = "${env.DATABASE_USER:webadmin}",
        password = "${env.DATABASE_PASSWORD:V79FHjyEho}"
)
public class MockDnicDataSourceConfig {
    // Datasource con variables de entorno para Railway
}

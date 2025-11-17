package com.hcen.mockdnic.config;

import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

@Singleton
@Startup
@DataSourceDefinition(
        name = "java:app/jdbc/MockDnicDS",
        className = "org.postgresql.xa.PGXADataSource",
        user = "webadmin",
        password = "V79FHjyEho",
        properties = {
                "serverName=179.31.2.178",
                "portNumber=5432",
                "databaseName=postgres",
                "sslMode=disable"
        }
)
public class MockDnicDataSourceConfig {

    // Datasource declaratively registered for the mock DNIC service.
}

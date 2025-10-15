package com.hcen.mockdnic.config;

import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

@Singleton
@Startup
@DataSourceDefinition(
        name = "java:app/jdbc/mock-dnic",
        className = "org.postgresql.xa.PGXADataSource",
        user = "postgres",
        password = "mFkefhPbCoGVFElQhfZhwonwwTLuRPaa",
        properties = {
                "serverName=centerbeam.proxy.rlwy.net",
                "portNumber=22214",
                "databaseName=railway",
                "sslMode=require"
        }
)
public class MockDnicDataSourceConfig {

    // Datasource declaratively registered for the mock DNIC service.
}

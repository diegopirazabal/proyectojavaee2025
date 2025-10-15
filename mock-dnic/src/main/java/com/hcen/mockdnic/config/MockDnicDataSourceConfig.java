package com.hcen.mockdnic.config;

import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

@Singleton
@Startup
@DataSourceDefinition(
        name = "java:app/jdbc/mock-dnic",
        className = "org.postgresql.ds.PGSimpleDataSource",
        url = "jdbc:postgresql://centerbeam.proxy.rlwy.net:22214/railway",
        user = "postgres",
        password = "mFkefhPbCoGVFElQhfZhwonwwTLuRPaa"
)
public class MockDnicDataSourceConfig {

    // Datasource declaratively registered for the mock DNIC service.
}

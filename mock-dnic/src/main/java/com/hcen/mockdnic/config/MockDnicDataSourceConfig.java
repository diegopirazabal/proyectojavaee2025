package com.hcen.mockdnic.config;

import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

/**
 * Configuración de DataSource para Mock DNIC.
 * 
 * NOTA PARA DEPLOYMENT EN RAILWAY:
 * El datasource se configura automáticamente en WildFly mediante el Dockerfile.
 * La configuración usa variables de entorno para credenciales seguras.
 * 
 * El datasource estará disponible en: java:jboss/datasources/MockDnicDS
 * 
 * La configuración hardcoded de abajo está comentada porque:
 * 1. No debe exponer credenciales en el código
 * 2. Railway configurará el datasource dinámicamente
 * 3. El Dockerfile maneja la configuración automática
 */
@Singleton
@Startup
/*
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
*/
public class MockDnicDataSourceConfig {

    // Datasource configurado por WildFly en Railway
    // Ver Dockerfile para detalles de configuración
}

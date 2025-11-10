# Multi-stage build para optimizar tamaño de imagen
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

# Copiar archivos del proyecto
WORKDIR /app
COPY pom.xml .
COPY src ./src

# Build del proyecto
RUN mvn clean package -DskipTests

# Imagen final con WildFly
FROM quay.io/wildfly/wildfly:31.0.1.Final-jdk17

# Establecer usuario root temporalmente para configuración
USER root

# Instalar PostgreSQL JDBC driver
RUN curl -L -o /opt/jboss/wildfly/standalone/deployments/postgresql.jar \
    https://jdbc.postgresql.org/download/postgresql-42.7.3.jar

# Crear directorio para scripts
RUN mkdir -p /opt/jboss/wildfly/customization

# Script de configuración para Railway (soporta variables de entorno)
RUN echo '#!/bin/bash\n\
set -e\n\
\n\
# Configurar variables de entorno con valores por defecto\n\
DB_HOST=${DATABASE_HOST:-179.31.2.178}\n\
DB_PORT=${DATABASE_PORT:-5432}\n\
DB_NAME=${DATABASE_NAME:-postgres}\n\
DB_USER=${DATABASE_USER:-webadmin}\n\
DB_PASS=${DATABASE_PASSWORD:-V79FHjyEho}\n\
\n\
echo "Configurando WildFly..."\n\
\n\
# Iniciar WildFly en modo admin\n\
/opt/jboss/wildfly/bin/standalone.sh --admin-only &\n\
WILDFLY_PID=$!\n\
\n\
# Esperar a que WildFly esté listo\n\
until /opt/jboss/wildfly/bin/jboss-cli.sh --connect --command=":read-attribute(name=server-state)" 2>/dev/null | grep -q "running"; do\n\
    echo "Esperando a que WildFly inicie..."\n\
    sleep 2\n\
done\n\
\n\
echo "Configurando datasource con PostgreSQL..."\n\
\n\
# Configurar el datasource usando CLI\n\
/opt/jboss/wildfly/bin/jboss-cli.sh --connect <<EOF\n\
/subsystem=datasources/xa-data-source=MockDnicDS:add(\\\n\
    jndi-name=java:jboss/datasources/MockDnicDS,\\\n\
    driver-name=postgresql.jar,\\\n\
    xa-datasource-class=org.postgresql.xa.PGXADataSource,\\\n\
    enabled=true,\\\n\
    use-java-context=true\\\n\
)\n\
/subsystem=datasources/xa-data-source=MockDnicDS/xa-datasource-properties=ServerName:add(value=${DB_HOST})\n\
/subsystem=datasources/xa-data-source=MockDnicDS/xa-datasource-properties=PortNumber:add(value=${DB_PORT})\n\
/subsystem=datasources/xa-data-source=MockDnicDS/xa-datasource-properties=DatabaseName:add(value=${DB_NAME})\n\
/subsystem=datasources/xa-data-source=MockDnicDS:write-attribute(name=user-name,value=${DB_USER})\n\
/subsystem=datasources/xa-data-source=MockDnicDS:write-attribute(name=password,value=${DB_PASS})\n\
exit\n\
EOF\n\
\n\
echo "Datasource configurado exitosamente"\n\
\n\
# Detener WildFly\n\
/opt/jboss/wildfly/bin/jboss-cli.sh --connect --command=:shutdown\n\
wait $WILDFLY_PID\n\
\n\
echo "Configuración completada"\n\
' > /opt/jboss/wildfly/customization/setup.sh

RUN chmod +x /opt/jboss/wildfly/customization/setup.sh

# Copiar el WAR desde el builder
COPY --from=builder /app/target/mock-dnic.war /opt/jboss/wildfly/standalone/deployments/

# Volver al usuario jboss
USER jboss

# Exponer puerto (Railway asigna dinámicamente pero WildFly usa 8080 internamente)
EXPOSE 8080

# Script de inicio que configura y ejecuta WildFly
CMD ["/bin/bash", "-c", "/opt/jboss/wildfly/customization/setup.sh && /opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0"]

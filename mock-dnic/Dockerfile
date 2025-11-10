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

# Copiar script de configuración
COPY setup-wildfly.sh /opt/jboss/wildfly/customization/setup.sh
RUN chmod +x /opt/jboss/wildfly/customization/setup.sh

# Copiar el WAR desde el builder
COPY --from=builder /app/target/mock-dnic.war /opt/jboss/wildfly/standalone/deployments/

# Volver al usuario jboss
USER jboss

# Exponer puerto (Railway asigna dinámicamente pero WildFly usa 8080 internamente)
EXPOSE 8080

# Script de inicio que configura y ejecuta WildFly
CMD ["/bin/bash", "-c", "/opt/jboss/wildfly/customization/setup.sh && /opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0"]

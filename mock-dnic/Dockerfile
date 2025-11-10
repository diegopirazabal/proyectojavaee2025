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

# Instalar PostgreSQL JDBC driver en módulos de WildFly
RUN mkdir -p /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main && \
    curl -L -o /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main/postgresql-42.7.3.jar \
    https://jdbc.postgresql.org/download/postgresql-42.7.3.jar

# Crear module.xml para PostgreSQL
RUN echo '<?xml version="1.0" encoding="UTF-8"?>' > /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main/module.xml && \
    echo '<module xmlns="urn:jboss:module:1.9" name="org.postgresql">' >> /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main/module.xml && \
    echo '  <resources>' >> /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main/module.xml && \
    echo '    <resource-root path="postgresql-42.7.3.jar"/>' >> /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main/module.xml && \
    echo '  </resources>' >> /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main/module.xml && \
    echo '  <dependencies>' >> /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main/module.xml && \
    echo '    <module name="javax.api"/>' >> /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main/module.xml && \
    echo '    <module name="javax.transaction.api"/>' >> /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main/module.xml && \
    echo '  </dependencies>' >> /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main/module.xml && \
    echo '</module>' >> /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main/module.xml

# Copiar el WAR desde el builder
COPY --from=builder /app/target/mock-dnic.war /opt/jboss/wildfly/standalone/deployments/

# Variables de entorno con valores por defecto
ENV DATABASE_HOST=179.31.2.178 \
    DATABASE_PORT=5432 \
    DATABASE_NAME=postgres \
    DATABASE_USER=webadmin \
    DATABASE_PASSWORD=V79FHjyEho

# Volver al usuario jboss
USER jboss

# Exponer puerto
EXPOSE 8080

# Iniciar WildFly normalmente
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0"]

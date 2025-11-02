# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Primero copiar parent pom y core-domain
COPY ../pom.xml ./pom.xml
COPY ../core-domain ./core-domain

# Copiar ambos pom.xml (backend y frontend)
COPY backend/pom.xml ./componente-periferico/backend/pom.xml
COPY frontend-admin-clinica/pom.xml ./componente-periferico/frontend-admin-clinica/pom.xml

# Build backend primero
WORKDIR /build/componente-periferico/backend
COPY backend/src ./src
RUN mvn clean package -DskipTests

# Build frontend
WORKDIR /build/componente-periferico/frontend-admin-clinica
COPY frontend-admin-clinica/src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM quay.io/wildfly/wildfly:31.0.0.Final-jdk17
USER root

# Install PostgreSQL driver
RUN curl -L https://jdbc.postgresql.org/download/postgresql-42.6.0.jar -o /opt/jboss/wildfly/standalone/deployments/postgresql-42.6.0.jar

# Copy datasource configuration script
COPY --chown=jboss:jboss backend/datasource-config.cli /opt/jboss/

# Copy startup script
COPY --chown=jboss:jboss backend/startup.sh /opt/jboss/
RUN chmod +x /opt/jboss/startup.sh

# Copy backend WAR
COPY --from=build --chown=jboss:jboss /build/componente-periferico/backend/target/multitenant-api.war /opt/jboss/wildfly/standalone/deployments/

# Copy frontend WAR
COPY --from=build --chown=jboss:jboss /build/componente-periferico/frontend-admin-clinica/target/prestador-salud.war /opt/jboss/wildfly/standalone/deployments/

# Expose port
EXPOSE 8080

USER jboss

# Start with custom script
CMD ["/opt/jboss/startup.sh"]

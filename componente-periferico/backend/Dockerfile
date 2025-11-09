# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Instalar parent pom primero (mejor cache)
COPY pom.xml ./pom.xml
RUN mvn install -N

# Copiar pom de cada módulo para aprovechar cache en dependencias
COPY componente-periferico/backend/pom.xml ./componente-periferico/backend/pom.xml
COPY componente-periferico/frontend-admin-clinica/pom.xml ./componente-periferico/frontend-admin-clinica/pom.xml
COPY componente-periferico/frontend-profesional-salud/pom.xml ./componente-periferico/frontend-profesional-salud/pom.xml

# Build backend (instala en repo local para que frontends lo consuman)
WORKDIR /build/componente-periferico/backend
COPY componente-periferico/backend/src ./src
RUN mvn clean install -DskipTests

# Build frontend admin
WORKDIR /build/componente-periferico/frontend-admin-clinica
COPY componente-periferico/frontend-admin-clinica/src ./src
RUN mvn clean package -DskipTests

# Build frontend profesional
WORKDIR /build/componente-periferico/frontend-profesional-salud
COPY componente-periferico/frontend-profesional-salud/src ./src
RUN mvn clean package -DskipTests

# Runtime stage con WildFly 37
FROM quay.io/wildfly/wildfly:37.0.1.Final-jdk17
USER root

# Instalar driver PostgreSQL
RUN curl -L https://repo1.maven.org/maven2/org/postgresql/postgresql/42.6.0/postgresql-42.6.0.jar -o /opt/jboss/wildfly/standalone/deployments/postgresql-42.6.0.jar

# Copiar configuración de WildFly y scripts auxiliares
COPY --chown=jboss:jboss componente-periferico/backend/standalone-servidor.xml /opt/jboss/wildfly/standalone/configuration/standalone.xml
COPY --chown=jboss:jboss componente-periferico/backend/datasource-config.cli /opt/jboss/
COPY --chown=jboss:jboss componente-periferico/backend/startup.sh /opt/jboss/
RUN chmod +x /opt/jboss/startup.sh

# Deploy backend y frontends
COPY --from=build --chown=jboss:jboss /build/componente-periferico/backend/target/multitenant-api.war /opt/jboss/wildfly/standalone/deployments/
COPY --from=build --chown=jboss:jboss /build/componente-periferico/frontend-admin-clinica/target/prestador-salud.war /opt/jboss/wildfly/standalone/deployments/
COPY --from=build --chown=jboss:jboss /build/componente-periferico/frontend-profesional-salud/target/profesional-salud.war /opt/jboss/wildfly/standalone/deployments/

EXPOSE 8080

USER jboss

CMD ["/opt/jboss/startup.sh"]

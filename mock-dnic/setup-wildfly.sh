#!/bin/bash
set -e

# Configurar variables de entorno con valores por defecto
DB_HOST=${DATABASE_HOST:-179.31.2.178}
DB_PORT=${DATABASE_PORT:-5432}
DB_NAME=${DATABASE_NAME:-postgres}
DB_USER=${DATABASE_USER:-webadmin}
DB_PASS=${DATABASE_PASSWORD:-V79FHjyEho}

echo "Configurando WildFly..."

# Iniciar WildFly en modo admin
/opt/jboss/wildfly/bin/standalone.sh --admin-only &
WILDFLY_PID=$!

# Esperar a que WildFly esté listo
until /opt/jboss/wildfly/bin/jboss-cli.sh --connect --command=":read-attribute(name=server-state)" 2>/dev/null | grep -q "running"; do
    echo "Esperando a que WildFly inicie..."
    sleep 2
done

echo "Configurando datasource con PostgreSQL..."

# Configurar el datasource usando CLI
/opt/jboss/wildfly/bin/jboss-cli.sh --connect <<EOF
/subsystem=datasources/xa-data-source=MockDnicDS:add(\
    jndi-name=java:jboss/datasources/MockDnicDS,\
    driver-name=postgresql.jar,\
    xa-datasource-class=org.postgresql.xa.PGXADataSource,\
    enabled=true,\
    use-java-context=true\
)
/subsystem=datasources/xa-data-source=MockDnicDS/xa-datasource-properties=ServerName:add(value=${DB_HOST})
/subsystem=datasources/xa-data-source=MockDnicDS/xa-datasource-properties=PortNumber:add(value=${DB_PORT})
/subsystem=datasources/xa-data-source=MockDnicDS/xa-datasource-properties=DatabaseName:add(value=${DB_NAME})
/subsystem=datasources/xa-data-source=MockDnicDS:write-attribute(name=user-name,value=${DB_USER})
/subsystem=datasources/xa-data-source=MockDnicDS:write-attribute(name=password,value=${DB_PASS})
exit
EOF

echo "Datasource configurado exitosamente"

# Detener WildFly
/opt/jboss/wildfly/bin/jboss-cli.sh --connect --command=:shutdown
wait $WILDFLY_PID

echo "Configuración completada"

#!/bin/bash

echo "Configurando datasource en WildFly..."

# Ejecutar script CLI para configurar datasource
/opt/jboss/wildfly/bin/jboss-cli.sh --file=/opt/jboss/datasource-config.cli

if [ $? -eq 0 ]; then
    echo "Datasource configurado exitosamente"
else
    echo "Error al configurar datasource, continuando con valores por defecto..."
fi

# Remover welcome-content para que no interfiera con las aplicaciones
echo "Removiendo welcome-content handler..."
/opt/jboss/wildfly/bin/jboss-cli.sh --commands="embed-server --server-config=standalone.xml,/subsystem=undertow/server=default-server/host=default-host/location=\/:remove,stop-embedded-server" 2>/dev/null || echo "Welcome-content ya removido o no existe"

# Iniciar WildFly
echo "Iniciando WildFly..."
/opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0

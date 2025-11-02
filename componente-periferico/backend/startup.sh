#!/bin/bash

echo "Configurando datasource en WildFly..."

# Ejecutar script CLI para configurar datasource
/opt/jboss/wildfly/bin/jboss-cli.sh --file=/opt/jboss/datasource-config.cli

if [ $? -eq 0 ]; then
    echo "Datasource configurado exitosamente"
else
    echo "Error al configurar datasource, continuando con valores por defecto..."
fi

# Iniciar WildFly
echo "Iniciando WildFly..."
/opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0

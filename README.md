# HCEN Platform

## Módulos
- `core-domain`: modelos y entidades compartidas (`jar`).
- `componente-central/backend`: backend central (`war`).
- `componente-periferico/backend`: backend periférico multitenant (`war`).

## Build
- Compilación completa: `mvn clean package`
- Solo backend central + dependencias: `mvn -pl componente-central/backend -am clean package`
- Solo backend periférico + dependencias: `mvn -pl componente-periferico/backend -am clean package`

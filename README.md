# HCEN Platform

## Módulos
- `core-domain`: modelos y entidades compartidas (`jar`).
- `componente-central/backend`: backend central (`war`).
- `componente-periferico/backend`: backend periférico multitenant (`war`).

## Build
- Compilación completa: `mvn clean package`
- Solo backend central + dependencias: `mvn -pl componente-central/backend -am clean package`
- Solo backend periférico + dependencias: `mvn -pl componente-periferico/backend -am clean package`

## Portal Usuario Salud – Certificados autofirmados
- Para entornos de prueba se puede desactivar la validación SSL del cliente REST de `HistoriaClinicaBean`.
- Establezca el `context-param` `hcen.trustAllCertificates` a `true` en `componente-central/frontend-usuario-salud/src/main/webapp/WEB-INF/web.xml` (o descriptor equivalente del servidor).
- Por defecto el valor es `false` para mantener la validación estándar del JDK; activar esta opción implica aceptar cualquier certificado y cualquier hostname.

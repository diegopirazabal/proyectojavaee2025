# Repository Guidelines

## Project Structure & Module Organization
- `componente-central/` agrupa los servicios y frontends del núcleo institucional; el backend Java EE vive en `componente-central/backend` (WAR Maven) y los clientes web futuros en los subdirectorios `frontend-*`.
- `componente-periferico/` replica la estructura para sedes externas, con su propio backend desplegable en `componente-periferico/backend`.
- `prestadores-externos/`, `docs/` y `scripts/` se reservan para integraciones externas, documentación funcional y utilidades de automatización; añade README locales antes de popular estas carpetas.

## Build, Test & Development Commands
- `cd componente-central/backend && mvn clean package` compila el backend central y genera el WAR en `target/`.
- `cd componente-periferico/backend && mvn clean package` hace lo mismo para el módulo periférico.
- Usa `mvn clean verify` antes de abrir PRs para ejecutar la fase completa (incluye tests cuando existan).

## Coding Style & Naming Conventions
- Código Java sigue Java 17, estructura de paquetes `com.hcen.<dominio>` y formato Maven por defecto; mantén 4 espacios de indentación y evita tabs.
- JSPs y XML deben permanecer en UTF-8; valida `web.xml` con el esquema Jakarta EE 10.
- Para frontends, nombra carpetas y componentes en kebab-case (`frontend-admin`, `gestion-turnos.vue`) y alinea estilos con el framework que se incorpore.

## Testing Guidelines
- Ubica pruebas Java en `src/test/java`, espejando la jerarquía del código fuente (`com.hcen.central.*`).
- Usa JUnit 5 y Mockito; configura dependencias en el `pom.xml` correspondiente.
- Nombra métodos de test con `should<Comportamiento>_when<Condicion>()` y mantén cobertura significativa en servicios críticos antes de desplegar.

## Commit & Pull Request Guidelines
- Los commits existentes son cortos y descriptivos en español; continua con mensajes imperativos de una línea (`Agregar validación de agenda`).
- Aglutina cambios relacionados y evita commits masivos generados automáticamente.
- Las PRs deben incluir: resumen funcional, pasos de prueba manual, issues vinculadas y capturas si afectan UI.
- Solicita revisión cruzada entre equipos (central ↔ periférico) cuando los cambios impacten integraciones compartidas.

## Environment & Deployment Notes
- Backend genera artefactos WAR; verifica despliegues en servidores compatibles con Jakarta EE 10 (por ejemplo Payara o WildFly actualizados).
- Aísla configuraciones sensibles vía variables de entorno del contenedor; no comitees credenciales ni `.env`.

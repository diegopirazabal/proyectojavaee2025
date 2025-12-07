# Módulo `tests`

Ejemplos de uso para ejecutar las pruebas y los planes de performance.

## Pruebas unitarias
- Ejecutar solo unitarios (Surefire):  
  `mvn -pl tests test`

## Pruebas de integración
- Ejecutar unitarios + IT (Failsafe) en el módulo:  
  `mvn -pl tests verify`
- Saltar IT si necesitas rapidez:  
  `mvn -pl tests verify -DskipITs=true`

## Pruebas de performance (JMeter)
- Limpieza previa recomendada:  
  `rm -f tests/target/jmeter/results/*.jtl tests/target/jmeter/results/summary.csv`
- El perfil `perf` ya genera nombres únicos para `.jtl` (`testResultsTimestamp=true` y formato `yyyyMMdd-HHmmss`).
- Planes disponibles en `tests/performance/`: `api-smoke.jmx`, `periferico-admin-login.jmx` y `periferico-documentos.jmx`.
- Para ejecutar solo el plan de login (sin el smoke), agrega `-Djmeter.testFilesIncluded=periferico-admin-login.jmx`.
- Comandos por nivel (ejemplo con el plan de login, ajusta host/path/credenciales). El plugin no permite fijar el prefijo vía CLI; renombra el `.jtl` generado para etiquetar el nivel:
  - Bajo:  
    ```
    mvn -pl tests -Pperf jmeter:configure jmeter:jmeter ^
      -Djmeter.testFilesIncluded=periferico-admin-login.jmx ^
      -Dprotocol=https ^
      -Dhost=<host> ^
      -Dport=443 ^
      -Dpath=/multitenant-api/auth/login ^
      -Dusername=<user> ^
      -Dpassword=<pass> ^
      -DtenantId=<tenant> ^
      -Dusers=5 ^
      -DrampSeconds=10 ^
      -Diterations=3
    # luego renombra el último .jtl generado:
    mv tests/target/jmeter/results/$(ls -t tests/target/jmeter/results/periferico-admin-login*.jtl | head -1) tests/target/jmeter/results/periferico-admin-login-bajo.jtl
    ```
  - Medio:  
    ```
    mvn -pl tests -Pperf jmeter:configure jmeter:jmeter ^
      -Djmeter.testFilesIncluded=periferico-admin-login.jmx ^
      -Dprotocol=https ^
      -Dhost=<host> ^
      -Dport=443 ^
      -Dpath=/multitenant-api/auth/login ^
      -Dusername=<user> ^
      -Dpassword=<pass> ^
      -DtenantId=<tenant> ^
      -Dusers=20 ^
      -DrampSeconds=20 ^
      -Diterations=10
    mv tests/target/jmeter/results/$(ls -t tests/target/jmeter/results/periferico-admin-login*.jtl | head -1) tests/target/jmeter/results/periferico-admin-login-medio.jtl
    ```
  - Alto:  
    ```
    mvn -pl tests -Pperf jmeter:configure jmeter:jmeter ^
      -Djmeter.testFilesIncluded=periferico-admin-login.jmx ^
      -Dprotocol=https ^
      -Dhost=<host> ^
      -Dport=443 ^
      -Dpath=/multitenant-api/auth/login ^
      -Dusername=<user> ^
      -Dpassword=<pass> ^
      -DtenantId=<tenant> ^
      -Dusers=50 ^
      -DrampSeconds=30 ^
      -Diterations=15
    mv tests/target/jmeter/results/$(ls -t tests/target/jmeter/results/periferico-admin-login*.jtl | head -1) tests/target/jmeter/results/periferico-admin-login-alto.jtl
    ```
- Plan de documentos periférico (solo GET):  
  ```
  mvn -pl tests -Pperf jmeter:configure jmeter:jmeter ^
    -Djmeter.testFilesIncluded=periferico-documentos.jmx ^
    -Djmeter.testFilesExcluded=api-smoke.jmx ^
    -Dprotocol=https ^
    -Dhost=prestador-salud.up.railway.app ^
    -Dport=443 ^
    -DtenantId=550e8400-e29b-41d4-a716-446655440001 ^
    -DcedulaPaciente=27425057 ^
    -DprofesionalCi=78945632 ^
    -DdocId=0c198285-95a8-4593-b8a3-3153686c2402 ^
    -Dusers=50 ^
    -DrampSeconds=30 ^
    -Diterations=15
  mv tests/target/jmeter/results/$(ls -t tests/target/jmeter/results/periferico-documentos*.jtl | head -1) tests/target/jmeter/results/periferico-documentos-alto.jtl
  ```

## Checklist previa para JMeter
- Hosts/puertos por entorno: central (Elastic Cloud) y periférico (Railway u otro), con protocolo/puerto correctos.
- Endpoints concretos a medir y reglas de acceso: rutas, headers/tokens, refresh/roles necesarios.
- Credenciales de prueba: usuarios para central (/api/auth/login o /api/auth/token) y periférico (/auth/login), más client/secret si aplica.
- Datos de negocio dummy válidos en el entorno: cédulas, documentoId, tenantId, etc.; nunca usar datos productivos.
- Límites del entorno: rate limits o políticas a respetar en la corrida.

## Flujos sugeridos para medir
- Central: `/api/auth/login` y `/api/auth/token`; `/api/usuarios-salud`; `/api/historia-clinica/mis-documentos` o `/api/historia-clinica/by-cedula/{cedula}`; `/api/politicas-acceso/validar` y `/api/politicas-acceso/documento/{documentoId}`; `/api/notifications/solicitudes-acceso` y `/api/notifications/usuarios/{cedula}`.
- Periférico: `/auth/login` y `/auth/profesional/login`; `/usuarios` y `/usuarios/{cedula}/clinica/{tenantId}`; `/documentos` (crear/consultar), `/documentos/paciente/{cedula}`, `/documentos/profesional/{ci}`; `/documentos/validar-acceso` y `/documentos/{id}/solicitar-acceso`; `/documentos/sincronizar-pendientes` y `/usuarios/pendientes-sync`; catálogos `/profesionales`, `/especialidades`, `/clinicas`; `/api/solicitudes-acceso`.

## Resumen de resultados JMeter
- Script para resumir todos los `.jtl` a CSV (avg, p50, p90, p95, min, max, errores):
  - `python tests/scripts/jtl_summary.py`
  - Salida: `tests/target/jmeter/results/summary.csv`

## Notas
- Los parámetros `protocol`, `host`, `port`, `path`, `username`, `password`, `tenantId`, `users`, `rampSeconds`, `iterations` son overrideables por línea de comando en el perfil `perf`.
- El perfil `perf` ejecuta también los tests unitarios/IT antes de lanzar JMeter (fase `verify`). Usa `-DskipTests`/`-DskipITs` para omitirlos en una corrida rápida.

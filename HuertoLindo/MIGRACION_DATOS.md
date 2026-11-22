# Fuente de datos Android

La API del backend es la fuente de verdad para el uso normal de la aplicación: autenticación, lecturas y escrituras. Room ya no se usa como caché ni como alternativa cuando falla la API. Las cookies de sesión se conservan en `SharedPreferences`; las fotografías se suben al backend, que guarda sus archivos en `uploads/` y sus relaciones en MySQL.

Room se conserva temporalmente para leer instalaciones anteriores y registrar el marcador de migración. Los datos demo conocidos se excluyen. La migración solo borra las filas locales después de comprobar en el backend los usuarios, sectores, cultivos y tareas compatibles.

La migración se detiene sin borrar datos ni marcarse como completa si encuentra reportes, evidencias, validaciones o actividades locales. El backend ahora permite consultar actividades, validaciones y evidencias asociadas, pero todavía no existe un flujo de importación que preserve todos sus IDs, relaciones y metadatos. También se detiene ante usuarios locales ausentes del servidor (no es posible reconstruir sus contraseñas desde el hash de Room), campos locales no representables o errores de red/autorización.

En una base MySQL existente, aplicar `src/main/resources/db/manual/2026-11-22-evidencia-asociaciones.sql` del backend antes de usar las asociaciones nuevas de evidencia. No se ha ejecutado una migración real ni se ha verificado contra una instancia MySQL en vivo; conserva Room hasta comprobar la migración de los datos locales que deban mantenerse.

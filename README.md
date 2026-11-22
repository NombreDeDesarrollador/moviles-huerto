
HuertoLindo

Aplicación Android para gestionar un huerto escolar: sectores, cultivos, tareas asignadas a estudiantes, evidencia fotográfica, validación por parte de un administrador y reporte de problemas. Funciona sin conexión, con los datos guardados localmente en el dispositivo.

Proyecto académico de desarrollo móvil (DUOC UC).

## Funcionalidades

**Para todos los usuarios**
- Registro e inicio de sesión con correo y contraseña.
- Consulta de sectores y cultivos, con detalle de cada uno.
- Historial de actividades de las tareas.
- Reporte de problemas del huerto (prioridad Baja, Media o Alta), con fotografía opcional.
- Lecturas de sensores de humedad y temperatura (**simuladas**, solo de demostración).
- Foto de perfil y cierre de sesión.

**Estudiante**
- Ve únicamente las tareas que tiene asignadas.
- Inicia una tarea pendiente (o una rechazada) y la entrega adjuntando evidencia fotográfica, tomada con la cámara o elegida desde la galería.
- Recibe aviso de tareas atrasadas.

**Administrador**
- Crea, edita y elimina sectores, cultivos y tareas.
- Asigna tareas a estudiantes por tipo: Riego, Siembra, Mantenimiento u Observación.
- Valida o rechaza las tareas entregadas. Para rechazar debe indicar un motivo.
- Actualiza el estado de los problemas reportados (Pendiente, En atención, Resuelto).
- Consulta el resumen general del huerto (sectores, cultivos, tareas completadas y pendientes).

### Flujo de una tarea

```
Pendiente ──► En desarrollo ──► Por validar ──► Completada
    ▲                                │
    └──────────── Rechazada ◄────────┘  (el administrador indica el motivo)
```

Un administrador no puede validar una tarea que tiene asignada a sí mismo.

## Tecnologías

| Área | Tecnología |
|---|---|
| Lenguaje | Kotlin 2.2.10 |
| Interfaz | Jetpack Compose + Material 3 (Compose BOM 2026.02.01) |
| Persistencia local | Room 2.7.2 (con KAPT), esquema versión 7 |
| API y base de datos remota | Retrofit + API REST del proyecto MovilesBackend; el backend administra MySQL |
| Imágenes | Coil 3.3.0 |
| Arquitectura | MVVM: `ViewModel` + `StateFlow` + patrón Repository |
| Build | Android Gradle Plugin 9.0.1, Gradle 9.2.1 |
| Pruebas | JUnit 4, AndroidX Test, Espresso |

Configuración de Android: `minSdk` 24, `targetSdk` 36, `compileSdk` 36.

## Arquitectura

```
HuertoLindo/app/src/main/java/com/example/huertolindo/
├── data/
│   ├── local/          HuertoDatabase (Room, DAOs y migraciones), LegacyUsuarioImporter
│   ├── model/          Entidades: Usuario, Sector, Cultivo, Tarea, Evidencia,
│   │                   Validacion, ReporteProblema, ActividadTarea, SensorSimulado
│   ├── repository/     HuertoRepository (interfaz) y RoomHuertoRepository
│   └── security/       PasswordHasher, PermisosHuerto, ReglasHuerto
├── ui/
│   ├── components/    Componentes Compose reutilizables, como DashboardAction
│   ├── screens/        Login, Registro, dashboards, sectores, cultivos, tareas, etc.
│   │                   HuertoUiState centraliza la recolección del estado compartido
│   ├── theme/          Color, Tema y Tipografía
│   └── MainActivity.kt, Foto.kt, CamaraUtils.kt, StatusChip.kt
└── viewmodel/          HuertoViewModel (estado de la UI y lógica de negocio)
```

- La navegación entre pantallas se maneja con estado en `MainActivity` y transiciones `Crossfade`.
- Las reglas de permisos por rol están centralizadas en `PermisosHuerto` y las reglas de fechas en `ReglasHuerto`, lo que permite probarlas con tests unitarios.
- La base de datos (`huerto_lindo_room.db`) incluye migraciones de la versión 1 a la 7.
- Retrofit se conecta con el backend Spring Boot. Autenticación, registro y la lectura de sectores, cultivos y tareas usan DTOs del contrato remoto.

## Seguridad

- Las contraseñas **no se guardan en texto plano**: se almacenan con sal aleatoria de 16 bytes y hash PBKDF2 (`PBKDF2WithHmacSHA1`, 120.000 iteraciones, 256 bits).
- La verificación usa comparación de tiempo constante.
- Si existen usuarios de una base de datos anterior, se importan y sus contraseñas en claro se eliminan.
- Las fotografías temporales se comparten mediante `FileProvider`, sin exponer rutas del sistema.

## Requisitos

- Android Studio reciente, con soporte para AGP 9.x.
- JDK 21 (el proyecto lo declara como toolchain del daemon de Gradle).
- Un emulador o dispositivo con Android 7.0 (API 24) o superior.

## Cómo ejecutar

1. Clona el repositorio y cambia a la rama `develop`:
   ```bash
   git clone https://github.com/NombreDeDesarrollador/moviles-huerto.git
   cd moviles-huerto
   git checkout develop
   ```
2. Abre la carpeta `HuertoLindo/` en Android Studio y espera a que termine la sincronización de Gradle.
3. Selecciona un emulador o dispositivo y presiona **Run ▶**.

### Conectar con MovilesBackend

1. Inicia MySQL y el backend, con la base de datos y credenciales configuradas en `MovilesBackend`.
2. La URL de Android está configurada en `RetrofitClient.kt` como `http://10.0.2.2:8080/api/`. `10.0.2.2` permite que el emulador Android acceda al computador anfitrión.
3. Para usar un teléfono físico, cambia `10.0.2.2` por la dirección IP local del computador y conecta ambos dispositivos a la misma red.
4. El backend debe permanecer accesible desde el dispositivo. Android se comunica con la API; solo el backend se conecta directamente a MySQL.

La app sincroniza sectores y cultivos, y permite crear y consultar tareas remotas. Como la API exige un cultivo y un usuario existentes para crear una tarea, ambos deben estar registrados en el backend. Para crear cultivos, el backend admite desde el estado **Sembrado**. El contrato remoto no almacena el estado de mantenimiento del sector, la frecuencia de riego ni la fecha estimada de cosecha; las categorías de tareas no compatibles se convierten al tipo genérico del backend. Los estados de las tareas se conservan en el campo `estado` del backend.

La sesión HTTP del backend se conserva entre reinicios en las preferencias privadas de la app y se elimina al cerrar sesión. Si falla la consulta de tareas, la app mantiene visibles los sectores y cultivos que sí pudo sincronizar y muestra una advertencia. Tras instalar esta actualización, cierra sesión e inicia sesión de nuevo una vez para crear la cookie persistente.

El panel docente incluye una opción **Cultivos** para crear cultivos y elegir el sector asociado. También se pueden agregar desde **Sectores → elegir sector → Agregar cultivo**. Si intentas crear una tarea en un sector sin cultivos, el formulario ofrece un acceso directo.

También puedes compilar desde la terminal, dentro de `HuertoLindo/`:

```bash
./gradlew assembleDebug      # genera el APK de depuración
./gradlew installDebug       # lo instala en el dispositivo conectado
```

## Pruebas

Desde `HuertoLindo/`:

```bash
./gradlew test                  # pruebas unitarias
./gradlew connectedAndroidTest  # pruebas instrumentadas (requiere emulador o dispositivo)
```

Las pruebas unitarias cubren los permisos por rol (`PermisosHuertoTest`), las reglas de fechas y tipos de tarea, y el hash de contraseñas (`PasswordHasherTest`).

## Notas

- Room mantiene una copia local y los datos de las funciones aún no conectadas a la API. Las operaciones remotas de sectores, cultivos y creación de tareas muestran un error si el backend las rechaza o no está disponible, en vez de indicar que se guardaron correctamente.
- Los sensores son simulados y no leen hardware real.
- La app trae cuentas iniciales precargadas (un administrador y estudiantes). También se pueden crear usuarios desde la pantalla de registro.

## Autores

- Matías Miranda
<!-- Agrega aquí a tus compañeros de equipo -->

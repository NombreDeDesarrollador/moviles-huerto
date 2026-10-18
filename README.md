
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
| Base de datos | Room 2.7.2 (con KAPT), esquema versión 6 |
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
│   ├── screens/        Login, Registro, dashboards, sectores, cultivos, tareas, etc.
│   ├── theme/          Color, Tema y Tipografía
│   └── MainActivity.kt, Foto.kt, CamaraUtils.kt, StatusChip.kt
└── viewmodel/          HuertoViewModel (estado de la UI y lógica de negocio)
```

- La navegación entre pantallas se maneja con estado en `MainActivity` y transiciones `Crossfade`.
- Las reglas de permisos por rol están centralizadas en `PermisosHuerto` y las reglas de fechas en `ReglasHuerto`, lo que permite probarlas con tests unitarios.
- La base de datos (`huerto_lindo_room.db`) incluye migraciones de la versión 1 a la 6.

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

- Los datos viven solo en el dispositivo: no hay servidor ni sincronización.
- Los sensores son simulados y no leen hardware real.
- La app trae cuentas iniciales precargadas (un administrador y estudiantes). También se pueden crear usuarios desde la pantalla de registro.

## Autores

- Matías Miranda
<!-- Agrega aquí a tus compañeros de equipo -->

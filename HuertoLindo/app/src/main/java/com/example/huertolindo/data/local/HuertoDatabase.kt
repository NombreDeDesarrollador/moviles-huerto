package com.example.huertolindo.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.huertolindo.data.model.ActividadTarea
import com.example.huertolindo.data.model.Cultivo
import com.example.huertolindo.data.model.Evidencia
import com.example.huertolindo.data.model.ReporteProblema
import com.example.huertolindo.data.model.Sector
import com.example.huertolindo.data.model.Tarea
import com.example.huertolindo.data.model.Usuario
import com.example.huertolindo.data.model.Validacion

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuarios ORDER BY nombre COLLATE NOCASE")
    suspend fun obtenerTodos(): List<Usuario>

    @Query("SELECT * FROM usuarios WHERE id = :usuarioId")
    suspend fun obtenerPorId(usuarioId: String): Usuario?

    @Query("SELECT * FROM usuarios WHERE correo = :correo COLLATE NOCASE LIMIT 1")
    suspend fun obtenerPorCorreo(correo: String): Usuario?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(usuario: Usuario)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun asegurar(usuario: Usuario)

    @Query(
        """
        UPDATE usuarios
        SET password_salt = :passwordSalt, password_hash = :passwordHash
        WHERE id = :usuarioId AND password_hash = ''
        """
    )
    suspend fun importarCredencialesSiVacias(
        usuarioId: String,
        passwordSalt: String,
        passwordHash: String
    )

    @Update
    suspend fun actualizar(usuario: Usuario)
}

@Dao
interface SectorDao {
    @Query("SELECT * FROM sectores ORDER BY nombre COLLATE NOCASE")
    suspend fun obtenerTodos(): List<Sector>

    @Query("SELECT * FROM sectores WHERE id = :sectorId")
    suspend fun obtenerPorId(sectorId: String): Sector?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(sector: Sector)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun asegurar(sector: Sector)

    @Update
    suspend fun actualizar(sector: Sector)

    @androidx.room.Delete
    suspend fun eliminar(sector: Sector)
}

@Dao
interface CultivoDao {
    @Query("SELECT * FROM cultivos ORDER BY fecha_siembra DESC")
    suspend fun obtenerTodos(): List<Cultivo>

    @Query("SELECT * FROM cultivos WHERE sector_id = :sectorId")
    suspend fun obtenerPorSector(sectorId: String): List<Cultivo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(cultivo: Cultivo)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun asegurar(cultivo: Cultivo)

    @Update
    suspend fun actualizar(cultivo: Cultivo)

    @androidx.room.Delete
    suspend fun eliminar(cultivo: Cultivo)
}

@Dao
interface TareaDao {
    @Query("SELECT * FROM tareas ORDER BY fecha_programada DESC")
    suspend fun obtenerTodos(): List<Tarea>

    @Query("SELECT * FROM tareas WHERE usuario_id = :usuarioId ORDER BY fecha_programada DESC")
    suspend fun obtenerPorUsuario(usuarioId: String): List<Tarea>

    @Query("SELECT * FROM tareas WHERE sector_id = :sectorId ORDER BY fecha_programada DESC")
    suspend fun obtenerPorSector(sectorId: String): List<Tarea>

    @Query("SELECT * FROM tareas WHERE id = :tareaId")
    suspend fun obtenerPorId(tareaId: String): Tarea?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(tarea: Tarea)

    @Update
    suspend fun actualizar(tarea: Tarea)

    @androidx.room.Delete
    suspend fun eliminar(tarea: Tarea)
}

@Dao
interface EvidenciaDao {
    @Query("SELECT * FROM evidencias ORDER BY fecha_registro DESC")
    suspend fun obtenerTodos(): List<Evidencia>

    @Query("SELECT * FROM evidencias WHERE tarea_id = :tareaId ORDER BY fecha_registro DESC")
    suspend fun obtenerPorTarea(tareaId: String): List<Evidencia>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(evidencia: Evidencia)

    @androidx.room.Delete
    suspend fun eliminar(evidencia: Evidencia)
}

@Dao
interface ValidacionDao {
    @Query("SELECT * FROM validaciones ORDER BY fecha_validacion DESC")
    suspend fun obtenerTodos(): List<Validacion>

    @Query("SELECT * FROM validaciones WHERE tarea_id = :tareaId ORDER BY fecha_validacion DESC")
    suspend fun obtenerPorTarea(tareaId: String): List<Validacion>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(validacion: Validacion)
}

@Dao
interface ActividadTareaDao {
    @Query("SELECT * FROM actividades_tarea ORDER BY fecha DESC")
    suspend fun obtenerTodos(): List<ActividadTarea>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(actividad: ActividadTarea)
}

@Dao
interface ReporteProblemaDao {
    @Query("SELECT * FROM reporte_problemas ORDER BY fecha_reporte DESC")
    suspend fun obtenerTodos(): List<ReporteProblema>

    @Query("SELECT * FROM reporte_problemas WHERE usuario_id = :usuarioId ORDER BY fecha_reporte DESC")
    suspend fun obtenerPorUsuario(usuarioId: String): List<ReporteProblema>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(reporteProblema: ReporteProblema)

    @Update
    suspend fun actualizar(reporteProblema: ReporteProblema)
}

@Database(
    entities = [
        Usuario::class,
        Sector::class,
        Cultivo::class,
        Tarea::class,
        Evidencia::class,
        Validacion::class,
        ReporteProblema::class,
        ActividadTarea::class
    ],
    version = 6,
    exportSchema = false
)
abstract class HuertoDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun sectorDao(): SectorDao
    abstract fun cultivoDao(): CultivoDao
    abstract fun tareaDao(): TareaDao
    abstract fun evidenciaDao(): EvidenciaDao
    abstract fun validacionDao(): ValidacionDao
    abstract fun reporteProblemaDao(): ReporteProblemaDao
    abstract fun actividadTareaDao(): ActividadTareaDao
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tareas ADD COLUMN motivo_rechazo TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE reporte_problemas RENAME TO reporte_problemas_v1")
        db.execSQL(
            """
            CREATE TABLE reporte_problemas (
                id TEXT NOT NULL PRIMARY KEY,
                sector_id TEXT NOT NULL,
                cultivo_id TEXT,
                usuario_id TEXT NOT NULL,
                descripcion TEXT NOT NULL,
                prioridad TEXT NOT NULL,
                estado TEXT NOT NULL,
                fecha_reporte TEXT NOT NULL,
                FOREIGN KEY(sector_id) REFERENCES sectores(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(cultivo_id) REFERENCES cultivos(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(usuario_id) REFERENCES usuarios(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO reporte_problemas
                (id, sector_id, cultivo_id, usuario_id, descripcion, prioridad, estado, fecha_reporte)
            SELECT r.id, t.sector_id, NULL, r.usuario_id, r.descripcion, r.prioridad, r.estado, r.fecha_reporte
            FROM reporte_problemas_v1 r
            INNER JOIN tareas t ON t.id = r.tarea_id
            """.trimIndent()
        )
        db.execSQL("DROP TABLE reporte_problemas_v1")
        db.execSQL("CREATE INDEX index_reporte_problemas_sector_id ON reporte_problemas(sector_id)")
        db.execSQL("CREATE INDEX index_reporte_problemas_cultivo_id ON reporte_problemas(cultivo_id)")
        db.execSQL("CREATE INDEX index_reporte_problemas_usuario_id ON reporte_problemas(usuario_id)")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE actividades_tarea (
                id TEXT NOT NULL PRIMARY KEY,
                tarea_id TEXT NOT NULL,
                titulo_tarea TEXT NOT NULL,
                usuario_relacionado_id TEXT NOT NULL,
                usuario_id TEXT NOT NULL,
                nombre_usuario TEXT NOT NULL,
                accion TEXT NOT NULL,
                estado TEXT NOT NULL,
                fecha TEXT NOT NULL,
                FOREIGN KEY(tarea_id) REFERENCES tareas(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(usuario_id) REFERENCES usuarios(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(usuario_relacionado_id) REFERENCES usuarios(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX index_actividades_tarea_tarea_id ON actividades_tarea(tarea_id)")
        db.execSQL("CREATE INDEX index_actividades_tarea_usuario_id ON actividades_tarea(usuario_id)")
        db.execSQL(
            "CREATE INDEX index_actividades_tarea_usuario_relacionado_id ON actividades_tarea(usuario_relacionado_id)"
        )
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE usuarios ADD COLUMN password_salt TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE usuarios ADD COLUMN password_hash TEXT NOT NULL DEFAULT ''")
        db.execSQL("CREATE UNIQUE INDEX index_usuarios_correo ON usuarios(correo)")
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE evidencias_nueva (
                id TEXT NOT NULL PRIMARY KEY,
                tarea_id TEXT,
                cultivo_id TEXT,
                reporte_id TEXT,
                tipo TEXT NOT NULL,
                ruta_archivo TEXT NOT NULL,
                descripcion TEXT NOT NULL,
                fecha_registro TEXT NOT NULL,
                FOREIGN KEY(tarea_id) REFERENCES tareas(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(cultivo_id) REFERENCES cultivos(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(reporte_id) REFERENCES reporte_problemas(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO evidencias_nueva
                (id, tarea_id, tipo, ruta_archivo, descripcion, fecha_registro)
            SELECT id, tarea_id, tipo, ruta_archivo, descripcion, fecha_registro
            FROM evidencias
            """.trimIndent()
        )
        db.execSQL("DROP TABLE evidencias")
        db.execSQL("ALTER TABLE evidencias_nueva RENAME TO evidencias")
        db.execSQL("CREATE INDEX index_evidencias_tarea_id ON evidencias(tarea_id)")
        db.execSQL("CREATE INDEX index_evidencias_cultivo_id ON evidencias(cultivo_id)")
        db.execSQL("CREATE INDEX index_evidencias_reporte_id ON evidencias(reporte_id)")
    }
}

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sectores ADD COLUMN ubicacion TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE cultivos ADD COLUMN fecha_cosecha_estimada TEXT NOT NULL DEFAULT ''")
        db.execSQL(
            "ALTER TABLE tareas ADD COLUMN tipo TEXT NOT NULL DEFAULT 'Mantenimiento'"
        )
    }
}

object HuertoDatabaseProvider {
    @Volatile
    private var INSTANCE: HuertoDatabase? = null

    fun getDatabase(context: Context): HuertoDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                HuertoDatabase::class.java,
                "huerto_lindo_room.db"
            )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6
                )
                .build()
            INSTANCE = instance
            instance
        }
    }
}

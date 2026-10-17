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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(usuario: Usuario)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun asegurar(usuario: Usuario)

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
}

@Dao
interface TareaDao {
    @Query("SELECT * FROM tareas ORDER BY fecha_programada DESC")
    suspend fun obtenerTodos(): List<Tarea>

    @Query("SELECT * FROM tareas WHERE usuario_id = :usuarioId ORDER BY fecha_programada DESC")
    suspend fun obtenerPorUsuario(usuarioId: String): List<Tarea>

    @Query("SELECT * FROM tareas WHERE sector_id = :sectorId ORDER BY fecha_programada DESC")
    suspend fun obtenerPorSector(sectorId: String): List<Tarea>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(tarea: Tarea)

    @Update
    suspend fun actualizar(tarea: Tarea)
}

@Dao
interface EvidenciaDao {
    @Query("SELECT * FROM evidencias WHERE tarea_id = :tareaId ORDER BY fecha_registro DESC")
    suspend fun obtenerPorTarea(tareaId: String): List<Evidencia>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(evidencia: Evidencia)
}

@Dao
interface ValidacionDao {
    @Query("SELECT * FROM validaciones WHERE tarea_id = :tareaId ORDER BY fecha_validacion DESC")
    suspend fun obtenerPorTarea(tareaId: String): List<Validacion>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(validacion: Validacion)
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
        ReporteProblema::class
    ],
    version = 2,
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
                .fallbackToDestructiveMigration()
                .build()
            INSTANCE = instance
            instance
        }
    }
}

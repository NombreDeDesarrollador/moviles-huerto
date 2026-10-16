package com.example.huertolindo.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Base64
import com.example.huertolindo.data.model.Usuario
import com.example.huertolindo.data.security.PasswordHasher
import java.util.Locale

class UsuarioDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_USERS (
                id TEXT PRIMARY KEY NOT NULL,
                correo TEXT NOT NULL UNIQUE COLLATE NOCASE,
                nombre TEXT NOT NULL,
                apellido TEXT NOT NULL DEFAULT '',
                perfil TEXT NOT NULL,
                taller_asignado TEXT NOT NULL,
                password TEXT NOT NULL DEFAULT '',
                password_salt TEXT NOT NULL,
                password_hash TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.beginTransaction()
        try {
            INITIAL_USERS.forEach { account ->
                val values = ContentValues().apply {
                    put("id", account.id)
                    put("correo", account.correo)
                    put("nombre", account.nombre)
                    put("apellido", "")
                    put("perfil", account.perfil)
                    put("taller_asignado", account.tallerAsignado)
                    put("password_salt", account.passwordSalt)
                    put("password_hash", account.passwordHash)
                }
                db.insertOrThrow(TABLE_USERS, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN apellido TEXT NOT NULL DEFAULT ''")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN password TEXT NOT NULL DEFAULT ''")
        }
    }

    fun autenticar(correo: String, password: String): Usuario? {
        val correoNormalizado = correo.trim().lowercase(Locale.ROOT)
        val usuario: Usuario
        val passwordGuardada: String
        val saltGuardada: String
        val hashGuardado: String
        readableDatabase.query(
            TABLE_USERS,
            arrayOf("id", "correo", "nombre", "apellido", "perfil", "taller_asignado", "password", "password_salt", "password_hash"),
            "correo = ?",
            arrayOf(correoNormalizado),
            null,
            null,
            null
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null

            usuario = Usuario(
                id = cursor.getString(0),
                correo = cursor.getString(1),
                nombre = cursor.getString(2),
                apellido = cursor.getString(3),
                perfil = cursor.getString(4),
                tallerAsignado = cursor.getString(5)
            )
            passwordGuardada = cursor.getString(6)
            saltGuardada = cursor.getString(7)
            hashGuardado = cursor.getString(8)
        }

        if (passwordGuardada.isNotEmpty()) {
            return usuario.takeIf { password == passwordGuardada }
        }

        if (!PasswordHasher.verify(password, decode(saltGuardada), decode(hashGuardado))) {
            return null
        }

        writableDatabase.update(
            TABLE_USERS,
            ContentValues().apply {
                put("password", password)
                put("password_salt", "")
                put("password_hash", "")
            },
            "id = ?",
            arrayOf(usuario.id)
        )
        return usuario
    }

    fun obtenerUsuarios(): List<Usuario> =
        readableDatabase.query(
            TABLE_USERS,
            arrayOf("id", "correo", "nombre", "apellido", "perfil", "taller_asignado"),
            null,
            null,
            null,
            null,
            "nombre COLLATE NOCASE"
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        Usuario(
                            id = cursor.getString(0),
                            correo = cursor.getString(1),
                            nombre = cursor.getString(2),
                            apellido = cursor.getString(3),
                            perfil = cursor.getString(4),
                            tallerAsignado = cursor.getString(5)
                        )
                    )
                }
            }
        }

    fun registrar(nombre: String, apellido: String, correo: String, password: String) {
        val values = ContentValues().apply {
            put("id", java.util.UUID.randomUUID().toString())
            put("correo", correo.trim().lowercase(Locale.ROOT))
            put("nombre", nombre.trim())
            put("apellido", apellido.trim())
            put("perfil", "estudiante")
            put("taller_asignado", "General")
            put("password", password)
            put("password_salt", "")
            put("password_hash", "")
        }
        writableDatabase.insertOrThrow(TABLE_USERS, null, values)
    }

    private fun decode(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)

    private data class CuentaInicial(
        val id: String,
        val nombre: String,
        val correo: String,
        val perfil: String,
        val tallerAsignado: String,
        val passwordSalt: String,
        val passwordHash: String
    )

    private companion object {
        const val DATABASE_NAME = "huerto_lindo.db"
        const val DATABASE_VERSION = 3
        const val TABLE_USERS = "usuarios"

        val INITIAL_USERS = listOf(
            CuentaInicial(
                "admin-christian", "Christian Acuña", "christian.acuna@profesor.cl",
                "administrador", "General", "KN3Zx43FoWBLCN9R872ZaA==",
                "8QLW++QgddM1gjRVq6+ZPrzIjRHep4WQd81zwfDMYFY="
            ),
            CuentaInicial(
                "student-vicente", "Vicente Garrido", "vicente.garrido@estudiante.cl",
                "estudiante", "General", "2GnBIZxTwj5SLH0w9qnmZg==",
                "4/Jo1HH4uwWMkJbMIrjJVIoOyNXfMHqgWSH8Ev6jTSM="
            ),
            CuentaInicial(
                "student-matias", "Matias Miranda", "matias.miranda@estudiante.cl",
                "estudiante", "General", "JghCFf8URQ9YIZniwY3qSQ==",
                "kKzk4YkgA++1ybj9l4hqQ7n9A3WXmx4KkTQ8pJYy/go="
            ),
            CuentaInicial(
                "student-oscar", "Oscar Silva", "oscar.silva@estudiante.cl",
                "estudiante", "General", "VOYPyAD8qQ+6mMqqw74KyA==",
                "NIIduLTSsXOPPN/P/G2HCCZb2hEWPjjH6VtximmaPQU="
            )
        )
    }
}

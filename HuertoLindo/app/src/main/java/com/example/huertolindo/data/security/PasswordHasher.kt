package com.example.huertolindo.data.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val SALT_SIZE_BYTES = 16
    private const val HASH_SIZE_BITS = 256
    private const val ITERATIONS = 120_000

    fun createSalt(): ByteArray = ByteArray(SALT_SIZE_BYTES).also(SecureRandom()::nextBytes)

    fun hash(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, HASH_SIZE_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun verify(password: String, salt: ByteArray, expectedHash: ByteArray): Boolean =
        MessageDigest.isEqual(hash(password, salt), expectedHash)
}

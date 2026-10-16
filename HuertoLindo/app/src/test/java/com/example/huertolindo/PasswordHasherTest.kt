package com.example.huertolindo

import com.example.huertolindo.data.security.PasswordHasher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {
    @Test
    fun verifiesPasswordAndRejectsIncorrectPassword() {
        val salt = PasswordHasher.createSalt()
        val hash = PasswordHasher.hash("test-password", salt)

        assertTrue(PasswordHasher.verify("test-password", salt, hash))
        assertFalse(PasswordHasher.verify("incorrecta", salt, hash))
    }

    @Test
    fun createsDifferentSaltsForEachAccount() {
        assertNotEquals(
            PasswordHasher.createSalt().toList(),
            PasswordHasher.createSalt().toList()
        )
    }
}

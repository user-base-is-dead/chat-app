package com.civenth.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val KEY_ALIAS = "civenth_session"
private const val KEYSTORE = "AndroidKeyStore"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val IV_SIZE = 12
private const val TAG_BITS = 128
private const val ACCESS = "access"
private const val REFRESH = "refresh"

class TokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    val accessToken: String? get() = read(ACCESS)
    val refreshToken: String? get() = read(REFRESH)

    fun save(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(ACCESS, encrypt(accessToken))
            .putString(REFRESH, encrypt(refreshToken))
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun read(name: String): String? {
        val stored = prefs.getString(name, null) ?: return null
        return try {
            decrypt(stored)
        } catch (_: Exception) {
            clear()
            null
        }
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val sealed = cipher.iv + cipher.doFinal(value.toByteArray())
        return Base64.encodeToString(sealed, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val sealed = Base64.decode(value, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(TAG_BITS, sealed, 0, IV_SIZE)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), spec)
        return String(cipher.doFinal(sealed, IV_SIZE, sealed.size - IV_SIZE))
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }
}

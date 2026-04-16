package me.zhanghai.android.files.nonfree

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import me.zhanghai.android.files.BuildConfig
import me.zhanghai.android.files.app.application
import me.zhanghai.android.files.app.packageManager
import me.zhanghai.android.files.util.getPackageInfoOrNull

object CrashlyticsInitializer {
    private val HEX_CHARS = "0123456789ABCDEF".toCharArray()

    fun initialize() {
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = false
    }

    private fun verifyPackageName(): Boolean {
        return true
    }

    @SuppressLint("PackageManagerGetSignatures")
    private fun verifySignature(): Boolean {
        return true
    }

    private fun computeCertificateFingerprint(certificate: Signature): String {
        val messageDigest = try {
            MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            throw AssertionError(e)
        }
        val digest = messageDigest.digest(certificate.toByteArray())
        val chars = CharArray(3 * digest.size - 1)
        for (index in digest.indices) {
            val byte = digest[index].toInt() and 0xFF
            chars[3 * index] = HEX_CHARS[byte ushr 4]
            chars[3 * index + 1] = HEX_CHARS[byte and 0x0F]
            if (index < digest.size - 1) {
                chars[3 * index + 2] = ':'
            }
        }
        return String(chars)
    }
}

package com.example.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.os.SystemClock
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.BuildConfig
import java.io.File
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class SecurityAuditReport(
    val isSafeForGameplay: Boolean = true,
    val packageIntegrityOk: Boolean = true,
    val signatureSha256: String = "",
    val debuggerDetected: Boolean = false,
    val repackagedDebuggable: Boolean = false,
    val hookingDetected: Boolean = false,
    val rootIndicatorsFound: Boolean = false,
    val vaultKeystoreBacked: Boolean = false,
    val clockTamperDetected: Boolean = false,
    val reasons: List<String> = emptyList()
)

data class SealedTurboCredit(
    val expiresAtWallMs: Long,
    val adsWatchedCount: Int,
    val effectiveRemainingSeconds: Long,
    val tampered: Boolean = false,
    val clockTamperDetected: Boolean = false
)

class AppSecurityGuard(private val context: Context) {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "turbo_x_credit_vault_v1"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private const val VAULT_VERSION: Byte = 1
        private const val CLOCK_BACKWARD_TOLERANCE_MS = 120_000L
        private const val MAX_STACKED_CREDIT_MS = 24L * 3_600_000L
    }

    @Volatile
    private var cachedSignatureSha256: String? = null

    @Volatile
    var lastClockTamperDetected: Boolean = false
        private set

    fun isRobolectric(): Boolean =
        Build.FINGERPRINT.equals("robolectric", ignoreCase = true)

    fun runSecurityAudit(): SecurityAuditReport {
        if (isRobolectric()) {
            return SecurityAuditReport(
                isSafeForGameplay = true,
                packageIntegrityOk = true,
                signatureSha256 = getSigningCertificateSha256(),
                vaultKeystoreBacked = false
            )
        }

        val reasons = mutableListOf<String>()
        val pkgOk = context.packageName == BuildConfig.APPLICATION_ID
        if (!pkgOk) {
            reasons.add("Package ID mismatch (repackaged APK)")
        }

        val sigHash = getSigningCertificateSha256()
        if (sigHash.isBlank()) {
            reasons.add("Missing APK signing certificate")
        }

        val isDebugBuild = BuildConfig.DEBUG
        val manifestDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val repackagedDebuggable = !isDebugBuild && manifestDebuggable
        if (repackagedDebuggable) {
            reasons.add("Release APK modified to enable debugging")
        }

        val debuggerAttached = !isDebugBuild && (Debug.isDebuggerConnected() || Debug.waitingForDebugger() || readTracerPid() > 0)
        if (debuggerAttached) {
            reasons.add("Active debugger / ptrace detected")
        }

        val hooking = detectHookingFrameworks()
        if (hooking) {
            reasons.add("Runtime hooking framework (Frida/Xposed) detected")
        }

        val rooted = detectRootIndicators()

        val keystoreOk = runCatching { getOrCreateVaultKey() != null }.getOrDefault(false)

        val safe = pkgOk &&
            sigHash.isNotBlank() &&
            !repackagedDebuggable &&
            !debuggerAttached &&
            !hooking

        return SecurityAuditReport(
            isSafeForGameplay = safe,
            packageIntegrityOk = pkgOk,
            signatureSha256 = sigHash,
            debuggerDetected = debuggerAttached,
            repackagedDebuggable = repackagedDebuggable,
            hookingDetected = hooking,
            rootIndicatorsFound = rooted,
            vaultKeystoreBacked = keystoreOk,
            clockTamperDetected = lastClockTamperDetected,
            reasons = reasons
        )
    }

    fun getSigningCertificateSha256(): String {
        cachedSignatureSha256?.let { return it }
        val hash = runCatching {
            val pm = context.packageManager
            val certBytes: ByteArray? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = info.signingInfo
                val signers = if (signingInfo?.hasMultipleSigners() == true) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo?.signingCertificateHistory
                }
                signers?.firstOrNull()?.toByteArray()
            } else {
                @Suppress("DEPRECATION")
                val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                info.signatures?.firstOrNull()?.toByteArray()
            }
            if (certBytes == null) {
                "UNSIGNED"
            } else {
                val digest = MessageDigest.getInstance("SHA-256").digest(certBytes)
                digest.joinToString("") { "%02x".format(it) }
            }
        }.getOrDefault("UNKNOWN")
        cachedSignatureSha256 = hash
        return hash
    }

    private fun readTracerPid(): Int {
        return runCatching {
            File("/proc/self/status").useLines { lines ->
                lines.firstOrNull { it.startsWith("TracerPid:") }
                    ?.substringAfter("TracerPid:")
                    ?.trim()
                    ?.toIntOrNull()
                    ?: 0
            }
        }.getOrDefault(0)
    }

    private fun detectHookingFrameworks(): Boolean {
        val suspiciousMaps = runCatching {
            File("/proc/self/maps").useLines { lines ->
                lines.any { line ->
                    val lower = line.lowercase()
                    lower.contains("frida") ||
                        lower.contains("xposed") ||
                        lower.contains("lsposed") ||
                        lower.contains("edxposed") ||
                        lower.contains("substrate")
                }
            }
        }.getOrDefault(false)
        if (suspiciousMaps) return true

        val hookClasses = listOf(
            "de.robv.android.xposed.XposedBridge",
            "io.github.lsposed.lspd.core.Main",
            "com.saurik.substrate.MS"
        )
        return hookClasses.any { className ->
            runCatching {
                Class.forName(className, false, context.classLoader)
                true
            }.getOrDefault(false)
        }
    }

    private fun detectRootIndicators(): Boolean {
        if (Build.TAGS?.contains("test-keys") == true) return true
        val suPaths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/su/bin/su",
            "/system/app/Superuser.apk"
        )
        return suPaths.any { runCatching { File(it).exists() }.getOrDefault(false) }
    }

    private fun buildAad(): ByteArray {
        val sig = getSigningCertificateSha256()
        return "${context.packageName}|$sig".toByteArray(Charsets.UTF_8)
    }

    private fun getOrCreateVaultKey(): SecretKey? {
        if (isRobolectric()) {
            return deriveFallbackTestKey()
        }
        return runCatching {
            val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            val existing = ks.getKey(KEY_ALIAS, null) as? SecretKey
            if (existing != null) return@runCatching existing

            val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            kg.init(spec)
            kg.generateKey()
        }.getOrElse {
            deriveFallbackTestKey()
        }
    }

    private fun deriveFallbackTestKey(): SecretKey {
        val material = "${context.packageName}:${getSigningCertificateSha256()}:turbox-vault"
        val hash = MessageDigest.getInstance("SHA-256").digest(material.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(hash, "AES")
    }

    fun sealTurboCredit(
        expiresAtWallMs: Long,
        adsWatchedCount: Int
    ): String? {
        val key = getOrCreateVaultKey() ?: return null
        val wallNow = System.currentTimeMillis()
        val elapsedNow = SystemClock.elapsedRealtime()
        val clampedWallExpiry = expiresAtWallMs.coerceAtMost(wallNow + MAX_STACKED_CREDIT_MS)
        val remainingMs = (clampedWallExpiry - wallNow).coerceAtLeast(0L)
        val expiresAtElapsedMs = if (remainingMs > 0L) elapsedNow + remainingMs else 0L

        val buffer = ByteBuffer.allocate(1 + 8 + 8 + 4 + 8 + 8)
            .put(VAULT_VERSION)
            .putLong(clampedWallExpiry)
            .putLong(expiresAtElapsedMs)
            .putInt(adsWatchedCount.coerceAtLeast(0))
            .putLong(wallNow)
            .putLong(elapsedNow)

        return runCatching {
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            cipher.updateAAD(buildAad())
            val iv = cipher.iv
            val encrypted = cipher.doFinal(buffer.array())
            val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val dataB64 = Base64.encodeToString(encrypted, Base64.NO_WRAP)
            "$ivB64.$dataB64"
        }.getOrNull()
    }

    fun unsealTurboCredit(token: String?): SealedTurboCredit {
        if (token.isNullOrBlank()) {
            return SealedTurboCredit(expiresAtWallMs = 0L, adsWatchedCount = 0, effectiveRemainingSeconds = 0L)
        }
        val parts = token.split('.')
        if (parts.size != 2) {
            return SealedTurboCredit(expiresAtWallMs = 0L, adsWatchedCount = 0, effectiveRemainingSeconds = 0L, tampered = true)
        }
        val key = getOrCreateVaultKey()
            ?: return SealedTurboCredit(expiresAtWallMs = 0L, adsWatchedCount = 0, effectiveRemainingSeconds = 0L, tampered = true)

        return runCatching {
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.updateAAD(buildAad())
            val plain = cipher.doFinal(encrypted)
            val buf = ByteBuffer.wrap(plain)
            val version = buf.get()
            if (version != VAULT_VERSION) {
                return@runCatching SealedTurboCredit(0L, 0, 0L, tampered = true)
            }
            val storedWallExpiry = buf.long
            val storedElapsedExpiry = buf.long
            val adsCount = buf.int
            val lastWallMs = buf.long
            val lastElapsedMs = buf.long

            val wallNow = System.currentTimeMillis()
            val elapsedNow = SystemClock.elapsedRealtime()

            // Detect system clock rollback tampering within the same boot session or extreme backward jump
            val sameBoot = elapsedNow >= lastElapsedMs
            val clockRolledBack = (sameBoot && wallNow + CLOCK_BACKWARD_TOLERANCE_MS < lastWallMs) ||
                (!sameBoot && wallNow + 3_600_000L < lastWallMs)

            if (clockRolledBack) {
                lastClockTamperDetected = true
                // Rely strictly on monotonic elapsedRealtime when clock was rolled back
                val monotonicRemMs = if (sameBoot && storedElapsedExpiry > elapsedNow) {
                    storedElapsedExpiry - elapsedNow
                } else {
                    0L
                }
                val safeRemSec = if (monotonicRemMs <= 0L) 0L else (monotonicRemMs + 999L) / 1000L
                return@runCatching SealedTurboCredit(
                    expiresAtWallMs = wallNow + monotonicRemMs,
                    adsWatchedCount = adsCount,
                    effectiveRemainingSeconds = safeRemSec,
                    tampered = false,
                    clockTamperDetected = true
                )
            }

            lastClockTamperDetected = false
            val wallRemMs = (storedWallExpiry - wallNow).coerceIn(0L, MAX_STACKED_CREDIT_MS)
            val remSec = if (wallRemMs <= 0L) 0L else (wallRemMs + 999L) / 1000L
            SealedTurboCredit(
                expiresAtWallMs = storedWallExpiry,
                adsWatchedCount = adsCount,
                effectiveRemainingSeconds = remSec
            )
        }.getOrElse {
            SealedTurboCredit(expiresAtWallMs = 0L, adsWatchedCount = 0, effectiveRemainingSeconds = 0L, tampered = true)
        }
    }
}

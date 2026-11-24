package com.example.extreme

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.data.GameTurboPreferencesRepository
import com.example.data.GameTurboSettings
import com.example.data.InstalledAppInfo

class SelectedGameManager(
    private val context: Context,
    private val repo: GameTurboPreferencesRepository,
    private val installedChecker: (String) -> Boolean = { pkg ->
        defaultIsPackageInstalled(context.packageManager, pkg)
    }
) {
    companion object {
        fun defaultIsPackageInstalled(pm: PackageManager, packageName: String): Boolean {
            if (packageName.isBlank()) return false
            return runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(packageName, 0)
                }
                true
            }.getOrDefault(false)
        }
    }

    fun buildAvailableGames(
        settings: GameTurboSettings,
        installedApps: List<InstalledAppInfo>
    ): List<SelectedGameInfo> {
        val byPkg = installedApps.associateBy { it.packageName }
        val selectedPkg = settings.superPerfSelectedGamePackage
            ?.takeIf { it in settings.addedGamePackages }

        return settings.addedGamePackages
            .map { pkg ->
                val known = byPkg[pkg]
                val installed = known != null || installedChecker(pkg)
                SelectedGameInfo(
                    packageName = pkg,
                    appName = known?.appName ?: resolveLabelFallback(pkg),
                    versionName = known?.versionName ?: "1.0",
                    isInstalled = installed,
                    isSelected = pkg == selectedPkg
                )
            }
            .sortedWith(compareByDescending<SelectedGameInfo> { it.isSelected }.thenBy { it.appName.lowercase() })
    }

    fun resolveSelectedGame(
        settings: GameTurboSettings,
        installedApps: List<InstalledAppInfo>
    ): SelectedGameInfo? {
        val selectedPkg = settings.superPerfSelectedGamePackage
            ?.takeIf { it in settings.addedGamePackages }
            ?: return null
        val known = installedApps.firstOrNull { it.packageName == selectedPkg }
        val installed = known != null || installedChecker(selectedPkg)
        return SelectedGameInfo(
            packageName = selectedPkg,
            appName = known?.appName ?: resolveLabelFallback(selectedPkg),
            versionName = known?.versionName ?: "1.0",
            isInstalled = installed,
            isSelected = true
        )
    }

    suspend fun selectSingleGame(
        packageName: String?,
        beforeSwitch: suspend (previousPkg: String?, newPkg: String?) -> Unit
    ) {
        var previousPkg: String? = null
        var allowed = true
        repo.updateSettings { current ->
            previousPkg = current.superPerfSelectedGamePackage
            if (packageName != null && packageName !in current.addedGamePackages) {
                allowed = false
                current
            } else {
                current
            }
        }
        if (!allowed) return
        if (previousPkg != packageName) {
            beforeSwitch(previousPkg, packageName)
        }
        repo.updateSettings { current ->
            val validated = packageName?.takeIf { it in current.addedGamePackages }
            current.copy(
                superPerfSelectedGamePackage = validated,
                selectedGamePackage = validated ?: current.selectedGamePackage
            )
        }
    }

    fun canActivateFor(selectedGame: SelectedGameInfo?): Boolean {
        return selectedGame != null && selectedGame.isInstalled
    }

    private fun resolveLabelFallback(packageName: String): String {
        return runCatching {
            val pm = context.packageManager
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName)
    }
}

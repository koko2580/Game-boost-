package com.example.extreme

import com.example.data.GameTurboPreferencesRepository
import com.example.data.GameTurboSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.util.UUID

class RewardedAdUnlockManager(
    private val repo: GameTurboPreferencesRepository,
    private val clockMs: () -> Long = { System.currentTimeMillis() }
) {
    private val _state = MutableStateFlow(RewardedUnlockState())
    val state: StateFlow<RewardedUnlockState> = _state.asStateFlow()

    @Volatile
    private var pendingAdToken: String? = null

    @Volatile
    private var pendingAdStep: Int = 0

    private val consumedTokens = LinkedHashSet<String>()

    fun syncFromSettings(settings: GameTurboSettings): RewardedUnlockState {
        val currentMsg = _state.value.lastAdMessage
        val synced = RewardedUnlockState(
            ad1Watched = settings.superPerfAd1Watched,
            ad2Watched = settings.superPerfAd1Watched && settings.superPerfAd2Watched,
            isUnlocked = settings.superPerfAd1Watched && settings.superPerfAd2Watched && settings.superPerfUnlocked,
            unlockedAtWallMs = settings.superPerfUnlockedAtMs,
            lastAdMessage = currentMsg
        )
        _state.value = synced
        return synced
    }

    suspend fun reloadPersistedState(): RewardedUnlockState {
        val settings = repo.settingsFlow.first()
        return syncFromSettings(settings)
    }

    fun beginAdAttempt(step: Int): String? {
        val current = _state.value
        if (current.isUnlocked) return null
        val expectedStep = current.nextRequiredStep
        if (step != expectedStep) return null
        val token = "ad_${step}_${UUID.randomUUID()}"
        pendingAdToken = token
        pendingAdStep = step
        return token
    }

    suspend fun verifyAdRewardEarned(step: Int, sessionToken: String): UnlockStepResult {
        if (sessionToken.isBlank() || sessionToken != pendingAdToken || step != pendingAdStep) {
            val cur = _state.value.copy(lastAdMessage = "Invalid or expired ad verification token")
            _state.value = cur
            return UnlockStepResult.Rejected("Invalid or expired ad token", cur)
        }
        if (sessionToken in consumedTokens) {
            val cur = _state.value.copy(lastAdMessage = "Duplicate ad reward blocked — watch next required ad")
            _state.value = cur
            return UnlockStepResult.Rejected("Duplicate reward prevented", cur)
        }

        consumedTokens.add(sessionToken)
        pendingAdToken = null
        pendingAdStep = 0

        val now = clockMs()
        var updatedState = _state.value

        if (step == 1) {
            repo.updateSettings { s ->
                s.copy(
                    superPerfAd1Watched = true,
                    superPerfAd2Watched = s.superPerfAd2Watched && s.superPerfUnlocked,
                    superPerfUnlocked = s.superPerfAd2Watched && s.superPerfUnlocked
                )
            }
            updatedState = RewardedUnlockState(
                ad1Watched = true,
                ad2Watched = false,
                isUnlocked = false,
                unlockedAtWallMs = 0L,
                lastAdMessage = "Ad 1 of 2 verified! Watch 1 more rewarded ad to unlock Super High Performance."
            )
            _state.value = updatedState
            return UnlockStepResult.Step1Completed(updatedState)
        }

        val currentSettings = repo.settingsFlow.first()
        if (!currentSettings.superPerfAd1Watched) {
            val rejected = RewardedUnlockState(
                ad1Watched = false,
                ad2Watched = false,
                isUnlocked = false,
                unlockedAtWallMs = 0L,
                lastAdMessage = "Complete Ad 1 of 2 before Ad 2."
            )
            _state.value = rejected
            return UnlockStepResult.Rejected("Ad 1 must be verified first", rejected)
        }

        repo.updateSettings { s ->
            s.copy(
                superPerfAd1Watched = true,
                superPerfAd2Watched = true,
                superPerfUnlocked = true,
                superPerfUnlockedAtMs = now
            )
        }
        updatedState = RewardedUnlockState(
            ad1Watched = true,
            ad2Watched = true,
            isUnlocked = true,
            unlockedAtWallMs = now,
            lastAdMessage = "Ad 2 of 2 verified! Super High Performance Mode UNLOCKED."
        )
        _state.value = updatedState
        return UnlockStepResult.Unlocked(updatedState)
    }

    fun recordAdSkippedOrFailed(reason: String) {
        pendingAdToken = null
        pendingAdStep = 0
        _state.value = _state.value.copy(lastAdMessage = reason)
    }

    suspend fun resetUnlockForRelock() {
        pendingAdToken = null
        pendingAdStep = 0
        consumedTokens.clear()
        repo.updateSettings { s ->
            s.copy(
                superPerfAd1Watched = false,
                superPerfAd2Watched = false,
                superPerfUnlocked = false,
                superPerfUnlockedAtMs = 0L
            )
        }
        _state.value = RewardedUnlockState(
            ad1Watched = false,
            ad2Watched = false,
            isUnlocked = false,
            unlockedAtWallMs = 0L,
            lastAdMessage = "Super High Performance Mode locked — 2 rewarded ads required."
        )
    }
}

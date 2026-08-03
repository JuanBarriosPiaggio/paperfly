package com.paperfly.paperplanedrift.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "paper_plane_prefs")

data class Progress(
    val highScore: Int = 0,
    val bestCleanStreak: Int = 0,
    val dailyBest: Int = 0,
    val dailyDate: String = "",
    val unlockedSkins: Set<String> = setOf(SkinRepository.DEFAULT_SKIN_ID),
    val equippedSkin: String = SkinRepository.DEFAULT_SKIN_ID,
    val adsRemoved: Boolean = false,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
)

class ProgressRepository(private val context: Context) {

    private object Keys {
        val HIGH_SCORE = intPreferencesKey("high_score")
        val BEST_STREAK = intPreferencesKey("best_clean_streak")
        val DAILY_BEST = intPreferencesKey("daily_best")
        val DAILY_DATE = stringPreferencesKey("daily_date")
        val UNLOCKED_SKINS = stringSetPreferencesKey("unlocked_skins")
        val EQUIPPED_SKIN = stringPreferencesKey("equipped_skin")
        val ADS_REMOVED = booleanPreferencesKey("ads_removed")
        val SOUND = booleanPreferencesKey("sound_enabled")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
    }

    val progress: Flow<Progress> = context.dataStore.data.map { prefs ->
        Progress(
            highScore = prefs[Keys.HIGH_SCORE] ?: 0,
            bestCleanStreak = prefs[Keys.BEST_STREAK] ?: 0,
            dailyBest = prefs[Keys.DAILY_BEST] ?: 0,
            dailyDate = prefs[Keys.DAILY_DATE] ?: "",
            unlockedSkins = (prefs[Keys.UNLOCKED_SKINS] ?: emptySet()) + SkinRepository.DEFAULT_SKIN_ID,
            equippedSkin = prefs[Keys.EQUIPPED_SKIN] ?: SkinRepository.DEFAULT_SKIN_ID,
            adsRemoved = prefs[Keys.ADS_REMOVED] ?: false,
            soundEnabled = prefs[Keys.SOUND] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS] ?: true,
        )
    }

    /** Records the results of a finished run, keeping bests. */
    suspend fun submitRun(score: Int, cleanStreak: Int, dailyMode: Boolean, dateKey: String) {
        context.dataStore.edit { prefs ->
            if (score > (prefs[Keys.HIGH_SCORE] ?: 0)) prefs[Keys.HIGH_SCORE] = score
            if (cleanStreak > (prefs[Keys.BEST_STREAK] ?: 0)) prefs[Keys.BEST_STREAK] = cleanStreak
            if (dailyMode) {
                val sameDay = prefs[Keys.DAILY_DATE] == dateKey
                val dailyBest = if (sameDay) prefs[Keys.DAILY_BEST] ?: 0 else 0
                if (score > dailyBest) prefs[Keys.DAILY_BEST] = score
                prefs[Keys.DAILY_DATE] = dateKey
            }
        }
    }

    suspend fun unlockSkin(id: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.UNLOCKED_SKINS] = (prefs[Keys.UNLOCKED_SKINS] ?: emptySet()) + id
        }
    }

    suspend fun equipSkin(id: String) {
        context.dataStore.edit { it[Keys.EQUIPPED_SKIN] = id }
    }

    suspend fun setAdsRemoved(removed: Boolean) {
        context.dataStore.edit { it[Keys.ADS_REMOVED] = removed }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS] = enabled }
    }
}

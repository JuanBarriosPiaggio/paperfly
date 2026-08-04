package com.paperfly.paperplanedrift.games

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk
import java.lang.ref.WeakReference

/**
 * Play Games Services v2 wrapper: automatic sign-in, leaderboards, achievements.
 *
 * All IDs below are PLACEHOLDERS. Before release:
 * 1. Enable Play Games Services for the app in Play Console (Grow > Play Games Services).
 * 2. Create the two leaderboards and the achievements, then paste their IDs here.
 * 3. Put the Games project ID in res/values/strings.xml (game_services_project_id).
 *
 * Every call fails soft: without a Play Games config the game works normally,
 * scores just aren't reported. PGS v2 signs the player in automatically at
 * startup when the app is properly configured.
 */
class PlayGamesManager(context: Context) {

    object Ids {
        // Leaderboards
        const val LEADERBOARD_HIGH_SCORE = "REPLACE_leaderboard_high_score"
        const val LEADERBOARD_DAILY = "REPLACE_leaderboard_daily_challenge"

        // Achievements
        const val ACH_FIRST_FLIGHT = "REPLACE_ach_first_flight"
        const val ACH_SCORE_100 = "REPLACE_ach_score_100"
        const val ACH_SCORE_250 = "REPLACE_ach_score_250"
        const val ACH_SCORE_500 = "REPLACE_ach_score_500"
        const val ACH_SCORE_1000 = "REPLACE_ach_score_1000"
        const val ACH_CLEAN_STREAK_3 = "REPLACE_ach_clean_streak_3"
        const val ACH_CLEAN_STREAK_5 = "REPLACE_ach_clean_streak_5"
        const val ACH_DAILY_CHALLENGE = "REPLACE_ach_daily_challenge"
        const val ACH_EARLY_CRUMPLE = "REPLACE_ach_early_crumple"
    }

    private var activityRef = WeakReference<Activity>(null)

    @Volatile
    var authenticated: Boolean = false
        private set

    init {
        runCatching { PlayGamesSdk.initialize(context) }
            .onFailure { Log.w(TAG, "PGS init failed (missing Play Console config?)", it) }
    }

    /** Call from the activity's onCreate; kicks off the silent sign-in check. */
    fun attach(activity: Activity) {
        activityRef = WeakReference(activity)
        runCatching {
            PlayGames.getGamesSignInClient(activity).isAuthenticated
                .addOnCompleteListener { task ->
                    authenticated = task.isSuccessful && task.result?.isAuthenticated == true
                }
        }
    }

    fun submitScore(leaderboardId: String, score: Long) {
        val activity = activityRef.get() ?: return
        if (!authenticated) return
        runCatching {
            PlayGames.getLeaderboardsClient(activity).submitScore(leaderboardId, score)
        }
    }

    fun unlockAchievement(achievementId: String) {
        val activity = activityRef.get() ?: return
        if (!authenticated) return
        runCatching {
            PlayGames.getAchievementsClient(activity).unlock(achievementId)
        }
    }

    fun showLeaderboards(activity: Activity) = withSignIn(activity) {
        PlayGames.getLeaderboardsClient(activity)
            .allLeaderboardsIntent
            .addOnSuccessListener { activity.startActivityForResult(it, RC_LEADERBOARD) }
    }

    fun showAchievements(activity: Activity) = withSignIn(activity) {
        PlayGames.getAchievementsClient(activity)
            .achievementsIntent
            .addOnSuccessListener { activity.startActivityForResult(it, RC_ACHIEVEMENTS) }
    }

    /** Runs [action] if signed in; otherwise triggers the sign-in flow first. */
    private fun withSignIn(activity: Activity, action: () -> Unit) {
        runCatching {
            if (authenticated) {
                action()
            } else {
                PlayGames.getGamesSignInClient(activity).signIn()
                    .addOnCompleteListener { task ->
                        authenticated = task.isSuccessful && task.result?.isAuthenticated == true
                        if (authenticated) action()
                        else Log.w(TAG, "Play Games sign-in unavailable")
                    }
            }
        }.onFailure { Log.w(TAG, "Play Games call failed", it) }
    }

    private companion object {
        const val TAG = "PlayGamesManager"
        const val RC_LEADERBOARD = 9101
        const val RC_ACHIEVEMENTS = 9102
    }
}

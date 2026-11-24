package com.nihongo.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nihongo.app.domain.model.TextSize
import com.nihongo.app.domain.model.ThemeMode
import com.nihongo.app.domain.model.UserPreferences
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(context: Context) {

    private val store = context.applicationContext.userPrefsDataStore

    val preferences: Flow<UserPreferences> = store.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            UserPreferences(
                themeMode = prefs[THEME_MODE].toEnumOrDefault(ThemeMode.SYSTEM),
                textSize = prefs[TEXT_SIZE].toEnumOrDefault(TextSize.NORMAL),
                dailyGoalMinutes = prefs[DAILY_GOAL] ?: DEFAULT_DAILY_GOAL,
                bookmarks = prefs[BOOKMARKS] ?: emptySet(),
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME_MODE] = mode.name }
    }

    suspend fun setTextSize(size: TextSize) {
        store.edit { it[TEXT_SIZE] = size.name }
    }

    suspend fun toggleBookmark(key: String) {
        store.edit { prefs ->
            val current = prefs[BOOKMARKS] ?: emptySet()
            prefs[BOOKMARKS] = if (key in current) current - key else current + key
        }
    }

    private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T =
        this?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default

    private companion object {
        const val DEFAULT_DAILY_GOAL = 20
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val TEXT_SIZE = stringPreferencesKey("text_size")
        val DAILY_GOAL = intPreferencesKey("daily_goal_minutes")
        val BOOKMARKS = stringSetPreferencesKey("bookmarks")
    }
}
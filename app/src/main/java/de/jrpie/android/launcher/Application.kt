package de.jrpie.android.launcher

import android.content.Context
import de.jrpie.android.launcher.preferences.LauncherPreferences

class Application : android.app.Application() {
    override fun onCreate() {
        super.onCreate()

        val preferences = getSharedPreferences(
            this.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE
        )
        LauncherPreferences.init(preferences, this.resources)
    }
}
package de.jrpie.android.launcher.actions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences.Editor
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.widget.Toast
import de.jrpie.android.launcher.R
import de.jrpie.android.launcher.apps.AppInfo
import de.jrpie.android.launcher.apps.AppInfo.Companion.INVALID_USER
import de.jrpie.android.launcher.apps.DetailedAppInfo
import de.jrpie.android.launcher.preferences.LauncherPreferences

interface Action {
    fun invoke(context: Context, rect: Rect? = null): Boolean
    fun bindToGesture(prefEditor: Editor, id: String)
    fun label(context: Context): String
    fun getIcon(context: Context): Drawable?
    fun isAvailable(context: Context): Boolean

    fun writeToIntent(intent: Intent)

    companion object {
        /**
         * Get an action for a specific id.
         * An id is of the form:
         *  - "launcher:${launcher_action_name}", see [LauncherAction]
         *  - "${package_name}", see [AppAction]
         *  - "${package_name}:${activity_name}", see  [AppAction]
         *
         *  @param id
         *  @param user a user id, ignored if the action is a [LauncherAction].
         *  @param context used to complete [AppInfo] if possible
         */
        private fun fromId(id: String, user: Int?, context: Context? = null): Action? {
            if (id.isEmpty()) {
                return null
            }
            if (LauncherAction.isOtherAction(id)) {
                return LauncherAction.byId(id)
            }

            val values =  id.split(";")

            var info = AppInfo(values[0], values.getOrNull(1), user ?: INVALID_USER)

            // try to complete an incomplete AppInfo if a context is provided
            if (context != null && (info.user == INVALID_USER || info.activityName == null)) {
                info = DetailedAppInfo.fromAppInfo(info, context)?.app?:info
            }

            return AppAction(info)
        }

        fun forGesture(gesture: Gesture): Action? {
            val id = gesture.id

            val preferences = LauncherPreferences.getSharedPreferences()
            val actionId = preferences.getString("$id.app", "")!!
            var u: Int? = preferences.getInt("$id.user", INVALID_USER)
            u = if (u == INVALID_USER) null else u

            return fromId(actionId, u)
        }

        fun resetToDefaultActions(context: Context) {
            val editor = LauncherPreferences.getSharedPreferences().edit()
            val boundActions = HashSet<String>()
            Gesture.entries.forEach { gesture ->
                context.resources
                    .getStringArray(gesture.defaultsResource)
                    .filterNot { boundActions.contains(it) }
                    .map { Pair(it, fromId(it, null, context)) }
                    .firstOrNull { it.second?.isAvailable(context) ?: false }
                    ?.apply {
                        boundActions.add(first)
                        second?.bindToGesture(editor, gesture.id)
                    }
            }
            editor.apply()
        }

        fun setActionForGesture(gesture: Gesture, action: Action?) {
            if (action == null) {
                clearActionForGesture(gesture)
                return
            }
            val editor = LauncherPreferences.getSharedPreferences().edit()
            action.bindToGesture(editor, gesture.id)
            editor.apply()
        }

        fun clearActionForGesture(gesture: Gesture) {
            LauncherPreferences.getSharedPreferences().edit()
                .putString(gesture.id + ".app", "")
                .putInt(gesture.id + ".user", INVALID_USER)
                .apply()
        }

        fun launch(
            action: Action?,
            context: Context,
            animationIn: Int = android.R.anim.fade_in,
            animationOut: Int = android.R.anim.fade_out
        ) {
            if (action != null && action.invoke(context)) {
                if (context is Activity) {
                    context.overridePendingTransition(animationIn, animationOut)
                }
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_cant_open_message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        fun fromIntent(data: Intent): Action? {
            val value = data.getStringExtra("action_id") ?: return null
            val user = data.getIntExtra("user", INVALID_USER)
            return fromId(value, user)
        }
    }
}
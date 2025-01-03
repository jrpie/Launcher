package de.jrpie.android.launcher

import android.app.Activity
import android.app.Service
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.util.Log
import de.jrpie.android.launcher.actions.Action
import de.jrpie.android.launcher.actions.Gesture
import de.jrpie.android.launcher.apps.AppInfo
import de.jrpie.android.launcher.apps.DetailedAppInfo
import de.jrpie.android.launcher.ui.tutorial.TutorialActivity


/* REQUEST CODES */

const val REQUEST_CHOOSE_APP = 1
const val REQUEST_UNINSTALL = 2

const val REQUEST_SET_DEFAULT_HOME = 42

const val LOG_TAG = "Launcher"

fun setDefaultHomeScreen(context: Context, checkDefault: Boolean = false) {

    if (checkDefault
        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        && context is Activity
    ) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
            context.startActivityForResult(
                roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME),
                REQUEST_SET_DEFAULT_HOME
            )
        }
        return
    }

    if (checkDefault) {
        val testIntent = Intent(Intent.ACTION_MAIN)
        testIntent.addCategory(Intent.CATEGORY_HOME)
        val defaultHome = testIntent.resolveActivity(context.packageManager)?.packageName
        if (defaultHome == context.packageName) {
            // Launcher is already the default home app
            return
        }
    }
    val intent = Intent(Settings.ACTION_HOME_SETTINGS)
    context.startActivity(intent)
}

fun getUserFromId(userId: Int?, context: Context): UserHandle {
    /* TODO: this is an ugly hack.
        Use userManager#getUserForSerialNumber instead (breaking change to SharedPreferences!)
     */
    val userManager = context.getSystemService(Service.USER_SERVICE) as UserManager
    val profiles = userManager.userProfiles
    return profiles.firstOrNull { it.hashCode() == userId } ?: profiles[0]
}

fun openInBrowser(url: String, context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    intent.putExtras(Bundle().apply { putBoolean("new_window", true) })
    context.startActivity(intent)
}

fun openTutorial(context: Context) {
    context.startActivity(Intent(context, TutorialActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}


/**
 * Load all apps.
 */
fun getApps(packageManager: PackageManager, context: Context): MutableList<DetailedAppInfo> {
    val start = System.currentTimeMillis()
    val loadList = mutableListOf<DetailedAppInfo>()

    val launcherApps = context.getSystemService(Service.LAUNCHER_APPS_SERVICE) as LauncherApps
    val userManager = context.getSystemService(Service.USER_SERVICE) as UserManager

    // TODO: shortcuts - launcherApps.getShortcuts()
    val users = userManager.userProfiles
    for (user in users) {
        launcherApps.getActivityList(null, user).forEach {
            loadList.add(DetailedAppInfo(it))
        }
    }

    // fallback option
    if (loadList.isEmpty()) {
        Log.w(LOG_TAG, "using fallback option to load packages")
        val i = Intent(Intent.ACTION_MAIN, null)
        i.addCategory(Intent.CATEGORY_LAUNCHER)
        val allApps = packageManager.queryIntentActivities(i, 0)
        for (ri in allApps) {
            val app = AppInfo(ri.activityInfo.packageName, null, AppInfo.INVALID_USER)
            val detailedAppInfo = DetailedAppInfo(
                app,
                ri.loadLabel(packageManager),
                ri.activityInfo.loadIcon(packageManager)
            )
            loadList.add(detailedAppInfo)
        }
    }
    loadList.sortBy { it.getCustomLabel(context).toString() }

    val end = System.currentTimeMillis()
    Log.i(LOG_TAG, "${loadList.size} apps loaded (${end - start}ms)")

    return loadList
}


// Used in Tutorial and Settings `ActivityOnResult`
fun saveListActivityChoice(data: Intent?) {
    val forGesture = data?.getStringExtra("forGesture") ?: return
    Gesture.byId(forGesture)?.let { Action.setActionForGesture(it, Action.fromIntent(data)) }
}


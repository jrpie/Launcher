package de.jrpie.android.launcher.actions

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.core.content.getSystemService
import de.jrpie.android.launcher.R

class LauncherAccessibilityService : AccessibilityService() {
    override fun onInterrupt() {
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    companion object {
        const val ACTION_LOCK_SCREEN = "ACTION_LOCK_SCREEN"
    }

    private fun isServiceEnabled(): Boolean {
        val accessibilityManager = getSystemService<AccessibilityManager>() ?: return false
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { it.id.contains(packageName) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            if (!isServiceEnabled()) {
                Toast.makeText(this, getString(R.string.toast_accessibility_service_not_enabled), Toast.LENGTH_LONG).show()
                startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return START_NOT_STICKY
            }

            when (action) {
                ACTION_LOCK_SCREEN -> handleLockScreen()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun handleLockScreen(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            Toast.makeText(this, getText(R.string.toast_lock_screen_not_supported), Toast.LENGTH_SHORT).show()
        }
    }
}
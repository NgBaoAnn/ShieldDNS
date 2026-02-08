package com.shielddns.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Broadcast receiver to auto-start VPN on device boot.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
        val AUTO_START_KEY = booleanPreferencesKey("auto_start_on_boot")
    }

    @Inject
    lateinit var vpnController: VpnServiceController

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        
        Log.d(TAG, "Boot completed, checking auto-start setting")
        
        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val preferences = context.dataStore.data.first()
                val autoStart = preferences[AUTO_START_KEY] ?: false
                
                if (autoStart) {
                    Log.d(TAG, "Auto-starting VPN service")
                    vpnController.startVpn()
                } else {
                    Log.d(TAG, "Auto-start disabled, not starting VPN")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking auto-start setting", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

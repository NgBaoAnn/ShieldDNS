package com.shielddns.app.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.shielddns.app.presentation.state.VpnConnectionState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controller for managing VPN service lifecycle.
 * Provides a clean API for UI to start/stop VPN.
 */
@Singleton
class VpnServiceController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val VPN_PERMISSION_REQUEST_CODE = 1001
    }

    /**
     * Current VPN connection state.
     */
    val connectionState: StateFlow<VpnConnectionState>
        get() = AdBlockVpnService.connectionState

    /**
     * Current blocked domains count.
     */
    val blockedCount: StateFlow<Long>
        get() = AdBlockVpnService.blockedCount

    /**
     * Check if VPN is currently connected.
     */
    val isConnected: Boolean
        get() = connectionState.value is VpnConnectionState.Connected

    /**
     * Prepare VPN - returns Intent if permission is needed, null if already granted.
     */
    fun prepare(): Intent? {
        return VpnService.prepare(context)
    }

    /**
     * Start the VPN service.
     * Call prepare() first and handle the permission request if needed.
     */
    fun startVpn() {
        val intent = Intent(context, AdBlockVpnService::class.java).apply {
            action = AdBlockVpnService.ACTION_START
        }
        context.startForegroundService(intent)
    }

    /**
     * Stop the VPN service.
     */
    fun stopVpn() {
        val intent = Intent(context, AdBlockVpnService::class.java).apply {
            action = AdBlockVpnService.ACTION_STOP
        }
        context.startService(intent)
    }

    /**
     * Toggle VPN state.
     * 
     * @param activity Activity for permission request if needed
     * @return true if operation started, false if permission dialog shown
     */
    fun toggle(activity: Activity): Boolean {
        return if (isConnected) {
            stopVpn()
            true
        } else {
            val prepareIntent = prepare()
            if (prepareIntent != null) {
                activity.startActivityForResult(prepareIntent, VPN_PERMISSION_REQUEST_CODE)
                false
            } else {
                startVpn()
                true
            }
        }
    }

    /**
     * Handle VPN permission result.
     */
    fun onPermissionResult(resultCode: Int) {
        if (resultCode == Activity.RESULT_OK) {
            startVpn()
        }
    }
}

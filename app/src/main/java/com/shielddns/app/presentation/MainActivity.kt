package com.shielddns.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.shielddns.app.presentation.screen.home.HomeScreen
import com.shielddns.app.presentation.theme.ShieldDnsTheme
import com.shielddns.app.service.VpnServiceController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main Activity for ShieldDNS.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var vpnController: VpnServiceController

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        vpnController.onPermissionResult(result.resultCode)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            ShieldDnsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(
                        onVpnToggle = { shouldEnable ->
                            if (shouldEnable) {
                                val prepareIntent = vpnController.prepare()
                                if (prepareIntent != null) {
                                    vpnPermissionLauncher.launch(prepareIntent)
                                } else {
                                    vpnController.startVpn()
                                }
                            } else {
                                vpnController.stopVpn()
                            }
                        }
                    )
                }
            }
        }
    }
}

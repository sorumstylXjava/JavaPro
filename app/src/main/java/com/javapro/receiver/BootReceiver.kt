package com.javapro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.javapro.utils.PreferenceManager
import com.javapro.utils.TweakExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefManager = PreferenceManager(context)
            val scope = CoroutineScope(Dispatchers.IO)

            scope.launch {
                val isEnabled = prefManager.bootApplyFlow.first()
                if (isEnabled) {
                    TweakExecutor.execute("echo performance > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
                    TweakExecutor.execute("echo 1 > /sys/class/kgsl/kgsl-3d0/force_clk_on")
                    TweakExecutor.execute("settings put system peak_refresh_rate 120")
                }
            }
        }
    }
}

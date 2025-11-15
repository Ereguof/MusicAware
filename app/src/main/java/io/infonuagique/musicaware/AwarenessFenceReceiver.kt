package io.infonuagique.musicaware

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.awareness.fence.FenceState

class AwarenessFenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val fenceState = FenceState.extract(intent)
        val status = when (fenceState.currentState) {
            FenceState.TRUE -> "Fence ${fenceState.fenceKey} : ACTIF"
            FenceState.FALSE -> "Fence ${fenceState.fenceKey} : INACTIF"
            FenceState.UNKNOWN -> "Fence ${fenceState.fenceKey} : INCONNU"
            else -> "Fence ${fenceState.fenceKey} : ÉTAT INCONNU"
        }
        android.util.Log.d("FENCE", "onReceive: état = ${fenceState.currentState}")
        val updateIntent = Intent("io.infonuagique.musicaware.AWARENESS_FENCE_STATUS")
        updateIntent.putExtra("FENCE_STATUS", status)
        context.sendBroadcast(updateIntent)
    }
}

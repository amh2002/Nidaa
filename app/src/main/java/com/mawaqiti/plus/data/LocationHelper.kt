package com.mawaqiti.plus.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Small LocationManager wrapper that avoids Google Play Services entirely.
 * It prefers a recent cached fix and otherwise requests one update.
 */
object LocationHelper {

    private const val CACHE_MAX_AGE_MS = 15 * 60 * 1000L
    private const val REQUEST_TIMEOUT_MS = 12_000L

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun currentLocation(context: Context): Location? {
        if (!hasPermission(context)) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        ).filter { provider ->
            runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
        }
        if (providers.isEmpty()) return null

        val cached = providers.mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time }

        if (cached != null && System.currentTimeMillis() - cached.time <= CACHE_MAX_AGE_MS) {
            return cached
        }

        return withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (continuation.isActive) {
                            manager.removeUpdates(this)
                            continuation.resume(location)
                        }
                    }

                    @Deprecated("Deprecated in API 29, still delivered on older devices")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

                    override fun onProviderEnabled(provider: String) = Unit
                    override fun onProviderDisabled(provider: String) = Unit
                }

                continuation.invokeOnCancellation {
                    runCatching { manager.removeUpdates(listener) }
                }

                try {
                    @Suppress("DEPRECATION")
                    manager.requestSingleUpdate(providers.first(), listener, Looper.getMainLooper())
                } catch (error: SecurityException) {
                    if (continuation.isActive) continuation.resume(null)
                } catch (error: IllegalArgumentException) {
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        } ?: cached
    }
}

package com.contoh.harinspeed

import android.location.Location
import kotlin.math.max

/**
 * Keadaan perjalanan saat ini. Hanya diubah dari thread utama
 * (oleh GpsService lewat LocationListener dan GnssStatus).
 */
object Perjalanan {
    var jalan = false
    var kecepatanKmh = 0f
    var maksKmh = 0f
    var jarakM = 0.0
    var waktuMs = 0L
    var lat: Double? = null
    var lng: Double? = null
    var altitudeM: Double? = null
    var akurasiM: Float? = null
    var arah: Float? = null
    var satelitDipakai = 0
    var satelitTerlihat = 0

    private var lokasiTerakhir: Location? = null
    private var kecepatanHalus = 0f

    /** Dipanggil saat START atau setelah jeda. Titik berikutnya dianggap awal baru. */
    fun mulaiSesi() {
        lokasiTerakhir = null
        kecepatanHalus = 0f
        kecepatanKmh = 0f
        jalan = true
    }

    fun hentikan() {
        jalan = false
        kecepatanKmh = 0f
        kecepatanHalus = 0f
        lokasiTerakhir = null
    }

    fun reset() {
        jarakM = 0.0
        waktuMs = 0L
        maksKmh = 0f
        kecepatanKmh = 0f
        kecepatanHalus = 0f
        lokasiTerakhir = null
    }

    fun terimaLokasi(loc: Location) {
        lat = loc.latitude
        lng = loc.longitude
        akurasiM = loc.accuracy
        if (loc.hasAltitude()) altitudeM = loc.altitude
        if (loc.hasBearing()) arah = loc.bearing
        if (!jalan) return
        if (loc.accuracy > 35f) return // sinyal terlalu lemah, titik diabaikan

        val prev = lokasiTerakhir
        val dtMs = if (prev != null) (loc.time - prev.time).coerceIn(0L, 5000L) else 0L
        val kmhGps = if (loc.hasSpeed() && loc.speed >= 0f) loc.speed * 3.6f else null
        var kmh = kmhGps ?: 0f

        if (prev != null) {
            val d = prev.distanceTo(loc).toDouble()
            if (kmhGps == null && dtMs > 0) kmh = (d / (dtMs / 1000.0) * 3.6).toFloat()
            waktuMs += dtMs
            val ambangJarak = max(3.0, loc.accuracy * 0.5)
            if (d >= ambangJarak && kmh >= 1f) jarakM += d
        }

        if (kmh < 1f) kmh = 0f
        kecepatanHalus = kecepatanHalus * 0.4f + kmh * 0.6f
        kecepatanKmh = if (kecepatanHalus < 1f) 0f else kecepatanHalus
        if (kecepatanKmh > maksKmh) maksKmh = kecepatanKmh
        lokasiTerakhir = loc
    }

    fun rataKmh(): Float {
        if (waktuMs <= 0L) return 0f
        return ((jarakM / 1000.0) / (waktuMs / 3_600_000.0)).toFloat()
    }

    fun formatWaktu(): String {
        val d = waktuMs / 1000L
        return "%02d:%02d:%02d".format(d / 3600, (d % 3600) / 60, d % 60)
    }
}

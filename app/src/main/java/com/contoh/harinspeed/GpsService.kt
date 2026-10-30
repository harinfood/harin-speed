package com.contoh.harinspeed

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import kotlin.math.roundToInt

/** Layanan latar depan yang merekam GPS selama perjalanan berjalan. */
class GpsService : Service() {

    companion object {
        private const val ID = 2001
        private const val KANAL = "perjalanan"

        fun mulai(c: Context) {
            c.startForegroundService(Intent(c, GpsService::class.java))
        }

        fun henti(c: Context) {
            c.stopService(Intent(c, GpsService::class.java))
        }
    }

    private var lm: LocationManager? = null
    private val handler = Handler(Looper.getMainLooper())

    private val pendengarLokasi = object : LocationListener {
        override fun onLocationChanged(loc: Location) {
            Perjalanan.terimaLokasi(loc)
            getSystemService(NotificationManager::class.java).notify(ID, bangunNotifikasi())
        }
    }

    private val statusSatelit = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            var dipakai = 0
            for (i in 0 until status.satelliteCount) {
                if (status.usedInFix(i)) dipakai++
            }
            Perjalanan.satelitDipakai = dipakai
            Perjalanan.satelitTerlihat = status.satelliteCount
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        buatKanal()
        val notif = bangunNotifikasi()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(ID, notif)
        }
        Perjalanan.mulaiSesi()
        mulaiLokasi()
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun mulaiLokasi() {
        val m = getSystemService(LocationManager::class.java)
        lm = m
        try {
            m.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, pendengarLokasi, Looper.getMainLooper())
            m.registerGnssStatusCallback(statusSatelit, handler)
        } catch (e: SecurityException) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        try {
            lm?.removeUpdates(pendengarLokasi)
            lm?.unregisterGnssStatusCallback(statusSatelit)
        } catch (e: Exception) {
            // sudah dilepas
        }
        Perjalanan.hentikan()
        Perjalanan.satelitDipakai = 0
        Perjalanan.satelitTerlihat = 0
        super.onDestroy()
    }

    private fun bangunNotifikasi(): Notification {
        val pi = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val isi = "${Perjalanan.kecepatanKmh.roundToInt()} km/h · ${"%.2f".format(Perjalanan.jarakM / 1000.0)} km"
        return Notification.Builder(this, KANAL)
            .setSmallIcon(R.drawable.ic_speed)
            .setContentTitle("Harin Speed merekam perjalanan")
            .setContentText(isi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pi)
            .build()
    }

    private fun buatKanal() {
        val nm = getSystemService(NotificationManager::class.java)
        val kanal = NotificationChannel(KANAL, "Perjalanan GPS", NotificationManager.IMPORTANCE_LOW)
        kanal.description = "Rekaman kecepatan dan jarak saat perjalanan"
        nm.createNotificationChannel(kanal)
    }
}

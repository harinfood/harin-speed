package com.contoh.harinspeed

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt

/** Warna tema Harin Speed. */
object Tema {
    val latarAtas = Color.parseColor("#0B1016")
    val latarBawah = Color.parseColor("#161E28")
    val kartu = Color.parseColor("#18212B")
    val garisKartu = Color.parseColor("#2A3440")
    val merah = Color.parseColor("#FF1744")
    val oranye = Color.parseColor("#FF6D00")
    val kuning = Color.parseColor("#FFD600")
    val hijau = Color.parseColor("#00E676")
    val biru = Color.parseColor("#2979FF")
    val teks = Color.WHITE
    val teksRedup = Color.parseColor("#8A96A3")
    val abu = Color.parseColor("#2A3440")
}

class MainActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var speedo: SpeedometerView
    private lateinit var chipGps: TextView
    private lateinit var chipSatelit: TextView
    private lateinit var chipAkurasi: TextView
    private lateinit var nilaiTrip: TextView
    private lateinit var nilaiMaks: TextView
    private lateinit var nilaiRata: TextView
    private lateinit var nilaiWaktu: TextView
    private lateinit var nilaiAltitude: TextView
    private lateinit var btnStart: TextView
    private lateinit var btnReset: TextView
    private lateinit var btnPeta: TextView
    private var animDenyutX: ObjectAnimator? = null
    private var sedangDenyut = false

    private val penyegar = object : Runnable {
        override fun run() {
            segarkan()
            handler.postDelayed(this, 500L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buatLayar())
        mintaIzin()
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(penyegar)
        handler.post(penyegar)
    }

    override fun onPause() {
        handler.removeCallbacks(penyegar)
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacks(penyegar)
        animDenyutX?.cancel()
        super.onDestroy()
    }

    // ---------- Tata letak ----------

    private fun buatLayar(): View {
        val scroll = ScrollView(this)
        scroll.isFillViewport = true
        scroll.setBackgroundColor(Tema.latarAtas)

        val root = linear(this)
        root.setPadding(dp(14), dp(12), dp(14), dp(28))
        root.background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Tema.latarAtas, Tema.latarBawah))
        scroll.addView(root)

        val header = buatHeader()
        root.addView(header)

        speedo = SpeedometerView(this)
        root.addView(speedo, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(340)))

        // Chip status: GPS, satelit, akurasi
        val chips = linear(this, false)
        chips.gravity = Gravity.CENTER_VERTICAL
        chipGps = chip(Tema.hijau)
        chipGps.setOnClickListener { tekanGps() }
        chips.addView(chipGps, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.3f))
        chipSatelit = chip(Tema.garisKartu)
        chips.addView(chipSatelit, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(6), 0, dp(6), 0) })
        chipAkurasi = chip(Tema.garisKartu)
        chips.addView(chipAkurasi, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.1f))
        root.addView(chips, lp(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0f, dp(4)))

        // Kartu statistik
        val kartuStat = linear(this)
        kartuStat.setPadding(dp(14), dp(14), dp(14), dp(14))
        kartuStat.background = kotak(this, Tema.kartu, 22, Tema.garisKartu)

        val baris1 = linear(this, false)
        val (tTrip, cTrip) = sel("TRIP", "km")
        nilaiTrip = tTrip
        val (tMaks, cMaks) = sel("MAX SPEED", "km/h")
        nilaiMaks = tMaks
        val (tRata, cRata) = sel("AVG SPEED", "km/h")
        nilaiRata = tRata
        baris1.addView(cTrip, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        baris1.addView(garisVertikal(), LinearLayout.LayoutParams(dp(1), dp(44)))
        baris1.addView(cMaks, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(10) })
        baris1.addView(garisVertikal(), LinearLayout.LayoutParams(dp(1), dp(44)))
        baris1.addView(cRata, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(10) })
        kartuStat.addView(baris1)

        kartuStat.addView(garisHorizontal(), lp(LinearLayout.LayoutParams.MATCH_PARENT, dp(1), 0f, dp(12), 0, 0, 0))

        val baris2 = linear(this, false)
        val (tWaktu, cWaktu) = sel("TIME", "")
        nilaiWaktu = tWaktu
        val (tAlt, cAlt) = sel("ALTITUDE", "m")
        nilaiAltitude = tAlt
        baris2.addView(cWaktu, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        baris2.addView(garisVertikal(), LinearLayout.LayoutParams(dp(1), dp(44)))
        baris2.addView(cAlt, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(10) })
        kartuStat.addView(baris2, lp(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0f, dp(12)))
        root.addView(kartuStat, lp(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0f, dp(14)))

        // Tombol START/PAUSE, RESET, PETA
        val tombol = linear(this, false)
        tombol.gravity = Gravity.CENTER
        btnReset = bulatan(68, "RESET", Tema.abu, Tema.abu)
        btnReset.setOnClickListener { tekanReset() }
        btnStart = bulatan(104, "START", Tema.merah, Tema.oranye)
        btnStart.setOnClickListener { tekanStart() }
        btnPeta = bulatan(68, "PETA", Tema.abu, Tema.abu)
        btnPeta.setOnClickListener { bukaPeta() }
        tombol.addView(btnReset, LinearLayout.LayoutParams(dp(68), dp(68)).apply { marginEnd = dp(18) })
        tombol.addView(btnStart, LinearLayout.LayoutParams(dp(104), dp(104)))
        tombol.addView(btnPeta, LinearLayout.LayoutParams(dp(68), dp(68)).apply { marginStart = dp(18) })
        root.addView(tombol, lp(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0f, dp(18)))

        munculkan(header, 0)
        munculkan(speedo, 1)
        munculkan(kartuStat, 2)
        munculkan(tombol, 3)
        return scroll
    }

    private fun munculkan(v: View, urutan: Int) {
        v.alpha = 0f
        v.translationY = dpF(24f)
        v.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(urutan * 120L)
            .setDuration(500L)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun buatHeader(): View {
        val h = linear(this, false)
        h.gravity = Gravity.CENTER_VERTICAL
        h.setPadding(dp(4), dp(8), dp(4), dp(8))

        val logo = ImageView(this)
        logo.setImageResource(R.drawable.logo_speed)
        logo.scaleType = ImageView.ScaleType.FIT_CENTER
        h.addView(logo, LinearLayout.LayoutParams(dp(56), dp(56)).apply { marginEnd = dp(10) })

        val kolom = linear(this)
        val judul = teks(this, "HARIN SPEED", 22f, Tema.kuning, true)
        judul.letterSpacing = 0.06f
        judul.typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        kolom.addView(judul)
        kolom.addView(teks(this, "speedometer GPS by Harinfood", 12f, Tema.teksRedup))
        h.addView(kolom, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        return h
    }

    private fun chip(warnaBorder: Int): TextView {
        val t = teks(this, "", 11f, Tema.teks, true)
        t.gravity = Gravity.CENTER
        t.setPadding(dp(8), dp(8), dp(8), dp(8))
        t.background = kotak(this, Tema.kartu, 14, warnaBorder)
        return t
    }

    private fun sel(label: String, satuan: String): Pair<TextView, LinearLayout> {
        val k = linear(this)
        val lbl = teks(this, label, 11f, Tema.teksRedup, true)
        lbl.letterSpacing = 0.05f
        k.addView(lbl)
        val baris = linear(this, false)
        baris.gravity = Gravity.BOTTOM
        val nilai = teks(this, "--", 26f, Tema.teks, true)
        nilai.typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        baris.addView(nilai)
        if (satuan.isNotEmpty()) {
            val u = teks(this, " $satuan", 12f, Tema.teksRedup)
            u.setPadding(0, 0, 0, dp(4))
            baris.addView(u)
        }
        k.addView(baris)
        return Pair(nilai, k)
    }

    private fun garisVertikal(): View {
        val v = View(this)
        v.setBackgroundColor(Tema.garisKartu)
        return v
    }

    private fun garisHorizontal(): View {
        val v = View(this)
        v.setBackgroundColor(Tema.garisKartu)
        return v
    }

    private fun bulatan(ukuranDp: Int, label: String, warnaA: Int, warnaB: Int): TextView {
        val t = teks(this, label, if (ukuranDp > 80) 17f else 12f, Color.WHITE, true)
        t.gravity = Gravity.CENTER
        val g = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(warnaA, warnaB))
        g.setShape(GradientDrawable.OVAL)
        g.setStroke(dp(2), Color.parseColor("#33FFFFFF"))
        t.background = RippleDrawable(ColorStateList.valueOf(Color.parseColor("#55FFFFFF")), g, null)
        return t
    }

    // ---------- Aksi tombol ----------

    private fun izinLokasi(): Boolean =
        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun gpsAktif(): Boolean =
        getSystemService(LocationManager::class.java).isProviderEnabled(LocationManager.GPS_PROVIDER)

    private fun mintaIzin() {
        val daftar = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= 33) daftar.add(Manifest.permission.POST_NOTIFICATIONS)
        if (!izinLokasi()) requestPermissions(daftar.toTypedArray(), 101)
    }

    private fun tekanStart() {
        if (!izinLokasi()) {
            mintaIzin()
            return
        }
        if (Perjalanan.jalan) {
            GpsService.henti(this)
            Perjalanan.hentikan()
        } else {
            if (!gpsAktif()) {
                Toast.makeText(this, "Aktifkan GPS dulu", Toast.LENGTH_SHORT).show()
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                return
            }
            GpsService.mulai(this)
        }
        segarkan()
    }

    private fun tekanReset() {
        if (Perjalanan.jalan) {
            Toast.makeText(this, "Jeda perjalanan dulu sebelum reset", Toast.LENGTH_SHORT).show()
            return
        }
        Perjalanan.reset()
        Toast.makeText(this, "Perjalanan direset", Toast.LENGTH_SHORT).show()
        segarkan()
    }

    private fun tekanGps() {
        if (!izinLokasi()) mintaIzin()
        else if (!gpsAktif()) startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    private fun bukaPeta() {
        val lat = Perjalanan.lat
        val lng = Perjalanan.lng
        if (lat == null || lng == null) {
            Toast.makeText(this, "Lokasi belum terbaca", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Lokasi saya)")
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    // ---------- Pembaruan tampilan ----------

    private fun segarkan() {
        val p = Perjalanan
        speedo.setKecepatan(p.kecepatanKmh)
        p.arah?.let { speedo.setArah(it) }

        nilaiTrip.text = "%.2f".format(p.jarakM / 1000.0)
        nilaiMaks.text = "%.0f".format(p.maksKmh)
        nilaiRata.text = "%.0f".format(p.rataKmh())
        nilaiWaktu.text = p.formatWaktu()
        nilaiAltitude.text = p.altitudeM?.let { "%.0f".format(it) } ?: "--"

        val izin = izinLokasi()
        val gps = izin && gpsAktif()
        when {
            !izin -> {
                chipGps.text = "IZIN LOKASI DIPERLUKAN"
                chipGps.setTextColor(Tema.oranye)
            }
            !gps -> {
                chipGps.text = "GPS MATI"
                chipGps.setTextColor(Tema.merah)
            }
            p.lat == null -> {
                chipGps.text = "MENCARI SINYAL"
                chipGps.setTextColor(Tema.kuning)
            }
            else -> {
                chipGps.text = "GPS CONNECTED"
                chipGps.setTextColor(Tema.hijau)
            }
        }
        chipSatelit.text = "SATELIT ${p.satelitDipakai}/${p.satelitTerlihat}"
        chipAkurasi.text = p.akurasiM?.let { "Accuracy: ${it.roundToInt()}m" } ?: "Accuracy: --"

        btnStart.text = if (p.jalan) "PAUSE" else "START"
        aturDenyut(p.jalan)
    }

    private fun aturDenyut(aktif: Boolean) {
        if (aktif && !sedangDenyut) {
            sedangDenyut = true
            animDenyutX = ObjectAnimator.ofPropertyValuesHolder(
                btnStart,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 1.06f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 1.06f)
            ).apply {
                setDuration(850L)
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
                start()
            }
        } else if (!aktif && sedangDenyut) {
            sedangDenyut = false
            animDenyutX?.cancel()
            animDenyutX = null
            btnStart.scaleX = 1f
            btnStart.scaleY = 1f
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        segarkan()
    }
}

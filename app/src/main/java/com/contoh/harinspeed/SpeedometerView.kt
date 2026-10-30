package com.contoh.harinspeed

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Speedometer analog dengan busur warna, jarum merah bercahaya,
 * angka digital, dan kompas kecil di bawah. Gerakannya dianimasikan halus.
 */
class SpeedometerView(context: Context) : View(context) {

    companion object {
        const val MAKS = 200f
        private const val MULAI = 135f
        private const val SAPUAN = 270f
    }

    private var kmhTampil = 0f
    private var kmhTarget = -1f
    private var animKm: ValueAnimator? = null
    private var arahTampil = 0f
    private var arahTarget = Float.NaN
    private var animArah: ValueAnimator? = null

    private val rect = RectF()
    private val tebal = context.dpF(16f)

    private val pCahaya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = context.dpF(12f)
        color = Color.parseColor("#302979FF")
    }
    private val pRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = context.dpF(2.5f)
        color = Color.parseColor("#2979FF")
    }
    private val pWajah = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pTrack = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = tebal
        color = Color.parseColor("#161E27")
    }
    private val pArc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = tebal
    }
    private val pRedup = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = tebal
        color = Color.parseColor("#B8070B12")
    }
    private val pTick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = context.dpF(1.5f)
        color = Color.parseColor("#7F8C99")
    }
    private val pTickBesar = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = context.dpF(3f)
        color = Color.WHITE
    }
    private val fontMiring = Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC)
    private val pLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C9D1D9")
        textAlign = Paint.Align.CENTER
        typeface = fontMiring
    }
    private val pDigit = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = fontMiring
    }
    private val pSatuan = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF1744")
        textAlign = Paint.Align.CENTER
        typeface = fontMiring
    }
    private val pCincinTengah = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = context.dpF(2f)
        color = Color.parseColor("#1E2A38")
    }
    private val pJarumCahaya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = context.dpF(14f)
        color = Color.parseColor("#55FF1744")
    }
    private val pJarum = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = context.dpF(5f)
        color = Color.parseColor("#FF1744")
    }
    private val pHub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1F2A36")
    }
    private val pHubCincin = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = context.dpF(2f)
        color = Color.parseColor("#FF1744")
    }
    private val pKompasCincin = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = context.dpF(3f)
    }
    private val pKompasIsi = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#0E151D")
    }
    private val pKompasHuruf = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val pKompasJarumMerah = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FF1744")
    }
    private val pKompasJarumPutih = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = context.dpF(2.5f)
        color = Color.parseColor("#B0BEC5")
    }

    /** Kecepatan (km/h) yang digerakkan halus ke nilai baru. */
    fun setKecepatan(v: Float) {
        val target = v.coerceIn(0f, MAKS)
        if (abs(target - kmhTarget) < 0.05f) return
        kmhTarget = target
        animKm?.cancel()
        animKm = ValueAnimator.ofFloat(kmhTampil, target).apply {
            duration = 450L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                kmhTampil = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    /** Arah jarum kompas (derajat dari utara). Memilih putaran terpendek. */
    fun setArah(derajat: Float) {
        val target = ((derajat % 360f) + 360f) % 360f
        if (!arahTarget.isNaN()) {
            var beda = target - (arahTarget % 360f)
            if (beda > 180f) beda -= 360f
            if (beda < -180f) beda += 360f
            if (abs(beda) < 1f) return
        }
        var beda = if (arahTarget.isNaN()) 0f else target - (arahTarget % 360f)
        if (beda > 180f) beda -= 360f
        if (beda < -180f) beda += 360f
        val akhir = arahTampil + beda
        arahTarget = akhir
        animArah?.cancel()
        animArah = ValueAnimator.ofFloat(arahTampil, akhir).apply {
            duration = 500L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                arahTampil = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun sudut(v: Float): Float = MULAI + (v.coerceIn(0f, MAKS) / MAKS) * SAPUAN

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h * 0.46f
        val r = minOf(w * 0.40f, h * 0.40f)

        // Cincin biru luar dengan cahaya
        c.drawCircle(cx, cy, r * 1.06f, pCahaya)
        c.drawCircle(cx, cy, r * 1.04f, pRing)

        // Wajah dial gelap
        pWajah.shader = RadialGradient(
            cx, cy, r,
            intArrayOf(Color.parseColor("#1C2633"), Color.parseColor("#0A0F15")),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        c.drawCircle(cx, cy, r * 0.98f, pWajah)

        // Jalur busur
        rect.set(cx - r * 0.86f, cy - r * 0.86f, cx + r * 0.86f, cy + r * 0.86f)
        c.drawArc(rect, MULAI, SAPUAN, false, pTrack)

        // Busur warna hijau -> kuning -> oranye -> merah
        val sg = SweepGradient(
            cx, cy,
            intArrayOf(
                Color.parseColor("#00E676"),
                Color.parseColor("#C6FF00"),
                Color.parseColor("#FFEB3B"),
                Color.parseColor("#FF9100"),
                Color.parseColor("#FF1744"),
                Color.parseColor("#FF1744")
            ),
            null
        )
        val matriks = Matrix()
        matriks.setRotate(MULAI, cx, cy)
        sg.setLocalMatrix(matriks)
        pArc.shader = sg
        c.drawArc(rect, MULAI, SAPUAN, false, pArc)

        // Redupkan bagian busur di atas kecepatan sekarang
        val sudutJarum = sudut(kmhTampil)
        if (kmhTampil < MAKS - 0.5f) {
            c.drawArc(rect, sudutJarum, MULAI + SAPUAN - sudutJarum, false, pRedup)
        }

        // Tanda skala setiap 10 km/h, label setiap 20 km/h
        val rDalam = r * 0.86f - tebal / 2f - context.dpF(2f)
        for (v in 0..200 step 10) {
            val a = Math.toRadians(sudut(v.toFloat()).toDouble())
            val besar = v % 20 == 0
            val panjang = if (besar) context.dpF(12f) else context.dpF(7f)
            val r1 = rDalam - panjang
            val p = if (besar) pTickBesar else pTick
            c.drawLine(cx + cos(a).toFloat() * r1, cy + sin(a).toFloat() * r1, cx + cos(a).toFloat() * rDalam, cy + sin(a).toFloat() * rDalam, p)
        }
        pLabel.textSize = r * 0.10f
        for (v in 0..200 step 20) {
            val a = Math.toRadians(sudut(v.toFloat()).toDouble())
            val rl = r * 0.66f
            val x = cx + cos(a).toFloat() * rl
            val y = cy + sin(a).toFloat() * rl + pLabel.textSize / 3f
            c.drawText(v.toString(), x, y, pLabel)
        }

        // Lingkaran tengah dan angka digital
        c.drawCircle(cx, cy, r * 0.62f, pCincinTengah)
        val angka = kmhTampil.roundToInt().toString()
        pDigit.textSize = r * 0.58f
        pDigit.shader = LinearGradient(
            0f, cy - r * 0.45f, 0f, cy + r * 0.15f,
            intArrayOf(Color.WHITE, Color.parseColor("#FFF59D")),
            null,
            Shader.TileMode.CLAMP
        )
        val fm = pDigit.fontMetrics
        c.drawText(angka, cx, cy - r * 0.08f - (fm.ascent + fm.descent) / 2f, pDigit)
        pSatuan.textSize = r * 0.16f
        c.drawText("km/h", cx, cy + r * 0.40f, pSatuan)

        // Jarum merah bercahaya
        val ang = Math.toRadians(sudutJarum.toDouble())
        val ujung = r * 0.74f
        val ex = cx + cos(ang).toFloat() * ujung
        val ey = cy + sin(ang).toFloat() * ujung
        c.drawLine(cx, cy, ex, ey, pJarumCahaya)
        c.drawLine(cx, cy, ex, ey, pJarum)
        c.drawCircle(cx, cy, r * 0.07f, pHub)
        c.drawCircle(cx, cy, r * 0.07f, pHubCincin)

        // Kompas di bawah dial
        val kcx = cx
        val kcy = cy + r * 0.80f
        val kr = r * 0.20f
        pKompasCincin.shader = SweepGradient(
            kcx, kcy,
            intArrayOf(
                Color.parseColor("#00E676"),
                Color.parseColor("#00E5FF"),
                Color.parseColor("#2979FF"),
                Color.parseColor("#00E676")
            ),
            null
        )
        c.drawCircle(kcx, kcy, kr, pKompasCincin)
        c.drawCircle(kcx, kcy, kr * 0.92f, pKompasIsi)
        pKompasHuruf.textSize = kr * 0.30f
        val huruf = listOf("N" to -90f, "E" to 0f, "S" to 90f, "W" to 180f)
        for ((t, deg) in huruf) {
            val a = Math.toRadians(deg.toDouble())
            val x = kcx + cos(a).toFloat() * kr * 0.64f
            val y = kcy + sin(a).toFloat() * kr * 0.64f + pKompasHuruf.textSize / 3f
            pKompasHuruf.color = if (t == "N") Color.parseColor("#FFD600") else Color.parseColor("#00E5FF")
            c.drawText(t, x, y, pKompasHuruf)
        }
        val aJarum = Math.toRadians((-90f + arahTampil).toDouble())
        val tx = kcx + cos(aJarum).toFloat() * kr * 0.62f
        val ty = kcy + sin(aJarum).toFloat() * kr * 0.62f
        val bx = kcx - cos(aJarum).toFloat() * kr * 0.42f
        val by = kcy - sin(aJarum).toFloat() * kr * 0.42f
        pKompasJarumPutih.strokeWidth = context.dpF(2.5f)
        c.drawLine(bx, by, kcx, kcy, pKompasJarumPutih)
        c.drawLine(kcx, kcy, tx, ty, pKompasJarumMerah)
        c.drawCircle(kcx, kcy, kr * 0.09f, pHub)
    }
}

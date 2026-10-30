package com.contoh.harinspeed

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.LinearLayout
import android.widget.TextView

fun Context.dp(v: Int): Int = Math.round(v * resources.displayMetrics.density)

fun Context.dpF(v: Float): Float = v * resources.displayMetrics.density

fun kotak(ctx: Context, warna: Int, radiusDp: Int, garis: Int = 0): GradientDrawable {
    val g = GradientDrawable()
    g.setColor(warna)
    g.setCornerRadius(ctx.dpF(radiusDp.toFloat()))
    if (garis != 0) g.setStroke(ctx.dp(1), garis)
    return g
}

fun teks(ctx: Context, isi: String, sp: Float, warna: Int, tebal: Boolean = false): TextView {
    val t = TextView(ctx)
    t.text = isi
    t.textSize = sp
    t.setTextColor(warna)
    if (tebal) t.typeface = Typeface.DEFAULT_BOLD
    return t
}

fun linear(ctx: Context, vertikal: Boolean = true): LinearLayout {
    val l = LinearLayout(ctx)
    l.orientation = if (vertikal) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
    return l
}

fun lp(
    lebar: Int,
    tinggi: Int,
    bobot: Float = 0f,
    atas: Int = 0,
    kiri: Int = 0,
    kanan: Int = 0,
    bawah: Int = 0
): LinearLayout.LayoutParams {
    val p = LinearLayout.LayoutParams(lebar, tinggi, bobot)
    p.setMargins(kiri, atas, kanan, bawah)
    return p
}

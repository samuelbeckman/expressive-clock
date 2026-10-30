package app.expressiveclock.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.res.ResourcesCompat
import app.expressiveclock.MainActivity
import app.expressiveclock.R
import app.expressiveclock.data.ClockSettings
import app.expressiveclock.data.ClockStore
import app.expressiveclock.data.DigitalBackground
import app.expressiveclock.data.DigitalLayout
import app.expressiveclock.data.DigitalTypefaceWeight
import app.expressiveclock.data.WidgetConfig
import app.expressiveclock.data.WidgetFontFamily
import app.expressiveclock.data.WidgetType
import java.io.ByteArrayOutputStream
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

object DigitalClockWidgetRenderer {

    suspend fun update(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
        val store = ClockStore(context)
        val config = store.widgetConfigNow(appWidgetId) ?: WidgetConfig(type = WidgetType.DIGITAL)
        if (config.type == WidgetType.ANALOG) {
            ClockWidgetRenderer.update(context, manager, appWidgetId)
            return
        }
        val settings = store.settingsNow()
        val options = manager.getAppWidgetOptions(appWidgetId)

        val views = build(context, config, settings, options)
        manager.updateAppWidget(appWidgetId, views)
    }

    private fun build(
        context: Context,
        config: WidgetConfig,
        settings: ClockSettings,
        options: Bundle,
    ): RemoteViews {
        val density = context.resources.displayMetrics.density
        fun px(dp: Int) = (dp * density).roundToInt().coerceAtLeast(1)

        val sizes: List<SizeF> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            @Suppress("DEPRECATION")
            options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
                ?.takeIf { it.isNotEmpty() }
                ?: emptyList()
        } else {
            emptyList()
        }

        if (sizes.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val map = sizes.associateWith { size ->
                one(context, config, settings, px(size.width.roundToInt()), px(size.height.roundToInt()))
            }
            return RemoteViews(map)
        }

        val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)
        val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minW)
        val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 70)
        val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minH)
        val portrait = one(context, config, settings, px(minW), px(maxH))
        val landscape = one(context, config, settings, px(maxW), px(minH))
        return RemoteViews(landscape, portrait)
    }

    private fun one(
        context: Context,
        config: WidgetConfig,
        settings: ClockSettings,
        w: Int,
        h: Int,
    ): RemoteViews {
        val colours = WidgetColours.resolve(context, config, settings)
        val views = RemoteViews(context.packageName, R.layout.widget_digital_clock)

        // Render full pixel-perfect bitmap for Digital Clock
        val bmp = renderDigitalBitmap(context, config, colours, w, h)
        views.setImageViewIcon(R.id.background_shape, bmp.toIcon())

        // Hide all included XML containers to avoid overlaying native TextClock
        listOf(
            R.id.horizontal_flex, R.id.horizontal_serif, R.id.horizontal_monospace,
            R.id.horizontal_cursive, R.id.horizontal_sans, R.id.stacked_flex,
            R.id.stacked_serif, R.id.stacked_monospace, R.id.stacked_cursive, R.id.stacked_sans,
        ).forEach { containerId ->
            views.setViewVisibility(containerId, View.GONE)
        }

        views.setOnClickPendingIntent(
            android.R.id.background,
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )

        return views
    }

    private fun renderDigitalBitmap(
        context: Context,
        config: WidgetConfig,
        colours: FaceRenderer.Colours,
        w: Int,
        h: Int,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // 1. Draw Background Shape
        if (config.digitalBackground != DigitalBackground.NONE) {
            val rx = (w.coerceAtMost(h) * 0.22f)
            val rect = RectF(4f, 4f, w - 4f, h - 4f)
            if (config.digitalBackground == DigitalBackground.FILLED) {
                val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = colours.face
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(rect, rx, rx, fillPaint)
            } else if (config.digitalBackground == DigitalBackground.OUTLINED) {
                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = colours.outline
                    style = Paint.Style.STROKE
                    strokeWidth = (w.coerceAtMost(h) * 0.03f).coerceAtLeast(3f)
                }
                canvas.drawRoundRect(rect, rx, rx, strokePaint)
            }
        }

        // 2. Resolve Typeface & Weight
        val isBold = config.digitalWeight == DigitalTypefaceWeight.BOLD || config.digitalWeight == DigitalTypefaceWeight.MEDIUM
        val style = if (isBold) Typeface.BOLD else Typeface.NORMAL

        val tf = when (config.digitalFont) {
            WidgetFontFamily.GOOGLE_SANS, WidgetFontFamily.GOOGLE_SANS_NORMAL -> ResourcesCompat.getFont(context, R.font.google_sans_flex) ?: Typeface.SANS_SERIF
            WidgetFontFamily.SERIF -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Typeface.create(Typeface.SERIF, config.digitalWeight.weightInt, false)
            } else {
                Typeface.create(Typeface.SERIF, style)
            }
            WidgetFontFamily.MONOSPACE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Typeface.create(Typeface.MONOSPACE, config.digitalWeight.weightInt, false)
            } else {
                Typeface.create(Typeface.MONOSPACE, style)
            }
            WidgetFontFamily.CURSIVE -> Typeface.create("cursive", Typeface.NORMAL)
        }

        val isStacked = config.digitalLayout == DigitalLayout.STACKED
        val isTimeOnly = !config.showDate && !config.showDayOfWeek
        val isCompact = config.digitalLayout == DigitalLayout.COMPACT

        val dateSkeleton = when {
            isTimeOnly || (!config.showDate && !config.showDayOfWeek) -> ""
            config.showDate && config.showDayOfWeek -> "EEEdMMM"
            config.showDayOfWeek -> "EEE"
            else -> "dMMM"
        }
        val showDateView = dateSkeleton.isNotEmpty()

        val hourColorInt = config.digitalHourColor?.toInt() ?: colours.hourHand
        val colonColorInt = config.digitalColonColor?.toInt() ?: colours.onFace
        val minuteColorInt = config.digitalMinuteColor?.toInt() ?: colours.minuteHand
        val dayColorInt = config.digitalDayColor?.toInt() ?: colours.minor
        val dateColorInt = config.digitalDateColor?.toInt() ?: colours.minor

        val widthVal = if (config.digitalFont == WidgetFontFamily.GOOGLE_SANS) 25f else 100f

        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = tf
            textAlign = Paint.Align.CENTER
            if (config.digitalFont == WidgetFontFamily.GOOGLE_SANS || config.digitalFont == WidgetFontFamily.GOOGLE_SANS_NORMAL) {
                fontFeatureSettings = "tnum, lnum"
                setFontVariationSettings("'opsz' 144,'wdth' $widthVal,'wght' ${config.digitalWeight.weightInt}")
            }
        }

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = tf
            color = dateColorInt
            textAlign = Paint.Align.CENTER
            if (config.digitalFont == WidgetFontFamily.GOOGLE_SANS || config.digitalFont == WidgetFontFamily.GOOGLE_SANS_NORMAL) {
                fontFeatureSettings = "tnum, lnum"
                setFontVariationSettings("'opsz' 144,'wdth' $widthVal,'wght' 600")
            }
        }

        val now = LocalTime.now()
        val is24 = DateFormat.is24HourFormat(context)
        val hourStr = if (is24) now.format(DateTimeFormatter.ofPattern("HH")) else now.format(DateTimeFormatter.ofPattern("hh"))
        val minStr = if (config.showSeconds && !isCompact) {
            now.format(DateTimeFormatter.ofPattern("mm:ss"))
        } else {
            now.format(DateTimeFormatter.ofPattern("mm"))
        }

        val dateText = if (showDateView) {
            val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), dateSkeleton)
            ZonedDateTime.now().format(DateTimeFormatter.ofPattern(pattern)).uppercase(Locale.getDefault())
        } else ""

        if (isStacked) {
            val sizePx = if (showDateView) h * 0.30f else h * 0.38f
            timePaint.textSize = sizePx
            datePaint.textSize = sizePx * 0.28f

            val cy = h / 2f
            timePaint.color = hourColorInt
            canvas.drawText(hourStr, w / 2f, cy - sizePx * 0.05f, timePaint)

            timePaint.color = minuteColorInt
            canvas.drawText(minStr, w / 2f, cy + sizePx * 0.75f, timePaint)

            if (showDateView) {
                datePaint.color = dateColorInt
                canvas.drawText(dateText, w / 2f, cy + sizePx * 1.18f, datePaint)
            }
        } else {
            val sizePx = when {
                isCompact -> h * 0.22f
                else -> h * 0.38f
            }
            timePaint.textSize = sizePx
            datePaint.textSize = if (isCompact) sizePx * 0.45f else sizePx * 0.32f

            val hourWidth = timePaint.measureText(hourStr)
            val colonWidth = timePaint.measureText(":")
            val minWidth = timePaint.measureText(minStr)
            val totalTimeWidth = hourWidth + colonWidth + minWidth

            val startX = (w - totalTimeWidth) / 2f
            val cy = if (showDateView) h / 2f - sizePx * 0.10f else h / 2f + sizePx * 0.32f

            timePaint.textAlign = Paint.Align.LEFT
            timePaint.color = hourColorInt
            canvas.drawText(hourStr, startX, cy, timePaint)

            timePaint.color = colonColorInt
            canvas.drawText(":", startX + hourWidth, cy, timePaint)

            timePaint.color = minuteColorInt
            canvas.drawText(minStr, startX + hourWidth + colonWidth, cy, timePaint)

            if (showDateView) {
                datePaint.textAlign = Paint.Align.CENTER
                datePaint.color = dateColorInt
                canvas.drawText(dateText, w / 2f, cy + sizePx * 0.48f, datePaint)
            }
        }

        return bmp
    }

    private fun Bitmap.toIcon(): Icon {
        val out = ByteArrayOutputStream(64 * 1024)
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSLESS
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
        compress(format, 100, out)
        val bytes = out.toByteArray()
        return Icon.createWithData(bytes, 0, bytes.size)
    }
}

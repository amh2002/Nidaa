package com.mawaqiti.plus.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaqiti.plus.R
import com.mawaqiti.plus.data.City
import com.mawaqiti.plus.engine.Qibla
import com.mawaqiti.plus.ui.components.GlassCard
import com.mawaqiti.plus.ui.theme.SkyPalette
import com.mawaqiti.plus.ui.theme.skyPalette
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun QiblaScreen(city: City, isArabic: Boolean, onBack: () -> Unit) {
    val palette = skyPalette
    val context = LocalContext.current
    var azimuth by remember { mutableFloatStateOf(0f) }
    var hasSensor by remember { mutableStateOf(true) }

    DisposableEffect(context) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (manager == null || sensor == null) {
            hasSensor = false
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val rotation = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotation, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotation, orientation)
                    azimuth = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            onDispose { manager.unregisterListener(listener) }
        }
    }

    val bearing = remember(city.order) { Qibla.bearing(city.latitude, city.longitude) }
    val distance = remember(city.order) { Qibla.distanceKm(city.latitude, city.longitude) }
    val delta = Qibla.signedDelta(bearing, azimuth.toDouble())
    val aligned = abs(delta) <= 5.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        ScreenTopBar(title = stringResource(R.string.qibla_title), palette = palette, onBack = onBack)

        Text(
            text = stringResource(R.string.qibla_city, city.label(isArabic)),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textMuted,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        if (!hasSensor) {
            GlassCard(emphasized = true, contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
                Text(
                    text = stringResource(R.string.qibla_sensor_missing),
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.text,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            GlassCard(
                emphasized = true,
                corner = 22.dp,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .size(320.dp),
                    contentAlignment = Alignment.Center
                ) {
                    QiblaDial(
                        azimuth = azimuth,
                        qibla = bearing,
                        palette = palette,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = when {
                    aligned -> stringResource(R.string.qibla_aligned)
                    delta > 0 -> stringResource(R.string.qibla_turn_right)
                    else -> stringResource(R.string.qibla_turn_left)
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (aligned) palette.accent else palette.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = String.format(Locale.US, stringResource(R.string.degrees_format), abs(delta)),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = palette.accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = stringResource(R.string.qibla_bearing),
                value = String.format(Locale.US, stringResource(R.string.degrees_format), bearing),
                palette = palette,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = stringResource(R.string.qibla_distance),
                value = String.format(Locale.US, stringResource(R.string.km_format), distance),
                palette = palette,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))
        MetricCard(
            title = stringResource(R.string.qibla_current_heading),
            value = String.format(Locale.US, stringResource(R.string.degrees_format), azimuth),
            palette = palette,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.qibla_calibrate),
            style = MaterialTheme.typography.bodySmall,
            color = palette.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
fun ScreenTopBar(title: String, palette: SkyPalette, onBack: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(palette.chipFill, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = palette.text
                    )
                }
            }
            Spacer(Modifier.size(10.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = palette.text,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QiblaDial(
    azimuth: Float,
    qibla: Double,
    palette: SkyPalette,
    modifier: Modifier = Modifier
) {
    val ring = palette.text.copy(alpha = 0.30f)
    val tick = palette.text
    val needle = palette.accent

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 8.dp.toPx()

        drawCircle(
            color = ColorWhite.copy(alpha = 0.06f),
            radius = radius,
            center = center
        )
        drawCircle(
            color = ring,
            radius = radius,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )

        for (index in 0 until 24) {
            val angle = index * 15.0
            val radians = Math.toRadians(angle - azimuth)
            val innerRadius = if (index % 6 == 0) radius - 18.dp.toPx() else radius - 9.dp.toPx()
            val outer = Offset(
                center.x + (radius * sin(radians)).toFloat(),
                center.y - (radius * cos(radians)).toFloat()
            )
            val inner = Offset(
                center.x + (innerRadius * sin(radians)).toFloat(),
                center.y - (innerRadius * cos(radians)).toFloat()
            )
            drawLine(
                color = if (index % 6 == 0) tick else tick.copy(alpha = 0.35f),
                start = inner,
                end = outer,
                strokeWidth = if (index % 6 == 0) 2.5.dp.toPx() else 1.5.dp.toPx()
            )
        }

        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 15.sp.toPx()
            color = tick.toArgb()
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        // Latin cardinal letters are unambiguous in both languages.
        listOf("N" to 0.0, "E" to 90.0, "S" to 180.0, "W" to 270.0).forEach { (label, angle) ->
            val radians = Math.toRadians(angle - azimuth)
            val distanceFromCenter = radius - 36.dp.toPx()
            val x = center.x + (distanceFromCenter * sin(radians)).toFloat()
            val y = center.y - (distanceFromCenter * cos(radians)).toFloat() + paint.textSize / 3f
            drawContext.canvas.nativeCanvas.drawText(label, x, y, paint)
        }

        val qiblaRadians = Math.toRadians(qibla - azimuth)
        val tip = Offset(
            center.x + ((radius - 8.dp.toPx()) * sin(qiblaRadians)).toFloat(),
            center.y - ((radius - 8.dp.toPx()) * cos(qiblaRadians)).toFloat()
        )
        drawLine(
            color = needle,
            start = center,
            end = tip,
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )

        val arrowBase = Offset(
            center.x + ((radius - 34.dp.toPx()) * sin(qiblaRadians)).toFloat(),
            center.y - ((radius - 34.dp.toPx()) * cos(qiblaRadians)).toFloat()
        )
        val perpendicular = qiblaRadians + Math.PI / 2
        val halfWidth = 11.dp.toPx()
        val left = Offset(
            arrowBase.x + (halfWidth * sin(perpendicular)).toFloat(),
            arrowBase.y - (halfWidth * cos(perpendicular)).toFloat()
        )
        val right = Offset(
            arrowBase.x - (halfWidth * sin(perpendicular)).toFloat(),
            arrowBase.y + (halfWidth * cos(perpendicular)).toFloat()
        )
        val path = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(left.x, left.y)
            lineTo(right.x, right.y)
            close()
        }
        drawPath(path, needle)

        drawCircle(color = needle, radius = 6.dp.toPx(), center = center)
        drawLine(
            color = needle,
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y - radius + 14.dp.toPx()),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private val ColorWhite = androidx.compose.ui.graphics.Color.White

@Composable
private fun MetricCard(
    title: String,
    value: String,
    palette: SkyPalette,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        corner = 18.dp,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 12.dp,
            vertical = 12.dp
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = palette.textMuted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = palette.text
            )
        }
    }
}

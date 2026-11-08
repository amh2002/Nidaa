package com.mawaqiti.plus.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun QiblaScreen(city: City, isArabic: Boolean) {
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
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.qibla_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.qibla_city, city.label(isArabic)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(18.dp))

        if (!hasSensor) {
            InfoCard(text = stringResource(R.string.qibla_sensor_missing))
        } else {
            Box(
                modifier = Modifier.size(310.dp),
                contentAlignment = Alignment.Center
            ) {
                QiblaDial(azimuth = azimuth, qibla = bearing, modifier = Modifier.fillMaxSize())
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
                color = if (aligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = String.format(Locale.US, stringResource(R.string.degrees_format), abs(delta)),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Spacer(Modifier.height(22.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = stringResource(R.string.qibla_bearing),
                value = String.format(Locale.US, stringResource(R.string.degrees_format), bearing),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = stringResource(R.string.qibla_distance),
                value = String.format(Locale.US, stringResource(R.string.km_format), distance),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))
        MetricCard(
            title = stringResource(R.string.qibla_current_heading),
            value = String.format(Locale.US, stringResource(R.string.degrees_format), azimuth),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.qibla_calibrate),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun QiblaDial(azimuth: Float, qibla: Double, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 6.dp.toPx()

        drawCircle(color = colors.surface, radius = radius, center = center)
        drawCircle(
            color = colors.primary.copy(alpha = 0.22f),
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
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
                color = if (index % 6 == 0) colors.primary
                else colors.primary.copy(alpha = 0.35f),
                start = inner,
                end = outer,
                strokeWidth = if (index % 6 == 0) 3.dp.toPx() else 1.5.dp.toPx()
            )
        }

        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 15.sp.toPx()
            color = colors.onSurface.toArgb()
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
            center.x + ((radius - 6.dp.toPx()) * sin(qiblaRadians)).toFloat(),
            center.y - ((radius - 6.dp.toPx()) * cos(qiblaRadians)).toFloat()
        )
        drawLine(
            color = colors.secondary,
            start = center,
            end = tip,
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )

        val arrowBase = Offset(
            center.x + ((radius - 32.dp.toPx()) * sin(qiblaRadians)).toFloat(),
            center.y - ((radius - 32.dp.toPx()) * cos(qiblaRadians)).toFloat()
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
        drawPath(path, colors.secondary)

        drawCircle(color = colors.primary, radius = 6.dp.toPx(), center = center)
        drawLine(
            color = colors.secondary,
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y - radius + 14.dp.toPx()),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun InfoCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(18.dp),
            textAlign = TextAlign.Center
        )
    }
}

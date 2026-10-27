package com.example.wizlite

import android.content.Context
import android.graphics.Color as AColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

// Tema personalizado: fondo #151515, botones/tarjetas #20201F, texto/énfasis #F0EFEC
private val WizColors = darkColorScheme(
    primary = Color(0xFFF0EFEC),
    onPrimary = Color(0xFF151515),
    secondary = Color(0xFFF0EFEC),
    onSecondary = Color(0xFF151515),
    background = Color(0xFF151515),
    onBackground = Color(0xFFF0EFEC),
    surface = Color(0xFF151515),
    onSurface = Color(0xFFF0EFEC),
    surfaceVariant = Color(0xFF20201F),
    onSurfaceVariant = Color(0xFFF0EFEC),
    secondaryContainer = Color(0xFF20201F),
    onSecondaryContainer = Color(0xFFF0EFEC),
    outline = Color(0xFFF0EFEC)
)
private val CardColor = Color(0xFF20201F)
private val InnerCardColor = Color(0xFF20201F)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Scheduler.scheduleAll(this)
        setContent {
            MaterialTheme(colorScheme = WizColors) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { Screen() }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardColor)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
fun Screen() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("wiz", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    var ip by remember { mutableStateOf(prefs.getString("ip", "") ?: "") }
    var status by remember { mutableStateOf("") }
    var on by remember { mutableStateOf(false) }
    var dim by remember { mutableFloatStateOf(100f) }
    var temp by remember { mutableFloatStateOf(3000f) }
    var hue by remember { mutableFloatStateOf(30f) }
    var sat by remember { mutableFloatStateOf(1f) }
    val busy = remember { AtomicBoolean(false) }

    var schedules by remember { mutableStateOf(ScheduleStore.load(ctx)) }
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Schedule?>(null) }

    fun upsert(s: Schedule) {
        schedules = schedules.filter { it.id != s.id } + s
        ScheduleStore.save(ctx, schedules); Scheduler.schedule(ctx, s)
    }
    fun remove(s: Schedule) {
        schedules = schedules.filter { it.id != s.id }
        ScheduleStore.save(ctx, schedules); Scheduler.cancel(ctx, s.id)
    }

    fun send(p: JSONObject) {
        scope.launch(Dispatchers.IO) {
            status = if (WizClient.setPilot(ip, p)) "OK" else "Sin respuesta del foco"
        }
    }

    fun colorParams(h: Float, s: Float): JSONObject {
        val c = AColor.HSVToColor(floatArrayOf(h, s, 1f))
        return JSONObject().put("state", true)
            .put("r", AColor.red(c)).put("g", AColor.green(c)).put("b", AColor.blue(c))
            .put("dimming", dim.toInt())
    }

    LaunchedEffect(Unit) {
        if (ip.isNotBlank()) launch(Dispatchers.IO) { WizClient.getState(ip)?.let { on = it } }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("WiZ Lite", style = MaterialTheme.typography.headlineMedium)

        Section("Conexión") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it.trim(); prefs.edit().putString("ip", ip).apply() },
                    label = { Text("IP del foco") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    status = "Buscando…"
                    scope.launch(Dispatchers.IO) {
                        val r = WizClient.discover()
                        if (r.isNotEmpty()) {
                            ip = r.first(); prefs.edit().putString("ip", ip).apply()
                            status = "Encontrado: ${r.joinToString()}"
                            WizClient.getState(ip)?.let { on = it }
                        } else status = "No se encontró ningún foco"
                    }
                }) { Text("Buscar") }
            }
        }

        Section("Luz") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Encendido")
                Switch(checked = on, onCheckedChange = { on = it; send(JSONObject().put("state", it)) })
            }
            Text("Brillo: ${dim.toInt()}%")
            Slider(value = dim, onValueChange = { dim = it }, valueRange = 10f..100f,
                onValueChangeFinished = {
                    on = true
                    send(JSONObject().put("state", true).put("dimming", dim.toInt()))
                })
            Text("Luz blanca: ${temp.toInt()} K")
            Slider(value = temp, onValueChange = { temp = it }, valueRange = 2200f..6500f,
                onValueChangeFinished = {
                    on = true
                    send(JSONObject().put("state", true).put("temp", temp.toInt()))
                })
        }

        Section("Dormitorio") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        temp = 3500f
                        on = true
                        send(JSONObject().put("state", true).put("temp", 3500))
                    },
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Text("☀️ Día", style = MaterialTheme.typography.titleMedium)
                }
                Button(
                    onClick = {
                        temp = 2400f
                        on = true
                        send(JSONObject().put("state", true).put("temp", 2400))
                    },
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Text("🌙 Noche", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        Section("Color") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Arrastra por la rueda", style = MaterialTheme.typography.bodySmall)
                Box(Modifier.size(26.dp).clip(CircleShape).background(Color.hsv(hue, sat, 1f)))
            }
            ColorWheel(
                hue = hue, sat = sat,
                onChange = { h, s ->
                    hue = h; sat = s
                    // vista previa en vivo (un solo paquete a la vez)
                    if (ip.isNotBlank() && !busy.getAndSet(true)) {
                        scope.launch(Dispatchers.IO) {
                            WizClient.setPilotOnce(ip, colorParams(h, s))
                            busy.set(false)
                        }
                    }
                },
                onFinish = { h, s -> on = true; send(colorParams(h, s)) },
                modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(0.85f)
            )
            Text("El centro es blanco; hacia el borde el color es más intenso. El brillo se ajusta arriba.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Section("Horarios") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (schedules.isEmpty()) "Sin horarios" else "Toca uno para editarlo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = { editing = null; showDialog = true }) { Text("Añadir") }
            }
            schedules.sortedWith(compareBy({ it.hour }, { it.minute })).forEach { s ->
                Card(
                    Modifier.fillMaxWidth().clickable { editing = s; showDialog = true },
                    colors = CardDefaults.cardColors(containerColor = InnerCardColor)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("%02d:%02d".format(s.hour, s.minute), style = MaterialTheme.typography.titleLarge)
                            Text((if (s.on) "Encender ${s.dimming}%" else "Apagar") + " · " + daysLabel(s.days),
                                style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { remove(s) }) { Text("Borrar") }
                        Switch(checked = s.enabled, onCheckedChange = { upsert(s.copy(enabled = it)) })
                    }
                }
            }
        }

        if (showDialog) ScheduleDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = { upsert(it); showDialog = false }
        )

        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val dayNames = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
private val dayLetters = listOf("L", "M", "X", "J", "V", "S", "D")

private fun daysLabel(d: Int): String =
    if (d == 127) "Todos los días"
    else dayNames.filterIndexed { i, _ -> (d and (1 shl i)) != 0 }.joinToString(", ")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDialog(initial: Schedule?, onDismiss: () -> Unit, onSave: (Schedule) -> Unit) {
    val tp = rememberTimePickerState(
        initialHour = initial?.hour ?: 22,
        initialMinute = initial?.minute ?: 0,
        is24Hour = true
    )
    var days by remember { mutableIntStateOf(initial?.days ?: 127) }
    var on by remember { mutableStateOf(initial?.on ?: false) }
    var dim by remember { mutableFloatStateOf((initial?.dimming ?: 100).toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF20201F),
        confirmButton = {
            TextButton(onClick = {
                if (days != 0) onSave(
                    Schedule(
                        initial?.id ?: (System.currentTimeMillis().toInt() and 0x7fffffff),
                        tp.hour, tp.minute, days, on, dim.toInt(), initial?.enabled ?: true
                    )
                )
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text(if (initial == null) "Nuevo horario" else "Editar horario") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeInput(state = tp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    dayLetters.forEachIndexed { i, l ->
                        val sel = (days and (1 shl i)) != 0
                        Box(
                            Modifier.size(34.dp).clip(CircleShape)
                                .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { days = days xor (1 shl i) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(l, color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !on, onClick = { on = false }, label = { Text("Apagar") })
                    FilterChip(selected = on, onClick = { on = true }, label = { Text("Encender") })
                }
                if (on) {
                    Text("Brillo: ${dim.toInt()}%")
                    Slider(value = dim, onValueChange = { dim = it }, valueRange = 10f..100f)
                }
            }
        }
    )
}

package ke.elgonlabs.barberbeyond

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import org.json.JSONObject
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

private val Gold = Color(0xFFE8BD64)
private val Dark = Color(0xFF101011)
private val Panel = Color(0xFF22211F)
private val TextColor = Color(0xFFF6F4EF)
private val Muted = Color(0xFFB6B4B0)
private val Danger = Color(0xFFFF8A80)
private val tabs = listOf("Dashboard", "Sales", "Expenses", "Customers", "Appointments", "Services", "Reports")
private val suggestedPrices = listOf(
    "Fade" to 100.0, "Beard Trim" to 50.0, "School Cuts" to 50.0,
    "Balding" to 100.0, "Balding + Facial Scrub" to 200.0,
    "Ear Piercing" to 250.0, "Dreadlocks Retouch" to 500.0,
    "Artificial Locks" to 2000.0, "Starter Locks" to 1000.0,
    "Facial Scrub" to 100.0, "Fade + Hair Rolling" to 150.0,
    "Hair Bleach" to 1000.0, "Hairline + Beard Trim" to 80.0
)
private const val BOOKING_MARKER = "BB_BOOKING_V1:"
private data class BookingMeta(val at: Long, val service: String, val status: String, val comment: String)
private val bookingStatuses = listOf("Booked", "Completed", "Cancelled")
private fun encodeBooking(booking: BookingMeta): String = BOOKING_MARKER + JSONObject().apply {
    put("at", booking.at)
    put("service", booking.service)
    put("status", booking.status)
    put("comment", booking.comment)
}.toString()
private fun decodeBooking(raw: String): BookingMeta? = try {
    if (!raw.startsWith(BOOKING_MARKER)) null else {
        val obj = JSONObject(raw.removePrefix(BOOKING_MARKER))
        val status = obj.getString("status")
        val whenAt = obj.getLong("at")
        if (status !in bookingStatuses || whenAt <= 0L) null
        else BookingMeta(whenAt, obj.optString("service"), status, obj.optString("comment"))
    }
} catch (_: Exception) { null }
private fun appointmentTime(value: Long): String =
    SimpleDateFormat("EEE, dd MMM yyyy • HH:mm", Locale.ENGLISH).format(Date(value))

private fun money(amount: Double) = "KES " + "%,.2f".format(Locale.US, amount)
private fun date(time: Long) = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(time))
private fun backupName() = "barber-beyond-${SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date())}.json"

private fun startOfToday(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun periodStart(period: String): Long = when (period) {
    "Today" -> startOfToday()
    "7 days" -> System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
    "Month" -> Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    else -> 0L
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Room.databaseBuilder(
            applicationContext, BusinessDatabase::class.java, "barber_beyond_local.db"
        ).build()
        setContent { Manager(db.records()) }
    }
}

@Composable
fun Manager(dao: RecordDao) {
    val records by dao.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var tab by remember { mutableStateOf("Dashboard") }
    var search by remember { mutableStateOf("") }
    var period by remember { mutableStateOf("Month") }
    var editing by remember { mutableStateOf<Record?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var editorType by remember { mutableStateOf("Sales") }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var payment by remember { mutableStateOf("Cash") }
    var formError by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<Record?>(null) }
    var restoreCandidate by remember { mutableStateOf<List<Record>?>(null) }
    var bookingAt by remember { mutableStateOf<Long?>(null) }
    var bookingService by remember { mutableStateOf("") }
    var bookingStatus by remember { mutableStateOf("Booked") }

    fun chooseBookingTime() {
        val initial = Calendar.getInstance().apply { bookingAt?.let { timeInMillis = it } }
        DatePickerDialog(context, { _, year, month, dayOfMonth ->
            val selected = Calendar.getInstance().apply {
                timeInMillis = initial.timeInMillis
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }
            TimePickerDialog(context, { _, hour, minute ->
                selected.set(Calendar.HOUR_OF_DAY, hour)
                selected.set(Calendar.MINUTE, minute)
                selected.set(Calendar.SECOND, 0)
                selected.set(Calendar.MILLISECOND, 0)
                bookingAt = selected.timeInMillis
            }, selected.get(Calendar.HOUR_OF_DAY), selected.get(Calendar.MINUTE), true).show()
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)).show()
    }

    fun notice(message: String) { scope.launch { snackbar.showSnackbar(message) } }
    fun openEditor(type: String, record: Record? = null) {
        editorType = type
        editing = record
        title = record?.title.orEmpty()
        amount = record?.let { if (type == "Sales" || type == "Expenses") it.amount.toString() else "" }.orEmpty()
        val booking = if (type == "Appointments") record?.let { decodeBooking(it.notes) } else null
        notes = if (type == "Appointments") booking?.comment ?: record?.notes.orEmpty() else record?.notes.orEmpty()
        bookingAt = booking?.at
        bookingService = booking?.service.orEmpty()
        bookingStatus = booking?.status ?: "Booked"
        payment = record?.payment ?: "Cash"
        formError = ""
        showEditor = true
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val snapshot = records.toList()
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val output = context.contentResolver.openOutputStream(uri)
                            ?: error("Cannot write to the selected location")
                        output.use { stream ->
                            stream.write(BackupCodec.encode(snapshot).toByteArray(Charsets.UTF_8))
                        }
                    }
                    notice("Backup saved (${snapshot.size} records). Keep the JSON file private.")
                } catch (e: Exception) {
                    notice("Backup failed: ${e.message ?: "unknown error"}")
                }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val imported = withContext(Dispatchers.IO) {
                        val input = context.contentResolver.openInputStream(uri)
                            ?: error("Cannot read the selected file")
                        val raw = input.bufferedReader(Charsets.UTF_8).use { reader ->
                            // Avoid loading arbitrarily large documents into memory.
                            reader.readText().also { require(it.length <= 15_000_000) { "Backup is too large" } }
                        }
                        BackupCodec.decode(raw)
                    }
                    restoreCandidate = imported
                } catch (e: Exception) {
                    notice("Restore file rejected: ${e.message ?: "invalid JSON"}")
                }
            }
        }
    }

    val sales = records.filter { it.type == "Sales" }
    val expenses = records.filter { it.type == "Expenses" }
    val services = records.filter { it.type == "Services" }.sortedBy { it.title.lowercase(Locale.US) }
    val upcomingBookings = records.asSequence()
        .filter { it.type == "Appointments" }
        .mapNotNull { r -> decodeBooking(r.notes)?.let { b -> r to b } }
        .filter { (_, b) -> b.status == "Booked" && b.at >= System.currentTimeMillis() }
        .sortedBy { (_, b) -> b.at }.toList()
    val todayStart = startOfToday()
    val todaySales = sales.filter { it.createdAt >= todayStart }.sumOf { it.amount }
    val todayExpenses = expenses.filter { it.createdAt >= todayStart }.sumOf { it.amount }
    val periodRecords = records.filter { it.createdAt >= periodStart(period) }
    val periodSales = periodRecords.filter { it.type == "Sales" }
    val periodExpenses = periodRecords.filter { it.type == "Expenses" }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Gold, onPrimary = Dark, background = Dark, surface = Panel,
            onBackground = TextColor, onSurface = TextColor, error = Danger
        )
    ) {
        Scaffold(
            containerColor = Dark,
            snackbarHost = { SnackbarHost(snackbar) }
        ) { contentPadding ->
            Column(Modifier.fillMaxSize().padding(contentPadding).background(Dark)) {
                Column(Modifier.fillMaxWidth().background(Panel).padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text("BARBER & BEYOND", color = Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("OFFLINE MANAGER  •  ELGON LABS  •  v1.3", color = Muted, fontSize = 11.sp)
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
                    tabs.chunked(4).forEach { rowTabs ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            rowTabs.forEach { item ->
                                val label = if (item == "Appointments") "Bookings" else item
                                Button(
                                    onClick = { tab = item; search = "" },
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (tab == item) Gold else Panel,
                                        contentColor = if (tab == item) Dark else TextColor
                                    )
                                ) {
                                    Text(label, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
                Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(tab, color = TextColor, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.weight(1f))
                        if (tab in listOf("Sales", "Expenses", "Customers", "Appointments", "Services")) {
                            Button(onClick = { openEditor(tab) }) { Text("+ Add") }
                        } else if (tab == "Dashboard") {
                            Button(onClick = { openEditor("Sales") }) { Text("+ Sale") }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    when (tab) {
                        "Dashboard" -> {
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    StatCard("Today's sales", money(todaySales), Modifier.weight(1f))
                                    StatCard("Today's expenses", money(todayExpenses), Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    StatCard("Today's profit*", money(todaySales - todayExpenses), Modifier.weight(1f))
                                    StatCard("Customers", records.count { it.type == "Customers" }.toString(), Modifier.weight(1f))
                                }
                                val weekStart = periodStart("7 days")
                                val weekSaleTotal = sales.filter { it.createdAt >= weekStart }.sumOf { it.amount }
                                val weekCostTotal = expenses.filter { it.createdAt >= weekStart }.sumOf { it.amount }
                                val monthStart = periodStart("Month")
                                val monthSaleTotal = sales.filter { it.createdAt >= monthStart }.sumOf { it.amount }
                                val monthCostTotal = expenses.filter { it.createdAt >= monthStart }.sumOf { it.amount }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    StatCard("7-day profit*", money(weekSaleTotal - weekCostTotal), Modifier.weight(1f))
                                    StatCard("Monthly profit*", money(monthSaleTotal - monthCostTotal), Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    StatCard("Upcoming bookings", upcomingBookings.size.toString(), Modifier.weight(1f))
                                    StatCard("Listed services", services.size.toString(), Modifier.weight(1f))
                                }
                                upcomingBookings.firstOrNull()?.let { (customer, booking) ->
                                    Text("Next: ${customer.title} • ${appointmentTime(booking.at)}", color = Gold, fontSize = 12.sp)
                                    Spacer(Modifier.height(8.dp))
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("Recent activity", color = TextColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(8.dp))
                                if (records.isEmpty()) {
                                    Text("No activity yet. Add your first sale to get started.", color = Muted)
                                } else {
                                    records.take(6).forEach { record ->
                                        RecentRecord(record)
                                        Spacer(Modifier.height(6.dp))
                                    }
                                }
                                Text("* Estimated profit = recorded sales − recorded expenses.", fontSize = 11.sp, color = Muted)
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                        "Reports" -> {
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("Today", "7 days", "Month", "All").forEach { value ->
                                        FilterChip(selected = period == value, onClick = { period = value }, label = { Text(value) })
                                    }
                                }
                                StatCard("Sales (${period.lowercase(Locale.US)})", money(periodSales.sumOf { it.amount }), Modifier.fillMaxWidth())
                                StatCard("Expenses", money(periodExpenses.sumOf { it.amount }), Modifier.fillMaxWidth())
                                StatCard("Estimated profit*", money(periodSales.sumOf { it.amount } - periodExpenses.sumOf { it.amount }), Modifier.fillMaxWidth())
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    StatCard("Cash sales", money(periodSales.filter { it.payment == "Cash" }.sumOf { it.amount }), Modifier.weight(1f))
                                    StatCard("M-Pesa sales", money(periodSales.filter { it.payment == "M-Pesa" }.sumOf { it.amount }), Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("Backup & restore", color = TextColor, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { exportLauncher.launch(backupName()) }, modifier = Modifier.weight(1f)) {
                                        Text("Export JSON", fontSize = 12.sp)
                                    }
                                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f)) {
                                        Text("Restore JSON", fontSize = 12.sp)
                                    }
                                }
                                Text("Export regularly. JSON backups are not encrypted. Keep them private. Restore replaces all existing records after confirmation.", color = Muted, fontSize = 12.sp)
                                Spacer(Modifier.height(10.dp))
                                Text("* Estimates exclude unrecorded costs and taxes. M-Pesa entries are manual, not payment verification.", color = Muted, fontSize = 11.sp)
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                        else -> {
                            if (tab == "Services") {
                                Text("Manage prices below. Tap Sell to copy a service into a new sale.", color = Muted, fontSize = 12.sp)
                                if (services.isEmpty()) {
                                    OutlinedButton(onClick = {
                                        scope.launch {
                                            try {
                                                withContext(Dispatchers.IO) {
                                                    suggestedPrices.forEach { (name, price) ->
                                                        dao.insert(Record(type = "Services", title = name, amount = price))
                                                    }
                                                }
                                                notice("Suggested prices loaded. Review and edit them.")
                                            } catch (e: Exception) { notice("Could not load prices: ${e.message}") }
                                        }
                                    }) { Text("Load suggested studio prices") }
                                }
                            }
                            if (tab == "Appointments") {
                                Text("Choose a date and time for new bookings. Older bookings remain available as legacy records.", color = Muted, fontSize = 12.sp)
                            }
                            OutlinedTextField(
                                value = search, onValueChange = { search = it },
                                modifier = Modifier.fillMaxWidth(), singleLine = true,
                                label = { Text("Search $tab") }
                            )
                            Spacer(Modifier.height(10.dp))
                            val matching = records.filter {
                                it.type == tab && (search.isBlank() ||
                                    it.title.contains(search, ignoreCase = true) ||
                                    it.notes.contains(search, ignoreCase = true) ||
                                    it.payment.contains(search, ignoreCase = true))
                            }.let { results ->
                                if (tab == "Appointments") results.sortedWith(
                                    compareBy<Record> {
                                        val b = decodeBooking(it.notes)
                                        b == null || b.status != "Booked" || b.at < System.currentTimeMillis()
                                    }.thenBy { decodeBooking(it.notes)?.at ?: Long.MAX_VALUE }
                                ) else results
                            }
                            if (matching.isEmpty()) {
                                Text(if (search.isBlank()) "No $tab records yet. Tap + Add." else "No matching records.", color = Muted)
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 14.dp)
                                ) {
                                    items(matching, key = { it.id }) { record ->
                                        Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                                Text(record.title, fontWeight = FontWeight.SemiBold, color = TextColor)
                                                if (tab in listOf("Sales", "Expenses")) {
                                                    Text("${money(record.amount)}  •  ${record.payment}", color = Gold)
                                                } else if (tab == "Services") {
                                                    Text(money(record.amount), color = Gold)
                                                }
                                                if (tab == "Appointments") {
                                                    val booking = decodeBooking(record.notes)
                                                    if (booking == null) {
                                                        Text("Legacy booking • choose a time when editing", color = Muted, fontSize = 12.sp)
                                                        if (record.notes.isNotBlank()) Text(record.notes, color = Muted, fontSize = 12.sp)
                                                    } else {
                                                        Text(appointmentTime(booking.at), color = Gold, fontSize = 13.sp)
                                                        Text("${booking.status}" + if (booking.service.isNotBlank()) " • ${booking.service}" else "", color = Muted, fontSize = 12.sp)
                                                        if (booking.comment.isNotBlank()) Text(booking.comment, color = Muted, fontSize = 12.sp)
                                                    }
                                                } else if (record.notes.isNotBlank()) Text(record.notes, color = Muted, fontSize = 12.sp)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("Added ${date(record.createdAt)}", color = Muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                                    if (tab == "Services") {
                                                        TextButton(onClick = {
                                                            tab = "Sales"
                                                            openEditor("Sales")
                                                            title = record.title
                                                            amount = record.amount.toString()
                                                        }) { Text("Sell") }
                                                    }
                                                    TextButton(onClick = { openEditor(tab, record) }) { Text("Edit") }
                                                    TextButton(onClick = { deleteTarget = record }) { Text("Delete", color = Danger) }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Text(
                    "Powered by ELGON LABS  •  Technology • Innovation • Digital Solutions",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp),
                    color = Muted, fontSize = 10.sp
                )
            }
        }

        if (showEditor) {
            AlertDialog(
                onDismissRequest = { showEditor = false },
                title = { Text(if (editing == null) "Add $editorType" else "Edit $editorType") },
                text = {
                    Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = title, onValueChange = { title = it; formError = "" },
                            label = { Text(when (editorType) {
                                "Customers" -> "Customer name"
                                "Appointments" -> "Customer name"
                                "Services" -> "Service name"
                                else -> "Service / description"
                            }) }, singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        if (editorType == "Sales" && services.isNotEmpty()) {
                            Text("Quick fill from your service prices", color = Muted, fontSize = 12.sp)
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                services.forEach { service ->
                                    AssistChip(onClick = { title = service.title; amount = service.amount.toString() }, label = { Text(service.title) })
                                }
                            }
                        }
                        if (editorType == "Appointments") {
                            OutlinedButton(onClick = { chooseBookingTime() }, modifier = Modifier.fillMaxWidth()) {
                                Text(bookingAt?.let(::appointmentTime) ?: "Choose appointment date & time")
                            }
                            OutlinedTextField(bookingService, { bookingService = it }, label = { Text("Service (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            if (services.isNotEmpty()) {
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    services.forEach { service ->
                                        AssistChip(onClick = { bookingService = service.title }, label = { Text(service.title) })
                                    }
                                }
                            }
                            Text("Booking status", color = Muted, fontSize = 12.sp)
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                bookingStatuses.forEach { status ->
                                    FilterChip(selected = bookingStatus == status, onClick = { bookingStatus = status }, label = { Text(status) })
                                }
                            }
                        }
                        if (editorType == "Sales" || editorType == "Expenses" || editorType == "Services") {
                            OutlinedTextField(
                                value = amount, onValueChange = { amount = it; formError = "" },
                                label = { Text("Amount (KES)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true, modifier = Modifier.fillMaxWidth()
                            )
                            if (editorType != "Services") {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = payment == "Cash", onClick = { payment = "Cash" })
                                    Text("Cash", color = TextColor)
                                    Spacer(Modifier.width(12.dp))
                                    RadioButton(selected = payment == "M-Pesa", onClick = { payment = "M-Pesa" })
                                    Text("M-Pesa", color = TextColor)
                                }
                            }
                        }
                        OutlinedTextField(
                            value = notes, onValueChange = { notes = it; formError = "" },
                            label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth()
                        )
                        if (formError.isNotBlank()) Text(formError, color = Danger, fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val parsed = if (editorType in listOf("Sales", "Expenses", "Services")) amount.toDoubleOrNull() else 0.0
                        when {
                            title.isBlank() || title.length > 250 -> formError = "Enter a title (up to 250 characters)."
                            notes.length > (if (editorType == "Appointments") 3_000 else 4_000) -> formError = "Notes are too long."
                            bookingService.length > 250 -> formError = "Service name is too long."
                            editorType == "Appointments" && bookingAt == null -> formError = "Choose an appointment date and time."
                            parsed == null || !parsed.isFinite() || parsed < 0 -> formError = "Enter a valid, non-negative amount."
                            else -> {
                                val old = editing
                                val savedNotes = if (editorType == "Appointments") {
                                    encodeBooking(BookingMeta(bookingAt!!, bookingService.trim(), bookingStatus, notes.trim()))
                                } else notes.trim()
                                val updated = (old ?: Record(type = editorType, title = title.trim())).copy(
                                    title = title.trim(), amount = parsed, payment = payment, notes = savedNotes
                                )
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            if (old == null) dao.insert(updated) else dao.update(updated)
                                        }
                                        notice(if (old == null) "Record added" else "Record updated")
                                    } catch (e: Exception) { notice("Save failed: ${e.message}") }
                                }
                                showEditor = false
                            }
                        }
                    }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { showEditor = false }) { Text("Cancel") } }
            )
        }

        deleteTarget?.let { candidate ->
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text("Delete this record?") },
                text = { Text("${candidate.title} will be permanently removed from this app. Export a backup first if needed.") },
                confirmButton = {
                    TextButton(onClick = {
                        deleteTarget = null
                        scope.launch {
                            try { withContext(Dispatchers.IO) { dao.delete(candidate) }; notice("Record deleted") }
                            catch (e: Exception) { notice("Delete failed: ${e.message}") }
                        }
                    }) { Text("Delete", color = Danger) }
                },
                dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
            )
        }

        restoreCandidate?.let { candidate ->
            AlertDialog(
                onDismissRequest = { restoreCandidate = null },
                title = { Text("Replace all records?") },
                text = { Text("The backup has ${candidate.size} records. Restoring will REPLACE the ${records.size} records currently in this app. Export your existing data first. Continue?") },
                confirmButton = {
                    TextButton(onClick = {
                        restoreCandidate = null
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { dao.replaceAll(candidate) }
                                notice("Restore complete: ${candidate.size} records")
                            } catch (e: Exception) { notice("Restore failed: ${e.message}") }
                        }
                    }) { Text("Replace records", color = Danger) }
                },
                dismissButton = { TextButton(onClick = { restoreCandidate = null }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier.padding(bottom = 10.dp), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(label, color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(5.dp))
            Text(value, color = Gold, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 2)
        }
    }
}

@Composable
private fun RecentRecord(record: Record) {
    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(10.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(record.title, color = TextColor, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${record.type}  •  ${date(record.createdAt)}", color = Muted, fontSize = 11.sp)
        }
        if (record.type == "Sales" || record.type == "Expenses") {
            Text(money(record.amount), color = Gold, fontSize = 12.sp)
        }
    }
}

package ke.elgonlabs.barberbeyond

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.room.Room
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

private val Gold = Color(0xFFE8BD64)
private val Dark = Color(0xFF101011)
private val Panel = Color(0xFF1C1B19)
private val Text = Color(0xFFF6F4EF)
private val Muted = Color(0xFFB6B4B0)
private val tabs = listOf("Dashboard", "Sales", "Expenses", "Customers", "Appointments", "Reports")
private fun money(n: Double) = "KES " + "%,.2f".format(Locale.US, n)
private fun day(t: Long) = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(t))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Room.databaseBuilder(applicationContext, BusinessDatabase::class.java, "barber_beyond_local.db").build()
        setContent { Manager(db.records()) }
    }
}

@Composable
fun Manager(dao: RecordDao) {
    val records by dao.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf("Dashboard") }
    var showAdd by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var payment by remember { mutableStateOf("Cash") }
    val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
    fun isToday(r: Record) = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(r.createdAt)) == today
    val sales = records.filter { it.type == "Sales" }
    val expenses = records.filter { it.type == "Expenses" }
    val totalSales = sales.sumOf { it.amount }
    val totalExpenses = expenses.sumOf { it.amount }

    MaterialTheme(colorScheme = darkColorScheme(primary = Gold, background = Dark, surface = Panel, onBackground = Text, onSurface = Text)) {
        Column(Modifier.fillMaxSize().background(Dark)) {
            Column(Modifier.fillMaxWidth().background(Panel).padding(18.dp)) {
                Text("BARBER & BEYOND", color = Gold, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                Text("OFFLINE MANAGER  •  ELGON LABS", color = Muted, fontSize = 11.sp)
            }
            Column(Modifier.horizontalScroll(rememberScrollState()).padding(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tabs.forEach { item ->
                        Button(onClick = { tab = item }, colors = ButtonDefaults.buttonColors(containerColor = if (tab == item) Gold else Panel, contentColor = if (tab == item) Dark else Text)) { Text(item, fontSize = 12.sp) }
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(tab, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    if (tab in listOf("Sales","Expenses","Customers","Appointments")) {
                        Button(onClick = { title = ""; amount = ""; notes = ""; payment = "Cash"; showAdd = true }) { Text("+ Add") }
                    }
                }
                Spacer(Modifier.height(12.dp))
                when (tab) {
                    "Dashboard" -> {
                        SummaryCard("Today's sales", money(sales.filter(::isToday).sumOf { it.amount }))
                        SummaryCard("Today's expenses", money(expenses.filter(::isToday).sumOf { it.amount }))
                        SummaryCard("Today's estimated profit", money(sales.filter(::isToday).sumOf { it.amount } - expenses.filter(::isToday).sumOf { it.amount }))
                        SummaryCard("Customer records", records.count { it.type == "Customers" }.toString())
                    }
                    "Reports" -> {
                        SummaryCard("All-time sales", money(totalSales))
                        SummaryCard("All-time expenses", money(totalExpenses))
                        SummaryCard("Estimated profit", money(totalSales - totalExpenses))
                        SummaryCard("Cash sales", money(sales.filter { it.payment == "Cash" }.sumOf { it.amount }))
                        SummaryCard("M-Pesa sales", money(sales.filter { it.payment == "M-Pesa" }.sumOf { it.amount }))
                        Text("Figures are calculated from records stored on this phone.", color = Muted, fontSize = 12.sp)
                    }
                    else -> {
                        val filtered = records.filter { it.type == tab }
                        if (filtered.isEmpty()) Text("No $tab records yet. Tap + Add to get started.", color = Muted)
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                            items(filtered, key = { it.id }) { r ->
                                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(12.dp)) {
                                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(r.title, fontWeight = FontWeight.SemiBold)
                                            if (tab in listOf("Sales","Expenses")) Text("${money(r.amount)} • ${r.payment}", color = Gold)
                                            if (r.notes.isNotBlank()) Text(r.notes, color = Muted, fontSize = 12.sp)
                                            Text(day(r.createdAt), color = Muted, fontSize = 11.sp)
                                        }
                                        TextButton(onClick = { scope.launch { dao.delete(r) } }) { Text("Delete") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Text("Powered by ELGON LABS • Technology • Innovation • Digital Solutions", modifier = Modifier.fillMaxWidth().padding(12.dp), color = Muted, fontSize = 10.sp)
        }
        if (showAdd) AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add $tab record") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(title, { title = it }, label = { Text(if (tab == "Appointments") "Customer and appointment time" else if (tab == "Customers") "Customer name" else "Service / description") }, singleLine = true)
                    if (tab in listOf("Sales","Expenses")) {
                        OutlinedTextField(amount, { amount = it }, label = { Text("Amount (KES)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = payment == "Cash", onClick = { payment = "Cash" }); Text("Cash")
                            RadioButton(selected = payment == "M-Pesa", onClick = { payment = "M-Pesa" }); Text("M-Pesa")
                        }
                    }
                    OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsed = amount.toDoubleOrNull()
                    if (title.isNotBlank() && (tab !in listOf("Sales","Expenses") || (parsed != null && parsed >= 0))) {
                        scope.launch { dao.insert(Record(type = tab, title = title.trim(), amount = parsed ?: 0.0, payment = payment, notes = notes.trim())) }
                        showAdd = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}
@Composable
private fun SummaryCard(label: String, value: String) {
    Card(Modifier.fillMaxWidth().padding(bottom = 10.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
        Column(Modifier.padding(18.dp)) {
            Text(label, color = Muted, fontSize = 13.sp)
            Text(value, color = Gold, fontWeight = FontWeight.Bold, fontSize = 23.sp)
        }
    }
}

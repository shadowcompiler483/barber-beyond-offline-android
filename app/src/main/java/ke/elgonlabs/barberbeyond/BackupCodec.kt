package ke.elgonlabs.barberbeyond

import org.json.JSONArray
import org.json.JSONObject

/** Plain JSON for a user-controlled, offline export. Keep backups private. */
internal object BackupCodec {
    private const val FORMAT = "barber-beyond-offline-backup"
    private const val SCHEMA = 2
    private const val MAX_RECORDS = 50_000
    private val ALLOWED_TYPES = setOf("Sales", "Expenses", "Customers", "Appointments", "Services")

    fun encode(records: List<Record>): String {
        val rows = JSONArray()
        records.forEach { record ->
            rows.put(JSONObject().apply {
                put("id", record.id)
                put("type", record.type)
                put("title", record.title)
                put("amount", record.amount)
                put("payment", record.payment)
                put("notes", record.notes)
                put("createdAt", record.createdAt)
            })
        }
        return JSONObject().apply {
            put("format", FORMAT)
            put("schemaVersion", SCHEMA)
            put("exportedAt", System.currentTimeMillis())
            put("records", rows)
        }.toString(2)
    }

    fun decode(json: String): List<Record> {
        require(json.length <= 15_000_000) { "Backup is too large" }
        val root = JSONObject(json)
        require(root.optString("format") == FORMAT && root.optInt("schemaVersion") in 1..SCHEMA) {
            "This is not a supported Barber & Beyond backup"
        }
        val rows = root.getJSONArray("records")
        require(rows.length() <= MAX_RECORDS) { "Backup contains too many records" }
        val ids = HashSet<Long>()
        val restored = ArrayList<Record>(rows.length())
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            val id = row.getLong("id")
            val type = row.getString("type")
            val title = row.getString("title")
            val amount = row.getDouble("amount")
            val payment = row.getString("payment")
            val notes = row.getString("notes")
            val createdAt = row.getLong("createdAt")
            require(id > 0 && ids.add(id)) { "Invalid or duplicate record ID" }
            require(type in ALLOWED_TYPES) { "Invalid record category" }
            require(title.isNotBlank() && title.length <= 250) { "Invalid record title" }
            require(amount.isFinite() && amount >= 0.0) { "Invalid record amount" }
            require(payment in setOf("Cash", "M-Pesa")) { "Invalid payment type" }
            require(notes.length <= 4_000) { "Record notes are too long" }
            require(createdAt > 0) { "Invalid record date" }
            restored += Record(id, type, title, amount, payment, notes, createdAt)
        }
        return restored
    }
}

package ke.elgonlabs.barberbeyond

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "records")
data class Record(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val title: String,
    val amount: Double = 0.0,
    val payment: String = "Cash",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface RecordDao {
    @Query("SELECT * FROM records ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<Record>>

    @Insert suspend fun insert(record: Record)
    @Insert suspend fun insertAll(records: List<Record>)
    @Update suspend fun update(record: Record)
    @Delete suspend fun delete(record: Record)
    @Query("DELETE FROM records") suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(records: List<Record>) {
        deleteAll()
        insertAll(records)
    }
}

// Schema is intentionally unchanged from the first app. Existing record fields and IDs
// are compatible with the v1.0 backup format if they can be exported via ADB.
@Database(entities = [Record::class], version = 1, exportSchema = false)
abstract class BusinessDatabase : RoomDatabase() {
    abstract fun records(): RecordDao
}

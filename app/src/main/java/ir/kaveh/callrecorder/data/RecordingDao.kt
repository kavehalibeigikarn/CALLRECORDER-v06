package ir.kaveh.callrecorder.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {

    @Query("SELECT * FROM recordings ORDER BY startTime DESC")
    fun observeAll(): Flow<List<Recording>>

    @Query(
        "SELECT * FROM recordings WHERE " +
            "contactName LIKE '%' || :q || '%' OR phoneNumber LIKE '%' || :q || '%' " +
            "ORDER BY startTime DESC"
    )
    fun search(q: String): Flow<List<Recording>>

    @Insert
    suspend fun insert(recording: Recording): Long

    @Delete
    suspend fun delete(recording: Recording)
}

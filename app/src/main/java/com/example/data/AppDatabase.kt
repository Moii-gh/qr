package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "scans")
data class ScanResult(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val rawValue: String,
    val format: Int,
    val type: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    
    // Additional requested fields
    val rawContent: String = "",
    val detectedType: String = "",
    val createdAt: Long = 0L,
    val favoriteAt: Long = 0L
)

@Dao
interface ScanDao {
    @Query("SELECT * FROM scans ORDER BY isFavorite DESC, CASE WHEN createdAt > 0 THEN createdAt ELSE timestamp END DESC, id DESC")
    fun getAllScans(): Flow<List<ScanResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanResult): Long

    @Query("DELETE FROM scans WHERE id = :id")
    suspend fun deleteScan(id: Int)
    
    @Query("UPDATE scans SET isFavorite = :isFav, favoriteAt = :favAt WHERE id = :id")
    suspend fun updateFavorite(id: Int, isFav: Boolean, favAt: Long)
}

@Database(entities = [ScanResult::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns to the existing table
                db.execSQL("ALTER TABLE scans ADD COLUMN rawContent TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE scans ADD COLUMN detectedType TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE scans ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE scans ADD COLUMN favoriteAt INTEGER NOT NULL DEFAULT 0")
                
                // Backfill values based on existing columns safely
                db.execSQL("UPDATE scans SET rawContent = rawValue, createdAt = timestamp")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "scan_database")
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}

class ScanRepository(private val scanDao: ScanDao) {
    val allScans: Flow<List<ScanResult>> = scanDao.getAllScans()

    suspend fun insert(scan: ScanResult) = scanDao.insertScan(scan)
    suspend fun delete(id: Int) = scanDao.deleteScan(id)
    suspend fun setFavorite(id: Int, isFav: Boolean) {
        val favoriteTime = if (isFav) System.currentTimeMillis() else 0L
        scanDao.updateFavorite(id, isFav, favoriteTime)
    }
}

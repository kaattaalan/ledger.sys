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
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Entity(tableName = "category_cores")
data class CategoryCore(
    @PrimaryKey val systemKey: String,
    val name: String,
    val iconName: String,
    val isSystemProtected: Boolean = false
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val currency: String,
    val memo: String,
    val categoryKey: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface CategoryCoreDao {
    @Query("SELECT * FROM category_cores")
    fun getAllCoresFlow(): Flow<List<CategoryCore>>

    @Query("SELECT * FROM category_cores")
    suspend fun getAllCores(): List<CategoryCore>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCore(core: CategoryCore)

    @Query("DELETE FROM category_cores WHERE systemKey = :key AND isSystemProtected = 0")
    suspend fun deleteCoreByKey(key: String)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()
}

@Database(entities = [CategoryCore::class, Transaction::class], version = 1, exportSchema = false)
abstract class LedgerDatabase : RoomDatabase() {
    abstract fun categoryCoreDao(): CategoryCoreDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: LedgerDatabase? = null

        fun getDatabase(context: Context): LedgerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LedgerDatabase::class.java,
                    "ledger_sys_database"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Prepopulate default system-protected cores
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getDatabase(context)
                            val dao = database.categoryCoreDao()
                            dao.insertCore(CategoryCore("food", "FOOD", "restaurant", true))
                            dao.insertCore(CategoryCore("travel", "TRAVEL", "flight", true))
                            dao.insertCore(CategoryCore("bills", "BILLS", "receipt", true))
                            dao.insertCore(CategoryCore("rec", "REC", "sports_esports", true))
                            dao.insertCore(CategoryCore("shop", "SHOP", "shopping_cart", true))
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                logInstance(instance)
            }
        }

        private fun logInstance(db: LedgerDatabase): LedgerDatabase {
            INSTANCE = db
            return db
        }
    }
}

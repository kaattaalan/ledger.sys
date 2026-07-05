package com.example.data

import kotlinx.coroutines.flow.Flow

class LedgerRepository(
    private val categoryCoreDao: CategoryCoreDao,
    private val transactionDao: TransactionDao
) {
    val allCores: Flow<List<CategoryCore>> = categoryCoreDao.getAllCoresFlow()
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactionsFlow()

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun insertCore(core: CategoryCore) {
        categoryCoreDao.insertCore(core)
    }

    suspend fun deleteCoreByKey(key: String) {
        categoryCoreDao.deleteCoreByKey(key)
    }

    suspend fun clearAllTransactions() {
        transactionDao.clearAllTransactions()
    }

    suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteTransaction(id)
    }
}

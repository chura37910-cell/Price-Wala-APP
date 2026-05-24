package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%'")
    fun searchProducts(query: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE stock <= :lowStockLimit")
    fun getLowStockProducts(lowStockLimit: Int = 5): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("DELETE FROM products WHERE barcode = :barcode")
    suspend fun deleteProductByBarcode(barcode: String)

    @Query("SELECT COUNT(*) FROM products")
    fun getProductCount(): Flow<Int>

    // --- SALE RECORD QUERIES ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleRecord)

    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<SaleRecord>>

    @Query("SELECT * FROM sales WHERE timestamp >= :startOfDay ORDER BY timestamp DESC")
    fun getTodaySales(startOfDay: Long): Flow<List<SaleRecord>>

    @Delete
    suspend fun deleteSale(sale: SaleRecord)

    @Query("DELETE FROM sales")
    suspend fun clearAllSales()

    // --- SCAN HISTORY QUERIES ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanHistory(entry: ScanHistoryEntry)

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC LIMIT 20")
    fun getRecentScans(): Flow<List<ScanHistoryEntry>>

    @Query("DELETE FROM scan_history")
    suspend fun clearScanHistory()
}

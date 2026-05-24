package com.example.data

import kotlinx.coroutines.flow.Flow

class ProductRepository(private val productDao: ProductDao) {

    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    
    val lowStockProducts: Flow<List<Product>> = productDao.getLowStockProducts()

    // --- SALES ---
    val allSales: Flow<List<SaleRecord>> = productDao.getAllSales()

    fun getTodaySales(startOfDay: Long): Flow<List<SaleRecord>> {
        return productDao.getTodaySales(startOfDay)
    }

    suspend fun insertSale(sale: SaleRecord) {
        productDao.insertSale(sale)
    }

    suspend fun deleteSale(sale: SaleRecord) {
        productDao.deleteSale(sale)
    }

    suspend fun clearAllSales() {
        productDao.clearAllSales()
    }

    // --- SCAN HISTORY ---
    val recentScans: Flow<List<ScanHistoryEntry>> = productDao.getRecentScans()

    suspend fun insertScanHistory(entry: ScanHistoryEntry) {
        productDao.insertScanHistory(entry)
    }

    suspend fun clearScanHistory() {
        productDao.clearScanHistory()
    }

    fun searchProducts(query: String): Flow<List<Product>> {
        return productDao.searchProducts(query)
    }

    suspend fun getProductByBarcode(barcode: String): Product? {
        return productDao.getProductByBarcode(barcode)
    }

    suspend fun insertProduct(product: Product) {
        productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: Product) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: Product) {
        productDao.deleteProduct(product)
    }

    suspend fun deleteProductByBarcode(barcode: String) {
        productDao.deleteProductByBarcode(barcode)
    }
}

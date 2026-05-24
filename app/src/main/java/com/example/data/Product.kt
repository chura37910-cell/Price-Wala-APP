package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "products")
data class Product(
    @PrimaryKey
    val barcode: String,
    val name: String,
    val category: String,
    val buyPrice: Double,
    val salePrice: Double,
    val stock: Int,
    val expiryDate: String, // format YYYY-MM-DD
    val lastUpdated: Long = System.currentTimeMillis()
) : Serializable {
    // Computed property for profit calculation
    val profit: Double
        get() = salePrice - buyPrice
}

@Entity(tableName = "sales")
data class SaleRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val barcode: String,
    val productName: String,
    val quantity: Int,
    val salePrice: Double,
    val buyPrice: Double,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable {
    val profit: Double
        get() = (salePrice - buyPrice) * quantity
    val totalAmount: Double
        get() = salePrice * quantity
}

@Entity(tableName = "scan_history")
data class ScanHistoryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val barcode: String,
    val productName: String,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable


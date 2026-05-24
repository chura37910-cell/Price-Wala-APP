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

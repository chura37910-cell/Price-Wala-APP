package com.example.ui

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PreferencesManager
import com.example.data.Product
import com.example.data.SaleRecord
import com.example.data.ScanHistoryEntry
import com.example.data.ProductDatabase
import com.example.data.ProductRepository
import com.example.util.VoiceAssistant
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ProductViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ProductDatabase.getDatabase(application)
    private val repository = ProductRepository(db.productDao)
    val prefs = PreferencesManager(application)
    val voiceAssistant = VoiceAssistant(application)
    private var toneGenerator: ToneGenerator? = null

    // Preferences reactive states
    private val _isLoggedIn = MutableStateFlow(prefs.isLoggedIn)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _language = MutableStateFlow(prefs.language)
    val language: StateFlow<String> = _language.asStateFlow()

    private val _voiceEnabled = MutableStateFlow(prefs.voiceEnabled)
    val voiceEnabled: StateFlow<Boolean> = _voiceEnabled.asStateFlow()

    private val _darkMode = MutableStateFlow(prefs.darkMode)
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _shopName = MutableStateFlow(prefs.shopName)
    val shopName: StateFlow<String> = _shopName.asStateFlow()

    // Search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Dynamic product lists
    val allProducts: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredProducts: StateFlow<List<Product>> = combine(allProducts, _searchQuery) { products, query ->
        if (query.isBlank()) {
            products
        } else {
            products.filter {
                it.name.contains(query, ignoreCase = true) || 
                it.barcode.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Stock alert list (low stock is stock <= 5)
    val lowStockProducts: StateFlow<List<Product>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Near-expiry alert list (within 30 days)
    val nearExpiryProducts: StateFlow<List<Product>> = allProducts.map { products ->
        products.filter { isExpiryNear(it.expiryDate) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Scanned product notification/success detail state
    private val _scannedProduct = MutableStateFlow<Product?>(null)
    val scannedProduct: StateFlow<Product?> = _scannedProduct.asStateFlow()

    // --- SALES AND DISPOSITION SYSTEM ---
    val allSales: StateFlow<List<SaleRecord>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getTodayStartTimestamp(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    val todaySales: StateFlow<List<SaleRecord>> = repository.allSales.map { salesList ->
        val startOfToday = getTodayStartTimestamp()
        salesList.filter { it.timestamp >= startOfToday }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- RECENT SCAN HISTORY SYSTEM ---
    val recentScans: StateFlow<List<ScanHistoryEntry>> = repository.recentScans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Helper to determine if expiration date is within 30 days
    private fun isExpiryNear(dateStr: String): Boolean {
        if (dateStr.isBlank()) return false
        val formats = listOf("yyyy-MM-dd", "dd-MM-yyyy", "dd/MM/yyyy")
        var date: Date? = null
        for (f in formats) {
            try {
                date = SimpleDateFormat(f, Locale.getDefault()).parse(dateStr)
                if (date != null) break
            } catch (ignored: Exception) {}
        }

        if (date == null) return false
        val today = Calendar.getInstance().apply { 
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val thirtyDaysFromNow = Calendar.getInstance().apply {
            time = today
            add(Calendar.DAY_OF_YEAR, 30)
        }.time

        return date.after(today) && date.before(thirtyDaysFromNow) || date == today
    }

    // UI actions
    fun setLogin(status: Boolean, shop: String = "") {
        viewModelScope.launch {
            prefs.isLoggedIn = status
            if (shop.isNotBlank()) {
                prefs.shopName = shop
                _shopName.value = shop
            }
            _isLoggedIn.value = status
        }
    }

    fun updatePreferences(lang: String, voice: Boolean, dark: Boolean, shop: String) {
        prefs.language = lang
        prefs.voiceEnabled = voice
        prefs.darkMode = dark
        prefs.shopName = shop

        _language.value = lang
        _voiceEnabled.value = voice
        _darkMode.value = dark
        _shopName.value = shop
    }

    fun search(query: String) {
        _searchQuery.value = query
    }

    // Triggered barcode scan
    fun processScannedBarcode(barcode: String, onMatch: (Product) -> Unit, onNoMatch: (String) -> Unit) {
        viewModelScope.launch {
            val product = repository.getProductByBarcode(barcode)
            if (product != null) {
                // Beep sound
                playSuccessBeep()
                // TTS Speak
                voiceAssistant.speakProduct(product.name, product.salePrice)
                _scannedProduct.value = product
                
                // Add to scan history
                repository.insertScanHistory(
                    ScanHistoryEntry(
                        barcode = product.barcode,
                        productName = product.name
                    )
                )
                
                onMatch(product)
            } else {
                onNoMatch(barcode)
            }
        }
    }

    // Force add barcode search scan entry when manual search produces a match
    fun logScannedHistoryManual(product: Product) {
        viewModelScope.launch {
            repository.insertScanHistory(
                ScanHistoryEntry(
                    barcode = product.barcode,
                    productName = product.name
                )
            )
        }
    }

    fun clearScannedProduct() {
        _scannedProduct.value = null
    }

    fun playSuccessBeep() {
        if (prefs.voiceEnabled) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveProduct(product: Product) {
        viewModelScope.launch {
            repository.insertProduct(product)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    fun deleteProductByBarcode(barcode: String) {
        viewModelScope.launch {
            repository.deleteProductByBarcode(barcode)
        }
    }

    suspend fun getProduct(barcode: String): Product? {
        return repository.getProductByBarcode(barcode)
    }

    // --- NEW SALES OPERATIONS ---
    fun recordSale(product: Product, quantity: Int, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            if (product.stock >= quantity) {
                // Decrement stock
                val updatedProduct = product.copy(stock = product.stock - quantity)
                repository.insertProduct(updatedProduct)
                
                // Track Sale Record
                val sale = SaleRecord(
                    barcode = product.barcode,
                    productName = product.name,
                    quantity = quantity,
                    buyPrice = product.buyPrice,
                    salePrice = product.salePrice
                )
                repository.insertSale(sale)
                onComplete()
            }
        }
    }

    fun deleteSale(sale: SaleRecord) {
        viewModelScope.launch {
            // Option to replenish stock
            val product = repository.getProductByBarcode(sale.barcode)
            if (product != null) {
                repository.insertProduct(product.copy(stock = product.stock + sale.quantity))
            }
            repository.deleteSale(sale)
        }
    }

    fun clearAllSales() {
        viewModelScope.launch {
            repository.clearAllSales()
        }
    }

    // --- NEW SCAN HISTORY OPERATIONS ---
    fun clearScanHistory() {
        viewModelScope.launch {
            repository.clearScanHistory()
        }
    }

    // --- LOCAL OFFLINE BACKUP SYSTEM (PREVENT PHONE DATA LOSS) ---
    fun backupData(onComplete: (String?, Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val products = allProducts.value
                val sales = allSales.value
                val scans = recentScans.value
                
                val mainJson = org.json.JSONObject()
                
                // 1. Pack Products
                val prodsArray = org.json.JSONArray()
                for (p in products) {
                    val obj = org.json.JSONObject().apply {
                        put("barcode", p.barcode)
                        put("name", p.name)
                        put("category", p.category)
                        put("buyPrice", p.buyPrice)
                        put("salePrice", p.salePrice)
                        put("stock", p.stock)
                        put("expiryDate", p.expiryDate)
                        put("lastUpdated", p.lastUpdated)
                    }
                    prodsArray.put(obj)
                }
                mainJson.put("products", prodsArray)
                
                // 2. Pack Sales
                val salesArray = org.json.JSONArray()
                for (s in sales) {
                    val obj = org.json.JSONObject().apply {
                        put("barcode", s.barcode)
                        put("productName", s.productName)
                        put("quantity", s.quantity)
                        put("buyPrice", s.buyPrice)
                        put("salePrice", s.salePrice)
                        put("timestamp", s.timestamp)
                    }
                    salesArray.put(obj)
                }
                mainJson.put("sales", salesArray)
                
                // 3. Pack Scans
                val scansArray = org.json.JSONArray()
                for (sc in scans) {
                    val obj = org.json.JSONObject().apply {
                        put("barcode", sc.barcode)
                        put("productName", sc.productName)
                        put("timestamp", sc.timestamp)
                    }
                    scansArray.put(obj)
                }
                mainJson.put("scans", scansArray)
                
                val backupFile = java.io.File(getApplication<Application>().getExternalFilesDir(null), "pricewala_backup.json")
                backupFile.writeText(mainJson.toString(2))
                onComplete(backupFile.name, true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(e.localizedMessage, false)
            }
        }
    }

    fun restoreData(customJsonString: String?, onComplete: (String?, Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val file = java.io.File(getApplication<Application>().getExternalFilesDir(null), "pricewala_backup.json")
                val jsonString = customJsonString ?: if (file.exists()) file.readText() else null
                
                if (jsonString.isNullOrBlank()) {
                    onComplete("Backup file 'pricewala_backup.json' not found. Please create backup first.", false)
                    return@launch
                }
                
                val mainJson = org.json.JSONObject(jsonString)
                
                // Restore Products
                if (mainJson.has("products")) {
                    val prodsArray = mainJson.getJSONArray("products")
                    for (i in 0 until prodsArray.length()) {
                        val obj = prodsArray.getJSONObject(i)
                        val p = Product(
                            barcode = obj.getString("barcode"),
                            name = obj.getString("name"),
                            category = obj.getString("category"),
                            buyPrice = obj.getDouble("buyPrice"),
                            salePrice = obj.getDouble("salePrice"),
                            stock = obj.getInt("stock"),
                            expiryDate = obj.optString("expiryDate", ""),
                            lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis())
                        )
                        repository.insertProduct(p)
                    }
                }
                
                // Restore Sales
                if (mainJson.has("sales")) {
                    val salesArray = mainJson.getJSONArray("sales")
                    for (i in 0 until salesArray.length()) {
                        val obj = salesArray.getJSONObject(i)
                        val s = SaleRecord(
                            barcode = obj.getString("barcode"),
                            productName = obj.getString("productName"),
                            quantity = obj.getInt("quantity"),
                            buyPrice = obj.getDouble("buyPrice"),
                            salePrice = obj.getDouble("salePrice"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                        repository.insertSale(s)
                    }
                }
                
                // Restore Scans
                if (mainJson.has("scans")) {
                    val scansArray = mainJson.getJSONArray("scans")
                    for (i in 0 until scansArray.length()) {
                        val obj = scansArray.getJSONObject(i)
                        val sc = ScanHistoryEntry(
                            barcode = obj.getString("barcode"),
                            productName = obj.getString("productName"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                        repository.insertScanHistory(sc)
                    }
                }
                
                onComplete("Data restored successfully!", true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(e.localizedMessage, false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceAssistant.shutdown()
        try {
            toneGenerator?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

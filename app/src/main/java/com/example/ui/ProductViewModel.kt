package com.example.ui

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PreferencesManager
import com.example.data.Product
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
    private val voiceAssistant = VoiceAssistant(application)
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
                onMatch(product)
            } else {
                onNoMatch(barcode)
            }
        }
    }

    fun clearScannedProduct() {
        _scannedProduct.value = null
    }

    private fun playSuccessBeep() {
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

package com.example.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.data.Product
import com.example.data.SaleRecord
import com.example.data.ScanHistoryEntry
import com.example.ui.ProductViewModel
import com.example.util.Localization
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat

// Decimal Formatter
val df = DecimalFormat("#,##0")

// Localized translation helper extension inside views
@Composable
fun translate(key: String, viewModel: ProductViewModel): String {
    val lang by viewModel.language.collectAsState()
    return Localization.translate(key, lang)
}

// Simple procedural non-composable translation helper
fun translateSimple(key: String, viewModel: ProductViewModel): String {
    return Localization.translate(key, viewModel.prefs.language)
}

// ==========================================
// 1. SPLASH SCREEN
// ==========================================
@Composable
fun SplashScreen(navController: NavController, viewModel: ProductViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    // Animation States
    val scale = remember { Animatable(0f) }
    var subtitleVisible by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        // Pulse logo
        scale.animateTo(
            targetValue = 1.1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        scale.animateTo(1.0f)
        subtitleVisible = true
        
        delay(1800) // Beautiful splash delay

        if (isLoggedIn) {
            navController.navigate("home") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Geometric Ambient Glow Back-circles
        Box(
            modifier = Modifier
                .size(320.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x1F22C55E),
                                Color.Transparent
                            )
                        ),
                        radius = size.width / 1.5f
                    )
                }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Shiny App Logo Shield
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(scale.value)
                    .shadow(16.dp, shape = RoundedCornerShape(28.dp))
                    .border(
                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Store,
                        contentDescription = "Store Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "PW",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = translate("app_title", viewModel),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = subtitleVisible,
                enter = fadeIn() + expandVertically()
            ) {
                Text(
                    text = translate("app_subtitle", viewModel),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

// ==========================================
// 2. LOGIN SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController, viewModel: ProductViewModel) {
    var shopText by remember { mutableStateOf(viewModel.prefs.shopName) }
    var passwordText by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(viewModel.prefs.rememberMe) }
    
    // Auto-fill preserved login if remember me was active
    LaunchedEffect(Unit) {
        if (viewModel.prefs.rememberMe) {
            passwordText = viewModel.prefs.savedPassword
        }
    }

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Upper Corner Ambient Graphic
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .align(Alignment.TopCenter)
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0x0C22C55E),
                                Color.Transparent
                            )
                        )
                    )
                }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant Rounded Card for Form
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, shape = RoundedCornerShape(24.dp))
                    .testTag("login_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.LockPerson,
                        contentDescription = "Secure Lock",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(56.dp)
                            .padding(bottom = 8.dp)
                    )

                    Text(
                        text = translate("login_title", viewModel),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Shop Name Input
                    OutlinedTextField(
                        value = shopText,
                        onValueChange = { shopText = it },
                        label = { Text(translate("shop_name", viewModel)) },
                        leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shop_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Input
                    OutlinedTextField(
                        value = passwordText,
                        onValueChange = { passwordText = it },
                        label = { Text(translate("password", viewModel)) },
                        leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Remember Me Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("remember_me_checkbox")
                        )
                        Text(
                            text = translate("remember_me", viewModel),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Big Submit Button
                    Button(
                        onClick = {
                            if (shopText.isBlank()) {
                                Toast.makeText(context, "Please enter Shop Name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            // Local Simple Credentials Bypass
                            viewModel.setLogin(true, shopText)
                            viewModel.prefs.rememberMe = rememberMe
                            if (rememberMe) {
                                viewModel.prefs.savedPassword = passwordText
                            } else {
                                viewModel.prefs.savedPassword = ""
                            }
                            Toast.makeText(context, "Safar Mubarik! Welcome to PriceWala", Toast.LENGTH_SHORT).show()
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("submit_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = translate("login_btn", viewModel),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. DASHBOARD SCREEN
// ==========================================
@Composable
fun DashboardScreen(navController: NavController, viewModel: ProductViewModel) {
    val shopName by viewModel.shopName.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val nearExpiryProducts by viewModel.nearExpiryProducts.collectAsState()
    val todaySales by viewModel.todaySales.collectAsState()
    val recentScans by viewModel.recentScans.collectAsState()

    // Calculate dynamic profit totals
    val totalProfit = allProducts.sumOf { (it.salePrice - it.buyPrice) * it.stock }
    val totalProductsCount = allProducts.size
    val totalStockSum = allProducts.sumOf { it.stock }
    val todaySalesAmount = todaySales.sumOf { it.salePrice * it.quantity }
    val todayProfitAmount = todaySales.sumOf { (it.salePrice - it.buyPrice) * it.quantity }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header Section - Beautifully Branded Like HTML Theme
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .drawBehind {
                        drawLine(
                            color = Color(0xFF334155).copy(alpha = 0.5f),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Price",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Wala",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                        )
                    }
                    Text(
                        text = "SMART DUKAAN PARTNER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF22C55E),
                            letterSpacing = 2.sp
                        ),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = "${translate("dash_welcome", viewModel)} - $shopName",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Custom Rounded avatar representation for logout
                IconButton(
                    onClick = {
                        viewModel.setLogin(false)
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .border(1.dp, Color(0xFF475569), CircleShape)
                        .testTag("logout_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Logout",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Main scrollable Grid content
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Item 1: High Priority Grand Scan Product Button (Spans 2 columns)
                item(span = { GridItemSpan(2) }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .shadow(12.dp, shape = RoundedCornerShape(24.dp))
                            .clickable { navController.navigate("scanner") }
                            .testTag("dashboard_scan_button"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF22C55E), Color(0xFF15803D))
                                    )
                                )
                                .padding(18.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Custom Scan Icon with white translucent background
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    
                                    // High Priority Badge
                                    Box(
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.25f), CircleShape)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Baray Meharbani Scan Karein".uppercase(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 9.sp
                                            )
                                        )
                                    }
                                }
                                
                                Column {
                                    Text(
                                        text = translate("menu_scan", viewModel),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            fontSize = 22.sp
                                        )
                                    )
                                    Text(
                                        text = "پروڈکٹ سکین کریں",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontWeight = FontWeight.Medium
                                        ),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // --- NEW 2x2 DUKAAN METRICS OVERVIEW ---
                item(span = { GridItemSpan(2) }) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "DUKAAN SUMMARY (خلا صہ)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 1.5.sp
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Total Listings Card
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(72.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("Listings (پروڈکٹس)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, color = Color(0xFF94A3B8)))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("$totalProductsCount Items", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                }
                            }
                            // Total Stock remaining Card
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(72.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("Total Stock (کل اسٹاک)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, color = Color(0xFF94A3B8)))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("${df.format(totalStockSum)} Qty", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8)))
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Today's Sales Card
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(72.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("Today Sales (آج کی فروخت)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp, color = Color(0xFF94A3B8)))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Rs. ${df.format(todaySalesAmount)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF22C55E)))
                                }
                            }
                            // Expected Profit Card
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(72.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("Estimated Profit (کل منافع)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, color = Color(0xFF94A3B8)))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Rs. ${df.format(totalProfit)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B)))
                                }
                            }
                        }
                    }
                }

                // --- COLLAPSIBLE RECENT SCAN HISTORY ---
                if (recentScans.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.History, contentDescription = null, size = 18.dp, tint = Color(0xFFF59E0B))
                                        Text(
                                            text = "RECENT SCANS (حالیہ اسکین)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.clearScanHistory() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    recentScans.take(3).forEach { scan ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                                                .clickable { navController.navigate("product_detail/${scan.barcode}") }
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Icon(Icons.Default.QrCode, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                                Text(
                                                    text = scan.productName,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                                )
                                            }
                                            val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                                            val timeString = sdf.format(java.util.Date(scan.timestamp))
                                            Text(
                                                text = timeString,
                                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8), fontSize = 10.sp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Item 2: Add Product
                item {
                    DashboardButtonModern(
                        title = translate("menu_add", viewModel),
                        arabicSubtitle = "نئی چیز شامل کریں",
                        icon = Icons.Default.Add,
                        iconBg = Color(0x1AF59E0B),
                        iconColor = Color(0xFFF59E0B),
                        tag = "dashboard_add_button",
                        onClick = { navController.navigate("add_edit_product") }
                    )
                }

                // Item 3: Manage Stock (Alerts)
                item {
                    val alertIndicatorColor = if (lowStockProducts.isNotEmpty()) Color(0xFFEF4444) else Color(0xFF38BDF8)
                    val bgAlertTint = if (lowStockProducts.isNotEmpty()) Color(0x22EF4444) else Color(0x1A38BDF8)
                    DashboardButtonModern(
                        title = translate("menu_stock", viewModel),
                        arabicSubtitle = "اسٹاک مینیجمنٹ",
                        icon = Icons.Default.Storage,
                        iconBg = bgAlertTint,
                        iconColor = alertIndicatorColor,
                        tag = "dashboard_stock_button",
                        onClick = { navController.navigate("reports") }
                    )
                }

                // Item 4: Daily Profit
                item {
                    val profitString = "Rs. ${df.format(todayProfitAmount)} Aaj"
                    DashboardButtonModern(
                        title = translate("menu_profit", viewModel),
                        arabicSubtitle = profitString,
                        icon = Icons.Default.TrendingUp,
                        iconBg = Color(0x1A22C55E),
                        iconColor = Color(0xFF22C55E),
                        tag = "dashboard_reports_button",
                        onClick = { navController.navigate("reports") }
                    )
                }

                // Item 5: Settings
                item {
                    DashboardButtonModern(
                        title = translate("menu_settings", viewModel),
                        arabicSubtitle = "سیٹنگز تبدیل کریں",
                        icon = Icons.Default.Settings,
                        iconBg = Color(0x1A94A3B8),
                        iconColor = Color(0xFF94A3B8),
                        tag = "dashboard_settings_button",
                        onClick = { navController.navigate("settings") }
                    )
                }
            }

            // Bottom Warning Bar - Adaptive low-stock warnings with real/placeholder alerts in HTML style
            val lowStockItem = lowStockProducts.firstOrNull()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFB020).copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFFFB020).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pulsing yellow indicator circle
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFF59E0B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (lowStockItem != null) {
                                "Stock Alert: ${lowStockItem.name} ${lowStockItem.stock} left"
                            } else {
                                "Stock Alert: Sugar 5kg left"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                        )
                        Text(
                            text = if (lowStockItem != null) {
                                "${lowStockItem.name} ختم ہونے والی ہے"
                            } else {
                                "چینی ختم ہونے والی ہے"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFF59E0B).copy(alpha = 0.8f),
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            // Navigation Bar - Replicating HTML Custom Tab Styling
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Nav Tab (Active)
                Column(
                    modifier = Modifier.clickable { },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Home",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF22C55E),
                            fontSize = 10.sp
                        )
                    )
                }

                // Search Nav Tab (Inactive)
                Column(
                    modifier = Modifier.clickable { navController.navigate("reports") },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Search",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    )
                }

                // Inventory Nav Tab (Inactive)
                Column(
                    modifier = Modifier.clickable { navController.navigate("reports") },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Inventory",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Inventory",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    )
                }

                // History / Reports Nav Tab (Inactive)
                Column(
                    modifier = Modifier.clickable { navController.navigate("reports") },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "History",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

// Gorgeous Slate Styled Sub-Grid Dashboard Button Modern Card
@Composable
fun DashboardButtonModern(
    title: String,
    arabicSubtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconColor: Color,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .testTag(tag),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = arabicSubtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

// ==========================================
// 4. SCANNER SCREEN (REAL CAMERA & SIMULATION FOR EMULATOR)
// ==========================================
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(navController: NavController, viewModel: ProductViewModel) {
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    val allProducts by viewModel.allProducts.collectAsState()
    var manualBarcodetext by remember { mutableStateOf("") }
    
    val context = LocalContext.current

    // QUICK ADD SYSTEM STATES
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var scannedBarcodeToQuickAdd by remember { mutableStateOf("") }
    var quickProdName by remember { mutableStateOf("") }
    var quickSalePrice by remember { mutableStateOf("") }
    var quickBuyPrice by remember { mutableStateOf("") }
    var quickStock by remember { mutableStateOf("") }
    var quickCategory by remember { mutableStateOf("Grocery") }
    var quickExpiry by remember { mutableStateOf("") }
    var showQuickBoxCalc by remember { mutableStateOf(false) }
    var quickBoxCount by remember { mutableStateOf("") }
    var quickPacketsPerBox by remember { mutableStateOf("") }
    
    // SELL CONFIRMATION SYSTEM STATES
    var showSellProductDialog by remember { mutableStateOf(false) }
    var matchedProductToSell by remember { mutableStateOf<Product?>(null) }

    // Viewfinder Scanner laser animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val lineOffset by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (cameraPermissionState.status.isGranted) {
            // Real camera build integration
            val lifecycleOwner = LocalLifecycleOwner.current
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val cameraSelector = when {
                                cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                                cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                                else -> null
                            }
                            if (cameraSelector != null) {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                            } else {
                                android.util.Log.e("ScannerScreen", "No available camera found on this device!")
                            }
                        } catch (exc: Exception) {
                            android.util.Log.e("ScannerScreen", "CameraX initialization failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                }
            )
        } else {
            // Permission explanation banner
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera Permission Required",
                        style = MaterialTheme.typography.titleLarge.copy(color = Color.White),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "PriceWala requires Camera permission to scan shop barcodes in real-time.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.LightGray),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { cameraPermissionState.launchPermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Grant Permission", color = Color.White)
                    }
                }
            }
        }

        // --- GORGEOUS view-finder layer over top of camera preview ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    // Darken background overlay except middle window view
                    val viewWidth = size.width * 0.75f
                    val viewHeight = size.height * 0.3f
                    val left = (size.width - viewWidth) / 2f
                    val top = (size.height - viewHeight) / 3f
                    
                    // Top cutout
                    drawRect(color = Color.Black.copy(alpha = 0.5f), topLeft = Offset.Zero, size = Size(size.width, top))
                    // Left cutout
                    drawRect(color = Color.Black.copy(alpha = 0.5f), topLeft = Offset(0f, top), size = Size(left, viewHeight))
                    // Right cutout
                    drawRect(color = Color.Black.copy(alpha = 0.5f), topLeft = Offset(left + viewWidth, top), size = Size(size.width - (left + viewWidth), viewHeight))
                    // Bottom cutout
                    drawRect(color = Color.Black.copy(alpha = 0.5f), topLeft = Offset(0f, top + viewHeight), size = Size(size.width, size.height - (top + viewHeight)))
                }
        )

        // Title and Back Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = translate("menu_scan", viewModel),
                style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.Bold)
            )
        }

        // Scanner viewfinder indicators
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .fillMaxHeight(0.3f)
                .align(Alignment.Center)
                .offset(y = (-80).dp)
                .border(BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary), RoundedCornerShape(12.dp))
                .shadow(12.dp, RoundedCornerShape(12.dp))
        ) {
            // Glowing pulsing laser
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (lineOffset * 220).dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, MaterialTheme.colorScheme.tertiary, Color.Transparent)
                        )
                    )
            )
        }

        // Subtitle instructions
        Text(
            text = translate("scan_desc", viewModel),
            style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 80.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .padding(8.dp)
        )

        // --- EMULATOR/TEST SIMULATION PANEL ---
        // Since shopkeepers might be viewing this on the AI Studio web emulator
        // we provide an awesome simulation panel at the bottom to inject barcodes easily.
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = translate("scan_manual_label", viewModel),
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.primary, 
                        fontWeight = FontWeight.Bold
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualBarcodetext,
                        onValueChange = { manualBarcodetext = it },
                        placeholder = { Text("E.g. 101, 102...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("barcode_manual_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (manualBarcodetext.isNotBlank()) {
                                viewModel.processScannedBarcode(
                                    barcode = manualBarcodetext,
                                    onMatch = { product ->
                                        matchedProductToSell = product
                                        showSellProductDialog = true
                                    },
                                    onNoMatch = { barcode ->
                                        scannedBarcodeToQuickAdd = barcode
                                        quickProdName = ""
                                        quickSalePrice = ""
                                        quickBuyPrice = ""
                                        quickStock = ""
                                        showQuickAddDialog = true
                                        Toast.makeText(context, "Register standard details below", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("barcode_simulate_button")
                    ) {
                        Text("Search", color = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = translate("scan_emulator_tip", viewModel),
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.tertiary),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Fast Quick Simulate Products List directly!
                if (allProducts.isEmpty()) {
                    Text(
                        text = "No saved products. Click 'Add Product' card to register stock first!",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        textAlign = TextAlign.Center
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    ) {
                        items(allProducts) { prod ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.processScannedBarcode(
                                            barcode = prod.barcode,
                                            onMatch = { product ->
                                                matchedProductToSell = product
                                                showSellProductDialog = true
                                            },
                                            onNoMatch = {}
                                        )
                                    }
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant, 
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(prod.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("Barcode: ${prod.barcode}", style = MaterialTheme.typography.bodySmall)
                                }
                                Icon(Icons.Default.ArrowForwardIos, contentDescription = null, size = 12.dp, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        // --- GORGEOUS REAL QUICK ADD PRODUCT DIALOG POPUP ---
        if (showQuickAddDialog) {
            AlertDialog(
                onDismissRequest = { showQuickAddDialog = false },
                title = {
                    Text(
                        "Quick Add Product (نیا مال شامل کریں)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Barcode ID: $scannedBarcodeToQuickAdd",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF22C55E),
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = quickProdName,
                            onValueChange = { quickProdName = it },
                            label = { Text("Product Name (نام)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = quickBuyPrice,
                                onValueChange = { quickBuyPrice = it },
                                label = { Text("Buy Price Rs.") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = quickSalePrice,
                                onValueChange = { quickSalePrice = it },
                                label = { Text("Sale Price Rs.") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Stock Multiplier Box Calculator
                        TextButton(
                            onClick = { showQuickBoxCalc = !showQuickBoxCalc },
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Icon(
                                imageVector = if (showQuickBoxCalc) Icons.Default.Cancel else Icons.Default.Calculate,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showQuickBoxCalc) "Hide Box Calculator (باکس کیلکولیٹر چھپائیں)" else "Use Box Calculator? (باکس کیلکولیٹر کھولیں)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (showQuickBoxCalc) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = quickBoxCount,
                                    onValueChange = { newValue ->
                                        quickBoxCount = newValue
                                        val boxes = newValue.toIntOrNull()
                                        val pps = quickPacketsPerBox.toIntOrNull()
                                        if (boxes != null && pps != null) {
                                            quickStock = (boxes * pps).toString()
                                        }
                                    },
                                    label = { Text("Box count (باکس تعداد)", fontSize = 11.sp) },
                                    placeholder = { Text("e.g. 2") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = quickPacketsPerBox,
                                    onValueChange = { newValue ->
                                        quickPacketsPerBox = newValue
                                        val boxes = quickBoxCount.toIntOrNull()
                                        val pps = newValue.toIntOrNull()
                                        if (boxes != null && pps != null) {
                                            quickStock = (boxes * pps).toString()
                                        }
                                    },
                                    label = { Text("Packets per Box (پیکٹس)", fontSize = 11.sp) },
                                    placeholder = { Text("e.g. 12") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }

                        OutlinedTextField(
                            value = quickStock,
                            onValueChange = { quickStock = it },
                            label = { Text("Stock Quantity (کل مقدار / سٹاک)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = quickExpiry,
                            onValueChange = { quickExpiry = it },
                            label = { Text("Expiry Date (میعاد کی تاریخ) [Optional]") },
                            placeholder = { Text("E.g. 2026-12-31") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val buyVal = quickBuyPrice.toDoubleOrNull() ?: 0.0
                            val saleVal = quickSalePrice.toDoubleOrNull() ?: 0.0
                            val stockInt = quickStock.toIntOrNull() ?: 10
                            if (quickProdName.isNotBlank() && saleVal > 0.0) {
                                val item = Product(
                                    barcode = scannedBarcodeToQuickAdd,
                                    name = quickProdName,
                                    category = quickCategory,
                                    buyPrice = buyVal,
                                    salePrice = saleVal,
                                    stock = stockInt,
                                    expiryDate = quickExpiry.ifBlank { "Safe" }
                                )
                                viewModel.saveProduct(item)
                                // Add scan history record
                                viewModel.logScannedHistoryManual(item)
                                Toast.makeText(context, "${quickProdName} registered!", Toast.LENGTH_SHORT).show()
                                
                                // Reset inputs
                                quickExpiry = ""
                                quickBoxCount = ""
                                quickPacketsPerBox = ""
                                showQuickBoxCalc = false
                                
                                showQuickAddDialog = false
                                navController.navigate("product_detail/${scannedBarcodeToQuickAdd}")
                            } else {
                                Toast.makeText(context, "Name and Sale Price required!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
                    ) {
                        Text("Add Stock (جمع کریں)", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showQuickAddDialog = false 
                            quickExpiry = ""
                            quickBoxCount = ""
                            quickPacketsPerBox = ""
                            showQuickBoxCalc = false
                        }
                    ) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                    }
                }
            )
        }

        // --- REAL-WORLD EXTREMELY VISUAL SELL/CANCEL CONFIRMATION POPUP FOR SHOPKEEPERS ---
        if (showSellProductDialog && matchedProductToSell != null) {
            val productToSell = matchedProductToSell!!
            val isOutOfStock = productToSell.stock <= 0
            val profitVal = productToSell.salePrice - productToSell.buyPrice
            
            AlertDialog(
                onDismissRequest = { 
                    showSellProductDialog = false 
                    matchedProductToSell = null
                },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Product Detected! (مال مل گیا)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = if (isOutOfStock) Color(0xFFEF4444) else Color(0xFF22C55E)
                            )
                        )
                        if (isOutOfStock) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFEF4444).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "OUT OF STOCK (سٹاک ختم)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .background(Color(0x1A22C55E), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF22C55E), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "AVAILABLE (${productToSell.stock} Pcs)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF22C55E)
                                )
                            }
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Product Name
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Name (نام):", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                            Text(productToSell.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                        }
                        
                        Divider(color = Color(0xFF334155).copy(alpha = 0.5f))
                        
                        // Sale Price
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sale Price (فروخت قیمت):", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                            Text("Rs. ${df.format(productToSell.salePrice)}", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black, color = Color(0xFFF59E0B)))
                        }

                        Divider(color = Color(0xFF334155).copy(alpha = 0.5f))

                        // Stock
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Stock (باقی مقدار):", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                            Text(
                                text = "${productToSell.stock} pkts / units",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOutOfStock) Color(0xFFEF4444) else Color(0xFF38BDF8)
                                )
                            )
                        }

                        Divider(color = Color(0xFF334155).copy(alpha = 0.5f))

                        // Profit Per Item
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Profit (فی آئٹم منافع):", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                            Text("Rs. ${df.format(profitVal)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF22C55E)))
                        }

                        Divider(color = Color(0xFF334155).copy(alpha = 0.5f))

                        // Expiry Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Expiry (میعاد کا حال):", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                            
                            val isExp = try {
                                if (productToSell.expiryDate.isBlank() || productToSell.expiryDate == "Safe") {
                                    false
                                } else {
                                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                    val dateObj = sdf.parse(productToSell.expiryDate)
                                    dateObj != null && dateObj.time < System.currentTimeMillis()
                                }
                            } catch (e: Exception) {
                                false
                            }
                            
                            val expiryText = if (productToSell.expiryDate.isBlank() || productToSell.expiryDate == "Safe") {
                                "No Expiry Set (میعاد کا پتا نہیں)"
                            } else {
                                val cleanDate = productToSell.expiryDate
                                if (isExp) "EXPIRED! (میعاد ختم: $cleanDate)" else "Fresh (محفوظ: $cleanDate)"
                            }
                            
                            Text(
                                text = expiryText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isExp) Color(0xFFEF4444) else Color(0xFF10B981)
                                )
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (!isOutOfStock) {
                                viewModel.recordSale(productToSell, 1) {
                                    viewModel.playSuccessBeep()
                                    viewModel.voiceAssistant.speakProduct(productToSell.name, productToSell.salePrice)
                                }
                                Toast.makeText(context, "${productToSell.name} Sold! (بک گیا)", Toast.LENGTH_SHORT).show()
                            }
                            showSellProductDialog = false
                            matchedProductToSell = null
                        },
                        enabled = !isOutOfStock,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF22C55E),
                            disabledContainerColor = Color(0xFF1E293B)
                        )
                    ) {
                        Text("Sell 1 Unit (فروخت کریں)", color = if (isOutOfStock) Color.Gray else Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                showSellProductDialog = false
                                matchedProductToSell = null
                                navController.navigate("product_detail/${productToSell.barcode}")
                            }
                        ) {
                            Text("View Info (تفصیلات)", color = Color(0xFF38BDF8))
                        }
                        TextButton(
                            onClick = {
                                showSellProductDialog = false
                                matchedProductToSell = null
                            }
                        ) {
                            Text("Cancel (منسوخ کریں)", color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                }
            )
        }
    }
}

// Icon helper wrapper
@Composable
fun Icon(imageVector: ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, tint: Color) {
    Icon(imageVector = imageVector, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size))
}


// ==========================================
// 5. PRODUCT DETAILS SCREEN
// ==========================================
@Composable
fun ProductDetailsScreen(barcode: String, navController: NavController, viewModel: ProductViewModel) {
    var product by remember { mutableStateOf<Product?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var quantityToSell by remember { mutableIntStateOf(1) }
    val context = LocalContext.current
    
    // Fetch product details
    LaunchedEffect(barcode) {
        product = viewModel.getProduct(barcode)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Product Details",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (product == null) {
                Box(
                    modifier = Modifier.fillGridUnitWeight(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                val item = product!!

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Huge Top Display Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(24.dp)),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingBag,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "Category: ${item.category}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Barcode Pill
                                SuggestionChip(
                                    onClick = { },
                                    label = { Text("Barcode: ${item.barcode}") },
                                    icon = { Icon(Icons.Default.QrCode, contentDescription = null, size = 16.dp, tint = MaterialTheme.colorScheme.tertiary) }
                                )
                            }
                        }
                    }

                    // Prices and Profit Dashboard Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "PRICING ENGINE",
                                    style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = translate("lbl_buy_price", viewModel), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = "Rs. ${df.format(item.buyPrice)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = translate("lbl_sale_price", viewModel), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = "Rs. ${df.format(item.salePrice)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Profit Panel with color configurations
                                val isHighProfit = item.profit > 10
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isHighProfit) Color(0x2222C55E) else Color(0x22EF4444)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(
                                                imageVector = if (isHighProfit) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                                tint = if (isHighProfit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                                contentDescription = null
                                            )
                                            Text(
                                                text = translate("lbl_profit", viewModel),
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                        Text(
                                            text = "Rs. ${df.format(item.profit)}",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Black,
                                                color = if (isHighProfit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Stock Details and Expiry Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                // Stock alerts
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = translate("lbl_qty", viewModel),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                    
                                    val isLow = item.stock <= 5
                                    val isOut = item.stock <= 0
                                    Surface(
                                        color = if (isOut) Color(0xFFEF4444).copy(alpha = 0.2f) else if (isLow) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        Text(
                                            text = if (isOut) "OUT OF STOCK (سٹاک ختم)" else "${item.stock} Items remaining",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isOut || isLow) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Expiry alerts
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = translate("lbl_expiry", viewModel),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = item.expiryDate,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }

                    // --- GORGEOUS HIGH-PRIORITY SALES REGISTER SECTION ---
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "SELL FOR CASH (فروخت کریں)",
                                    style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Quantity (تعداد):",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                                    )
                                    
                                    // Quantity selector +/-
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        IconButton(
                                            onClick = { if (quantityToSell > 1) quantityToSell-- },
                                            modifier = Modifier.background(Color(0xFF334155), CircleShape).size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Minus", tint = Color.White)
                                        }
                                        
                                        Text(
                                            text = quantityToSell.toString(),
                                            style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.Black)
                                        )
                                        
                                        IconButton(
                                            onClick = { if (quantityToSell < item.stock) quantityToSell++ },
                                            modifier = Modifier.background(Color(0xFF334155), CircleShape).size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Plus", tint = Color.White)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                val totalCashAmount = item.salePrice * quantityToSell
                                Button(
                                    onClick = {
                                        if (item.stock >= quantityToSell) {
                                            viewModel.recordSale(item, quantityToSell) {
                                                viewModel.playSuccessBeep()
                                                viewModel.voiceAssistant.speakProduct(item.name, totalCashAmount)
                                                Toast.makeText(context, "Rs. ${df.format(totalCashAmount)} Sale Recorded Successfully!", Toast.LENGTH_SHORT).show()
                                                navController.popBackStack()
                                            }
                                        } else {
                                            Toast.makeText(context, "Insufficient stock!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = item.stock > 0,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF22C55E),
                                        disabledContainerColor = Color(0xFF475569)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("details_sell_button")
                                ) {
                                    Icon(Icons.Default.Payments, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Sell (فروخت کریں) - Rs. ${df.format(totalCashAmount)}",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Delete and Edit triggers
                    item {
                        Button(
                            onClick = {
                                showDeleteDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Product", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                navController.navigate("add_edit_product?barcode=${item.barcode}")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit Product Settings", fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }

                // --- REAL DATA SAFETY DELETE CONFIRMATION DIALOG ---
                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Product delete karein? (تصدیق)") },
                        text = { Text("Kya aap waqai '${item.name}' ko stock se mukamal tor par kharij karna chahte hain?") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.deleteProduct(item)
                                    showDeleteDialog = false
                                    navController.popBackStack()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Haan, Delete Karein")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) {
                                Text("Nahi, Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}

// Custom Grid Weight Constraint Modifier helper
fun Modifier.fillGridUnitWeight(): Modifier = this.fillMaxSize()


// ==========================================
// 6. ADD / EDIT PRODUCT SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(barcodeParam: String?, navController: NavController, viewModel: ProductViewModel) {
    var barcode by remember { mutableStateOf(barcodeParam ?: "") }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var buyPriceStr by remember { mutableStateOf("") }
    var salePriceStr by remember { mutableStateOf("") }
    var stockStr by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    
    // BOX CALCULATOR STATES
    var showBoxCalc by remember { mutableStateOf(false) }
    var boxCount by remember { mutableStateOf("") }
    var packetsPerBox by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Load product if editing
    LaunchedEffect(barcodeParam) {
        if (!barcodeParam.isNullOrBlank()) {
            val existed = viewModel.getProduct(barcodeParam)
            if (existed != null) {
                barcode = existed.barcode
                name = existed.name
                category = existed.category
                buyPriceStr = existed.buyPrice.toInt().toString()
                salePriceStr = existed.salePrice.toInt().toString()
                stockStr = existed.stock.toString()
                expiryDate = existed.expiryDate
            }
        }
    }

    // Live profit calculation
    val buyVal = buyPriceStr.toDoubleOrNull() ?: 0.0
    val saleVal = salePriceStr.toDoubleOrNull() ?: 0.0
    val calculatedProfit = saleVal - buyVal

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // App Bar Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (barcodeParam != null) "Edit Product" else "Register Product",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Form Fields Card Layout
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            
                            // Barcode Input
                            OutlinedTextField(
                                value = barcode,
                                onValueChange = { barcode = it },
                                label = { Text(translate("lbl_barcode", viewModel)) },
                                modifier = Modifier.fillMaxWidth().testTag("barcode_field"),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))

                            // Name Input
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text(translate("lbl_prod_name", viewModel)) },
                                modifier = Modifier.fillMaxWidth().testTag("name_field"),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))

                            // Category selector
                            OutlinedTextField(
                                value = category,
                                onValueChange = { category = it },
                                label = { Text(translate("lbl_category", viewModel)) },
                                modifier = Modifier.fillMaxWidth().testTag("category_field"),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // Prices Block Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("PRICES & PROFIT MARGIN", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Buy Price
                                OutlinedTextField(
                                    value = buyPriceStr,
                                    onValueChange = { buyPriceStr = it },
                                    label = { Text(translate("lbl_buy_price", viewModel)) },
                                    modifier = Modifier.weight(1f).testTag("buy_price_field"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )

                                // Sale Price
                                OutlinedTextField(
                                    value = salePriceStr,
                                    onValueChange = { salePriceStr = it },
                                    label = { Text(translate("lbl_sale_price", viewModel)) },
                                    modifier = Modifier.weight(1f).testTag("sale_price_field"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Live Profit Calculation View
                            Surface(
                                color = MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Calculated Margin profit:")
                                    Text(
                                        text = "Rs. ${df.format(calculatedProfit)}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (calculatedProfit > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Inventory Stock card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Box Calculator Toggle
                            TextButton(
                                onClick = { showBoxCalc = !showBoxCalc },
                                modifier = Modifier.align(Alignment.Start)
                            ) {
                                Icon(
                                    imageVector = if (showBoxCalc) Icons.Default.Cancel else Icons.Default.Calculate,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (showBoxCalc) "Hide Box Calculator (باکس کیلکولیٹر چھپائیں)" else "Use Box Calculator? (باکس کیلکولیٹر کھولیں)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (showBoxCalc) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = boxCount,
                                        onValueChange = { newValue ->
                                            boxCount = newValue
                                            val boxes = newValue.toIntOrNull()
                                            val pps = packetsPerBox.toIntOrNull()
                                            if (boxes != null && pps != null) {
                                                stockStr = (boxes * pps).toString()
                                            }
                                        },
                                        label = { Text("Box count (باکس تعداد)", fontSize = 11.sp) },
                                        placeholder = { Text("e.g. 2") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = packetsPerBox,
                                        onValueChange = { newValue ->
                                            packetsPerBox = newValue
                                            val boxes = boxCount.toIntOrNull()
                                            val pps = newValue.toIntOrNull()
                                            if (boxes != null && pps != null) {
                                                stockStr = (boxes * pps).toString()
                                            }
                                        },
                                        label = { Text("Packets per Box (پیکٹس)", fontSize = 11.sp) },
                                        placeholder = { Text("e.g. 12") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // Stock Count
                            OutlinedTextField(
                                value = stockStr,
                                onValueChange = { stockStr = it },
                                label = { Text(translate("lbl_qty", viewModel)) },
                                modifier = Modifier.fillMaxWidth().testTag("stock_field"),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))

                            // Expiry Date (YYYY-MM-DD)
                            OutlinedTextField(
                                value = expiryDate,
                                onValueChange = { expiryDate = it },
                                label = { Text(translate("lbl_expiry", viewModel)) },
                                placeholder = { Text("E.g. 2026-12-31") },
                                modifier = Modifier.fillMaxWidth().testTag("expiry_field"),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // Register Submit button
                item {
                    Button(
                        onClick = {
                            if (barcode.isBlank() || name.isBlank() || buyPriceStr.isBlank() || salePriceStr.isBlank()) {
                                Toast.makeText(context, "Fill out required fields: Barcode, Name, Buy, Sale", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val finalBuy = buyPriceStr.toDoubleOrNull() ?: 0.0
                            val finalSale = salePriceStr.toDoubleOrNull() ?: 0.0
                            val finalStock = stockStr.toIntOrNull() ?: 0

                            val sampleProduct = Product(
                                barcode = barcode,
                                name = name,
                                category = category.ifBlank { "Grocery" },
                                buyPrice = finalBuy,
                                salePrice = finalSale,
                                stock = finalStock,
                                expiryDate = expiryDate.ifBlank { "Safe" }
                            )

                            viewModel.saveProduct(sampleProduct)
                            Toast.makeText(context, "Product Saved Offline!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("save_product_button")
                    ) {
                        Text(
                            text = translate("btn_save", viewModel),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}


// ==========================================
// 7. FINANCIAL REPORTS SCREEN (STOCKS & PROFITS)
// ==========================================
@Composable
fun ReportsScreen(navController: NavController, viewModel: ProductViewModel) {
    val allProducts by viewModel.allProducts.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val nearExpiryProducts by viewModel.nearExpiryProducts.collectAsState()
    val allSales by viewModel.allSales.collectAsState()
    val todaySales by viewModel.todaySales.collectAsState()
    
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredProducts by viewModel.filteredProducts.collectAsState()

    // Tab Index: 0 -> General, 1 -> Daily Sales, 2 -> Low stock list, 3 -> Expiry Alerts
    var activeTab by remember { mutableIntStateOf(0) }

    // Dialogue confirmation state
    var showClearSalesDialog by remember { mutableStateOf(false) }
    var saleToReverse by remember { mutableStateOf<com.example.data.SaleRecord?>(null) }

    // Aggregate Calculations
    val totalProducts = allProducts.size
    val totalStockSum = allProducts.sumOf { it.stock }
    val estimatedProfitSum = allProducts.sumOf { (it.salePrice - it.buyPrice) * it.stock }
    
    // Today Sales calculations
    val todaySalesAmount = todaySales.sumOf { it.salePrice * it.quantity }
    val todayProfitAmount = todaySales.sumOf { (it.salePrice - it.buyPrice) * it.quantity }
    val todaySoldItemsCount = todaySales.sumOf { it.quantity }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = translate("menu_profit", viewModel),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            // --- SIMPLE HIGHLY FUNCTIONAL SEARCH BAR ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.search(it) },
                placeholder = { Text("Search by Name or Barcode (نام یا بارکوڈ تلاش کریں)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, size = 20.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.search("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", size = 20.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .testTag("reports_search_field"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp)
            )

            if (searchQuery.isNotBlank()) {
                // --- DYNAMIC SEARCH RESULTS LIST VIEW DISPLAY ---
                Text(
                    text = "Search Results (تلاش کے نتائج) - ${filteredProducts.size} found",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                
                if (filteredProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching products found! (کوئی مال نہیں ملا)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredProducts) { prod ->
                            val isOutOfStock = prod.stock <= 0
                            val isLowStock = prod.stock <= 5 && !isOutOfStock
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { navController.navigate("product_detail/${prod.barcode}") },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(
                                    1.dp,
                                    if (isOutOfStock) Color(0xFFEF4444).copy(alpha = 0.6f) 
                                    else if (isLowStock) Color(0xFFF59E0B).copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prod.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Barcode: ${prod.barcode} | Cat: ${prod.category}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Rs. ${df.format(prod.salePrice)}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = Color(0xFF22C55E))
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isOutOfStock) Color(0x33EF4444)
                                                    else if (isLowStock) Color(0x33F59E0B)
                                                    else Color(0x2238BDF8),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isOutOfStock) "Out of Stock (ختم)" 
                                                       else if (isLowStock) "Low Stock (${prod.stock})"
                                                       else "Stock: ${prod.stock} pkts",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isOutOfStock) Color(0xFFEF4444)
                                                        else if (isLowStock) Color(0xFFF59E0B)
                                                        else Color(0xFF38BDF8)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Quick Tabs Selector - 4 distinct tabs
                TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Overview") }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Daily Sales") }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("Low Stock (${lowStockProducts.size})") }
                )
                Tab(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    text = { Text("Expiring (%d)".format(nearExpiryProducts.size)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (activeTab) {
                0 -> {
                    // --- TAB 0: MAIN METRICS OVERVIEW ---
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                text = translate("reports_header", viewModel),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Metric Card: Total Products
                        item {
                            MetricBlock(
                                title = translate("total_prods", viewModel),
                                value = totalProducts.toString(),
                                caption = "Different inventory listings",
                                icon = Icons.Default.Inventory,
                                colors = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Metric Card: Total stock quantities
                        item {
                            MetricBlock(
                                title = translate("total_stock", viewModel),
                                value = df.format(totalStockSum),
                                caption = "Total volume count in inventory",
                                icon = Icons.Default.Category,
                                colors = Color(0xFF38BDF8)
                            )
                        }

                        // Metric Card: Today's Realized cash Sales
                        item {
                            MetricBlock(
                                title = "TODAY'S CASH REGISTER (آج کی فروخت)",
                                value = "Rs. ${df.format(todaySalesAmount)}",
                                caption = "Profit today: Rs. ${df.format(todayProfitAmount)} | Volume: $todaySoldItemsCount pieces",
                                icon = Icons.Default.Paid,
                                colors = Color(0xFF22C55E)
                            )
                        }

                        // Metric Card: Total expected earnings / margins
                        item {
                            MetricBlock(
                                title = translate("estimated_profit", viewModel),
                                value = "Rs. ${df.format(estimatedProfitSum)}",
                                caption = "Profit generated after full stock clearance",
                                icon = Icons.Default.Payments,
                                colors = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
                1 -> {
                    // --- TAB 1: DAILY SALES AND SALES HISTORY ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 20.dp)
                    ) {
                        // Sticky Sales Summary Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Today Sales (فروخت)", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                                    Text("Rs. ${df.format(todaySalesAmount)}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = Color(0xFF22C55E)))
                                }
                                Column {
                                    Text("Today Profit (منافع)", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                                    Text("Rs. ${df.format(todayProfitAmount)}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = Color(0xFFF59E0B)))
                                }
                                Column {
                                    Text("Quantity (تعداد)", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                                    Text("$todaySoldItemsCount Sold", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = Color(0xFF38BDF8)))
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SALES RECORDS (کل فروخت)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), letterSpacing = 1.2.sp)
                            )
                            if (allSales.isNotEmpty()) {
                                TextButton(onClick = { showClearSalesDialog = true }) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = null, size = 16.dp, tint = Color(0xFFEF4444))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear All", color = Color(0xFFEF4444), fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (allSales.isEmpty()) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text(
                                    "No sales registered yet. Sell products through Scanner or Search detail screen first!",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF94A3B8)),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(24.dp)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(allSales) { sale ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                        border = BorderStroke(1.dp, Color(0xFF334155))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(sale.productName, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Qty: ${sale.quantity}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF38BDF8))
                                                    Text("|", style = MaterialTheme.typography.bodySmall, color = Color(0xFF334155))
                                                    val sdf = java.text.SimpleDateFormat("hh:mm a - dd MMM", java.util.Locale.getDefault())
                                                    val strTime = sdf.format(java.util.Date(sale.timestamp))
                                                    Text(strTime, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                                                }
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text("Rs. ${df.format(sale.salePrice * sale.quantity)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF22C55E)))
                                                    val pft = (sale.salePrice - sale.buyPrice) * sale.quantity
                                                    Text("Profit: Rs. ${df.format(pft)}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = Color(0xFFF59E0B)))
                                                }
                                                // Undo Button
                                                IconButton(
                                                    onClick = { saleToReverse = sale },
                                                    modifier = Modifier
                                                        .background(Color(0x22EF4444), CircleShape)
                                                        .size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Undo, contentDescription = "Reverse Sale", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // --- TAB 2: LOW STOCK PRODUCTS LIST ---
                    if (lowStockProducts.isEmpty()) {
                        EmptyWarningState(msg = "All stock levels are perfectly healthy!")
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(lowStockProducts) { prod ->
                                AlertProductItem(prod = prod, type = "stock", navController = navController)
                            }
                        }
                    }
                }
                3 -> {
                    // --- TAB 3: NEAR EXPIRY LIST ---
                    if (nearExpiryProducts.isEmpty()) {
                        EmptyWarningState(msg = "Awesome! No stock expiring within next 30 days!")
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(nearExpiryProducts) { prod ->
                                AlertProductItem(prod = prod, type = "expiry", navController = navController)
                            }
                        }
                    }
                }
            }
            }

            // --- SALES DIALOG CONFIRMATIONS ---
            if (showClearSalesDialog) {
                AlertDialog(
                    onDismissRequest = { showClearSalesDialog = false },
                    title = { Text("Clear All Sales? (ساری فروخت مٹائیں)") },
                    text = { Text("Kya aap waqai aaj tak ki tamaam sales records ko mukammal mitaana chahte hain? Stock counts par asar nahi hoga.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.clearAllSales()
                                showClearSalesDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Text("Haan, Clear Karein")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearSalesDialog = false }) {
                            Text("Nahi, Cancel")
                        }
                    }
                )
            }

            if (saleToReverse != null) {
                val currentSaleItem = saleToReverse!!
                AlertDialog(
                    onDismissRequest = { saleToReverse = null },
                    title = { Text("Undo Sale? (فروخت منسوخ کریں)") },
                    text = { Text("Kya aap waqai '${currentSaleItem.productName}' ki is sale (Qty: ${currentSaleItem.quantity}) ko reverse karna chahte hain? Is silsile me product stock waapis add ho jaegi.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteSale(currentSaleItem)
                                saleToReverse = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Text("Haan, Reverse Karein")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { saleToReverse = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

// Custom Warning State Element when empty
@Composable
fun EmptyWarningState(msg: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
                contentDescription = null
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = msg, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        }
    }
}

// Metric Block styling helper for Reports stats
@Composable
fun MetricBlock(
    title: String,
    value: String,
    caption: String,
    icon: ImageVector,
    colors: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = value, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black))
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(colors.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = colors, modifier = Modifier.size(28.dp))
            }
        }
    }
}

// Alert specific list item card
@Composable
fun AlertProductItem(prod: Product, type: String, navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate("product_detail/${prod.barcode}") },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = prod.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Category: ${prod.category}  •  Barcode: ${prod.barcode}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (type == "stock") {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${prod.stock} Left",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    )
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = prod.expiryDate,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    )
                }
            }
        }
    }
}


// ==========================================
// 8. SETTINGS SCREEN
// ==========================================
@Composable
fun SettingsScreen(navController: NavController, viewModel: ProductViewModel) {
    val currentLang by viewModel.language.collectAsState()
    val voiceEnabled by viewModel.voiceEnabled.collectAsState()
    val darkModeOn by viewModel.darkMode.collectAsState()
    val shopNameVal by viewModel.shopName.collectAsState()

    var editingShopName by remember { mutableStateOf(shopNameVal) }

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = translate("menu_settings", viewModel),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Shop Profile Rename Box
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = translate("set_shop_info", viewModel),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = editingShopName,
                                onValueChange = { editingShopName = it },
                                modifier = Modifier.fillMaxWidth().testTag("settings_shop_name_input"),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            viewModel.updatePreferences(currentLang, voiceEnabled, darkModeOn, editingShopName)
                                            Toast.makeText(context, "Shop Name Updated!", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )
                        }
                    }
                }

                // Configuration Settings Row Toggles Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            
                            // 1. Language Toggle English / Urdu
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = translate("set_lang", viewModel), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(text = "Currently: " + if (currentLang == "ur") "اردو" else "English", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row {
                                    ElevatedButton(
                                        onClick = { viewModel.updatePreferences("en", voiceEnabled, darkModeOn, editingShopName) },
                                        colors = ButtonDefaults.elevatedButtonColors(
                                            containerColor = if (currentLang == "en") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("lang_en_button")
                                    ) {
                                        Text("EN", color = if (currentLang == "en") Color.Black else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    ElevatedButton(
                                        onClick = { viewModel.updatePreferences("ur", voiceEnabled, darkModeOn, editingShopName) },
                                        colors = ButtonDefaults.elevatedButtonColors(
                                            containerColor = if (currentLang == "ur") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("lang_ur_button")
                                    ) {
                                        Text("اردو", color = if (currentLang == "ur") Color.Black else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))

                            // 2. Local TTS Voice Alert Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = translate("set_voice", viewModel), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(text = "Announces name & price details on scan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = voiceEnabled,
                                    onCheckedChange = { viewModel.updatePreferences(currentLang, it, darkModeOn, editingShopName) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.testTag("voice_assistant_switch")
                                )
                            }

                            Divider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))

                            // 3. Interface Dark Mode Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = translate("set_dark", viewModel), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(text = "Toggles high contrast dark interfaces", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = darkModeOn,
                                    onCheckedChange = { viewModel.updatePreferences(currentLang, voiceEnabled, it, editingShopName) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.testTag("dark_mode_switch")
                                )
                            }
                        }
                    }
                }

                // Data Backup & Restore Panel (Full local JSON export/import system)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = translate("set_backup", viewModel),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Save product price details, sale records, and scan history to your phone storage locally (data security).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.backupData { filePath, success ->
                                            if (success) {
                                                Toast.makeText(context, "Backup saved to: $filePath", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Backup failed: $filePath", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                    modifier = Modifier.weight(1f).testTag("backup_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, size = 18.dp, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Backup Now", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                
                                Button(
                                    onClick = {
                                        viewModel.restoreData(null) { statusText, success ->
                                            if (success) {
                                                Toast.makeText(context, "Data restored successfully!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Restore error: $statusText", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                    modifier = Modifier.weight(1f).testTag("restore_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, size = 18.dp, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Restore", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

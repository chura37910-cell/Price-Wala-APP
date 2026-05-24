package com.example.util

object Localization {
    private val enMap = mapOf(
        "app_title" to "PriceWala",
        "app_subtitle" to "Smart Dukaan Partner",
        "login_title" to "Merchant Login",
        "shop_name" to "Shop Name",
        "password" to "Password",
        "remember_me" to "Remember Me",
        "login_btn" to "Access Store Dashboard",
        "field_required" to "This field is required",
        "logout" to "Secure Logout",
        
        "menu_scan" to "Scan Product",
        "menu_add" to "Add Product",
        "menu_stock" to "Stock Alert",
        "menu_profit" to "Profit Report",
        "menu_settings" to "Settings",
        
        "dash_welcome" to "Assalam-o-Alaikum",
        "dash_tagline" to "Manage your shop inventory effortlessly",
        "reports_header" to "Dukaan Financial Status",
        "total_prods" to "Total Products",
        "total_stock" to "Total Stock",
        "estimated_profit" to "Estimated Profit",
        "low_stock_warnings" to "Low Stock Items",
        "expiry_warnings" to "Near Expiry Items",
        
        "lbl_barcode" to "Barcode",
        "lbl_prod_name" to "Product Name",
        "lbl_category" to "Category",
        "lbl_buy_price" to "Buy Price (Rs.)",
        "lbl_sale_price" to "Sale Price (Rs.)",
        "lbl_qty" to "Stock Quantity",
        "lbl_expiry" to "Expiry Date (DD/MM/YYYY)",
        "lbl_profit" to "Calculated Profit",
        
        "btn_save" to "Save Product Details",
        "btn_delete" to "Delete Product",
        "btn_edit" to "Edit Product Settings",
        "btn_scan_now" to "Tap to Scan",
        "btn_back" to "Go Back",
        
        "scan_desc" to "Place a barcode inside the glowing viewfinder",
        "scan_emulator_tip" to "Tip: Running in browser? Use the quick selector below to simulate real barcode scanning!",
        "scan_manual_label" to "Simulate Scanning / Manual Search",
        "scan_not_found" to "No product with barcode %s exists. Would you like to create it?",
        "scan_success" to "Success! Product found.",
        
        "set_lang" to "Application Language",
        "set_voice" to "Voice Assistant Speech Alerts",
        "set_dark" to "Dark Mode Interface",
        "set_sound_test" to "Test Voice Synth Sound",
        "set_shop_info" to "Update Shop Name",
        "set_backup" to "Data Backup & Restore",
        "set_backup_success" to "Local Backup Saved to Storage Successfully!",
        
        "stock_good" to "Healthy Stock Levels",
        "stock_low" to "Low Stock Alert (%d left)",
        "expiry_good" to "Safe (Expires %s)",
        "expiry_warning" to "Expiring Soon (%s)",
        "profit_good" to "Optimal Profit Margin (+Rs. %s)",
        "profit_low" to "Low Profit Margin (+Rs. %s)"
    )

    private val urMap = mapOf(
        "app_title" to "پرائس والا",
        "app_subtitle" to "اسمارٹ دکان پارٹنر",
        "login_title" to "دکاندار لاگ ان",
        "shop_name" to "دکان کا نام",
        "password" to "پاس ورڈ (خفیہ کوڈ)",
        "remember_me" to "مجھے یاد رکھیں",
        "login_btn" to "ڈیش بورڈ پر جائیں",
        "field_required" to "یہ معلومات لکھنا لازمی ہے",
        "logout" to "لاگ آؤٹ کریں",
        
        "menu_scan" to "پروڈکٹ اسکین کریں",
        "menu_add" to "نیا مال درج کریں",
        "menu_stock" to "کم اسٹاک الرٹ",
        "menu_profit" to "منافع کی رپورٹ",
        "menu_settings" to "سیٹنگز تبدیل کریں",
        
        "dash_welcome" to "السلام علیکم",
        "dash_tagline" to "اپنی دکان کا حساب کتاب اب نہایت آسان کریں",
        "reports_header" to "دکان کی مالی حالت",
        "total_prods" to "کل پروڈکٹس کی تعداد",
        "total_stock" to "کل اسٹاک کا مال",
        "estimated_profit" to "کل متوقع منافع",
        "low_stock_warnings" to "کم اسٹاک والی اشیاء",
        "expiry_warnings" to "قریب ترین ایکسپائری",
        
        "lbl_barcode" to "بارکوڈ نمبر",
        "lbl_prod_name" to "پروڈکٹ کا نام",
        "lbl_category" to "کیٹیگری (قسم)",
        "lbl_buy_price" to "قیمتِ خرید (روپے)",
        "lbl_sale_price" to "قیمتِ فروخت (روپے)",
        "lbl_qty" to "اسٹاک کی تعداد (تھوک)",
        "lbl_expiry" to "تنسیخی تاریخ (تاریخِ ایکسپائری)",
        "lbl_profit" to "بچنے والا منافع",
        
        "btn_save" to "پروڈکٹ محفوظ کریں",
        "btn_delete" to "پروڈکٹ حذف کریں",
        "btn_edit" to "تبدیلی کریں",
        "btn_scan_now" to "اسکین کریں",
        "btn_back" to "واپس جائیں",
        
        "scan_desc" to "بارکوڈ کو چمکتی ہوئی پیلی پٹی کے اندر لائیں",
        "scan_emulator_tip" to "براؤزر میں چل رہا ہے؟ بارکوڈ کی نقالی کیلئے نیچے سے فورا پروڈکٹ منتخب کریں!",
        "scan_manual_label" to "بارکوڈ لکھ کر تلاش کریں",
        "scan_not_found" to "بارکوڈ %s کا کوئی مال درج نہیں ہے۔ کیا آپ نیا پروڈکٹ درج کرنا چاہتے ہیں؟",
        "scan_success" to "کامیابی! مال مل گیا۔",
        
        "set_lang" to "رابطہ کی زبان (Language)",
        "set_voice" to "آوازی گائیڈ (اسکین بولنا)",
        "set_dark" to "ڈارک موڈ انٹرفیس",
        "set_sound_test" to "آواز کا تجربہ کریں",
        "set_shop_info" to "دکان کا نام ٹھیک کریں",
        "set_backup" to "لوکل ڈیٹا بیک اپ اور ری اسٹور",
        "set_backup_success" to "ڈیٹا کا بیک اپ لوکل کارڈ پر کامیابی سے محفوظ ہو گیا!",
        
        "stock_good" to "اسٹاک مکمل ہے",
        "stock_low" to "ہوشیار! اسٹاک کم ہے (%d باقی ہے)",
        "expiry_good" to "محفوظ ہے (%s ایکسپائری)",
        "expiry_warning" to "ہوشیار! جلد ختم ہونے والا ہے (%s)",
        "profit_good" to "مناسب منافع حاصل (+Rs. %s)",
        "profit_low" to "کم منافع (-Rs. %s)"
    )

    fun translate(key: String, lang: String): String {
        return if (lang == "ur") {
            urMap[key] ?: enMap[key] ?: key
        } else {
            enMap[key] ?: key
        }
    }
}

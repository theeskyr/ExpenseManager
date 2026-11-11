# Proguard rules for Expense Manager (Release Optimization)

# 1. Room Database & SQLite
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.migration.Migration { *; }

# 2. Application Entities, DAOs, Models & AI
-keep class com.expensetracker.app.data.local.entity.** { *; }
-keep class com.expensetracker.app.data.local.dao.** { *; }
-keep class com.expensetracker.app.data.model.** { *; }
-keep class com.expensetracker.app.data.ai.** { *; }
-keep class com.expensetracker.app.data.service.** { *; }
-keep class com.expensetracker.app.utils.SavedCsvResult { *; }
-keep class com.expensetracker.app.utils.ParsedBankNotification { *; }
-keep class com.expensetracker.app.ui.viewmodel.ChatMessage { *; }
-keep class com.expensetracker.app.ui.viewmodel.TripSplitData { *; }

# 3. Google Generative AI SDK
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# 4. Kotlin Coroutines & Reflection
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }

# 5. AndroidX Lifecycle & ViewModel
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class androidx.lifecycle.** { *; }

# 6. Compose Runtime & UI
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# 7. Safe JSON Parsing
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

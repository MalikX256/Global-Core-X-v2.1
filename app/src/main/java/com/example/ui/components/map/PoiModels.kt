package com.example.ui.components.map

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class PoiCategory(
    val title: String,
    val emoji: String,
    val searchQuery: String,
    val accentColor: Color,
    val description: String
) {
    ALL("All POIs", "📍", "landmark", SleekBlue, "All nearby prominent locations"),
    HOSPITAL("Hospitals", "🏥", "hospital", SleekSosRed, "Hospitals, medical clinics & emergency care"),
    POLICE("Police", "👮", "police", SleekBlue, "Police stations & public safety posts"),
    GAS_STATION("Gas Stations", "⛽", "gas station fuel", SleekOrange, "Gasoline, diesel & EV charging stations"),
    HOTEL("Hotels", "🏨", "hotel resort lodging", SleekPurple, "Hotels, resorts, suites & hostels"),
    SCHOOL("Schools", "🏫", "school university college", SleekGreen, "Schools, colleges, universities & academies"),
    RESTAURANT("Dining", "🍽️", "restaurant cafe", SleekAmber, "Restaurants, eateries & cafes"),
    PHARMACY("Pharmacies", "💊", "pharmacy drugstore", SleekCyan, "Pharmacies & medical dispensaries"),
    BANK_ATM("Banks & ATMs", "🏧", "bank atm", SleekGreen, "Bank branches, ATMs & cash points")
}

data class MapPoiItem(
    val id: String,
    val name: String,
    val category: PoiCategory,
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val distanceMeters: Double = 0.0,
    val phone: String? = null,
    val typeName: String = ""
)

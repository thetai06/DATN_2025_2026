package org.o7planning.myapplication.data

class dataStore(
    val storeId:String? = null,
    val ownerId: String? = null,
    val name:String? = null,
    val imageURL: Int? = null,
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val tableNumber: String? = null,
    val des: String? = null,
    val openingHour: String? = null,
    val closingHour: String? = null,
    val priceTable: Int? = 0,
    val latitude: Double? = 0.0,
    val longitude: Double? = 0.0,
    var distance: Double? = null
)
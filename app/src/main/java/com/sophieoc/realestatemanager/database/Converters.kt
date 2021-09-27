package com.sophieoc.realestatemanager.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.sophieoc.realestatemanager.model.Address
import com.sophieoc.realestatemanager.model.Photo
import com.sophieoc.realestatemanager.util.PropertyAvailability
import com.sophieoc.realestatemanager.util.PropertyType
import java.util.*

class Converters {

    @TypeConverter
    fun listPhotoToJson(value: List<Photo>?): String = Gson().toJson(value)

    @TypeConverter
    fun jsonToListPhoto(value: String) = Gson().fromJson(value, Array<Photo>::class.java).toList()

    @TypeConverter
    fun enumToJson(value: PropertyType?): String = Gson().toJson(value)

    @TypeConverter
    fun jsonToEnumPropertyType(value: String): PropertyType = Gson().fromJson(value, PropertyType::class.java)

    @TypeConverter
    fun enumToJson(value: PropertyAvailability?): String = Gson().toJson(value)

    @TypeConverter
    fun jsonToEnumPropertyAvailability(value: String): PropertyAvailability = Gson().fromJson(value, PropertyAvailability::class.java)

    @TypeConverter
    fun addressToJson(value: Address?): String = Gson().toJson(value)

    @TypeConverter
    fun jsonToAddress(value: String): Address = Gson().fromJson(value, Address::class.java)

    @TypeConverter
    fun toDate(dateLong: Long?): Date? {
        return dateLong?.let { Date(it) }
    }

    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }
}
package com.p2lem8dev.internetRadio.database.converters


import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class JsonConverters {

    @TypeConverter
    fun getListOfStringsFromJson(json: String): ArrayList<String>? {
        val listType = object : TypeToken<ArrayList<String>>() {

        }.type
        return Gson().fromJson<ArrayList<String>>(json, listType)
    }

    @TypeConverter
    fun putListOfStringsToJson(arrayList: ArrayList<String>): String {
        return Gson().toJson(arrayList)
    }

    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun toDate(dateTime: Long?): Date? {
         return if (dateTime == null) null else Date(dateTime)
    }
}
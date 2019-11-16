package com.p2lem8dev.internetRadio.database.converters


import android.util.Log
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.Exception
import java.util.*

class JsonConverters {

    @TypeConverter
    fun getListOfStringsFromJson(json: String?): ArrayList<String>? {
        if (json == null) return null
        val listType = object : TypeToken<ArrayList<String>>() {}.type

        return try {
            Gson().fromJson<ArrayList<String>>(json, listType)
        } catch (e: Exception) {
            Log.wtf("SYNC", json)
            null
        }
    }

    @TypeConverter
    fun putListOfStringsToJson(arrayList: ArrayList<String>?): String? {
        if (arrayList == null) return null
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
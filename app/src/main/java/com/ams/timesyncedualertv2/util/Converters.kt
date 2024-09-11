package com.ams.timesyncedualertv2.util
import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromIntList(value: List<Int>): String {
        return value.joinToString(",")  // 将 List<Int> 转换为逗号分隔的字符串
    }

    @TypeConverter
    fun toIntList(value: String): List<Int> {
        return if (value.isNotEmpty()) {
            value.split(",").map { it.trim().toInt() }  // 将字符串转换为 List<Int>
        } else {
            emptyList()
        }
    }
}

package com.ams.timesyncedualertv2.model

import android.graphics.Color
import java.time.LocalTime

data class Course(
    val weekday: List<Int>,
    val times: Pair<LocalTime, LocalTime>,
    val name: String,
    val location: String,
    val description: String,
    val color: Int
)

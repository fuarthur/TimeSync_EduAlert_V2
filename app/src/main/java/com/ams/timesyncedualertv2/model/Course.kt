package com.ams.timesyncedualertv2.model

import java.util.Date

data class Course(
    val weekday: List<Int>,
    val times: Pair<Date?, Date?>,
    val name: String,
    val location: String,
    val description: String,
    val color: Int
)

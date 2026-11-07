package com.ams.timesyncedualertv2.domain

import com.ams.timesyncedualertv2.model.CourseEntity
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser

object BackupCodec {
    const val MAX_BYTES = 1_048_576
    const val MAX_COURSES = 1000

    fun decode(json: String): List<CourseEntity> {
        try {
            require(json.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) {
                "Backup is larger than 1 MB"
            }
            val root = JsonParser.parseString(json)
            val array =
                if (root.isJsonArray) root.asJsonArray
                else {
                    require(root.isJsonObject) { "Expected a timetable backup" }
                    val document = root.asJsonObject
                    val version = document.get("schemaVersion")
                    require(
                        version != null &&
                            version.isJsonPrimitive &&
                            version.asJsonPrimitive.isNumber &&
                            version.asString == "1",
                    ) {
                        "Unsupported backup version"
                    }
                    val values = document.get("courses")
                    require(values != null && values.isJsonArray) { "Courses are missing" }
                    values.asJsonArray
                }
            require(array.size() <= MAX_COURSES) { "Too many courses" }
            val courses = array.map { value ->
                require(value.isJsonObject) { "Each course must be an object" }
                val o = value.asJsonObject
                val days = o.get("weekday")
                require(days != null && days.isJsonArray) { "Weekdays are missing" }
                CourseRules.validate(
                    CourseEntity(
                        id = if (o.has("id")) integer(o, "id") else 0,
                        weekday =
                            days.asJsonArray.map { day ->
                                require(
                                    day.isJsonPrimitive && day.asString.matches(Regex("[0-9]+")),
                                ) {
                                    "Invalid weekday"
                                }
                                day.asString.toInt()
                            },
                        startTime = text(o, "startTime"),
                        endTime = text(o, "endTime"),
                        name = text(o, "name"),
                        location = text(o, "location"),
                        description = text(o, "description"),
                        color = integer(o, "color"),
                    ),
                )
            }
            val ids = courses.map { it.id }.filter { it != 0 }
            require(ids.distinct().size == ids.size) { "Duplicate course IDs" }
            return CourseRules.validateSchedule(courses)
        } catch (e: Exception) {
            throw IllegalArgumentException(e.message ?: "Invalid timetable backup", e)
        }
    }

    fun encode(courses: List<CourseEntity>): String {
        require(courses.size <= MAX_COURSES) { "Too many courses" }
        val validated = courses.map(CourseRules::validate)
        val gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
        val document =
            JsonObject().apply {
                addProperty("schemaVersion", 1)
                addProperty("format", "timesync-timetable")
                add("courses", gson.toJsonTree(validated))
            }
        val json = gson.toJson(document)
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup is larger than 1 MB" }
        return json
    }

    private fun text(o: JsonObject, key: String): String {
        val value = o.get(key)
        require(value != null && value.isJsonPrimitive && value.asJsonPrimitive.isString) {
            "Missing or invalid $key"
        }
        return value.asString
    }

    private fun integer(o: JsonObject, key: String): Int {
        val value = o.get(key)
        require(
            value != null && value.isJsonPrimitive && value.asString.matches(Regex("-?[0-9]+")),
        ) {
            "Invalid $key"
        }
        return value.asString.toInt()
    }
}

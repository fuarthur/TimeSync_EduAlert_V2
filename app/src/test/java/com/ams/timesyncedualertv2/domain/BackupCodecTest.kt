package com.ams.timesyncedualertv2.domain

import org.junit.Assert.*
import org.junit.Test

class BackupCodecTest {
    private fun json(days: String = "[1]", start: String = "09:00", end: String = "10:00") =
        """[{"id":1,"weekday":$days,"startTime":"$start","endTime":"$end","name":"Math","location":"Hall","description":"","color":-65536}]"""

    @Test
    fun acceptsLegacyCourseArray() {
        assertEquals("Math", BackupCodec.decode(json()).single().name)
    }

    @Test
    fun rejectsEmptyWeekdays() {
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(json("[]")) }
    }

    @Test
    fun rejectsMalformedTime() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decode(json(start = "bad"))
        }
    }

    @Test
    fun rejectsOutOfRangeDay() {
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(json("[8]")) }
    }

    @Test
    fun rejectsNullAndMissingFields() {
        for (value in listOf("null", "[null]", "[{}]")) assertThrows(
            IllegalArgumentException::class.java,
        ) {
            BackupCodec.decode(value)
        }
    }

    @Test
    fun rejectsDuplicateIds() {
        val one = json().removePrefix("[").removeSuffix("]")
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode("[$one,$one]") }
    }

    @Test
    fun overlappingCoursesAreRejectedBeforePreview() {
        val one = json().removePrefix("[").removeSuffix("]")
        val second = one.replace("\"id\":1", "\"id\":2")
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode("[$one,$second]") }
    }
}

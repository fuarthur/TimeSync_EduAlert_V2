package com.ams.timesyncedualertv2.domain

import com.ams.timesyncedualertv2.model.CourseEntity
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class VersionedBackupTest {
    @Test
    fun versionedBackupRoundTripsAllCourseFields() {
        val courses =
            listOf(
                CourseEntity(
                    42,
                    listOf(1, 7),
                    "09:00",
                    "10:30",
                    "数学 & Art",
                    "教室 101",
                    "Bring a notebook\n第二页",
                    -65536,
                ),
            )
        val json = BackupCodec.encode(courses)
        assertEquals(1, JsonParser.parseString(json).asJsonObject.get("schemaVersion").asInt)
        assertEquals(courses, BackupCodec.decode(json))
    }

    @Test
    fun emptyTimetableStillProducesUsableBackup() {
        assertTrue(BackupCodec.decode(BackupCodec.encode(emptyList())).isEmpty())
    }

    @Test
    fun unknownVersionIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decode("""{"schemaVersion":99,"courses":[]}""")
        }
    }
}

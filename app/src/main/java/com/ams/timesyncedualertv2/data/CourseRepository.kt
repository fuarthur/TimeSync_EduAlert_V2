package com.ams.timesyncedualertv2.data

import androidx.room.withTransaction
import com.ams.timesyncedualertv2.db.AppDatabase
import com.ams.timesyncedualertv2.domain.CourseRules
import com.ams.timesyncedualertv2.model.CourseEntity

class CourseConflictException(val courseName: String) :
    IllegalArgumentException("Conflicts with $courseName")

class CourseMissingException : IllegalArgumentException("This course no longer exists")

class CourseRepository(private val db: AppDatabase) {
    private val dao = db.courseDao()
    val courses = dao.observeAll()

    suspend fun all() = dao.getAllCourses()

    suspend fun get(id: Int) = dao.getCourseById(id)

    suspend fun save(course: CourseEntity): Int = db.withTransaction {
        val valid = CourseRules.validate(course)
        if (valid.id != 0 && dao.getCourseById(valid.id) == null) throw CourseMissingException()
        val others = dao.getAllCourses().filter { it.id != valid.id }
        others
            .firstOrNull { CourseRules.overlaps(it, valid) }
            ?.let { throw CourseConflictException(it.name) }
        if (valid.id == 0) insertWithId(valid.copy(id = freeId(others.map { it.id }.toSet())))
        else {
            dao.update(valid)
            valid.id
        }
    }

    suspend fun replace(courses: List<CourseEntity>) {
        val valid = CourseRules.validateSchedule(courses)
        require(valid.size <= 1000) { "Too many courses" }
        val ids = valid.map { it.id }.filter { it != 0 }
        require(ids.distinct().size == ids.size) { "Duplicate course IDs" }
        val used = valid.map { it.id }.filter { it > 0 }.toMutableSet()
        val allocated = valid.map { course ->
            if (course.id != 0) course else course.copy(id = freeId(used).also { used.add(it) })
        }
        db.withTransaction {
            dao.deleteAllCourses()
            dao.insertAll(allocated)
        }
    }

    suspend fun delete(id: Int): CourseEntity? = db.withTransaction {
        dao.getCourseById(id)?.also { dao.delete(it) }
    }

    suspend fun restore(course: CourseEntity): Int = db.withTransaction {
        var valid = CourseRules.validate(course)
        val others = dao.getAllCourses()
        others
            .firstOrNull { CourseRules.overlaps(it, valid) }
            ?.let { throw CourseConflictException(it.name) }
        if (valid.id == 0 || others.any { it.id == valid.id })
            valid = valid.copy(id = freeId(others.map { it.id }.toSet()))
        insertWithId(valid)
    }

    private fun freeId(used: Set<Int>): Int {
        val maximum = used.maxOrNull() ?: 0
        if (maximum < Int.MAX_VALUE) return maxOf(1, maximum + 1)
        var candidate = 1
        while (candidate in used) {
            check(candidate < Int.MAX_VALUE) { "No course IDs available" }
            candidate++
        }
        return candidate
    }

    private suspend fun insertWithId(course: CourseEntity): Int {
        check(course.id > 0)
        check(dao.insert(course) == course.id.toLong()) { "Unexpected database ID" }
        return course.id
    }

    suspend fun clear() = dao.deleteAllCourses()
}

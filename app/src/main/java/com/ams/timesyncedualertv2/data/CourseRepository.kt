package com.ams.timesyncedualertv2.data

import androidx.room.withTransaction
import com.ams.timesyncedualertv2.db.AppDatabase
import com.ams.timesyncedualertv2.domain.CourseRules
import com.ams.timesyncedualertv2.model.CourseEntity

class CourseConflictException(val courseName:String):IllegalArgumentException("Conflicts with $courseName")
class CourseMissingException:IllegalArgumentException("This course no longer exists")

class CourseRepository(private val db:AppDatabase) {
    private val dao=db.courseDao()
    val courses=dao.observeAll()
    suspend fun all()=dao.getAllCourses()
    suspend fun get(id:Int)=dao.getCourseById(id)

    suspend fun save(course:CourseEntity):Int = db.withTransaction {
        val valid=CourseRules.validate(course)
        if(valid.id!=0 && dao.getCourseById(valid.id)==null) throw CourseMissingException()
        val others=dao.getAllCourses().filter { it.id!=valid.id }
        others.firstOrNull { CourseRules.overlaps(it,valid) }?.let { throw CourseConflictException(it.name) }
        if(valid.id==0) dao.insert(valid).toInt() else {
            dao.update(valid)
            valid.id
        }
    }

    suspend fun replace(courses:List<CourseEntity>) {
        val valid=courses.map(CourseRules::validate)
        require(valid.size<=1000) { "Too many courses" }
        val ids=valid.map { it.id }.filter { it!=0 }
        require(ids.distinct().size==ids.size) { "Duplicate course IDs" }
        db.withTransaction { dao.deleteAllCourses(); dao.insertAll(valid) }
    }

    suspend fun delete(id:Int):CourseEntity? = db.withTransaction {
        dao.getCourseById(id)?.also { dao.delete(it) }
    }
    suspend fun restore(course:CourseEntity):Int = db.withTransaction {
        var valid=CourseRules.validate(course)
        val others=dao.getAllCourses()
        others.firstOrNull { CourseRules.overlaps(it,valid) }?.let { throw CourseConflictException(it.name) }
        if(others.any { it.id==valid.id }) valid=valid.copy(id=0)
        dao.insert(valid).toInt()
    }
    suspend fun clear()=dao.deleteAllCourses()
}

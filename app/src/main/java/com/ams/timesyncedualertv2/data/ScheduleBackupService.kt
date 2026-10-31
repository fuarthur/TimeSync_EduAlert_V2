package com.ams.timesyncedualertv2.data

import android.content.Context
import android.net.Uri
import com.ams.timesyncedualertv2.domain.BackupCodec
import com.ams.timesyncedualertv2.model.CourseEntity
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

class ScheduleBackupService(context:Context,private val repository:CourseRepository) {
    private val resolver=context.applicationContext.contentResolver
    suspend fun read(uri:Uri):List<CourseEntity> = withContext(Dispatchers.IO) {
        val bytes=resolver.openInputStream(uri)?.use { input ->
            val output=ByteArrayOutputStream()
            val buffer=ByteArray(8192)
            while(true) {
                val count=input.read(buffer)
                if(count==-1) break
                require(output.size()+count<=BackupCodec.MAX_BYTES) { "Backup is larger than 1 MB" }
                output.write(buffer,0,count)
            }
            output.toByteArray()
        } ?: throw IOException("Cannot open backup")
        BackupCodec.decode(bytes.toString(Charsets.UTF_8))
    }
    suspend fun write(uri:Uri) = withContext(Dispatchers.IO) {
        val json=Gson().toJson(repository.all())
        resolver.openOutputStream(uri,"wt")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(json) }
            ?: throw IOException("Cannot create backup")
    }
}

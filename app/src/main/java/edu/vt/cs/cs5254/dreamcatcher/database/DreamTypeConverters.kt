package edu.vt.cs.cs5254.dreamcatcher.database

import androidx.room.TypeConverter
import edu.vt.cs.cs5254.dreamcatcher.DreamEntryKind
import java.util.Date
import java.util.UUID

class DreamTypeConverters {
    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }
    @TypeConverter
    fun toDate(millisSinceEpoch: Long?): Date? {
        return millisSinceEpoch?.let {
            Date(it)
        }
    }
    @TypeConverter
    fun toUUID(uuid: String?): UUID? {
        return UUID.fromString(uuid)
    }
    @TypeConverter
    fun fromUUID(uuid: UUID?): String? {
        return uuid?.toString()
    }

    @TypeConverter
    fun toDreamEntryKind(str:String): DreamEntryKind {
        return DreamEntryKind.valueOf(str)
    }

    @TypeConverter
    fun fromDreamEntryKind(kind: DreamEntryKind): String? {
        return kind.name
    }
}
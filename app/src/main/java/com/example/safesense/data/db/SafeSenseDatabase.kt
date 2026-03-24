package com.example.safesense.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.safesense.data.db.dao.ContactDao
import com.example.safesense.data.db.dao.IncidentDao
import com.example.safesense.data.db.entity.ContactEntity
import com.example.safesense.data.db.entity.IncidentEntity

@Database(
    entities = [ContactEntity::class, IncidentEntity::class],
    version = 2,
    exportSchema = false
)
abstract class SafeSenseDatabase : RoomDatabase() {

    abstract fun contactDao(): ContactDao
    abstract fun incidentDao(): IncidentDao
}

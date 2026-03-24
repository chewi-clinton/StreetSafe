package com.example.safesense.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.safesense.data.db.dao.ContactDao
import com.example.safesense.data.db.entity.ContactEntity

// entities = every table in this database. Add IncidentEntity and SessionEntity here later.
// version = 1 for now. Bump this number whenever you change a table's structure.
// exportSchema = false keeps things simple while building — set to true before production.
@Database(
    entities = [ContactEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SafeSenseDatabase : RoomDatabase() {

    // Room generates the implementation of this automatically
    abstract fun contactDao(): ContactDao
}
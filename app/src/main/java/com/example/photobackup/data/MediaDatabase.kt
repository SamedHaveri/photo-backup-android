package com.example.photobackup.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.photobackup.data.dao.MediaBackupDao
import com.example.photobackup.data.entity.MediaBackup

@Database(entities = [MediaBackup::class], version = 6, exportSchema = false)
abstract class MediaDatabase : RoomDatabase() {
    abstract fun mediaBackup(): MediaBackupDao

    companion object {
        @Volatile
        private var INSTANCE: MediaDatabase? = null

        fun getDatabase(context: Context): MediaDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null)
                return tempInstance
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MediaDatabase::class.java,
                    "media_database")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
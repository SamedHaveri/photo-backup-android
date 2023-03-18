package com.example.photobackup.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.photobackup.data.dao.MediaToUploadDao
import com.example.photobackup.data.dao.UploadedMedediaDao
import com.example.photobackup.data.entity.MediaToUpload
import com.example.photobackup.data.entity.UploadedMedia

@Database(entities = [UploadedMedia::class, MediaToUpload::class], version = 4, exportSchema = false)
abstract class MediaDatabase : RoomDatabase() {
    abstract fun uploadedMediaDao(): UploadedMedediaDao
    abstract fun mediaToUploadDao(): MediaToUploadDao

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
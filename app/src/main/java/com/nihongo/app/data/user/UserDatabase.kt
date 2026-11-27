package com.nihongo.app.data.user

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SrsCardEntity::class,
        StudySessionEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class UserDatabase : RoomDatabase() {

    abstract fun studyDao(): StudyDao

    companion object {
        private const val DATABASE_NAME = "user.db"

        fun create(context: Context): UserDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                UserDatabase::class.java,
                DATABASE_NAME,
            ).build()
    }
}
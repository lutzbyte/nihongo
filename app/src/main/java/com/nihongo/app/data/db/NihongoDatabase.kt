package com.nihongo.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        VocabularyEntity::class,
        KanjiEntity::class,
        GrammarEntity::class,
        DatasetStateEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class NihongoDatabase : RoomDatabase() {

    abstract fun curriculumDao(): CurriculumDao

    companion object {
        private const val DATABASE_NAME = "nihongo.db"

        fun create(context: Context): NihongoDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                NihongoDatabase::class.java,
                DATABASE_NAME,
            ).build()
    }
}
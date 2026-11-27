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
        KanaEntity::class,
        LessonEntity::class,
        DatasetStateEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class NihongoDatabase : RoomDatabase() {

    abstract fun curriculumDao(): CurriculumDao

    abstract fun practiceDao(): PracticeDao

    companion object {
        private const val DATABASE_NAME = "nihongo.db"

        /**
         * Database ini HANYA berisi materi yang bisa diimpor ulang, jadi tabel dibuang saat
         * skema naik. Data belajar pengguna ada di UserDatabase dan tidak terpengaruh.
         */
        fun create(context: Context): NihongoDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                NihongoDatabase::class.java,
                DATABASE_NAME,
            )
                .fallbackToDestructiveMigration(true)
                .build()
    }
}
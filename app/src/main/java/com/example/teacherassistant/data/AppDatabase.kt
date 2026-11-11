package com.example.teacherassistant.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "schools")
data class School(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "school_classes",
    foreignKeys = [ForeignKey(
        entity = School::class,
        parentColumns = ["id"],
        childColumns = ["schoolId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("schoolId")]
)
data class SchoolClass(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolId: Long,
    val name: String
)

@Entity(tableName = "lessons")
data class Lesson(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "class_lessons",
    primaryKeys = ["classId", "lessonId"],
    foreignKeys = [
        ForeignKey(entity = SchoolClass::class, parentColumns = ["id"], childColumns = ["classId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Lesson::class, parentColumns = ["id"], childColumns = ["lessonId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("lessonId")]
)
data class ClassLessonCrossRef(val classId: Long, val lessonId: Long)

@Dao
interface SchoolDao {
    @Query("SELECT * FROM schools ORDER BY name")
    fun observeAll(): Flow<List<School>>

    @Insert
    suspend fun insert(school: School)

    @Delete
    suspend fun delete(school: School)
}

@Dao
interface ClassDao {
    @Query("SELECT * FROM school_classes ORDER BY name")
    fun observeAll(): Flow<List<SchoolClass>>

    @Insert
    suspend fun insert(item: SchoolClass)

    @Delete
    suspend fun delete(item: SchoolClass)
}

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons ORDER BY name")
    fun observeAll(): Flow<List<Lesson>>

    @Insert
    suspend fun insert(item: Lesson)

    @Delete
    suspend fun delete(item: Lesson)
}

@Dao
interface ClassLessonDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun link(item: ClassLessonCrossRef)

    @Query("SELECT lessonId FROM class_lessons WHERE classId = :classId")
    suspend fun lessonIdsForClass(classId: Long): List<Long>
}

@Database(
    entities = [School::class, SchoolClass::class, Lesson::class, ClassLessonCrossRef::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun schoolDao(): SchoolDao
    abstract fun classDao(): ClassDao
    abstract fun lessonDao(): LessonDao
    abstract fun classLessonDao(): ClassLessonDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "teacher_assistant.db"
                ).build().also { INSTANCE = it }
            }
    }
}


package com.example.teacherassistant.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        ForeignKey(
            entity = SchoolClass::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Lesson::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("lessonId")]
)
data class ClassLessonCrossRef(
    val classId: Long,
    val lessonId: Long
)

// جدول دانش‌آموزان
@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(
            entity = SchoolClass::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("classId"),
        Index(
            value = ["classId", "studentNumber"],
            unique = true
        )
    ]
)
data class Student(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val classId: Long,
    val lastName: String,
    val firstName: String,
    val studentNumber: String,
    val fatherName: String
)

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

// مدیریت اطلاعات دانش‌آموزان
@Dao
interface StudentDao {

    @Query("""
        SELECT * FROM students
        WHERE classId = :classId
        ORDER BY lastName, firstName
    """)
    fun observeByClass(classId: Long): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(student: Student): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(students: List<Student>): List<Long>

    @Update
    suspend fun update(student: Student)

    @Delete
    suspend fun delete(student: Student)

    @Query("""
        SELECT * FROM students
        WHERE classId = :classId
        AND (
            lastName LIKE '%' || :query || '%'
            OR firstName LIKE '%' || :query || '%'
            OR studentNumber LIKE '%' || :query || '%'
        )
        ORDER BY lastName, firstName
    """)
    fun search(classId: Long, query: String): Flow<List<Student>>
}

@Database(
    entities = [
        School::class,
        SchoolClass::class,
        Lesson::class,
        ClassLessonCrossRef::class,
        Student::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun schoolDao(): SchoolDao
    abstract fun classDao(): ClassDao
    abstract fun lessonDao(): LessonDao
    abstract fun classLessonDao(): ClassLessonDao
    abstract fun studentDao(): StudentDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS students (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        classId INTEGER NOT NULL,
                        lastName TEXT NOT NULL,
                        firstName TEXT NOT NULL,
                        studentNumber TEXT NOT NULL,
                        fatherName TEXT NOT NULL,
                        FOREIGN KEY(classId)
                        REFERENCES school_classes(id)
                        ON UPDATE NO ACTION
                        ON DELETE CASCADE
                    )
                """)

                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_students_classId
                    ON students(classId)
                """)

                db.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    index_students_classId_studentNumber
                    ON students(classId, studentNumber)
                """)
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "teacher_assistant.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

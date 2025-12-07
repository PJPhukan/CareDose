package com.example.caredose.database.dao
import androidx.room.*
import com.example.caredose.database.entities.User

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: User): Long

    @Update
    suspend fun update(user: User)

    @Delete
    suspend fun delete(user: User)

    @Query("SELECT * FROM users WHERE userId = :id")
    suspend fun getById(id: Long): User?


    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): User?

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE phone = :phone)")
    suspend fun isPhoneExists(phone: String): Boolean

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<User>
}
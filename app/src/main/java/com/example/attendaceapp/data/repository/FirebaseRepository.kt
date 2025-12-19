package com.example.attendaceapp.data.repository

import com.example.attendaceapp.data.model.AttendanceRecord
import com.example.attendaceapp.data.model.AttendanceSession
import com.example.attendaceapp.data.model.AttendanceStatus
import com.example.attendaceapp.data.model.User
import com.example.attendaceapp.data.model.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.mindrot.jbcrypt.BCrypt

class FirebaseRepository {
    private val db = FirebaseFirestore.getInstance()

    // Collection
    private val usersCollection = db.collection("users")
    private val coursesCollection = db.collection("courses")
    private val sessionsCollection = db.collection("attendance_sessions")
    private val recordsCollection = db.collection("attendance_records")

    // AUTHENTICATION 
    suspend fun login(nim: String, password: String): Result<User> {
        return try {
            val snapshot = db.collection("users")
                .whereEqualTo("nim", nim)
                .get()
                .await()

            if (snapshot.isEmpty) {
                return Result.failure(Exception("NIM atau password tidak ditemukan"))
            }

            val document = snapshot.documents[0]
            val user = document.toObject(User::class.java)

            if (user == null) {
                return Result.failure(Exception("Error parsing user data"))
            }

            if (!user.isActive) {
                return Result.failure(Exception("Akun tidak aktif. Hubungi admin."))
            }

            val passwordValid = BCrypt.checkpw(password, user.passwordHash)

            if (!passwordValid) {
                return Result.failure(Exception("NIM atau password salah"))
            }

            // Update last login
            db.collection("users")
                .document(user.id)
                .update("lastLogin", System.currentTimeMillis())
                .await()

            Result.success(user)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Terjadi kesalahan: ${e.message}"))
        }
    }

    // User Management (Lecturer)
    suspend fun createStudent(
        nim: String,
        name: String,
        email: String,
        department: String,
        defaultPassword: String
    ): Result<User> {
        return try {
            // Check if NIM already exists
            val existing = usersCollection
                .whereEqualTo("nim", nim)
                .get()
                .await()

            if (!existing.isEmpty) {
                return Result.failure(Exception("NIM sudah terdaftar"))
            }

            val hashedPassword = BCrypt.hashpw(defaultPassword, BCrypt.gensalt())
            val user = User(
                id = usersCollection.document().id,
                nim = nim,
                name = name,
                role = UserRole.STUDENT,
                passwordHash = hashedPassword,
                department = department,
            )

            usersCollection.document(user.id).set(user).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Attendance Session Management
    suspend fun createAttendanceSession(session: AttendanceSession): Result<String> {
        return try {
            val docRef = sessionsCollection.document()
            val newSession = session.copy(id = docRef.id)
            docRef.set(newSession).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveSessionsByCourse(courseId: String): List<AttendanceSession> {
        return try {
            sessionsCollection
                .whereEqualTo("courseId", courseId)
                .whereEqualTo("isActive", true)
                .get()
                .await()
                .toObjects(AttendanceSession::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Attendance Recording for Students
    suspend fun recordAttendance(
        sessionId: String,
        studentNIM: String,
        studentName: String
    ): Result<AttendanceRecord> {
        return try {
            // Validate session
            val session = sessionsCollection.document(sessionId)
                .get()
                .await()
                .toObject(AttendanceSession::class.java)
                ?: return Result.failure(Exception("Sesi tidak ditemukan"))

            if (!session.isActive) {
                return Result.failure(Exception("Sesi sudah tidak aktif"))
            }

            if (System.currentTimeMillis() > session.expiresAt) {
                return Result.failure(Exception("QR Code sudah exprired"))
            }

            // Check duplicate
            val existing = recordsCollection
                .whereEqualTo("sessionId", sessionId)
                .whereEqualTo("studentNIM", studentNIM)
                .get()
                .await()

            if (!existing.isEmpty) {
                return Result.failure(Exception("Anda sudah melakukan absensi untuk sesi ini"))
            }

            // Calculate status (late or on time)
            val now = System.currentTimeMillis()
            val lateThresholdMillis = session.lateThreshold * 60 * 1000
            val isLate = (now - session.createdAt) > lateThresholdMillis

            val record = AttendanceRecord(
                id = recordsCollection.document().id,
                sessionId = sessionId,
                courseId = session.courseId,
                courseName = session.courseName,
                studentNIM = studentNIM,
                studentName = studentName,
                lecturerId = session.lecturerId,
                status = if (isLate) AttendanceStatus.LATE else AttendanceStatus.PRESENT
            )

            recordsCollection.document(record.id).set(record).await()

            // Update attendance count
            sessionsCollection.document(sessionId)
                .update("attendanceCount", session.attendanceCount + 1)
                .await()


            Result.success(record)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Attendance History
    suspend fun getStudentAttendanceHistory(nim: String): List<AttendanceRecord> {
        return try {
            recordsCollection
                .whereEqualTo("studentNIM", nim)
                .get()
                .await()
                .toObjects(AttendanceRecord::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Untuk melihat siapa saja yang sudah absen di sesi tertentu
    suspend fun getSessionAttendanceRecords(sessionId: String): List<AttendanceRecord> {
        return try {
            recordsCollection
                .whereEqualTo("sessionId", sessionId)
                .get()
                .await()
                .toObjects(AttendanceRecord::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
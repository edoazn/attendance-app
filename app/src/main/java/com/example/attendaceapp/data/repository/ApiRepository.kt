package com.example.attendaceapp.data.repository

import com.example.attendaceapp.data.model.AttendanceRecord
import com.example.attendaceapp.data.model.AttendanceSession
import com.example.attendaceapp.data.model.AttendanceStatus
import com.example.attendaceapp.data.model.User
import com.example.attendaceapp.data.model.UserRole

open class ApiRepository {
    private val users = mutableListOf(
        User(
            id = "lecturer-1",
            nim = "1001",
            name = "Dosen",
            email = "lecturer@example.com",
            role = UserRole.LECTURER
        ),
        User(
            id = "student-1",
            nim = "2001",
            name = "Mahasiswa",
            email = "student@example.com",
            role = UserRole.STUDENT
        )
    )

    private val sessions = mutableListOf<AttendanceSession>()
    private val records = mutableListOf<AttendanceRecord>()

    open suspend fun login(nim: String, password: String): Result<User> {
        if (nim.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("NIM dan password wajib diisi"))
        }

        val user = users.firstOrNull { it.nim.equals(nim, ignoreCase = true) }
            ?: return Result.failure(Exception("NIM atau password tidak ditemukan"))

        return Result.success(user)
    }

    open suspend fun createStudent(
        nim: String,
        name: String,
        email: String,
        department: String,
        defaultPassword: String
    ): Result<User> {
        if (nim.isBlank() || name.isBlank() || defaultPassword.isBlank()) {
            return Result.failure(IllegalArgumentException("Data mahasiswa belum lengkap"))
        }

        if (users.any { it.nim.equals(nim, ignoreCase = true) }) {
            return Result.failure(Exception("NIM sudah terdaftar"))
        }

        val user = User(
            id = "student-${users.size + 1}",
            nim = nim,
            name = name,
            email = email,
            role = UserRole.STUDENT,
            department = department,
            passwordHash = defaultPassword
        )
        users.add(user)
        return Result.success(user)
    }

    open suspend fun createAttendanceSession(session: AttendanceSession): Result<String> {
        val id = "session-${sessions.size + 1}"
        sessions.add(session.copy(id = id))
        return Result.success(id)
    }

    open suspend fun getActiveSessionsByCourse(courseId: String): List<AttendanceSession> {
        return sessions.filter { it.courseId == courseId && it.isActive }
    }

    open suspend fun recordAttendance(
        sessionId: String,
        studentNIM: String,
        studentName: String
    ): Result<AttendanceRecord> {
        val session = sessions.firstOrNull { it.id == sessionId }
            ?: return Result.failure(Exception("Sesi tidak ditemukan"))

        val alreadyRecorded = records.any { it.sessionId == sessionId && it.studentNIM == studentNIM }
        if (alreadyRecorded) {
            return Result.failure(Exception("Anda sudah melakukan absensi untuk sesi ini"))
        }

        val now = System.currentTimeMillis()
        val isLate = (now - session.createdAt) > (session.lateThreshold * 60 * 1000L)

        val record = AttendanceRecord(
            id = "record-${records.size + 1}",
            sessionId = sessionId,
            courseId = session.courseId,
            courseName = session.courseName,
            studentNIM = studentNIM,
            studentName = studentName,
            lecturerId = session.lecturerId,
            status = if (isLate) AttendanceStatus.LATE else AttendanceStatus.PRESENT
        )

        records.add(record)
        return Result.success(record)
    }

    open suspend fun getStudentAttendanceHistory(nim: String): List<AttendanceRecord> {
        return records.filter { it.studentNIM == nim }
    }

    open suspend fun getSessionAttendanceRecords(sessionId: String): List<AttendanceRecord> {
        return records.filter { it.sessionId == sessionId }
    }
}


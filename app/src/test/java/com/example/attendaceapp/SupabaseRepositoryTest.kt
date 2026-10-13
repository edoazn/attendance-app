package com.example.attendaceapp

import com.example.attendaceapp.data.model.User
import com.example.attendaceapp.data.model.UserRole
import com.example.attendaceapp.data.repository.SessionStore
import com.example.attendaceapp.data.repository.SupabaseRepository
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit test SupabaseRepository dengan MockWebServer (tanpa backend asli).
 * Memverifikasi kontrak Edge Functions Task 2:
 * login-with-nim {nim,password} dan submit-attendance {schedule_id, method, ...}.
 */
class SupabaseRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var fakeStore: FakeSessionStore
    private lateinit var repository: SupabaseRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        fakeStore = FakeSessionStore()
        repository = SupabaseRepository(
            context = null,
            functionBaseUrl = server.url("/functions/v1").toString().trimEnd('/'),
            restBaseUrl = server.url("/rest/v1").toString().trimEnd('/'),
            anonKey = "test-anon-key",
            sessionStore = fakeStore
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `login sukses simpan token dan return User`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"token":"t","user":{"id":"11111111-2222-3333-4444-555555555555","identity_number":"2114","name":"Budi","email":"budi@kampus.ac.id","role":"STUDENT"}}
                """.trimIndent()
            )
        )

        val result = repository.login("2114", "pw")

        assertTrue(result.isSuccess)
        val user = result.getOrThrow()
        assertEquals("2114", user.nim)
        assertEquals("Budi", user.name)
        assertEquals(UserRole.STUDENT, user.role)
        assertEquals("t", fakeStore.token)

        val request = server.takeRequest()
        assertEquals("/functions/v1/login-with-nim", request.path)
        assertTrue(request.body.readUtf8().contains("\"nim\""))
    }

    @Test
    fun `login gagal 401 kembalikan pesan generik tanpa simpan token`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(401).setBody(
                """{"error":"NIM atau kata sandi salah."}"""
            )
        )

        val result = repository.login("2114", "salah")

        assertTrue(result.isFailure)
        assertEquals("NIM atau kata sandi salah.", result.exceptionOrNull()?.message)
        assertNull(fakeStore.token)
    }

    @Test
    fun `getTodaySchedules sukses parsing DTO termasuk flag dan kolom R1`() = runBlocking {
        fakeStore.token = "tok"
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                [{"id":1,"course_name":"Pemrograman Mobile","lecturer_name":"Dr. Ani","room":"R101","start_time":"08:00:00","end_time":"10:00:00","day":"Monday","is_active":true,"has_qr":true,"has_active_code":false,"late_threshold_minutes":15,"latitude":-6.2,"longitude":106.8,"radius_meters":100}]
                """.trimIndent()
            )
        )

        val result = repository.getTodaySchedules()

        assertTrue(result.isSuccess)
        val list = result.getOrThrow()
        assertEquals(1, list.size)
        assertEquals("Pemrograman Mobile", list[0].courseName)
        assertEquals(true, list[0].hasQr)
        assertEquals(15, list[0].lateThresholdMinutes)
        assertEquals("08:00", list[0].startTimeShort())

        val request = server.takeRequest()
        assertTrue(request.path!!.startsWith("/rest/v1/today_schedules_with_flags"))
        assertEquals("Bearer tok", request.getHeader("Authorization"))
    }

    @Test
    fun `submit QR sukses status HADIR verbatim dan created_at ISO`() = runBlocking {
        fakeStore.token = "tok"
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"status":"HADIR","method":"qr_code","attendance":{"id":7,"status":"HADIR","method":"qr_code","created_at":"2026-10-13T08:00:00+07:00"}}
                """.trimIndent()
            )
        )

        val result = repository.submitAttendanceQr(1, "qr-token-uuid")

        assertTrue(result.isSuccess)
        val data = result.getOrThrow()
        assertEquals("HADIR", data.status)
        assertEquals("qr_code", data.method)
        assertEquals("2026-10-13T08:00:00+07:00", data.attendance?.createdAt)
    }

    @Test
    fun `submit duplikat 409 kembalikan pesan duplikat`() = runBlocking {
        fakeStore.token = "tok"
        server.enqueue(
            MockResponse().setResponseCode(409).setBody(
                """{"error":"Kehadiran sudah tercatat untuk jadwal ini."}"""
            )
        )

        val result = repository.submitAttendanceCode(1, "ABC123")

        assertTrue(result.isFailure)
        assertEquals(
            "Kehadiran sudah tercatat untuk jadwal ini.",
            result.exceptionOrNull()?.message
        )
    }

    class FakeSessionStore : SessionStore {
        @JvmField var token: String? = null
        var user: User? = null

        override fun getToken(): String? = token

        override fun save(token: String, user: User) {
            this.token = token
            this.user = user
        }

        override fun clear() {
            token = null
            user = null
        }
    }
}

package com.example.attendaceapp

import com.example.attendaceapp.data.repository.SupabaseRepository
import com.example.attendaceapp.ui.screens.history.HistoryUiState
import com.example.attendaceapp.ui.screens.history.HistoryViewModel
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit test Task 4: riwayat dari Supabase (tanpa dummy) + profil dari sesi asli.
 * Pakai MockWebServer (tanpa backend asli); hanya assert state riwayat
 * (kosong → flag empty-state true) tanpa field ViewModel lain yang tak relevan.
 */
class HistoryProfileTest {

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
    fun `history kosong tampilkan empty state`() = runBlocking {
        fakeStore.token = "tok"
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("[]")
        )

        val result = repository.getHistory()

        assertTrue(result.isSuccess)
        val uiState = HistoryUiState(history = result.getOrThrow())
        assertTrue(uiState.history.isEmpty())
        assertTrue(uiState.isEmpty)

        // ViewModel baru mulai dari empty state (belum ada data).
        val viewModel = HistoryViewModel(repository)
        assertTrue(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun `history berisi tampilkan daftar bukan empty state`() = runBlocking {
        fakeStore.token = "tok"
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                [{"id":3,"schedule_id":1,"method":"qr_code","status":"HADIR","distance":12.5,"created_at":"2026-10-13T08:05:00+07:00","schedules":{"id":1,"course_name":"Pemrograman Mobile","lecturer_name":"Dr. Ani","room":"R101","start_time":"08:00:00","end_time":"10:00:00","day":"Monday","is_active":true}}]
                """.trimIndent()
            )
        )

        val result = repository.getHistory()

        assertTrue(result.isSuccess)
        val list = result.getOrThrow()
        assertEquals(1, list.size)
        assertEquals("HADIR", list[0].status)
        assertEquals("Pemrograman Mobile", list[0].schedule?.courseName)
        assertEquals("R101", list[0].schedule?.room)

        val uiState = HistoryUiState(history = list)
        assertFalse(uiState.isEmpty)

        val request = server.takeRequest()
        assertTrue(request.path!!.startsWith("/rest/v1/attendance_records"))
        assertEquals("Bearer tok", request.getHeader("Authorization"))
    }

    @Test
    fun `getHistory tanpa sesi gagal dengan pesan login ulang`() = runBlocking {
        fakeStore.token = null

        val result = repository.getHistory()

        assertTrue(result.isFailure)
        assertEquals(
            "Sesi login tidak ditemukan. Silakan login ulang.",
            result.exceptionOrNull()?.message
        )
    }
}

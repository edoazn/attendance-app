# 📱 AttendanceApp - Aplikasi Presensi Berbasis QR Code

Aplikasi presensi untuk mahasiswa menggunakan QR Code dengan Kotlin, Jetpack Compose, dan Firebase Firestore.

---

## 📋 Project Overview

| Item | Detail |
|------|--------|
| **Platform** | Android |
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose |
| **Backend** | Firebase Firestore |
| **Architecture** | MVVM (Model-View-ViewModel) |
| **Min SDK** | 24 |

---

## 🔐 Sistem Autentikasi

- **Tidak menggunakan Firebase Auth** - Login manual dengan NIM & Password
- **Password di-hash** menggunakan BCrypt
- **Akun mahasiswa dibuat oleh dosen** (tidak ada fitur register)

### User Roles

| Role | Kemampuan |
|------|-----------|
| **LECTURER (Dosen)** | Buat akun mahasiswa, Buat sesi presensi, Generate QR Code, Lihat laporan |
| **STUDENT (Mahasiswa)** | Scan QR Code, Lihat riwayat presensi |

---

## 📁 Project Structure

```
app/src/main/java/com/example/attendaceapp/
├── data/
│   ├── model/
│   │   ├── User.kt                    ✅ Done
│   │   ├── UserRole.kt                ✅ Done
│   │   ├── AttendanceSession.kt       ✅ Done
│   │   ├── AttendanceRecord.kt        ✅ Done
│   │   └── AttendanceStatus.kt        ✅ Done
│   └── repository/
│       └── FirebaseRepository.kt      ✅ Done
│
├── ui/
│   ├── screens/
│   │   ├── auth/
│   │   │   ├── AuthViewModel.kt       ✅ Done
│   │   │   └── LoginScreen.kt         ⏳ Pending
│   │   ├── lecturer/
│   │   │   ├── LecturerViewModel.kt   ✅ Done
│   │   │   ├── LecturerDashboard.kt   ⏳ Pending
│   │   │   ├── CreateStudentScreen.kt ⏳ Pending
│   │   │   └── QRGeneratorScreen.kt   ⏳ Pending
│   │   ├── student/
│   │   │   ├── StudentViewModel.kt    ✅ Done
│   │   │   ├── StudentDashboard.kt    ⏳ Pending
│   │   │   ├── QRScannerScreen.kt     ⏳ Pending
│   │   │   └── AttendanceHistory.kt   ⏳ Pending
│   │   └── profile/
│   │       └── ProfileScreen.kt       ✅ Done (UI)
│   │
│   ├── state/
│   │   ├── AuthState.kt               ✅ Done
│   │   ├── LecturerUiState.kt         ✅ Done
│   │   └── StudentUiState.kt          ✅ Done
│   │
│   ├── components/                    ✅ Done (UI Components)
│   └── theme/                         ✅ Done
│
├── navigation/
│   └── AppNavigation.kt               ✅ Done (UI)
│
└── MainActivity.kt                    ✅ Done
```

---

## 🎯 Development Phases

---

### ✅ Phase 1: Data Layer (COMPLETED)

| Task | Status | File |
|------|--------|------|
| User Model | ✅ Done | `data/model/User.kt` |
| UserRole Enum | ✅ Done | `data/model/UserRole.kt` |
| AttendanceSession Model | ✅ Done | `data/model/AttendanceSession.kt` |
| AttendanceRecord Model | ✅ Done | `data/model/AttendanceRecord.kt` |
| AttendanceStatus Enum | ✅ Done | `data/model/AttendanceStatus.kt` |
| FirebaseRepository | ✅ Done | `data/repository/FirebaseRepository.kt` |

#### Data Models:

**User.kt**
```kotlin
data class User(
    val id: String,
    val nim: String,
    val name: String,
    val role: UserRole,
    val passwordHash: String,
    val department: String,
    val isActive: Boolean,
    val lastLogin: Long
)
```

**AttendanceSession.kt**
```kotlin
data class AttendanceSession(
    val id: String,
    val courseId: String,
    val courseName: String,
    val lecturerId: String,
    val lecturerName: String,
    val qrCode: String,
    val sessionDate: String,
    val createdAt: Long,
    val expiresAt: Long,
    val isActive: Boolean,
    val attendanceCount: Int,
    val lateThreshold: Int  // dalam menit
)
```

**AttendanceRecord.kt**
```kotlin
data class AttendanceRecord(
    val id: String,
    val sessionId: String,
    val courseId: String,
    val courseName: String,
    val studentNIM: String,
    val studentName: String,
    val lecturerId: String,
    val status: AttendanceStatus,
    val timestamp: Long
)
```

#### Repository Functions:

| Function | Description |
|----------|-------------|
| `login(nim, password)` | Login dengan NIM & password |
| `createStudent(nim, name, department, password)` | Dosen membuat akun mahasiswa |
| `createAttendanceSession(session)` | Buat sesi presensi baru |
| `getActiveSessionsByCourse(courseId)` | Ambil sesi aktif per course |
| `recordAttendance(sessionId, studentNIM, studentName)` | Mahasiswa record presensi |
| `getStudentAttendanceHistory(nim)` | Riwayat presensi mahasiswa |

---

### ✅ Phase 2: ViewModel & State (COMPLETED)

| Task | Status | File |
|------|--------|------|
| AuthState | ✅ Done | `ui/state/AuthState.kt` |
| AuthViewModel | ✅ Done | `ui/screens/auth/AuthViewModel.kt` |
| LecturerUiState | ✅ Done | `ui/state/LecturerUiState.kt` |
| LecturerViewModel | ✅ Done | `ui/screens/lecturer/LecturerViewModel.kt` |
| StudentUiState | ✅ Done | `ui/state/StudentUiState.kt` |
| StudentViewModel | ✅ Done | `ui/screens/student/StudentViewModel.kt` |

#### ViewModel Functions:

**AuthViewModel:**
- `login(nim, password)` - Handle login
- `logout()` - Handle logout
- `resetAuthState()` - Reset state

**LecturerViewModel:**
- `createStudent(nim, name, email, password)` - Buat akun mahasiswa
- `createAttendanceSession(courseId, courseName, lecturerId, lecturerName, duration)` - Buat sesi
- `loadActiveSessions(lecturerId)` - Load sesi aktif
- `clearMessages()` - Clear error/success messages

**StudentViewModel:**
- `recordAttendance(sessionId, studentNIM, studentName)` - Record presensi
- `loadAttendanceHistory(studentNIM)` - Load riwayat
- `clearMessages()` - Clear messages

---

### 🔄 Phase 3: UI Screens (IN PROGRESS)

| Task | Status | Description |
|------|--------|-------------|
| LoginScreen | ⏳ Pending | Screen login dengan NIM & password |
| LecturerDashboardScreen | ⏳ Pending | Dashboard untuk dosen |
| CreateStudentScreen | ⏳ Pending | Form tambah mahasiswa |
| QRGeneratorScreen | ⏳ Pending | Generate QR Code untuk sesi |
| StudentDashboardScreen | ⏳ Pending | Dashboard untuk mahasiswa |
| QRScannerScreen | ✅ Done | Scan QR Code (ML Kit) |
| AttendanceHistoryScreen | ⏳ Pending | Riwayat presensi mahasiswa |
| ProfileScreen | ✅ Done | Profile user (UI only) |

---

### ⏳ Phase 4: Navigation Integration (PENDING)

| Task | Status | Description |
|------|--------|-------------|
| Connect LoginScreen | ⏳ Pending | Integrate dengan AuthViewModel |
| Role-based Navigation | ⏳ Pending | Navigate berdasarkan role user |
| Protected Routes | ⏳ Pending | Guard screen yang perlu auth |

---

### ⏳ Phase 5: QR Code Features (PENDING)

| Task | Status | Description |
|------|--------|-------------|
| QR Generator | ⏳ Pending | Generate QR dari session ID (ZXing) |
| QR Scanner Integration | ✅ Done | ML Kit Barcode Scanner |
| QR Validation | ⏳ Pending | Validate QR dan record attendance |

---

### ⏳ Phase 6: Testing & Polish (PENDING)

| Task | Status | Description |
|------|--------|-------------|
| Unit Tests | ⏳ Pending | Test ViewModel logic |
| UI Tests | ⏳ Pending | Test Compose screens |
| Error Handling | ⏳ Pending | Better error messages |
| Loading States | ⏳ Pending | Shimmer/skeleton loading |
| Offline Support | ⏳ Pending | Cache data locally |

---

## 📦 Dependencies

```kotlin
// Firebase
implementation("com.google.firebase:firebase-firestore-ktx")

// Compose
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose")
implementation("androidx.navigation:navigation-compose")

// QR Code
implementation("com.google.mlkit:barcode-scanning:17.2.0")  // Scanner
implementation("com.google.zxing:core:3.5.2")               // Generator

// CameraX
implementation("androidx.camera:camera-camera2")
implementation("androidx.camera:camera-lifecycle")
implementation("androidx.camera:camera-view")

// BCrypt
implementation("org.mindrot:jbcrypt:0.4")
```

---

## 🗄️ Firebase Firestore Schema

### Collections:

**users**
```json
{
  "id": "user_123",
  "nim": "12345678",
  "name": "John Doe",
  "role": "STUDENT",
  "passwordHash": "$2a$10$...",
  "department": "Teknik Informatika",
  "isActive": true,
  "lastLogin": 1702234567890
}
```

**attendance_sessions**
```json
{
  "id": "session_123",
  "courseId": "course_001",
  "courseName": "Pemrograman Mobile",
  "lecturerId": "lecturer_001",
  "lecturerName": "Dr. Smith",
  "qrCode": "unique-qr-string",
  "sessionDate": "2024-12-10 10:00:00",
  "createdAt": 1702234567890,
  "expiresAt": 1702238167890,
  "isActive": true,
  "attendanceCount": 25,
  "lateThreshold": 15
}
```

**attendance_records**
```json
{
  "id": "record_123",
  "sessionId": "session_123",
  "courseId": "course_001",
  "courseName": "Pemrograman Mobile",
  "studentNIM": "12345678",
  "studentName": "John Doe",
  "lecturerId": "lecturer_001",
  "status": "PRESENT",
  "timestamp": 1702234600000
}
```

---

## 🚀 Next Steps

1. **Phase 3** - Buat UI Screens (LoginScreen, Dashboard, dll)
2. **Phase 4** - Integrate Navigation dengan ViewModel
3. **Phase 5** - Implement QR Generator
4. **Phase 6** - Testing & Polish

---

## 📝 Notes

- Password di-hash menggunakan BCrypt sebelum disimpan
- QR Code expire berdasarkan `expiresAt` timestamp
- Late threshold default: 15 menit dari `createdAt`
- Session bisa di-deactivate manual oleh dosen
- Mahasiswa hanya bisa absen 1x per session (duplicate check)

---

## 🐛 Known Issues

- [ ] `loadActiveSessions` di LecturerViewModel belum fully implemented
- [ ] Perlu tambah function `getActiveSessions` by lecturerId di repository

---

## 📅 Last Updated

**10 Desember 2024**

---

## 👨‍💻 Development Notes

### Phase 2 Completion Notes:
- ViewModels sudah dibuat dengan parameter yang sesuai dengan Repository
- State management menggunakan StateFlow
- Error handling dengan Result<T> pattern

### Upcoming Phase 3:
- Prioritas: LoginScreen → LecturerDashboard → StudentDashboard
- UI sudah ada beberapa (ProfileScreen, QRScanner)
- Perlu connect UI dengan ViewModel


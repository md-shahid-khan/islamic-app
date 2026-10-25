package com.codominator.angusmasail.network

data class AuthRequest(
    val phone: String,
    val otp: String = "123456",
    val name: String? = null,
    val locality: String? = null,
    val manhaj: String? = null,
    val role: String? = null,
    val masjidName: String? = null,
    val masjidCity: String? = null,
    val fiqhSchool: String? = null
)

data class ImamRegistrationRequest(
    val imamName: String,
    val phone: String,
    val masjidName: String,
    val masjidAddress: String,
    val masjidCity: String? = null,
    val fiqhSchool: String? = "Hanafi",
    val qualification: String? = null,
    val bio: String? = null,
    val mosqueId: String? = null,
    val otp: String = "123456"
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val phone: String,
    val name: String? = null
)

data class VerifyEmailOtpRequest(
    val email: String,
    val otp: String
)

data class LoginRequest(
    val identifier: String,
    val password: String? = null,
    val otp: String? = null
)

data class RegisterResponse(
    val status: String,
    val message: String,
    val email: String,
    val devOtp: String? = null
)

data class UserDto(
    val id: String,
    val username: String? = null,
    val email: String? = null,
    val emailVerified: Boolean? = false,
    val phone: String,
    val name: String?,
    val locality: String?,
    val city: String? = null,
    val address: String? = null,
    val preferredLanguage: String? = null,
    val bio: String? = null,
    val manhaj: String?,
    val role: String,
    val selectedImamProfileId: String?,
    val selectedMasjidName: String?,
    val fcmToken: String?,
    val isImamPending: Boolean? = false,
    val isImamVerified: Boolean? = false
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserDto
)

data class ImamProfileDto(
    val id: String,
    val userId: String,
    val imamName: String?,
    val phone: String?,
    val masjidName: String,
    val masjidCity: String?,
    val bio: String?,
    val fiqhSchool: String?,
    val hijriOffsetDays: Int?,
    val verified: Boolean
)

data class PrayerTimetableDto(
    val id: String?,
    val imamProfileId: String?,
    val date: String?,
    val fajrStartTime: String? = null,
    val fajrEndTime: String? = null,
    val fajrAzan: String?,
    val fajrJamaat: String?,
    val sunriseStartTime: String? = null,
    val sunriseEndTime: String? = null,
    val sunrise: String?,
    val dhuhrStartTime: String? = null,
    val dhuhrEndTime: String? = null,
    val dhuhrAzan: String?,
    val dhuhrJamaat: String?,
    val asrStartTime: String? = null,
    val asrEndTime: String? = null,
    val asrAzan: String?,
    val asrJamaat: String?,
    val maghribStartTime: String? = null,
    val maghribEndTime: String? = null,
    val maghribAzan: String?,
    val maghribJamaat: String?,
    val ishaStartTime: String? = null,
    val ishaEndTime: String? = null,
    val ishaAzan: String?,
    val ishaJamaat: String?,
    val jumuahStartTime: String? = null,
    val jumuahEndTime: String? = null,
    val jumuahAzan: String?,
    val jumuahJamaat: String?,
    val hijriOffsetDays: Int? = null
)

data class MasailQueryDto(
    val id: String,
    val userId: String,
    val imamProfileId: String,
    val tag: String,
    val userNameSnapshot: String?,
    val userContactSnapshot: String?,
    val localitySnapshot: String?,
    val manhajSnapshot: String?,
    val questionText: String,
    val questionAudioUrl: String?,
    val isUrgent: Boolean,
    val status: String,
    val imamReplyText: String?,
    val imamReplyAudioUrl: String?,
    val isPublicFaq: Boolean,
    val createdAt: String?,
    val resolvedAt: String?
)

data class BannerDto(
    val id: String,
    val imageUrl: String,
    val title: String?,
    val actionUrl: String?,
    val displayOrder: Int?,
    val createdAt: String?
)

data class ImamResourceDto(
    val id: String,
    val imamProfileId: String,
    val type: String,
    val title: String,
    val description: String?,
    val fileUrl: String?,
    val coverImageUrl: String?,
    val createdAt: String?
)

data class ImamStatusDto(
    val id: String,
    val imamProfileId: String,
    val contentText: String?,
    val mediaUrl: String?,
    val backgroundColorHex: String?,
    val createdAt: String?,
    val expiresAt: String?
)

data class ChatMessageDto(
    val id: String? = null,
    val chatRequestId: String? = null,
    val senderId: String? = null,
    val senderName: String? = null,
    val messageText: String,
    val mediaUrl: String? = null,
    val sentAt: String? = null,
    val readAt: String? = null
)

data class ChatRequestDto(
    val id: String,
    val userId: String,
    val userName: String? = null,
    val userLocality: String? = null,
    val userPhone: String? = null,
    val userManhaj: String? = null,
    val imamProfileId: String,
    val imamName: String? = null,
    val relatedQueryId: String? = null,
    val initialReason: String,
    val status: String,
    val createdAt: String? = null,
    val respondedAt: String? = null
)

data class ChatUploadResponse(
    val url: String,
    val fileName: String,
    val fileType: String,
    val fileSize: Long
)

data class SurahHeaderDto(
    val number: Int,
    val name: String,
    val englishName: String,
    val englishNameTranslation: String,
    val numberOfAyahs: Int,
    val revelationType: String
)

data class AyahDto(
    val numberInSurah: Int,
    val globalNumber: Int,
    val arabicText: String,
    val urduTranslation: String, // Kanzul Iman by Ala Hazrat Ahmad Raza Khan Barelvi
    val audioUrl: String? = null,
    val juz: Int? = null,
    val page: Int? = null,
    val surahNumber: Int? = null,
    val surahName: String? = null
)

data class SurahDetailDto(
    val number: Int,
    val name: String,
    val englishName: String,
    val englishNameTranslation: String,
    val numberOfAyahs: Int,
    val revelationType: String,
    val bismillahArabic: String? = null,
    val bismillahTranslation: String? = null,
    val ayahs: List<AyahDto> = emptyList()
)

data class MosqueDto(
    val id: String,
    val name: String,
    val city: String,
    val address: String? = null,
    val country: String? = "India",
    val fiqhSchool: String? = "Hanafi",
    val imageUrl: String? = null,
    val hijriOffsetDays: Int = 0,
    val verified: Boolean = true,
    val hasImam: Boolean = false,
    val currentImamId: String? = null,
    val currentImamName: String? = null,
    val registeredByName: String? = null,
    val createdAt: String? = null
)

data class CreateMosqueRequest(
    val name: String,
    val city: String,
    val address: String? = null,
    val country: String = "India",
    val fiqhSchool: String = "Hanafi",
    val imageUrl: String? = null,
    val hijriOffsetDays: Int = 0
)

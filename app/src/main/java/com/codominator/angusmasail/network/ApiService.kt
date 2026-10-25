package com.codominator.angusmasail.network

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("/api/v1/auth/register")
    suspend fun registerUser(@Body req: RegisterRequest): Response<RegisterResponse>

    @POST("/api/v1/auth/verify-email-otp")
    suspend fun verifyEmailOtp(@Body req: VerifyEmailOtpRequest): Response<AuthResponse>

    @POST("/api/v1/auth/login")
    suspend fun loginWithCredentials(@Body req: LoginRequest): Response<AuthResponse>

    @PUT("/api/v1/users/profile")
    suspend fun updateProfile(@Body req: UserDto): Response<UserDto>

    @POST("/api/v1/auth/verify-otp")
    suspend fun verifyOtp(@Body req: AuthRequest): Response<AuthResponse>

    @POST("/api/v1/auth/register-imam")
    suspend fun registerImam(@Body req: ImamRegistrationRequest): Response<AuthResponse>

    @POST("/api/v1/auth/send-otp")
    suspend fun sendOtp(@Query("phone") phone: String): Response<String>

    @GET("/api/v1/imams")
    suspend fun getImams(): Response<List<ImamProfileDto>>

    @GET("/api/v1/admin/pending-imams")
    suspend fun getPendingImams(): Response<List<ImamProfileDto>>

    @PUT("/api/v1/admin/imams/{imamProfileId}/approve")
    suspend fun approveImam(@Path("imamProfileId") imamProfileId: String): Response<ImamProfileDto>

    @PUT("/api/v1/admin/imams/{imamProfileId}/reject")
    suspend fun rejectImam(@Path("imamProfileId") imamProfileId: String): Response<Unit>

    @DELETE("/api/v1/admin/imams/{imamProfileId}")
    suspend fun deleteImam(@Path("imamProfileId") imamProfileId: String): Response<Unit>

    @DELETE("/api/v1/admin/mosques/{imamProfileId}")
    suspend fun deleteMosque(@Path("imamProfileId") imamProfileId: String): Response<Unit>

    @DELETE("/api/v1/library/books/{bookId}")
    suspend fun deleteBookResource(@Path("bookId") bookId: String): Response<Unit>

    @GET("/api/v1/users/my-imam-status")
    suspend fun getMyImamStatus(): Response<ImamProfileDto>

    @GET("/api/v1/users/me")
    suspend fun getCurrentUser(): Response<UserDto>

    @PUT("/api/v1/users/select-imam/{imamProfileId}")
    suspend fun selectImam(@Path("imamProfileId") imamProfileId: String): Response<UserDto>

    @GET("/api/v1/prayer/daily")
    suspend fun getPrayerTimes(
        @Query("imamId") imamProfileId: String,
        @Query("date") date: String
    ): Response<PrayerTimetableDto>

    @PUT("/api/v1/imam/prayer/update")
    suspend fun updatePrayerTimes(@Body timetable: PrayerTimetableDto): Response<PrayerTimetableDto>

    @FormUrlEncoded
    @POST("/api/v1/queries")
    suspend fun submitQuery(
        @Field("tag") tag: String,
        @Field("questionText") questionText: String,
        @Field("audioUrl") audioUrl: String? = null,
        @Field("imamProfileId") imamProfileId: String? = null
    ): Response<MasailQueryDto>

    @POST("/api/v1/queries/check-duplicate")
    suspend fun checkDuplicates(
        @Query("imamProfileId") imamProfileId: String,
        @Query("questionText") questionText: String
    ): Response<List<MasailQueryDto>>

    @GET("/api/v1/queries/my")
    suspend fun getMyQueries(): Response<List<MasailQueryDto>>

    @GET("/api/v1/imam/queries")
    suspend fun getImamQueries(
        @Query("tag") tag: String? = null,
        @Query("status") status: String? = null
    ): Response<List<MasailQueryDto>>

    @POST("/api/v1/imam/queries/{queryId}/accept")
    suspend fun acceptQuery(@Path("queryId") queryId: String): Response<MasailQueryDto>

    @POST("/api/v1/imam/queries/{queryId}/resolve")
    suspend fun resolveQuery(
        @Path("queryId") queryId: String,
        @Query("replyText") replyText: String,
        @Query("replyAudioUrl") replyAudioUrl: String? = null,
        @Query("isPublicFaq") isPublicFaq: Boolean = false
    ): Response<MasailQueryDto>

    @POST("/api/v1/chat/request")
    suspend fun requestChat(
        @Query("initialReason") initialReason: String,
        @Query("relatedQueryId") relatedQueryId: String? = null,
        @Query("imamProfileId") imamProfileId: String? = null,
        @Query("mediaUrl") mediaUrl: String? = null
    ): Response<ChatRequestDto>

    @GET("/api/v1/chat/my-requests")
    suspend fun getMyChatRequests(): Response<List<ChatRequestDto>>

    @GET("/api/v1/imam/chat/requests")
    suspend fun getImamChatRequests(
        @Query("status") status: String? = null
    ): Response<List<ChatRequestDto>>

    @POST("/api/v1/imam/chat/{requestId}/accept")
    suspend fun acceptChat(@Path("requestId") requestId: String): Response<ChatRequestDto>

    @POST("/api/v1/imam/chat/{requestId}/reject")
    suspend fun rejectChat(@Path("requestId") requestId: String): Response<ChatRequestDto>

    @GET("/api/v1/chat/{requestId}/messages")
    suspend fun getChatMessages(@Path("requestId") requestId: String): Response<List<ChatMessageDto>>

    @FormUrlEncoded
    @POST("/api/v1/chat/{requestId}/messages")
    suspend fun postChatMessage(
        @Path("requestId") requestId: String,
        @Field("messageText") messageText: String,
        @Field("mediaUrl") mediaUrl: String? = null
    ): Response<ChatMessageDto>

    @Multipart
    @POST("/api/v1/chat/upload")
    suspend fun uploadChatFile(
        @Part file: MultipartBody.Part
    ): Response<ChatUploadResponse>

    @GET("/api/v1/resources")
    suspend fun getResources(
        @Query("imamId") imamProfileId: String,
        @Query("type") type: String? = null
    ): Response<List<ImamResourceDto>>

    @GET("/api/v1/library/books")
    suspend fun getLibraryBooks(
        @Query("category") category: String? = null
    ): Response<List<ImamResourceDto>>

    @POST("/api/v1/library/books")
    suspend fun createLibraryBook(
        @Body payload: Map<String, String>
    ): Response<ImamResourceDto>

    @GET("/api/v1/status/active")
    suspend fun getActiveStatuses(
        @Query("imamId") imamProfileId: String
    ): Response<List<ImamStatusDto>>

    @GET("/api/v1/quran/surahs")
    suspend fun getSurahs(): Response<List<SurahHeaderDto>>

    @GET("/api/v1/quran/surah/{surahNumber}")
    suspend fun getSurah(@Path("surahNumber") surahNumber: Int): Response<SurahDetailDto>

    @GET("/api/v1/quran/ayah/{surahNumber}/{ayahNumber}")
    suspend fun getAyah(
        @Path("surahNumber") surahNumber: Int,
        @Path("ayahNumber") ayahNumber: Int
    ): Response<AyahDto>

    @GET("/api/v1/quran/ayah-of-the-day")
    suspend fun getAyahOfTheDay(): Response<AyahDto>

    @GET("/api/v1/banners")
    suspend fun getBanners(): Response<List<BannerDto>>

    @POST("/api/v1/banners")
    suspend fun createBanner(@Body payload: Map<String, String>): Response<BannerDto>

    @DELETE("/api/v1/banners/{id}")
    suspend fun deleteBanner(@Path("id") id: String): Response<Map<String, String>>
}

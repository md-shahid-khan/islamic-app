package com.codominator.angusmasail.viewmodel

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codominator.angusmasail.network.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _imams = MutableStateFlow<List<ImamProfileDto>>(emptyList())
    val imams: StateFlow<List<ImamProfileDto>> = _imams

    private val _pendingImams = MutableStateFlow<List<ImamProfileDto>>(emptyList())
    val pendingImams: StateFlow<List<ImamProfileDto>> = _pendingImams

    private val _pendingMosques = MutableStateFlow<List<MosqueDto>>(emptyList())
    val pendingMosques: StateFlow<List<MosqueDto>> = _pendingMosques

    private val _selectedImam = MutableStateFlow<ImamProfileDto?>(null)
    val selectedImam: StateFlow<ImamProfileDto?> = _selectedImam

    private val _prayerTimes = MutableStateFlow<PrayerTimetableDto?>(null)
    val prayerTimes: StateFlow<PrayerTimetableDto?> = _prayerTimes

    private val _resources = MutableStateFlow<List<ImamResourceDto>>(emptyList())
    val resources: StateFlow<List<ImamResourceDto>> = _resources

    private val _libraryBooks = MutableStateFlow<List<ImamResourceDto>>(emptyList())
    val libraryBooks: StateFlow<List<ImamResourceDto>> = _libraryBooks

    private val _isLoadingLibrary = MutableStateFlow(false)
    val isLoadingLibrary: StateFlow<Boolean> = _isLoadingLibrary

    private val _banners = MutableStateFlow<List<BannerDto>>(emptyList())
    val banners: StateFlow<List<BannerDto>> = _banners

    private val _isLoadingBanners = MutableStateFlow(false)
    val isLoadingBanners: StateFlow<Boolean> = _isLoadingBanners

    private val _statuses = MutableStateFlow<List<ImamStatusDto>>(emptyList())
    val statuses: StateFlow<List<ImamStatusDto>> = _statuses

    private val _myQueries = MutableStateFlow<List<MasailQueryDto>>(emptyList())
    val myQueries: StateFlow<List<MasailQueryDto>> = _myQueries

    private val _imamQueries = MutableStateFlow<List<MasailQueryDto>>(emptyList())
    val imamQueries: StateFlow<List<MasailQueryDto>> = _imamQueries

    private val _imamChatRequests = MutableStateFlow<List<ChatRequestDto>>(emptyList())
    val imamChatRequests: StateFlow<List<ChatRequestDto>> = _imamChatRequests

    private val _userChatRequests = MutableStateFlow<List<ChatRequestDto>>(emptyList())
    val userChatRequests: StateFlow<List<ChatRequestDto>> = _userChatRequests

    private val _activeChatMessages = MutableStateFlow<List<ChatMessageDto>>(emptyList())
    val activeChatMessages: StateFlow<List<ChatMessageDto>> = _activeChatMessages

    private val _selectedChatRequestId = MutableStateFlow<String?>(null)
    val selectedChatRequestId: StateFlow<String?> = _selectedChatRequestId

    private val _duplicateCheckResults = MutableStateFlow<List<MasailQueryDto>>(emptyList())
    val duplicateCheckResults: StateFlow<List<MasailQueryDto>> = _duplicateCheckResults

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _userRole = MutableStateFlow("ROLE_USER")
    val userRole: StateFlow<String> = _userRole

    private val _currentUser = MutableStateFlow<UserDto?>(null)
    val currentUser: StateFlow<UserDto?> = _currentUser

    private val _mosques = MutableStateFlow<List<MosqueDto>>(emptyList())
    val mosques: StateFlow<List<MosqueDto>> = _mosques

    private val _availableMosques = MutableStateFlow<List<MosqueDto>>(emptyList())
    val availableMosques: StateFlow<List<MosqueDto>> = _availableMosques

    // --- Quran Online & Kanzul Iman Translation State ---
    private val _surahs = MutableStateFlow<List<SurahHeaderDto>>(emptyList())
    val surahs: StateFlow<List<SurahHeaderDto>> = _surahs

    private val _currentSurah = MutableStateFlow<SurahDetailDto?>(null)
    val currentSurah: StateFlow<SurahDetailDto?> = _currentSurah

    private val _selectedSurahNumber = MutableStateFlow(1)
    val selectedSurahNumber: StateFlow<Int> = _selectedSurahNumber

    private val _ayahOfTheDay = MutableStateFlow<AyahDto?>(null)
    val ayahOfTheDay: StateFlow<AyahDto?> = _ayahOfTheDay

    private val _bookmarkedAyahs = MutableStateFlow<Set<String>>(emptySet())
    val bookmarkedAyahs: StateFlow<Set<String>> = _bookmarkedAyahs

    private val _currentlyPlayingAyahKey = MutableStateFlow<String?>(null)
    val currentlyPlayingAyahKey: StateFlow<String?> = _currentlyPlayingAyahKey

    private val _isLoadingQuran = MutableStateFlow(false)
    val isLoadingQuran: StateFlow<Boolean> = _isLoadingQuran

    private var quranMediaPlayer: MediaPlayer? = null
    private val quranPrefs = application.getSharedPreferences("angus_quran_prefs", Context.MODE_PRIVATE)

    init {
        TokenManager.init(application)
        loadBookmarks()
        fetchSurahs()
        selectAndFetchSurah(1)
        fetchAyahOfTheDay()
        fetchLibraryBooks()
        fetchBanners()
        fetchMosques()
        fetchAvailableMosques()
        restoreUserSession()
    }

    fun restoreUserSession() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = TokenManager.getToken()
                if (!token.isNullOrBlank()) {
                    val meRes = ApiClient.api.getCurrentUser()
                    if (meRes.isSuccessful && meRes.body() != null) {
                        val user = meRes.body()!!
                        _currentUser.value = user
                        _userRole.value = user.role
                        if (user.role == "ROLE_IMAM") {
                            fetchImamQueries()
                            fetchImamChatRequests()
                        } else {
                            fetchMyQueries()
                            fetchUserChatRequests()
                        }
                    } else {
                        TokenManager.saveToken(getApplication(), "")
                        _currentUser.value = null
                        _userRole.value = "ROLE_GUEST"
                    }
                } else {
                    _currentUser.value = null
                    _userRole.value = "ROLE_GUEST"
                }
                fetchImamsAndSetDefault()
            } catch (e: Exception) {
                e.printStackTrace()
                fetchImamsAndSetDefault()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun registerWithEmailOtp(
        username: String,
        email: String,
        password: String,
        phone: String,
        onResult: (Boolean, String?, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val req = RegisterRequest(
                    username = username.trim(),
                    email = email.trim(),
                    password = password,
                    phone = phone.trim(),
                    name = username.trim()
                )
                val res = ApiClient.api.registerUser(req)
                if (res.isSuccessful && res.body() != null) {
                    val body = res.body()!!
                    onResult(true, null, body.devOtp)
                } else {
                    val err = ApiClient.extractUserFriendlyError(res, "Registration could not be completed")
                    onResult(false, err, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, e.localizedMessage ?: "Registration failed. Check network.", null)
            }
        }
    }

    fun verifyEmailOtp(
        email: String,
        otp: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val req = VerifyEmailOtpRequest(email = email.trim(), otp = otp.trim())
                val res = ApiClient.api.verifyEmailOtp(req)
                if (res.isSuccessful && res.body() != null) {
                    val authBody = res.body()!!
                    TokenManager.saveToken(getApplication(), authBody.accessToken)
                    _currentUser.value = authBody.user
                    _userRole.value = authBody.user.role
                    fetchImamsAndSetDefault()
                    onResult(true, null)
                } else {
                    val err = ApiClient.extractUserFriendlyError(res, "Invalid or expired OTP code")
                    onResult(false, err)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, e.localizedMessage ?: "Verification failed. Check network.")
            }
        }
    }

    fun loginWithCredentials(
        identifier: String,
        password: String? = null,
        otp: String? = null,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val req = LoginRequest(
                    identifier = identifier.trim(),
                    password = password?.takeIf { it.isNotBlank() },
                    otp = otp?.takeIf { it.isNotBlank() }
                )
                val res = ApiClient.api.loginWithCredentials(req)
                if (res.isSuccessful && res.body() != null) {
                    val authBody = res.body()!!
                    TokenManager.saveToken(getApplication(), authBody.accessToken)
                    _currentUser.value = authBody.user
                    _userRole.value = authBody.user.role
                    fetchImamsAndSetDefault()
                    onResult(true, null)
                } else {
                    val err = ApiClient.extractUserFriendlyError(res, "Invalid username, email, phone or password")
                    onResult(false, err)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, e.localizedMessage ?: "Login failed. Check server connection.")
            }
        }
    }

    fun updateUserProfile(
        updatedUser: UserDto,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.updateProfile(updatedUser)
                if (res.isSuccessful && res.body() != null) {
                    _currentUser.value = res.body()
                    onResult(true, null)
                } else {
                    val err = ApiClient.extractUserFriendlyError(res, "Failed to update profile")
                    onResult(false, err)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, e.localizedMessage ?: "Failed to update profile")
            }
        }
    }

    fun login(phone: String, otp: String = "123456", onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.verifyOtp(AuthRequest(phone = phone, otp = otp))
                if (res.isSuccessful && res.body() != null) {
                    val authBody = res.body()!!
                    TokenManager.saveToken(getApplication(), authBody.accessToken)
                    _currentUser.value = authBody.user
                    _userRole.value = authBody.user.role
                    fetchImamsAndSetDefault()
                    onResult(true, null)
                } else {
                    val err = ApiClient.extractUserFriendlyError(res, "Invalid login credentials")
                    onResult(false, err)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, e.localizedMessage ?: "Login failed. Check server connection.")
            }
        }
    }

    fun register(
        name: String,
        phone: String,
        locality: String,
        manhaj: String,
        role: String,
        masjidName: String? = null,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val req = AuthRequest(
                    phone = phone,
                    otp = "123456",
                    name = name,
                    locality = locality,
                    manhaj = manhaj,
                    role = role,
                    masjidName = masjidName,
                    fiqhSchool = manhaj
                )
                val res = ApiClient.api.verifyOtp(req)
                if (res.isSuccessful && res.body() != null) {
                    val authBody = res.body()!!
                    TokenManager.saveToken(getApplication(), authBody.accessToken)
                    _currentUser.value = authBody.user
                    _userRole.value = authBody.user.role
                    fetchImamsAndSetDefault()
                    onResult(true, null)
                } else {
                    val err = ApiClient.extractUserFriendlyError(res, "Registration failed")
                    onResult(false, err)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, e.localizedMessage ?: "Registration failed")
            }
        }
    }

    fun registerImam(
        imamName: String,
        phone: String,
        masjidName: String,
        masjidAddress: String,
        masjidCity: String,
        fiqhSchool: String,
        qualification: String,
        bio: String,
        mosqueId: String? = null,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val req = ImamRegistrationRequest(
                    imamName = imamName,
                    phone = phone,
                    masjidName = masjidName,
                    masjidAddress = masjidAddress,
                    masjidCity = masjidCity,
                    fiqhSchool = fiqhSchool,
                    qualification = qualification,
                    bio = bio,
                    mosqueId = mosqueId,
                    otp = "123456"
                )
                val res = ApiClient.api.registerImam(req)
                if (res.isSuccessful && res.body() != null) {
                    val authBody = res.body()!!
                    TokenManager.saveToken(getApplication(), authBody.accessToken)
                    _currentUser.value = authBody.user
                    _userRole.value = authBody.user.role
                    fetchImamsAndSetDefault()
                    fetchImamQueries()
                    fetchImamChatRequests()
                    onResult(true, null)
                } else {
                    val err = ApiClient.extractUserFriendlyError(res, "Imam registration failed")
                    onResult(false, err)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, e.localizedMessage ?: "Imam registration failed")
            }
        }
    }

    fun switchRole(newRole: String) {
        _userRole.value = newRole
        _currentUser.value = _currentUser.value?.copy(role = newRole)
        when (newRole) {
            "ROLE_IMAM" -> {
                fetchImamQueries()
                fetchImamChatRequests()
            }
            "ROLE_ADMIN" -> {
                loadPendingImams()
                viewModelScope.launch {
                    fetchImamsAndSetDefault()
                }
            }
            else -> {
                fetchMyQueries()
                fetchUserChatRequests()
            }
        }
    }

    fun loadPendingImams() {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.getPendingImams()
                if (res.isSuccessful && res.body() != null) {
                    _pendingImams.value = res.body()!!
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        loadPendingMosques()
    }

    fun loadPendingMosques() {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.getPendingMosques()
                if (res.isSuccessful && res.body() != null) {
                    _pendingMosques.value = res.body()!!
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun approveMosque(mosqueId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.approveMosque(mosqueId)
                if (res.isSuccessful) {
                    loadPendingMosques()
                    fetchMosques()
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun rejectMosque(mosqueId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.rejectMosque(mosqueId)
                if (res.isSuccessful) {
                    loadPendingMosques()
                    fetchMosques()
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun approveImam(imamProfileId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.approveImam(imamProfileId)
                if (res.isSuccessful) {
                    loadPendingImams()
                    fetchImamsAndSetDefault()
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }


    fun deleteImamOrMosque(imamProfileId: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.deleteImam(imamProfileId)
                if (res.isSuccessful) {
                    fetchImamsAndSetDefault()
                    loadPendingImams()
                    onComplete(true, null)
                } else {
                    onComplete(false, "Failed with status code ${res.code()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false, e.localizedMessage ?: "Delete failed")
            }
        }
    }

    fun fetchLibraryBooks(category: String? = null) {
        _isLoadingLibrary.value = true
        viewModelScope.launch {
            try {
                val res = ApiClient.api.getLibraryBooks(category)
                if (res.isSuccessful && res.body() != null) {
                    _libraryBooks.value = res.body()!!
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingLibrary.value = false
            }
        }
    }

    fun uploadLibraryBook(
        title: String,
        author: String,
        category: String,
        description: String,
        fileUrl: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val payload = mapOf(
                    "title" to title,
                    "author" to author,
                    "category" to category,
                    "description" to description,
                    "fileUrl" to fileUrl
                )
                val res = ApiClient.api.createLibraryBook(payload)
                if (res.isSuccessful && res.body() != null) {
                    fetchLibraryBooks()
                    onResult(true, null)
                } else {
                    onResult(false, "Upload error ${res.code()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, e.localizedMessage ?: "Upload failed")
            }
        }
    }

    fun deleteBookResource(bookId: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.deleteBookResource(bookId)
                if (res.isSuccessful) {
                    fetchLibraryBooks()
                    onComplete(true, null)
                } else {
                    val rawErr = res.errorBody()?.string()
                    val parsedMsg = try {
                        if (!rawErr.isNullOrBlank() && rawErr.trim().startsWith("{")) {
                            org.json.JSONObject(rawErr).optString("message", rawErr)
                        } else rawErr ?: "Failed to delete book: code ${res.code()}"
                    } catch (e: Exception) {
                        rawErr ?: "Failed to delete book: code ${res.code()}"
                    }
                    onComplete(false, parsedMsg)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false, e.localizedMessage ?: "Delete book failed")
            }
        }
    }

    fun rejectImam(imamProfileId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.rejectImam(imamProfileId)
                if (res.isSuccessful) {
                    loadPendingImams()
                    fetchImamsAndSetDefault()
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }


    fun fetchBanners() {
        _isLoadingBanners.value = true
        viewModelScope.launch {
            try {
                val res = ApiClient.api.getBanners()
                if (res.isSuccessful && res.body() != null) {
                    _banners.value = res.body()!!
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingBanners.value = false
            }
        }
    }

    fun uploadBanner(
        imageUrl: String,
        title: String? = null,
        actionUrl: String? = null,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val payload = mutableMapOf<String, String>("imageUrl" to imageUrl)
                if (!title.isNullOrBlank()) payload["title"] = title
                if (!actionUrl.isNullOrBlank()) payload["actionUrl"] = actionUrl

                val res = ApiClient.api.createBanner(payload)
                if (res.isSuccessful && res.body() != null) {
                    fetchBanners()
                    onResult(true, null)
                } else {
                    val err = ApiClient.extractUserFriendlyError(res, "Failed to upload banner")
                    onResult(false, err)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, e.localizedMessage ?: "Upload failed")
            }
        }
    }

    fun deleteBanner(bannerId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.deleteBanner(bannerId)
                if (res.isSuccessful) {
                    fetchBanners()
                    onResult(true, null)
                } else {
                    onResult(false, "Failed to delete banner (${res.code()})")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, e.localizedMessage ?: "Delete failed")
            }
        }
    }

    fun checkMyImamStatus(onResult: (hasApplication: Boolean, isApproved: Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.getMyImamStatus()
                if (res.isSuccessful && res.body() != null) {
                    val profile = res.body()!!
                    if (profile.verified) {
                        _userRole.value = "ROLE_IMAM"
                        _currentUser.value = _currentUser.value?.copy(
                            role = "ROLE_IMAM",
                            isImamVerified = true,
                            isImamPending = false,
                            selectedImamProfileId = profile.id,
                            selectedMasjidName = profile.masjidName
                        )
                        _selectedImam.value = profile
                        fetchImamQueries()
                        fetchImamChatRequests()
                        fetchAllDataForImam(profile.id)
                        onResult(true, true)
                    } else {
                        onResult(true, false)
                    }
                } else {
                    onResult(false, false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, false)
            }
        }
    }

    private suspend fun fetchImamsAndSetDefault() {
        try {
            val imamsRes = ApiClient.api.getImams()
            if (imamsRes.isSuccessful && imamsRes.body() != null) {
                val list = imamsRes.body()!!
                _imams.value = list

                val storedId = TokenManager.getImamProfileId()
                if (storedId != null) {
                    val found = list.find { it.id == storedId }
                    if (found != null) {
                        _selectedImam.value = found
                        fetchAllDataForImam(found.id)
                    } else if (list.isNotEmpty()) {
                        selectImam(list.first())
                    }
                } else if (list.isNotEmpty()) {
                    selectImam(list.first())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun selectImam(imam: ImamProfileDto) {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.selectImam(imam.id)
                if (res.isSuccessful) {
                    TokenManager.saveImamProfileId(getApplication(), imam.id)
                    _selectedImam.value = imam
                    fetchAllDataForImam(imam.id)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun fetchAllDataForImam(imamProfileId: String) {
        // Fetch Prayer Times
        try {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
            val pRes = ApiClient.api.getPrayerTimes(imamProfileId, today)
            if (pRes.isSuccessful && pRes.body() != null) {
                _prayerTimes.value = pRes.body()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fetch Resources (Books, News, etc.)
        try {
            val rRes = ApiClient.api.getResources(imamProfileId)
            if (rRes.isSuccessful && rRes.body() != null) {
                _resources.value = rRes.body()!!
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fetch Active Statuses
        try {
            val sRes = ApiClient.api.getActiveStatuses(imamProfileId)
            if (sRes.isSuccessful && sRes.body() != null) {
                _statuses.value = sRes.body()!!
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fetch Imam Dashboard data
        fetchImamQueries()
        fetchImamChatRequests()
    }

    fun refreshData() {
        viewModelScope.launch {
            _selectedImam.value?.id?.let {
                fetchAllDataForImam(it)
            }
        }
    }

    fun submitMasail(
        tag: String,
        questionText: String,
        audioUrl: String? = null,
        targetImamProfileId: String? = null,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (TokenManager.getToken().isNullOrBlank()) {
                    onResult(false, "Please sign in or register to submit a Masail inquiry.")
                    return@launch
                }

                // Ensure an Imam is selected for this user
                var effectiveImamId = targetImamProfileId ?: _selectedImam.value?.id
                if (effectiveImamId == null) {
                    val imamsRes = ApiClient.api.getImams()
                    if (imamsRes.isSuccessful && !imamsRes.body().isNullOrEmpty()) {
                        val firstImam = imamsRes.body()!!.first()
                        _selectedImam.value = firstImam
                        effectiveImamId = firstImam.id
                        ApiClient.api.selectImam(firstImam.id)
                    }
                }

                val res = ApiClient.api.submitQuery(tag, questionText, audioUrl, effectiveImamId)
                if (res.isSuccessful) {
                    fetchMyQueries()
                    fetchImamQueries()
                    onResult(true, null)
                } else {
                    val errorMsg = ApiClient.extractUserFriendlyError(res, "Server returned error ${res.code()}")
                    onResult(false, errorMsg)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, e.localizedMessage ?: "Network connection failed")
            }
        }
    }

    fun checkDuplicates(questionText: String) {
        val imamId = _selectedImam.value?.id ?: return
        if (questionText.isBlank() || questionText.length < 4) {
            _duplicateCheckResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                val res = ApiClient.api.checkDuplicates(imamId, questionText)
                if (res.isSuccessful && res.body() != null) {
                    _duplicateCheckResults.value = res.body()!!
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchMyQueries() {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.getMyQueries()
                if (res.isSuccessful && res.body() != null) {
                    _myQueries.value = res.body()!!
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchImamQueries() {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.getImamQueries()
                if (res.isSuccessful && res.body() != null) {
                    _imamQueries.value = res.body()!!
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun acceptMasailQuery(queryId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.acceptQuery(queryId)
                if (res.isSuccessful) {
                    fetchImamQueries()
                    fetchMyQueries()
                    onResult(true, null)
                } else {
                    val err = ApiClient.extractUserFriendlyError(res, "Failed to accept query")
                    onResult(false, err)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, e.localizedMessage ?: "Network error")
            }
        }
    }

    fun resolveImamQuery(
        queryId: String,
        replyText: String,
        replyAudioUrl: String? = null,
        isPublicFaq: Boolean = false,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.resolveQuery(queryId, replyText, replyAudioUrl, isPublicFaq)
                if (res.isSuccessful) {
                    fetchImamQueries()
                    fetchMyQueries()
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    fun logout() {
        TokenManager.saveToken(getApplication(), "")
        _currentUser.value = null
        _userRole.value = "ROLE_GUEST"
        _myQueries.value = emptyList()
        _imamQueries.value = emptyList()
        _userChatRequests.value = emptyList()
        _imamChatRequests.value = emptyList()
        _activeChatMessages.value = emptyList()
        _selectedChatRequestId.value = null
    }

    fun selectChatRequest(requestId: String?) {
        _selectedChatRequestId.value = requestId
        if (requestId != null) {
            fetchChatMessages(requestId)
        }
    }

    fun fetchUserChatRequests() {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.getMyChatRequests()
                if (res.isSuccessful && res.body() != null) {
                    _userChatRequests.value = res.body()!!
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchChatMessages(requestId: String) {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.getChatMessages(requestId)
                if (res.isSuccessful && res.body() != null) {
                    _activeChatMessages.value = res.body()!!
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendChatMessage(
        requestId: String,
        messageText: String,
        mediaUrl: String? = null,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.postChatMessage(requestId, messageText, mediaUrl)
                if (res.isSuccessful && res.body() != null) {
                    fetchChatMessages(requestId)
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    fun uploadChatAttachment(
        uri: android.net.Uri,
        onResult: (success: Boolean, response: ChatUploadResponse?, error: String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val contentResolver = context.contentResolver

                // Extract filename and size
                var fileName = "attachment_${System.currentTimeMillis()}"
                var fileSize: Long = -1

                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx != -1) fileName = cursor.getString(nameIdx) ?: fileName
                        if (sizeIdx != -1) fileSize = cursor.getLong(sizeIdx)
                    }
                }

                val maxSizeBytes = 15 * 1024 * 1024L // 15 MB limit
                if (fileSize > maxSizeBytes) {
                    onResult(false, null, "File exceeds 15 MB limit. Please select a smaller file.")
                    return@launch
                }
                if (fileSize == 0L) {
                    onResult(false, null, "File is empty.")
                    return@launch
                }

                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    onResult(false, null, "Unable to read selected file.")
                    return@launch
                }

                val bytes = inputStream.use { it.readBytes() }
                if (bytes.size > maxSizeBytes) {
                    onResult(false, null, "File exceeds 15 MB limit.")
                    return@launch
                }
                if (bytes.isEmpty()) {
                    onResult(false, null, "File is empty.")
                    return@launch
                }

                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", fileName, requestBody)

                val res = ApiClient.api.uploadChatFile(part)
                if (res.isSuccessful && res.body() != null) {
                    onResult(true, res.body(), null)
                } else {
                    onResult(false, null, "Upload failed with status code ${res.code()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, null, e.localizedMessage ?: "Failed to upload attachment")
            }
        }
    }

    fun fetchImamChatRequests() {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.getImamChatRequests()
                if (res.isSuccessful && res.body() != null) {
                    _imamChatRequests.value = res.body()!!
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun acceptChatRequest(requestId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.acceptChat(requestId)
                if (res.isSuccessful) {
                    fetchImamChatRequests()
                    fetchUserChatRequests()
                    // Select this accepted chat so it opens in the chat section!
                    _selectedChatRequestId.value = requestId
                    fetchChatMessages(requestId)
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    fun rejectChatRequest(requestId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.rejectChat(requestId)
                if (res.isSuccessful) {
                    fetchImamChatRequests()
                    fetchUserChatRequests()
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    fun openDirectChatWithImam(imam: ImamProfileDto) {
        _selectedImam.value = imam
        val existing = _userChatRequests.value.find { it.imamProfileId == imam.id }
        _selectedChatRequestId.value = existing?.id
        if (existing != null) {
            fetchChatMessages(existing.id)
        } else {
            _activeChatMessages.value = emptyList()
        }
    }

    fun requestChatSession(
        initialReason: String,
        targetImamProfileId: String? = null,
        mediaUrl: String? = null,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val effectiveImamId = targetImamProfileId ?: _selectedImam.value?.id
                val res = ApiClient.api.requestChat(initialReason, imamProfileId = effectiveImamId, mediaUrl = mediaUrl)
                if (res.isSuccessful && res.body() != null) {
                    val newReq = res.body()!!
                    fetchUserChatRequests()
                    _selectedChatRequestId.value = newReq.id
                    fetchChatMessages(newReq.id)
                    onResult(true, newReq.id)
                } else {
                    onResult(false, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, null)
            }
        }
    }

    fun updatePrayerTimes(timetable: PrayerTimetableDto, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                if (TokenManager.getToken().isNullOrBlank()) {
                    onResult(false, "Authentication required. Please sign in as an Imam.")
                    return@launch
                }

                // If timetable has no imamProfileId, fill it from the currently selected imam
                val target = if (timetable.imamProfileId == null) {
                    timetable.copy(imamProfileId = _selectedImam.value?.id)
                } else {
                    timetable
                }

                val res = ApiClient.api.updatePrayerTimes(target)
                if (res.isSuccessful && res.body() != null) {
                    _prayerTimes.value = res.body()
                    _selectedImam.value?.id?.let { fetchAllDataForImam(it) }
                    onResult(true, null)
                } else {
                    val rawErr = res.errorBody()?.string()
                    val parsedMsg = try {
                        if (!rawErr.isNullOrBlank() && rawErr.trim().startsWith("{")) {
                            org.json.JSONObject(rawErr).optString("message", rawErr)
                        } else rawErr ?: "Server error ${res.code()}"
                    } catch (e: Exception) {
                        rawErr ?: "Server error ${res.code()}"
                    }
                    onResult(false, parsedMsg)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, e.localizedMessage ?: "Network error while updating prayer times")
            }
        }
    }

    // --- Quran Methods ---
    fun loadBookmarks() {
        val saved = quranPrefs.getStringSet("bookmarked_ayahs", emptySet()) ?: emptySet()
        _bookmarkedAyahs.value = saved
    }

    fun toggleBookmark(surahNumber: Int, ayahNumber: Int) {
        val key = "$surahNumber:$ayahNumber"
        val current = _bookmarkedAyahs.value.toMutableSet()
        if (current.contains(key)) {
            current.remove(key)
        } else {
            current.add(key)
        }
        _bookmarkedAyahs.value = current
        quranPrefs.edit().putStringSet("bookmarked_ayahs", current).apply()
    }

    fun isAyahBookmarked(surahNumber: Int, ayahNumber: Int): Boolean {
        return _bookmarkedAyahs.value.contains("$surahNumber:$ayahNumber")
    }

    fun fetchSurahs() {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.getSurahs()
                if (res.isSuccessful && res.body() != null) {
                    _surahs.value = res.body()!!
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectAndFetchSurah(surahNumber: Int) {
        _selectedSurahNumber.value = surahNumber
        _isLoadingQuran.value = true
        viewModelScope.launch {
            try {
                val res = ApiClient.api.getSurah(surahNumber)
                if (res.isSuccessful && res.body() != null) {
                    _currentSurah.value = res.body()!!
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingQuran.value = false
            }
        }
    }

    fun fetchAyahOfTheDay() {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.getAyahOfTheDay()
                if (res.isSuccessful && res.body() != null) {
                    _ayahOfTheDay.value = res.body()!!
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playAyahAudio(surahNumber: Int, ayahNumber: Int, audioUrl: String?) {
        val key = "$surahNumber:$ayahNumber"
        if (_currentlyPlayingAyahKey.value == key) {
            stopAyahAudio()
            return
        }
        stopAyahAudio()
        if (audioUrl.isNullOrBlank()) return

        try {
            quranMediaPlayer = MediaPlayer().apply {
                setDataSource(audioUrl)
                setOnPreparedListener {
                    start()
                    _currentlyPlayingAyahKey.value = key
                }
                setOnCompletionListener {
                    stopAyahAudio()
                }
                setOnErrorListener { _, _, _ ->
                    stopAyahAudio()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopAyahAudio()
        }
    }

    fun stopAyahAudio() {
        try {
            quranMediaPlayer?.stop()
            quranMediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        quranMediaPlayer = null
        _currentlyPlayingAyahKey.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAyahAudio()
    }

    fun fetchMosques() {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.getMosques()
                if (res.isSuccessful && res.body() != null) {
                    _mosques.value = res.body()!!
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchAvailableMosques() {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.getAvailableMosques()
                if (res.isSuccessful && res.body() != null) {
                    _availableMosques.value = res.body()!!
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createMosque(req: CreateMosqueRequest, onSuccess: (MosqueDto) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.createMosque(req)
                if (res.isSuccessful && res.body() != null) {
                    val created = res.body()!!
                    fetchMosques()
                    fetchAvailableMosques()
                    onSuccess(created)
                } else {
                    onError("Failed with status ${res.code()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.localizedMessage ?: "Network error")
            }
        }
    }

    fun selectMosque(mosque: MosqueDto) {
        viewModelScope.launch {
            try {
                if (_currentUser.value != null) {
                    try {
                        ApiClient.api.selectMosque(mosque.id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                val matchingImam = _imams.value.find { it.masjidName.equals(mosque.name, ignoreCase = true) }
                if (matchingImam != null) {
                    selectImam(matchingImam)
                } else {
                    val mosqueAsImam = ImamProfileDto(
                        id = mosque.id,
                        userId = "",
                        imamName = mosque.currentImamName ?: "Congregation Mosque",
                        phone = null,
                        masjidName = mosque.name,
                        masjidCity = mosque.city,
                        bio = mosque.address ?: "Local Mosque",
                        fiqhSchool = mosque.fiqhSchool ?: "Hanafi",
                        hijriOffsetDays = mosque.hijriOffsetDays,
                        verified = mosque.verified
                    )
                    TokenManager.saveImamProfileId(getApplication(), mosque.id)
                    _selectedImam.value = mosqueAsImam
                    fetchAllDataForImam(mosque.id)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun disassociateImam(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = ApiClient.api.disassociateImam()
                if (res.isSuccessful && res.body() != null) {
                    val auth = res.body()!!
                    TokenManager.saveToken(getApplication(), auth.accessToken)
                                        _userRole.value = "ROLE_USER"
                    _currentUser.value = auth.user
                    fetchImamsAndSetDefault()
                    fetchMosques()
                    fetchAvailableMosques()
                    onSuccess()
                } else {
                    onError("Failed with code ${res.code()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.localizedMessage ?: "Error during disassociation")
            }
        }
    }
}
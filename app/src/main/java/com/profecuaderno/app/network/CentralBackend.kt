package com.profecuaderno.app.network

import android.content.Context
import com.google.gson.annotations.SerializedName
import com.profecuaderno.app.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

class AuthTokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("central_auth", Context.MODE_PRIVATE)

    var accessToken: String?
        get() = prefs.getString("access_token", null)
        set(value) {
            prefs.edit().apply {
                if (value.isNullOrBlank()) remove("access_token") else putString("access_token", value)
            }.apply()
        }

    fun clear() {
        prefs.edit().clear().apply()
    }
}

data class LoginRequest(val email: String, val password: String)

data class RegisterRequest(
    val email: String,
    val password: String,
    @SerializedName("full_name") val fullName: String,
    val role: String = "teacher"
)

data class UserDto(
    val id: Int,
    val email: String,
    @SerializedName("full_name") val fullName: String,
    val role: String,
    @SerializedName("institution_id") val institutionId: Int?
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String = "bearer",
    val user: UserDto
)

data class ClassDto(
    val id: Int,
    val name: String,
    val subject: String,
    @SerializedName("period_name") val periodName: String,
    @SerializedName("class_code") val classCode: String,
    @SerializedName("teacher_id") val teacherId: Int,
    @SerializedName("institution_id") val institutionId: Int?,
    val active: Boolean
)

data class CreateClassRequest(
    val name: String,
    val subject: String,
    @SerializedName("period_name") val periodName: String
)

data class NoticeRequest(val title: String, val body: String)

data class DirectorNoticeDto(
    val id: Int,
    val title: String,
    val body: String,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
)

data class AttendanceRequest(
    @SerializedName("student_id") val studentId: Int,
    val date: String,
    val status: String,
    val note: String? = null
)

data class GradeRequest(
    @SerializedName("student_id") val studentId: Int,
    val category: String,
    @SerializedName("activity_key") val activityKey: String,
    @SerializedName("activity_name") val activityName: String,
    val score: Double,
    @SerializedName("max_score") val maxScore: Double = 100.0,
    val source: String = "teacher_app"
)

interface TeacherCentralApi {
    @GET("health")
    suspend fun health(): Map<String, String>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): TokenResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): TokenResponse

    @GET("me")
    suspend fun me(): UserDto

    @GET("teacher-notices")
    suspend fun teacherNotices(): List<DirectorNoticeDto>

    @GET("classes")
    suspend fun classes(): List<ClassDto>

    @POST("classes")
    suspend fun createClass(@Body request: CreateClassRequest): ClassDto

    @GET("classes/{classId}/students")
    suspend fun students(@Path("classId") classId: Int): List<UserDto>

    @POST("classes/{classId}/notices")
    suspend fun createNotice(@Path("classId") classId: Int, @Body request: NoticeRequest): Map<String, Any?>

    @PUT("classes/{classId}/attendance")
    suspend fun setAttendance(@Path("classId") classId: Int, @Body request: AttendanceRequest): Map<String, Any?>

    @PUT("classes/{classId}/grades")
    suspend fun setGrade(@Path("classId") classId: Int, @Body request: GradeRequest): Map<String, Any?>
}

class CentralBackend(context: Context) {
    val tokenStore = AuthTokenStore(context.applicationContext)

    private val authInterceptor = Interceptor { chain ->
        val token = tokenStore.accessToken
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        chain.proceed(request)
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()

    val api: TeacherCentralApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TeacherCentralApi::class.java)

    val isConfigured: Boolean
        get() = !BuildConfig.API_BASE_URL.contains("example.invalid")

    fun saveSession(response: TokenResponse) {
        tokenStore.accessToken = response.accessToken
    }

    fun signOut() = tokenStore.clear()
}

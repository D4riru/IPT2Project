package com.example.myapplication.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

// Models matching our REST API and app expectations
data class User(
    val id: Int,
    @SerializedName("full_name") val fullName: String,
    val email: String
)

data class AuthResponse(val status: String, val message: String, val user: User?)
data class ModuleData(val id: String, val title: String, val status: String, val flashcardCount: Int)
data class Flashcard(val category: String, val question: String, val options: List<String>, val correctIndex: Int)
data class StatsResponse(val masteredPercentage: String, val needsReviewCount: String, val needsReviewQuestion: String, val needsReviewHint: String)

object ApiClient {
    private const val TAG = "ApiClient"
    
    // IMPORTANT: 
    // - Use "http://10.0.2.2:5000" if running in the Android Emulator.
    var baseUrl = "http://10.0.2.2:5000"

    // Keep track of logged-in user
    var currentUser: User? = null
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
        
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // 1. Register User
    fun register(fullName: String, email: String, password: String, onResult: (AuthResponse) -> Unit) {
        val payload = mapOf("full_name" to fullName, "email" to email, "password" to password)
        val body = gson.toJson(payload).toRequestBody(jsonMediaType)
        val request = Request.Builder().url("$baseUrl/api/register").post(body).build()
        
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e(TAG, "Registration failed: ${e.message}")
                onResult(AuthResponse("error", "Cannot connect to server. Ensure Flask is running.", null))
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val bodyStr = response.body?.string() ?: ""
                    try {
                        val res = gson.fromJson(bodyStr, AuthResponse::class.java)
                        if (res.status == "success" && res.user != null) {
                            currentUser = res.user
                        }
                        onResult(res)
                    } catch (e: Exception) {
                        onResult(AuthResponse("error", "Server error: ${response.code}", null))
                    }
                }
            }
        })
    }

    // 2. Login User
    fun login(email: String, password: String, onResult: (AuthResponse) -> Unit) {
        val payload = mapOf("email" to email, "password" to password)
        val body = gson.toJson(payload).toRequestBody(jsonMediaType)
        val request = Request.Builder().url("$baseUrl/api/login").post(body).build()
        
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e(TAG, "Login failed: ${e.message}")
                onResult(AuthResponse("error", "Cannot connect to server.", null))
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val bodyStr = response.body?.string() ?: ""
                    try {
                        val res = gson.fromJson(bodyStr, AuthResponse::class.java)
                        if (res.status == "success" && res.user != null) {
                            currentUser = res.user
                        }
                        onResult(res)
                    } catch (e: Exception) {
                        onResult(AuthResponse("error", "Invalid response from server", null))
                    }
                }
            }
        })
    }

    // 3. Get Modules List
    fun getModules(onResult: (List<ModuleData>) -> Unit) {
        val request = Request.Builder().url("$baseUrl/api/modules").get().build()
        
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e(TAG, "Get modules failed: ${e.message}")
                onResult(emptyList())
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        val type = object : TypeToken<List<ModuleData>>() {}.type
                        val list: List<ModuleData> = gson.fromJson(bodyStr, type)
                        onResult(list)
                    } else {
                        onResult(emptyList())
                    }
                }
            }
        })
    }

    // 3a. Create Module
    fun createModule(title: String, cards: List<Map<String, String>> = emptyList(), onResult: (Boolean) -> Unit) {
        val payload = mapOf("title" to title, "cards" to cards)
        val body = gson.toJson(payload).toRequestBody(jsonMediaType)
        val request = Request.Builder().url("$baseUrl/api/modules").post(body).build()
        
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e(TAG, "Create module failed: ${e.message}")
                onResult(false)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                onResult(response.isSuccessful)
            }
        })
    }

    // 3b. Update Module
    fun updateModule(moduleId: String, title: String, onResult: (Boolean) -> Unit) {
        val payload = mapOf("title" to title)
        val body = gson.toJson(payload).toRequestBody(jsonMediaType)
        val request = Request.Builder().url("$baseUrl/api/modules/$moduleId").put(body).build()
        
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e(TAG, "Update module failed: ${e.message}")
                onResult(false)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                onResult(response.isSuccessful)
            }
        })
    }

    // 3c. Delete Module
    fun deleteModule(moduleId: String, onResult: (Boolean) -> Unit) {
        val request = Request.Builder().url("$baseUrl/api/modules/$moduleId").delete().build()
        
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e(TAG, "Delete module failed: ${e.message}")
                onResult(false)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                onResult(response.isSuccessful)
            }
        })
    }

    // 4. Get Quiz Flashcards for Module
    fun getQuiz(moduleId: String, onResult: (List<Flashcard>) -> Unit) {
        val request = Request.Builder().url("$baseUrl/api/modules/$moduleId/quiz").get().build()
        
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e(TAG, "Get quiz failed: ${e.message}")
                onResult(emptyList())
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        val type = object : TypeToken<List<Flashcard>>() {}.type
                        val list: List<Flashcard> = gson.fromJson(bodyStr, type)
                        onResult(list)
                    } else {
                        onResult(emptyList())
                    }
                }
            }
        })
    }

    // 5. Get User Stats
    fun getStats(onResult: (StatsResponse) -> Unit) {
        val userId = currentUser?.id ?: return
        val request = Request.Builder().url("$baseUrl/api/users/$userId/stats").get().build()
        
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e(TAG, "Get stats failed: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        val res = gson.fromJson(bodyStr, StatsResponse::class.java)
                        onResult(res)
                    }
                }
            }
        })
    }

    // 6. Update User Stats
    fun updateStats(score: Int, total: Int, onComplete: (Boolean) -> Unit = {}) {
        val userId = currentUser?.id ?: return 
        val payload = mapOf("score" to score, "total" to total)
        val body = gson.toJson(payload).toRequestBody(jsonMediaType)
        val request = Request.Builder().url("$baseUrl/api/users/$userId/stats").post(body).build()
        
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e(TAG, "Update stats failed: ${e.message}")
                onComplete(false)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    Log.i(TAG, "Stats updated. Server returned code: ${response.code}")
                    onComplete(response.isSuccessful)
                }
            }
        })
    }
}

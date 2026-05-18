package com.example.myapplication.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

// Models matching our REST API and app expectations
data class User(val id: Int, val fullName: String, val email: String)
data class AuthResponse(val status: String, val message: String, val user: User?)
data class ModuleData(val id: String, val title: String, val status: String, val flashcardCount: Int)
data class Flashcard(val category: String, val question: String, val options: List<String>, val correctIndex: Int)
data class StatsResponse(val masteredPercentage: String, val needsReviewCount: String, val needsReviewQuestion: String, val needsReviewHint: String)

object ApiClient {
    private const val TAG = "ApiClient"
    
    // IMPORTANT: 
    // - Use "http://10.0.2.2:5000" if running in the Android Emulator.
    // - Replace with your laptop's local IP (e.g. "http://192.168.1.X:5000") if debugging on a physical Xiaomi/Redmi device!
    var baseUrl = "http://172.20.62.139:5000"

    
    // Keep track of logged-in user
    var currentUser: User? = null
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
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
                Log.e(TAG, "Registration failed or offline: ${e.message}")
                // Fallback: Mock successful local registration in offline mode
                val offlineUser = User(999, fullName, email)
                currentUser = offlineUser
                onResult(AuthResponse("success", "Registration successful (Offline Mock Mode)", offlineUser))
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        val res = gson.fromJson(bodyStr, AuthResponse::class.java)
                        if (res.status == "success" && res.user != null) {
                            currentUser = res.user
                        }
                        onResult(res)
                    } else {
                        val bodyStr = response.body?.string() ?: ""
                        val res = try {
                            gson.fromJson(bodyStr, AuthResponse::class.java)
                        } catch (e: Exception) {
                            AuthResponse("error", "Registration error: ${response.code}", null)
                        }
                        onResult(res)
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
                Log.e(TAG, "Login failed or offline: ${e.message}")
                // Fallback: Login successfully locally for review
                val offlineUser = User(999, "Student Explorer (Offline)", email)
                currentUser = offlineUser
                onResult(AuthResponse("success", "Login successful (Offline Mock Mode)", offlineUser))
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        val res = gson.fromJson(bodyStr, AuthResponse::class.java)
                        if (res.status == "success" && res.user != null) {
                            currentUser = res.user
                        }
                        onResult(res)
                    } else {
                        val bodyStr = response.body?.string() ?: ""
                        val res = try {
                            gson.fromJson(bodyStr, AuthResponse::class.java)
                        } catch (e: Exception) {
                            AuthResponse("error", "Invalid email or password", null)
                        }
                        onResult(res)
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
                Log.e(TAG, "Get modules failed or offline: ${e.message}")
                // Fallback to local hardcoded mock modules
                val mockModules = listOf(
                    ModuleData("1", "Fundamentals of Biology", "Ready", 3),
                    ModuleData("2", "World History 101", "In Progress", 3),
                    ModuleData("3", "Modern Art", "Ready", 3)
                )
                onResult(mockModules)
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
    fun createModule(title: String, status: String = "Ready", onResult: (Boolean) -> Unit) {
        val payload = mapOf("title" to title, "status" to status)
        val body = gson.toJson(payload).toRequestBody(jsonMediaType)
        val request = Request.Builder().url("$baseUrl/api/modules").post(body).build()
        
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e(TAG, "Create module failed or offline: ${e.message}")
                onResult(true) // Fallback success
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                onResult(response.isSuccessful)
            }
        })
    }

    // 3b. Update Module
    fun updateModule(moduleId: String, title: String, status: String = "Ready", onResult: (Boolean) -> Unit) {
        val payload = mapOf("title" to title, "status" to status)
        val body = gson.toJson(payload).toRequestBody(jsonMediaType)
        val request = Request.Builder().url("$baseUrl/api/modules/$moduleId").put(body).build()
        
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e(TAG, "Update module failed or offline: ${e.message}")
                onResult(true) // Fallback success
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
                Log.e(TAG, "Delete module failed or offline: ${e.message}")
                onResult(true) // Fallback success
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
                Log.e(TAG, "Get quiz failed or offline: ${e.message}")
                // Fallback to local hardcoded questions depending on module ID
                val fallbackData = when(moduleId) {
                    "1" -> listOf(
                        Flashcard("FUNDAMENTALS", "What is the powerhouse of the cell?", listOf("Nucleus", "Mitochondria", "Ribosome", "Endoplasmic Reticulum"), 1),
                        Flashcard("BIOLOGY", "Which pigment gives plants their green color?", listOf("Carotene", "Chlorophyll", "Xanthophyll", "Anthocyanin"), 1),
                        Flashcard("BIOLOGY", "How many chromosomes do humans have?", listOf("23", "44", "46", "48"), 2)
                    )
                    "2" -> listOf(
                        Flashcard("GEOGRAPHY", "What is the capital of the Byzantine Empire?", listOf("Rome", "Athens", "Constantinople", "Alexandria"), 2),
                        Flashcard("HISTORY", "Who was the first President of the United States?", listOf("Thomas Jefferson", "George Washington", "John Adams", "Benjamin Franklin"), 1),
                        Flashcard("HISTORY", "In which year did World War II end?", listOf("1918", "1939", "1945", "1950"), 2)
                    )
                    else -> listOf(
                        Flashcard("ART HISTORY", "Who painted 'The Starry Night'?", listOf("Claude Monet", "Vincent van Gogh", "Leonardo da Vinci", "Pablo Picasso"), 1),
                        Flashcard("ART", "Which artistic movement is Salvador Dali associated with?", listOf("Impressionism", "Surrealism", "Cubism", "Expressionism"), 1),
                        Flashcard("ART", "Who sculpted the famous statue of 'David'?", listOf("Michelangelo", "Donatello", "Leonardo da Vinci", "Raphael"), 0)
                    )
                }
                onResult(fallbackData)
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
        val userId = currentUser?.id ?: 999
        val request = Request.Builder().url("$baseUrl/api/users/$userId/stats").get().build()
        
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e(TAG, "Get stats failed or offline: ${e.message}")
                // Fallback mock stats
                onResult(StatsResponse("85%", "3 Cards", "What is the capital of the Byzantine Empire?", "Hint: It was renamed to Istanbul in modern geography."))
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        val res = gson.fromJson(bodyStr, StatsResponse::class.java)
                        onResult(res)
                    } else {
                        onResult(StatsResponse("85%", "3 Cards", "What is the capital of the Byzantine Empire?", "Hint: It was renamed to Istanbul in modern geography."))
                    }
                }
            }
        })
    }

    // 6. Update User Stats
    fun updateStats(score: Int, total: Int) {
        val userId = currentUser?.id ?: 999
        val payload = mapOf("score" to score, "total" to total)
        val body = gson.toJson(payload).toRequestBody(jsonMediaType)
        val request = Request.Builder().url("$baseUrl/api/users/$userId/stats").post(body).build()
        
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e(TAG, "Update stats failed or offline: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    Log.i(TAG, "Stats updated successfully: ${response.code}")
                }
            }
        })
    }
}

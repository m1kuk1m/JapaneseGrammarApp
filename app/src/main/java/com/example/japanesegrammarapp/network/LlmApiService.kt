package com.example.japanesegrammarapp.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

// OpenAI Compatible Models
data class OpenAiResponseFormat(val type: String)

data class OpenAiRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double? = null,
    val response_format: OpenAiResponseFormat? = null
)
data class OpenAiMessage(val role: String, val content: Any) // content can be String or List<OpenAiContentPart>
data class OpenAiContentPart(
    val type: String, // "text" or "image_url"
    val text: String? = null,
    val image_url: OpenAiImageUrl? = null
)
data class OpenAiImageUrl(
    val url: String // "data:image/jpeg;base64,{base64}"
)
data class OpenAiResponse(val choices: List<OpenAiChoice>)
data class OpenAiChoice(val message: OpenAiResponseMessage)
data class OpenAiResponseMessage(val role: String, val content: String) // Responses are always textual content
data class OpenAiModelListResponse(val data: List<OpenAiModel>)
data class OpenAiModel(val id: String)

// Gemini Models
data class GeminiGenerationConfig(
    val temperature: Double? = null,
    val responseMimeType: String? = null
)

data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig? = null
)
data class GeminiContent(val role: String = "user", val parts: List<GeminiPart>)
data class GeminiPart(val text: String? = null, val inlineData: GeminiInlineData? = null)
data class GeminiInlineData(val mimeType: String, val data: String) // Base64 data
data class GeminiResponse(val candidates: List<GeminiCandidate>?)
data class GeminiCandidate(val content: GeminiContent?)
data class GeminiModelListResponse(val models: List<GeminiModel>)
data class GeminiModel(val name: String, val supportedGenerationMethods: List<String>? = null)

interface LlmApiService {
    // OpenAI-compatible: chat completions
    @POST
    suspend fun generateOpenAiCompatible(
        @Url url: String,
        @Header("Authorization") authHeader: String,
        @Body request: OpenAiRequest
    ): OpenAiResponse

    // OpenAI-compatible: list models
    @GET
    suspend fun getOpenAiModels(
        @Url url: String,
        @Header("Authorization") authHeader: String
    ): OpenAiModelListResponse

    // Gemini/Vertex: generate content (uses ?key= URL parameter)
    @POST
    suspend fun generateGemini(
        @Url url: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    // Gemini/Vertex: list models (uses ?key= URL parameter to bypass proxy header drops)
    @GET
    suspend fun getGeminiModels(
        @Url url: String
    ): GeminiModelListResponse
}

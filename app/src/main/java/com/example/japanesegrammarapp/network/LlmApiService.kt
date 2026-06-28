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
    val response_format: OpenAiResponseFormat? = null,
    val stream: Boolean? = null,
    val stream_options: Map<String, Boolean>? = null
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
data class OpenAiResponse(val choices: List<OpenAiChoice>, val usage: OpenAiUsage? = null)
data class OpenAiUsage(val total_tokens: Int? = null, val prompt_tokens: Int? = null, val completion_tokens: Int? = null)
data class OpenAiChoice(val message: OpenAiResponseMessage?, val delta: OpenAiResponseMessage? = null, val finish_reason: String? = null)
data class OpenAiResponseMessage(val role: String? = null, val content: String? = null) // Responses are always textual content
data class OpenAiModelListResponse(val data: List<OpenAiModel>)
data class OpenAiModel(val id: String)

// Gemini Models
data class GeminiGenerationConfig(
    val temperature: Double? = null,
    val responseMimeType: String? = null
)

data class GeminiSystemInstruction(val parts: List<GeminiPart>)

data class GeminiSafetySetting(
    val category: String,
    val threshold: String
)

data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiSystemInstruction? = null,
    val generationConfig: GeminiGenerationConfig? = null,
    val safetySettings: List<GeminiSafetySetting>? = null
)
data class GeminiContent(val role: String = "user", val parts: List<GeminiPart>)
data class GeminiPart(val text: String? = null, val inlineData: GeminiInlineData? = null)
data class GeminiInlineData(val mimeType: String, val data: String) // Base64 data
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?,
    val usageMetadata: GeminiUsageMetadata? = null,
    val promptFeedback: GeminiPromptFeedback? = null
)
data class GeminiUsageMetadata(val totalTokenCount: Int? = null, val promptTokenCount: Int? = null, val candidatesTokenCount: Int? = null)
data class GeminiCandidate(
    val content: GeminiContent?,
    val finishReason: String? = null,
    val safetyRatings: List<GeminiSafetyRating>? = null
)
data class GeminiPromptFeedback(
    val blockReason: String? = null,
    val safetyRatings: List<GeminiSafetyRating>? = null
)
data class GeminiSafetyRating(
    val category: String? = null,
    val probability: String? = null,
    val blocked: Boolean? = null
)
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

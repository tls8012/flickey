package com.example.flickey

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.nio.LongBuffer
import java.util.Locale
import kotlin.math.min

class NextWordManager(private val context: Context) {

    companion object {
        private const val MODEL_ONNX = "next_word_2layer.onnx"
        private const val MODEL_DATA = "next_word_2layer.onnx.data"
        private const val STOI_JSON = "stoi.json"
        private const val ITOS_JSON = "itos.json"

        private const val PAD = "<PAD>"
        private const val UNK = "<UNK>"
        private const val BOS = "<BOS>"
        private const val NUM = "<NUM>"
        private const val URL = "<URL>"
        private const val EMAIL = "<EMAIL>"
    }

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    private val stoi: Map<String, Int>
    private val itos: List<String>

    private val padId: Int
    private val unkId: Int
    private val bosId: Int
    private val numId: Int
    private val urlId: Int
    private val emailId: Int

    private val LOGLOCATION: String = "TEMPLOGFILE.csv"

    init {
        copyAssetIfNeeded(MODEL_ONNX)
        copyAssetIfNeeded(MODEL_DATA)
        copyAssetIfNeeded(STOI_JSON)
        copyAssetIfNeeded(ITOS_JSON)

        stoi = loadStoiFromAssets(STOI_JSON)
        itos = loadItosFromAssets(ITOS_JSON)

        padId = stoi[PAD] ?: 0
        unkId = stoi[UNK] ?: 1
        bosId = stoi[BOS] ?: 2
        numId = stoi[NUM] ?: 3
        urlId = stoi[URL] ?: 4
        emailId = stoi[EMAIL] ?: 5

        val sessionOptions = OrtSession.SessionOptions()
        val modelPath = File(context.filesDir, MODEL_ONNX).absolutePath
        session = env.createSession(modelPath, sessionOptions)
    }

    /**
     * beforeCursor 전체 텍스트를 받아, 직전 최대 3개 단어를 기반으로
     * 다음 단어 topK 추천 반환
     */
    suspend fun getSuggestions(beforeCursor: String, topK: Int = 3): List<String> =
        withContext(Dispatchers.Default) {
            val starttme = System.currentTimeMillis()
            val prevTokens = extractContextTokens(beforeCursor)
            val tokenex = (System.currentTimeMillis()-starttme)
            val inputIds = buildInputIds(prevTokens)
            val idex = (System.currentTimeMillis()-starttme+tokenex)

            val shape = longArrayOf(1, 3)
            val inputName = session.inputNames.first()

            val result = OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds), shape).use { tensor ->
                session.run(mapOf(inputName to tensor)).use { results ->
                    val output = results[0].value
                    val logits = extractLogits(output)

                    val topIndices = topKIndices(logits, topK)
                    topIndices
                        .mapNotNull { idx -> itos.getOrNull(idx) }
                        .filter { token ->
                            token.isNotBlank() &&
                                    token != PAD &&
                                    token != UNK &&
                                    token != BOS
                        }
                }
            }
            val edtime = (System.currentTimeMillis()-starttme)
            val row = buildString {
                append(prevTokens.toString())
                append(",")
                append(result.toString())
                append(",")
                append(tokenex.toString())
                append(",")
                append(idex.toString())
                append(",")
                append(edtime.toString())
            }
            File(context.filesDir, LOGLOCATION).appendText(row + "\n")
            result
        }


    /**
     * 직전 최대 3개 토큰 -> 왼쪽 패딩 [1,3]
     * 예:
     * [] -> [PAD, PAD, BOS]
     * [hello] -> [PAD, PAD, hello]
     * [i, am] -> [PAD, i, am]
     * [i, am, here] -> [i, am, here]
     */
    private fun buildInputIds(prevTokens: List<String>): LongArray {
        val ids = prevTokens.takeLast(3).map { normalizeAndMapToken(it).toLong() }
        val result = MutableList(3) { padId.toLong() }

        if (ids.isEmpty()) {
            result[2] = bosId.toLong()
        } else {
            val start = 3 - ids.size
            for (i in ids.indices) {
                result[start + i] = ids[i]
            }
        }
        return result.toLongArray()
    }

    /**
     * 커서 앞 문자열에서 "완성된 단어들"만 뽑는다.
     * 마지막이 공백/문장부호면 직전 단어는 이미 끝난 것으로 보고 포함.
     * 마지막이 영문/숫자면 아직 단어 중간이라고 보고, 마지막 조각은 제외.
     */
    private fun extractContextTokens(beforeCursor: String): List<String> {
        if (beforeCursor.isBlank()) return emptyList()

        val raw = beforeCursor
        val endsWithWordChar = raw.lastOrNull()?.let { isWordChar(it) } == true

        val pieces = raw.split(Regex("\\s+"))
            .flatMap { chunk ->
                chunk.split(Regex("(?<=[.,!?;:()\\[\\]{}\"'])|(?=[.,!?;:()\\[\\]{}\"'])"))
            }
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (pieces.isEmpty()) return emptyList()

        return if (endsWithWordChar) {
            // 단어 중간이므로 마지막 진행 중인 토큰 제외
            pieces.dropLast(1).filter { looksLikeToken(it) }
        } else {
            pieces.filter { looksLikeToken(it) }
        }
    }

    private fun normalizeAndMapToken(token: String): Int {
        return when {
            isUrl(token) -> urlId
            isEmail(token) -> emailId
            isNumber(token) -> numId
            else -> stoi[token.lowercase(Locale.ROOT)] ?: unkId
        }
    }

    private fun topKIndices(logits: FloatArray, k: Int): List<Int> {
        return logits.indices
            .sortedByDescending { logits[it] }
            .take(k)
    }

    /**
     * 보통 출력 shape은 [1, vocab] 또는 [vocab]일 가능성이 높다.
     * 둘 다 처리.
     */
    private fun extractLogits(output: Any?): FloatArray {
        return when (output) {
            is Array<*> -> {
                val row = output.firstOrNull()
                when (row) {
                    is FloatArray -> row
                    else -> error("Unsupported ONNX output type: ${output::class.java}")
                }
            }
            is FloatArray -> output
            else -> error("Unsupported ONNX output type: ${output?.javaClass}")
        }
    }

    private fun isWordChar(c: Char): Boolean {
        return c.isLetterOrDigit() || c == '\'' || c == '-'
    }

    private fun looksLikeToken(s: String): Boolean {
        return s.any { it.isLetterOrDigit() }
    }

    private fun isNumber(s: String): Boolean {
        return s.matches(Regex("^[+-]?\\d+([.,]\\d+)?$"))
    }

    private fun isUrl(s: String): Boolean {
        return s.startsWith("http://", ignoreCase = true) ||
                s.startsWith("https://", ignoreCase = true) ||
                s.startsWith("www.", ignoreCase = true)
    }

    private fun isEmail(s: String): Boolean {
        return s.matches(Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
    }

    private fun copyAssetIfNeeded(fileName: String) {
        val outFile = File(context.filesDir, fileName)
        if (outFile.exists()) return

        context.assets.open(fileName).use { input ->
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun loadStoiFromAssets(fileName: String): Map<String, Int> {
        val text = context.assets.open(fileName).bufferedReader().use { it.readText() }
        val json = org.json.JSONObject(text)
        val map = mutableMapOf<String, Int>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = json.getInt(key)
        }
        return map
    }

    /**
     * itos.json 이
     * 1) JSON array  ["<PAD>", "<UNK>", ...]
     * 또는
     * 2) JSON object {"0":"<PAD>", "1":"<UNK>", ...}
     * 둘 다 처리
     */
    private fun loadItosFromAssets(fileName: String): List<String> {
        val text = context.assets.open(fileName).bufferedReader().use { it.readText() }
        val trimmed = text.trim()

        return if (trimmed.startsWith("[")) {
            val arr = JSONArray(trimmed)
            List(arr.length()) { idx -> arr.getString(idx) }
        } else {
            val json = org.json.JSONObject(trimmed)
            val maxIndex = json.keys().asSequence().map { it.toInt() }.maxOrNull() ?: -1
            MutableList(maxIndex + 1) { "" }.also { list ->
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    list[key.toInt()] = json.getString(key)
                }
            }
        }
    }
}
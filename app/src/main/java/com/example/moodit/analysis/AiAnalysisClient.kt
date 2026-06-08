package com.example.moodit.analysis

import com.example.moodit.data.ExpenseRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AiAnalysisClient {
    private const val ANALYSIS_SERVER_URL = "http://10.0.2.2:3000/analyze"

    suspend fun analyze(records: List<ExpenseRecord>): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(ANALYSIS_SERVER_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 20_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }

            val body = JSONObject()
                .put("records", recordsToJson(records))
                .toString()

            connection.outputStream.use { stream ->
                stream.write(body.toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            val responseText = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            }

            if (responseCode !in 200..299) {
                error("AI 서버 응답 오류: $responseCode $responseText")
            }

            JSONObject(responseText).getString("report")
        }
    }

    private fun recordsToJson(records: List<ExpenseRecord>): JSONArray {
        val array = JSONArray()

        records.forEach { record ->
            array.put(
                JSONObject()
                    .put("amount", record.amount)
                    .put("category", record.category.label)
                    .put("subCategory", record.subCategory.label)
                    .put("mood", record.mood.label)
                    .put("memo", record.memo)
                    .put("createdAt", record.createdAt)
            )
        }

        return array
    }
}

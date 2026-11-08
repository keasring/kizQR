package com.kizqr.app.data

import android.content.Context
import com.kizqr.app.qr.QrContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class HistoryKind { CREATED, SCANNED }

data class HistoryEntry(
    val id: Long,
    val kind: HistoryKind,
    val payload: String,
    val contentType: QrContentType,
    /** Кодированный стиль (только для CREATED). */
    val styleEncoded: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * История QR-кодов. JSON-файл в filesDir + StateFlow.
 * Для объёма первого порядка это проще и надёжнее Room без кодогенерации.
 */
class HistoryRepository(context: Context) {

    private val file = File(context.filesDir, "history.json")
    private val mutex = Mutex()
    private val _entries = MutableStateFlow<List<HistoryEntry>>(emptyList())

    val entries: StateFlow<List<HistoryEntry>> = _entries

    init {
        _entries.value = load()
    }

    suspend fun add(entry: HistoryEntry) = withContext(Dispatchers.IO) {
        mutex.withLock {
            _entries.update { current -> (listOf(entry) + current).take(MAX_ENTRIES) }
            writeCurrent()
        }
    }

    suspend fun remove(id: Long) = withContext(Dispatchers.IO) {
        mutex.withLock {
            _entries.update { current -> current.filterNot { it.id == id } }
            writeCurrent()
        }
    }

    suspend fun clear(kind: HistoryKind) = withContext(Dispatchers.IO) {
        mutex.withLock {
            _entries.update { current -> current.filterNot { it.kind == kind } }
            writeCurrent()
        }
    }

    private fun writeCurrent() {
        val arr = JSONArray()
        for (e in _entries.value) {
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("kind", e.kind.name)
                    .put("payload", e.payload)
                    .put("type", e.contentType.name)
                    .put("style", e.styleEncoded ?: "")
                    .put("createdAt", e.createdAt),
            )
        }
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(arr.toString())
        if (!tmp.renameTo(file)) {
            file.writeText(arr.toString())
            tmp.delete()
        }
    }

    private fun load(): List<HistoryEntry> = runCatching {
        if (!file.exists()) return emptyList()
        val arr = JSONArray(file.readText())
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            HistoryEntry(
                id = o.getLong("id"),
                kind = HistoryKind.valueOf(o.getString("kind")),
                payload = o.getString("payload"),
                contentType = runCatching { QrContentType.valueOf(o.getString("type")) }
                    .getOrDefault(QrContentType.TEXT),
                styleEncoded = o.optString("style").takeIf { it.isNotEmpty() },
                createdAt = o.getLong("createdAt"),
            )
        }
    }.getOrDefault(emptyList())

    companion object {
        private const val MAX_ENTRIES = 500

        @Volatile
        private var nextId: Long = System.currentTimeMillis()

        fun newId(): Long = nextId++
    }
}

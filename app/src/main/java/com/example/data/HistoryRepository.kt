package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ConversionHistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class HistoryRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("conversion_history_prefs", Context.MODE_PRIVATE)

    private val _history = MutableStateFlow<List<ConversionHistoryItem>>(emptyList())
    val history: StateFlow<List<ConversionHistoryItem>> = _history.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        val jsonStr = prefs.getString("history_items", null) ?: return
        try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<ConversionHistoryItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val item = ConversionHistoryItem(
                    id = obj.getString("id"),
                    sourceName = obj.getString("sourceName"),
                    targetName = obj.getString("targetName"),
                    sourceFormat = obj.getString("sourceFormat"),
                    targetFormat = obj.getString("targetFormat"),
                    filePath = obj.getString("filePath"),
                    fileSize = obj.getLong("fileSize"),
                    timestamp = obj.getLong("timestamp"),
                    isSigned = obj.optBoolean("isSigned", false)
                )
                // Only keep if file actually still exists on device
                if (File(item.filePath).exists()) {
                    list.add(item)
                }
            }
            _history.value = list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveHistory(list: List<ConversionHistoryItem>) {
        try {
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("sourceName", item.sourceName)
                    put("targetName", item.targetName)
                    put("sourceFormat", item.sourceFormat)
                    put("targetFormat", item.targetFormat)
                    put("filePath", item.filePath)
                    put("fileSize", item.fileSize)
                    put("timestamp", item.timestamp)
                    put("isSigned", item.isSigned)
                }
                arr.put(obj)
            }
            prefs.edit().putString("history_items", arr.toString()).apply()
            _history.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addHistoryItem(item: ConversionHistoryItem) {
        val current = _history.value.toMutableList()
        current.removeAll { it.filePath == item.filePath }
        current.add(0, item)
        saveHistory(current)
    }

    fun deleteHistoryItem(id: String) {
        val current = _history.value.toMutableList()
        val item = current.find { it.id == id }
        if (item != null) {
            try {
                File(item.filePath).delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            current.remove(item)
            saveHistory(current)
        }
    }

    fun clearAllHistory() {
        for (item in _history.value) {
            try {
                File(item.filePath).delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        saveHistory(emptyList())
    }
}

package com.kizqr.app.ui.scan

import android.app.Application
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kizqr.app.KizQrApp
import com.kizqr.app.data.HistoryEntry
import com.kizqr.app.data.HistoryKind
import com.kizqr.app.data.HistoryRepository
import com.kizqr.app.qr.QrContent
import com.kizqr.app.qr.QrDecoder
import com.kizqr.app.qr.parseScanned
import kotlinx.coroutines.launch

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as KizQrApp

    data class ScanResult(val raw: String, val content: QrContent)

    var result by mutableStateOf<ScanResult?>(null)
        private set

    var galleryNotFound by mutableStateOf(false)
        private set

    var wifiRequested by mutableStateOf(false)
        private set

    private var lastScanAt = 0L

    fun onScanned(raw: String) {
        val now = System.currentTimeMillis()
        if (now - lastScanAt < SCAN_COOLDOWN_MS) return
        lastScanAt = now
        publish(raw)
    }

    fun scanFromGallery(uri: Uri) {
        viewModelScope.launch {
            val value = QrDecoder.decodeFromUri(app, uri)
            if (value != null) {
                galleryNotFound = false
                publish(value)
            } else {
                galleryNotFound = true
            }
        }
    }

    fun dismiss() {
        result = null
        wifiRequested = false
    }

    fun clearNotFound() {
        galleryNotFound = false
    }

    /** Подключение к Wi-Fi через WifiNetworkSuggestion (API 29+). */
    fun connectWifi(wifi: QrContent.Wifi): Boolean {
        if (Build.VERSION.SDK_INT < 29) return false
        if (wifi.encryption == QrContent.WifiEncryption.NOPASS || wifi.password.isEmpty()) return false
        val builder = WifiNetworkSuggestion.Builder()
            .setSsid(wifi.ssid)
            .setWpa2Passphrase(wifi.password)
        val suggestion = builder.build()
        val manager = app.getSystemService(WifiManager::class.java)
        val status = manager.addNetworkSuggestions(listOf(suggestion))
        val ok = status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS
        if (ok) wifiRequested = true
        return ok
    }

    private fun publish(raw: String) {
        galleryNotFound = false
        wifiRequested = false
        val content = parseScanned(raw)
        result = ScanResult(raw, content)
        viewModelScope.launch {
            app.historyRepository.add(
                HistoryEntry(
                    id = HistoryRepository.newId(),
                    kind = HistoryKind.SCANNED,
                    payload = raw,
                    contentType = content.kind,
                ),
            )
        }
    }

    companion object {
        private const val SCAN_COOLDOWN_MS = 1500L
    }
}

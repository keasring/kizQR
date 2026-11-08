package com.kizqr.app.ui.create

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kizqr.app.KizQrApp
import com.kizqr.app.data.HistoryEntry
import com.kizqr.app.data.HistoryKind
import com.kizqr.app.data.HistoryRepository
import com.kizqr.app.data.QrExporter
import com.kizqr.app.qr.LogoPreset
import com.kizqr.app.qr.QrContent
import com.kizqr.app.qr.QrContentType
import com.kizqr.app.qr.QrPresets
import com.kizqr.app.qr.QrRenderer
import com.kizqr.app.qr.QrStyle
import com.kizqr.app.qr.parseScanned
import com.kizqr.app.qr.payload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as KizQrApp

    // — Тип контента —
    var selectedType by mutableStateOf(QrContentType.TEXT)

    // — Поля форм —
    var text by mutableStateOf("")
    var url by mutableStateOf("")
    var wifiSsid by mutableStateOf("")
    var wifiPassword by mutableStateOf("")
    var wifiEncryption by mutableStateOf(QrContent.WifiEncryption.WPA)
    var wifiHidden by mutableStateOf(false)
    var contactFirst by mutableStateOf("")
    var contactLast by mutableStateOf("")
    var contactOrg by mutableStateOf("")
    var contactPhone by mutableStateOf("")
    var contactEmail by mutableStateOf("")
    var contactWebsite by mutableStateOf("")
    var contactNote by mutableStateOf("")
    var smsPhone by mutableStateOf("")
    var smsMessage by mutableStateOf("")
    var phoneNumber by mutableStateOf("")
    var geoLat by mutableStateOf("")
    var geoLng by mutableStateOf("")
    var geoLabel by mutableStateOf("")

    // — Стиль —
    var style by mutableStateOf(QrStyle())
    var appliedPreset by mutableStateOf(LogoPreset.NONE)
    var logoBitmap by mutableStateOf<Bitmap?>(null)

    // — Автоподсказка пресета по ссылке —
    var suggestedPreset by mutableStateOf<LogoPreset?>(null)
    private var presetManual = false

    // — Превью —
    var preview by mutableStateOf<Bitmap?>(null)
        private set
    var contentReady by mutableStateOf(false)
        private set

    private var renderJob: Job? = null

    init {
        scheduleRender()
    }

    // ————————————————————————————————— действия форм ——

    fun selectType(type: QrContentType) {
        selectedType = type
        scheduleRender()
    }

    fun onTextChange(v: String) { text = v; scheduleRender() }

    fun onUrlChange(v: String) {
        url = v
        if (!presetManual && v.isNotBlank()) {
            suggestedPreset = QrPresets.detect(v.trim())
        } else if (v.isBlank()) {
            suggestedPreset = null
        }
        scheduleRender()
    }

    fun onWifiChange(ssid: String? = null, password: String? = null) {
        ssid?.let { wifiSsid = it }
        password?.let { wifiPassword = it }
        scheduleRender()
    }

    fun onWifiEncryptionChange(v: QrContent.WifiEncryption) {
        wifiEncryption = v
        scheduleRender()
    }

    fun onWifiHiddenChange(v: Boolean) {
        wifiHidden = v
        scheduleRender()
    }

    fun onContactChange(
        first: String? = null, last: String? = null, org: String? = null,
        phone: String? = null, email: String? = null, website: String? = null, note: String? = null,
    ) {
        first?.let { contactFirst = it }
        last?.let { contactLast = it }
        org?.let { contactOrg = it }
        phone?.let { contactPhone = it }
        email?.let { contactEmail = it }
        website?.let { contactWebsite = it }
        note?.let { contactNote = it }
        scheduleRender()
    }

    fun onSmsChange(phone: String? = null, message: String? = null) {
        phone?.let { smsPhone = it }
        message?.let { smsMessage = it }
        scheduleRender()
    }

    fun onPhoneChange(v: String) { phoneNumber = v; scheduleRender() }

    fun onGeoChange(lat: String? = null, lng: String? = null, label: String? = null) {
        lat?.let { geoLat = it }
        lng?.let { geoLng = it }
        label?.let { geoLabel = it }
        scheduleRender()
    }

    // ————————————————————————————————— пресеты и стиль ——

    fun acceptSuggestedPreset() {
        val preset = suggestedPreset ?: return
        applyPreset(preset)
        suggestedPreset = null
    }

    fun dismissSuggestedPreset() {
        presetManual = true
        suggestedPreset = null
    }

    fun selectPreset(preset: LogoPreset) {
        presetManual = true
        suggestedPreset = null
        applyPreset(preset)
    }

    private fun applyPreset(preset: LogoPreset) {
        appliedPreset = preset
        logoBitmap = null
        style = if (preset == LogoPreset.NONE) {
            QrStyle()
        } else {
            QrPresets.byId[preset]?.style ?: QrStyle()
        }
        scheduleRender()
    }

    fun updateStyle(newStyle: QrStyle) {
        style = newStyle
        scheduleRender()
    }

    /** Выбор брендового логотипа — фото-логотип сбрасывается. */
    fun selectLogoPreset(preset: LogoPreset) {
        logoBitmap = null
        style = style.copy(logo = preset, logoUri = null)
        scheduleRender()
    }

    fun setLogoUri(uri: Uri) {
        viewModelScope.launch {
            val decoded = decodeSampledBitmap(getApplication(), uri)
            if (decoded != null) {
                logoBitmap = decoded
                style = style.copy(logoUri = uri.toString(), logo = LogoPreset.NONE)
                scheduleRender()
            }
        }
    }

    fun clearLogo() {
        logoBitmap = null
        style = style.copy(logoUri = null)
        scheduleRender()
    }

    // ————————————————————————————————— контент ——

    fun buildContent(): QrContent? = when (selectedType) {
        QrContentType.TEXT -> text.trim().takeIf { it.isNotEmpty() }?.let { QrContent.Text(it) }
        QrContentType.URL -> url.trim().takeIf { it.isNotEmpty() }?.let { QrContent.Url(it) }
        QrContentType.WIFI -> wifiSsid.isNotBlank().takeIf { it }?.let {
            QrContent.Wifi(wifiSsid.trim(), wifiPassword, wifiEncryption, wifiHidden)
        }
        QrContentType.CONTACT -> (
            (contactFirst + contactLast + contactOrg + contactPhone + contactEmail + contactWebsite + contactNote)
                .isNotBlank()
            ).takeIf { it }?.let {
            QrContent.Contact(
                firstName = contactFirst.trim(), lastName = contactLast.trim(),
                org = contactOrg.trim(), phone = contactPhone.trim(),
                email = contactEmail.trim(), website = contactWebsite.trim(), note = contactNote.trim(),
            )
        }
        QrContentType.SMS -> smsPhone.isNotBlank().takeIf { it }?.let {
            QrContent.Sms(smsPhone.trim(), smsMessage)
        }
        QrContentType.PHONE -> phoneNumber.trim().takeIf { it.isNotEmpty() }?.let { QrContent.Phone(it) }
        QrContentType.GEO -> (geoLat.isNotBlank() && geoLng.isNotBlank()).takeIf { it }?.let {
            QrContent.Geo(geoLat.trim(), geoLng.trim(), geoLabel.trim())
        }
    }

    fun prefill(raw: String) {
        when (val content = parseScanned(raw)) {
            is QrContent.Text -> { selectedType = QrContentType.TEXT; text = content.text }
            is QrContent.Url -> { selectedType = QrContentType.URL; url = content.url }
            is QrContent.Wifi -> {
                selectedType = QrContentType.WIFI
                wifiSsid = content.ssid
                wifiPassword = content.password
                wifiEncryption = content.encryption
                wifiHidden = content.hidden
            }
            is QrContent.Contact -> {
                selectedType = QrContentType.CONTACT
                contactFirst = content.firstName
                contactLast = content.lastName
                contactOrg = content.org
                contactPhone = content.phone
                contactEmail = content.email
                contactWebsite = content.website
                contactNote = content.note
            }
            is QrContent.Sms -> { selectedType = QrContentType.SMS; smsPhone = content.phone; smsMessage = content.message }
            is QrContent.Phone -> { selectedType = QrContentType.PHONE; phoneNumber = content.number }
            is QrContent.Geo -> { selectedType = QrContentType.GEO; geoLat = content.lat; geoLng = content.lng; geoLabel = content.label }
        }
        scheduleRender()
    }

    // ————————————————————————————————— рендер и экспорт ——

    fun scheduleRender() {
        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            delay(RENDER_DEBOUNCE_MS)
            val content = buildContent()
            contentReady = content != null
            val payload = content?.payload()
            if (payload.isNullOrBlank()) {
                preview = null
                return@launch
            }
            val s = style
            val logo = logoBitmap
            preview = withContext(Dispatchers.Default) {
                runCatching { QrRenderer.render(payload, s, PREVIEW_SIZE, logo) }.getOrNull()
            }
        }
    }

    fun savePng(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val content = buildContent() ?: return@launch onResult(false)
            val payload = content.payload()
            val s = style
            val logo = logoBitmap
            val bitmap = withContext(Dispatchers.Default) {
                runCatching { QrRenderer.render(payload, s, EXPORT_SIZE, logo) }.getOrNull()
            } ?: return@launch onResult(false)
            val ok = QrExporter.saveToGallery(app, bitmap).isSuccess
            if (ok) {
                app.historyRepository.add(
                    HistoryEntry(
                        id = HistoryRepository.newId(),
                        kind = HistoryKind.CREATED,
                        payload = payload,
                        contentType = content.kind,
                        styleEncoded = s.encode(),
                    ),
                )
            }
            onResult(ok)
        }
    }

    fun sharePng(onResult: (android.content.Intent?) -> Unit) {
        viewModelScope.launch {
            val content = buildContent() ?: return@launch onResult(null)
            val payload = content.payload()
            val s = style
            val logo = logoBitmap
            val bitmap = withContext(Dispatchers.Default) {
                runCatching { QrRenderer.render(payload, s, EXPORT_SIZE, logo) }.getOrNull()
            } ?: return@launch onResult(null)
            onResult(QrExporter.shareIntent(app, bitmap).getOrNull())
        }
    }

    companion object {
        private const val RENDER_DEBOUNCE_MS = 250L
        private const val PREVIEW_SIZE = 800
        const val EXPORT_SIZE = 2048
    }
}

/** Декодирование фото-логотипа с сэмплингом до ~1024px. */
suspend fun decodeSampledBitmap(context: android.content.Context, uri: Uri, maxDim: Int = 1024): Bitmap? =
    withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 && bounds.outHeight <= 0) return@runCatching null
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDim) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
        }.getOrNull()
    }

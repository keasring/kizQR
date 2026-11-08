package com.kizqr.app

import android.app.Application
import com.kizqr.app.data.HistoryRepository
import com.kizqr.app.data.SettingsRepository

class KizQrApp : Application() {

    val historyRepository: HistoryRepository by lazy { HistoryRepository(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
}

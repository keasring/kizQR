package com.kizqr.app.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kizqr.app.KizQrApp
import com.kizqr.app.data.HistoryKind
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as KizQrApp).historyRepository

    val entries = repo.entries

    fun remove(id: Long) {
        viewModelScope.launch { repo.remove(id) }
    }

    fun clear(kind: HistoryKind) {
        viewModelScope.launch { repo.clear(kind) }
    }
}

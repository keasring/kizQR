package com.kizqr.app.ui.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kizqr.app.R
import com.kizqr.app.data.HistoryEntry
import com.kizqr.app.data.HistoryKind
import com.kizqr.app.data.QrExporter
import com.kizqr.app.qr.QrContentType
import com.kizqr.app.qr.QrRenderer
import com.kizqr.app.qr.QrStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel()) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var detail by remember { mutableStateOf<HistoryEntry?>(null) }

    val kind = if (tab == 0) HistoryKind.CREATED else HistoryKind.SCANNED
    val list = entries.filter { it.kind == kind }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.nav_history),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { viewModel.clear(kind) }) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = stringResource(R.string.history_clear))
            }
        }
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.history_created)) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.history_scanned)) })
        }

        if (list.isEmpty()) {
            EmptyHistory(kind == HistoryKind.CREATED)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                items(list, key = { it.id }) { entry ->
                    HistoryRow(
                        entry = entry,
                        onClick = { detail = entry },
                        onDelete = { viewModel.remove(entry.id) },
                    )
                }
            }
        }
    }

    detail?.let { entry ->
        HistoryDetailSheet(
            entry = entry,
            onDismiss = { detail = null },
            onDelete = {
                viewModel.remove(entry.id)
                detail = null
            },
        )
    }
}

@Composable
private fun EmptyHistory(created: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Rounded.History,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.outlineVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (created) {
                stringResource(R.string.history_empty_created)
            } else {
                stringResource(R.string.history_empty_scanned)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry, onClick: () -> Unit, onDelete: () -> Unit) {
    val date = remember(entry.createdAt) {
        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(entry.createdAt))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(entry.contentType.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.contentType.title,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = entry.payload.replace("\n", " "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Rounded.DeleteOutline,
                contentDescription = stringResource(R.string.history_delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryDetailSheet(
    entry: HistoryEntry,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val style = entry.styleEncoded?.let { QrStyle.decode(it) } ?: QrStyle()
    val bitmap: Bitmap? = remember(entry.id, style) {
        runCatching { QrRenderer.render(entry.payload, style, 512) }.getOrNull()
    }

    fun copy(text: String) {
        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(
            ClipData.newPlainText("kizQR", text),
        )
        Toast.makeText(context, R.string.result_copied, Toast.LENGTH_SHORT).show()
    }

    fun share(bitmap: Bitmap) {
        scope.launch {
            QrExporter.shareIntent(context, bitmap).getOrNull()?.let { intent ->
                runCatching { context.startActivity(Intent.createChooser(intent, null)) }
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            bitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(220.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(entry.contentType.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                entry.payload,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { copy(entry.payload) }) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.result_copy))
                }
                FilledTonalButton(onClick = onDelete) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.history_delete))
                }
            }
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(
                onClick = { bitmap?.let(::share) },
                enabled = bitmap != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_share))
            }
        }
    }
}

private fun QrContentType.icon(): ImageVector = when (this) {
    QrContentType.TEXT -> Icons.Rounded.CenterFocusStrong
    QrContentType.URL -> Icons.Rounded.Link
    QrContentType.WIFI -> Icons.Rounded.Wifi
    QrContentType.CONTACT -> Icons.Rounded.Person
    QrContentType.SMS -> Icons.Rounded.Sms
    QrContentType.PHONE -> Icons.Rounded.Call
    QrContentType.GEO -> Icons.Rounded.LocationOn
}

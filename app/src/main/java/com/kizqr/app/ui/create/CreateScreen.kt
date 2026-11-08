package com.kizqr.app.ui.create

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kizqr.app.R
import com.kizqr.app.qr.LogoPreset
import com.kizqr.app.qr.QrContentType
import com.kizqr.app.ui.components.BrandIconCanvas
import com.kizqr.app.ui.components.QrPreview
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateScreen(
    prefilledPayload: String?,
    viewModel: CreateViewModel = viewModel(),
) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showStyleEditor by rememberSaveable { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(viewModel::setLogoUri) }

    LaunchedEffect(prefilledPayload) {
        prefilledPayload?.takeIf { it.isNotBlank() }?.let(viewModel::prefill)
    }

    fun notify(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))

            QrPreview(
                bitmap = viewModel.preview,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .aspectRatio(1f),
                loading = viewModel.contentReady && viewModel.preview == null,
                emptyText = stringResource(R.string.empty_content),
            )

            Spacer(Modifier.height(12.dp))

            viewModel.suggestedPreset?.let { preset ->
                PresetSuggestionCard(
                    preset = preset,
                    onAccept = viewModel::acceptSuggestedPreset,
                    onDismiss = viewModel::dismissSuggestedPreset,
                )
                Spacer(Modifier.height(12.dp))
            }

            TypeSelector(
                selected = viewModel.selectedType,
                onSelect = viewModel::selectType,
            )

            Spacer(Modifier.height(8.dp))

            when (viewModel.selectedType) {
                QrContentType.TEXT -> TextForm(viewModel)
                QrContentType.URL -> UrlForm(viewModel)
                QrContentType.WIFI -> WifiForm(viewModel)
                QrContentType.CONTACT -> ContactForm(viewModel)
                QrContentType.SMS -> SmsForm(viewModel)
                QrContentType.PHONE -> PhoneForm(viewModel)
                QrContentType.GEO -> GeoForm(viewModel)
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = { showStyleEditor = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_style))
                }
                Button(
                    onClick = {
                        viewModel.savePng { ok ->
                            notify(
                                if (ok) context.getString(R.string.saved_ok)
                                else context.getString(R.string.saved_error),
                            )
                        }
                    },
                    enabled = viewModel.contentReady,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_save))
                }
            }

            Spacer(Modifier.height(8.dp))

            FilledTonalButton(
                onClick = {
                    viewModel.sharePng { intent ->
                        if (intent != null) {
                            context.startActivity(Intent.createChooser(intent, null))
                        } else {
                            notify(context.getString(R.string.saved_error))
                        }
                    }
                },
                enabled = viewModel.contentReady,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_share))
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showStyleEditor) {
        StyleEditorSheet(
            viewModel = viewModel,
            onDismiss = { showStyleEditor = false },
            onPickPhoto = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
        )
    }
}

@Composable
private fun TypeSelector(selected: QrContentType, onSelect: (QrContentType) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (type in QrContentType.entries) {
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(type) },
                label = { Text(stringResource(type.titleRes())) },
                leadingIcon = {
                    Icon(type.icon(), contentDescription = null, modifier = Modifier.size(16.dp))
                },
            )
        }
    }
}

@Composable
private fun PresetSuggestionCard(
    preset: LogoPreset,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandIconCanvas(
                logo = preset,
                tint = com.kizqr.app.qr.QrPresets.byId[preset]?.style?.color1 ?: 0xFF229ED9.toInt(),
                knockout = 0xFFFFFFFF.toInt(),
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.preset_suggested, preset.title()),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onAccept) { Text("Применить") }
            TextButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun QrContentType.titleRes(): Int = when (this) {
    QrContentType.TEXT -> R.string.type_text
    QrContentType.URL -> R.string.type_url
    QrContentType.WIFI -> R.string.type_wifi
    QrContentType.CONTACT -> R.string.type_contact
    QrContentType.SMS -> R.string.type_sms
    QrContentType.PHONE -> R.string.type_phone
    QrContentType.GEO -> R.string.type_geo
}

private fun QrContentType.icon(): ImageVector = when (this) {
    QrContentType.TEXT -> Icons.Rounded.TextFields
    QrContentType.URL -> Icons.Rounded.Link
    QrContentType.WIFI -> Icons.Rounded.Wifi
    QrContentType.CONTACT -> Icons.Rounded.Person
    QrContentType.SMS -> Icons.Rounded.Sms
    QrContentType.PHONE -> Icons.Rounded.Call
    QrContentType.GEO -> Icons.Rounded.LocationOn
}

private fun LogoPreset.title(): String = when (this) {
    LogoPreset.TELEGRAM -> "Telegram"
    LogoPreset.WHATSAPP -> "WhatsApp"
    LogoPreset.VK -> "VK"
    LogoPreset.INSTAGRAM -> "Instagram"
    LogoPreset.YOUTUBE -> "YouTube"
    LogoPreset.SPOTIFY -> "Spotify"
    LogoPreset.NONE -> ""
}

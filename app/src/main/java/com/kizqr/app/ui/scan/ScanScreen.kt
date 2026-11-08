package com.kizqr.app.ui.scan

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kizqr.app.R
import com.kizqr.app.qr.QrContent
import com.kizqr.app.qr.QrContentType
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onCreateQr: (String) -> Unit,
    viewModel: ScanViewModel = viewModel(),
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    var torchOn by rememberSaveable { mutableStateOf(false) }

    val galleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(viewModel::scanFromGallery) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasPermission) {
            CameraSection(
                torchOn = torchOn,
                onScanned = viewModel::onScanned,
            )
        } else {
            RationaleContent(
                onAllow = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            )
        }

        // Кнопки поверх камеры
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            ) {
                Row(modifier = Modifier.padding(horizontal = 6.dp)) {
                    if (hasPermission) {
                        IconButton(onClick = { torchOn = !torchOn }) {
                            Icon(
                                if (torchOn) Icons.Rounded.FlashlightOff else Icons.Rounded.FlashlightOn,
                                contentDescription = stringResource(R.string.scan_torch),
                            )
                        }
                    }
                    IconButton(onClick = {
                        galleryPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    }) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = stringResource(R.string.scan_gallery))
                    }
                }
            }
        }

        if (viewModel.galleryNotFound) {
            LaunchedEffect(Unit) { viewModel.clearNotFound() }
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 96.dp),
            ) {
                Text(
                    stringResource(R.string.scan_not_found),
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }

    viewModel.result?.let { r ->
        ScanResultSheet(
            result = r,
            onDismiss = viewModel::dismiss,
            onConnect = viewModel::connectWifi,
            onCreateQr = { onCreateQr(r.raw) },
        )
    }
}

@Composable
private fun RationaleContent(onAllow: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Rounded.CenterFocusStrong,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.scan_rationale),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAllow) { Text(stringResource(R.string.scan_allow)) }
    }
}

@Composable
private fun CameraSection(torchOn: Boolean, onScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val cameraRef = remember { mutableStateOf<Camera?>(null) }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

    DisposableEffect(Unit) {
        val executor = Executors.newSingleThreadExecutor()
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(executor, QrAnalyzer(onScanned)) }
                provider.unbindAll()
                cameraRef.value = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
            } catch (_: Exception) {
                // Камера недоступна — превью просто не появится
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            executor.shutdown()
            runCatching { providerFuture.get().unbindAll() }
        }
    }

    DisposableEffect(torchOn) {
        cameraRef.value?.cameraControl?.enableTorch(torchOn)
        onDispose { }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ScanResultSheet(
    result: ScanViewModel.ScanResult,
    onDismiss: () -> Unit,
    onConnect: (QrContent.Wifi) -> Boolean,
    onCreateQr: () -> Unit,
) {
    val context = LocalContext.current

    fun copy(text: String) {
        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(
            ClipData.newPlainText("kizQR", text),
        )
        android.widget.Toast.makeText(context, R.string.result_copied, android.widget.Toast.LENGTH_SHORT).show()
    }

    fun launch(intent: Intent) {
        runCatching { context.startActivity(Intent.createChooser(intent, null)) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(result.content.kind.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(result.content.kind.titleRes()), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))

            ResultFields(result.content)

            Spacer(Modifier.height(8.dp))
            Text(
                result.raw,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (val content = result.content) {
                    is QrContent.Url -> {
                        ActionChip(Icons.Rounded.OpenInNew, stringResource(R.string.result_open)) {
                            launch(Intent(Intent.ACTION_VIEW, Uri.parse(content.url)))
                        }
                        ActionChip(Icons.Rounded.ContentCopy, stringResource(R.string.result_copy)) {
                            copy(content.url)
                        }
                    }
                    is QrContent.Wifi -> {
                        ActionChip(Icons.Rounded.Wifi, stringResource(R.string.result_connect)) {
                            val ok = onConnect(content)
                            if (!ok) copy(content.password.ifEmpty { content.ssid })
                        }
                        ActionChip(Icons.Rounded.ContentCopy, stringResource(R.string.result_copy)) {
                            copy(content.password)
                        }
                    }
                    is QrContent.Phone -> {
                        ActionChip(Icons.Rounded.Call, stringResource(R.string.result_dial)) {
                            launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + content.number)))
                        }
                    }
                    is QrContent.Sms -> {
                        ActionChip(Icons.Rounded.Sms, stringResource(R.string.result_sms)) {
                            launch(
                                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + content.phone)).apply {
                                    putExtra("sms_body", content.message)
                                },
                            )
                        }
                    }
                    is QrContent.Geo -> {
                        ActionChip(Icons.Rounded.Map, stringResource(R.string.result_map)) {
                            launch(
                                Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + content.lat + "," + content.lng)),
                            )
                        }
                    }
                    is QrContent.Contact -> {
                        ActionChip(Icons.Rounded.PersonAdd, stringResource(R.string.result_to_contacts)) {
                            launch(
                                Intent(ContactsContract.Intents.Insert.ACTION).apply {
                                    type = ContactsContract.RawContacts.CONTENT_TYPE
                                    putExtra(
                                        ContactsContract.Intents.Insert.NAME,
                                        listOf(content.firstName, content.lastName)
                                            .filter { it.isNotBlank() }
                                            .joinToString(" "),
                                    )
                                    putExtra(ContactsContract.Intents.Insert.PHONE, content.phone)
                                    putExtra(ContactsContract.Intents.Insert.EMAIL, content.email)
                                    putExtra(ContactsContract.Intents.Insert.COMPANY, content.org)
                                },
                            )
                        }
                    }
                    is QrContent.Text -> Unit
                }
                // Общая «Копировать» — только если нет специфичной для типа
                if (result.content !is QrContent.Url && result.content !is QrContent.Wifi) {
                    ActionChip(Icons.Rounded.ContentCopy, stringResource(R.string.result_copy)) {
                        copy(result.raw)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            FilledTonalButton(
                onClick = onCreateQr,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.result_make_qr))
            }
        }
    }
}

@Composable
private fun ResultFields(content: QrContent) {
    val line = when (content) {
        is QrContent.Url -> content.url
        is QrContent.Wifi -> buildString {
            append(content.ssid)
            if (content.encryption != QrContent.WifiEncryption.NOPASS && content.password.isNotEmpty()) {
                append("\n")
                append(content.password)
            }
            append("\n").append(content.encryption.label)
        }
        is QrContent.Text -> content.text
        is QrContent.Contact -> buildString {
            append(listOf(content.firstName, content.lastName).filter { it.isNotBlank() }.joinToString(" "))
            if (content.org.isNotBlank()) append("\n").append(content.org)
            if (content.phone.isNotBlank()) append("\n").append(content.phone)
            if (content.email.isNotBlank()) append("\n").append(content.email)
        }
        is QrContent.Sms -> content.phone + "\n" + content.message
        is QrContent.Phone -> content.number
        is QrContent.Geo -> content.lat + ", " + content.lng + if (content.label.isNotBlank()) " · " + content.label else ""
    }
    Text(
        line,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ActionChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label)
    }
}

private fun QrContentType.icon(): ImageVector = when (this) {
    QrContentType.URL -> Icons.Rounded.Link
    QrContentType.WIFI -> Icons.Rounded.Wifi
    QrContentType.TEXT -> Icons.Rounded.CenterFocusStrong
    QrContentType.CONTACT -> Icons.Rounded.PersonAdd
    QrContentType.SMS -> Icons.Rounded.Sms
    QrContentType.PHONE -> Icons.Rounded.Call
    QrContentType.GEO -> Icons.Rounded.LocationOn
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

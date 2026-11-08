package com.kizqr.app.ui.create

import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kizqr.app.R
import com.kizqr.app.qr.DotStyle
import com.kizqr.app.qr.EyeStyle
import com.kizqr.app.qr.LogoPreset
import com.kizqr.app.qr.QrColorMode
import com.kizqr.app.qr.QrErrorCorrection
import com.kizqr.app.qr.QrPresets
import com.kizqr.app.qr.QrRenderer
import com.kizqr.app.ui.components.BrandIconCanvas

/** Палитра цветов точек. */
private val dotPalette = listOf(
    0xFF17171F.toInt(), 0xFF5B8AF5.toInt(), 0xFF4B52D8.toInt(),
    0xFF00B8A9.toInt(), 0xFF22C55E.toInt(), 0xFF25D366.toInt(),
    0xFF229ED9.toInt(), 0xFFEF4444.toInt(), 0xFFF97316.toInt(),
    0xFFEC4899.toInt(), 0xFF8B5CF6.toInt(), 0xFFF59E0B.toInt(),
)

/** Палитра фонов. */
private val bgPalette = listOf(
    0xFFFFFFFF.toInt(), 0xFFFAF3E8.toInt(), 0xFFE9F1FB.toInt(),
    0xFFEFF3F6.toInt(), 0xFF1B1B22.toInt(), 0xFF191414.toInt(),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleEditorSheet(
    viewModel: CreateViewModel,
    onDismiss: () -> Unit,
    onPickPhoto: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(stringResource(R.string.style_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            SectionTitle(stringResource(R.string.style_presets))
            PresetsRow(
                applied = viewModel.appliedPreset,
                onSelect = viewModel::selectPreset,
            )
            Spacer(Modifier.height(16.dp))

            SectionTitle(stringResource(R.string.style_dots))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (dot in DotStyle.entries) {
                    DotStylePreview(
                        style = dot,
                        color = viewModel.style.color1,
                        selected = viewModel.style.dotStyle == dot,
                        onSelect = {
                            viewModel.updateStyle(viewModel.style.copy(dotStyle = dot))
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            SectionTitle(stringResource(R.string.style_eyes))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (eye in EyeStyle.entries) {
                    EyeStylePreview(
                        style = eye,
                        color = viewModel.style.eyeColor ?: viewModel.style.color1,
                        selected = viewModel.style.eyeStyle == eye,
                        onSelect = {
                            viewModel.updateStyle(viewModel.style.copy(eyeStyle = eye))
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            SectionTitle(stringResource(R.string.style_gradient))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (mode in QrColorMode.entries) {
                    FilterChip(
                        selected = viewModel.style.colorMode == mode,
                        onClick = { viewModel.updateStyle(viewModel.style.copy(colorMode = mode)) },
                        label = {
                            Text(
                                when (mode) {
                                    QrColorMode.SOLID -> "Однотонный"
                                    QrColorMode.LINEAR -> "Линейный"
                                    QrColorMode.RADIAL -> "Радиальный"
                                },
                            )
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            SectionSubtitle(stringResource(R.string.style_color))
            SwatchRow(
                colors = dotPalette,
                selected = viewModel.style.color1,
                onPick = { viewModel.updateStyle(viewModel.style.copy(color1 = it)) },
            )
            if (viewModel.style.colorMode != QrColorMode.SOLID) {
                Spacer(Modifier.height(8.dp))
                SectionSubtitle("Второй цвет")
                SwatchRow(
                    colors = dotPalette,
                    selected = viewModel.style.color2,
                    onPick = { viewModel.updateStyle(viewModel.style.copy(color2 = it)) },
                )
            }
            Spacer(Modifier.height(8.dp))

            SectionSubtitle(stringResource(R.string.style_background))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SwatchRow(
                    colors = bgPalette,
                    selected = viewModel.style.bgColor,
                    onPick = {
                        viewModel.updateStyle(
                            viewModel.style.copy(bgColor = it, transparentBg = false),
                        )
                    },
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        stringResource(R.string.style_transparent_bg),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Switch(
                        checked = viewModel.style.transparentBg,
                        onCheckedChange = {
                            viewModel.updateStyle(viewModel.style.copy(transparentBg = it))
                        },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            SectionTitle(stringResource(R.string.style_logo))
            LogoRow(
                style = viewModel.style,
                logoBitmap = viewModel.logoBitmap,
                onSelectPreset = viewModel::selectLogoPreset,
                onClear = viewModel::clearLogo,
                onPickPhoto = onPickPhoto,
            )
            Spacer(Modifier.height(16.dp))

            SectionTitle(stringResource(R.string.style_error_correction))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (ec in QrErrorCorrection.entries) {
                    FilterChip(
                        selected = viewModel.style.errorCorrection == ec,
                        onClick = { viewModel.updateStyle(viewModel.style.copy(errorCorrection = ec)) },
                        label = { Text(ec.label) },
                    )
                }
            }
            Text(
                text = if (viewModel.style.hasLogo) {
                    "При логотипе автоматически используется уровень H (30%)"
                } else {
                    stringResource(R.string.ec_note)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.style_quiet_zone), modifier = Modifier.weight(1f))
                Switch(
                    checked = viewModel.style.quietZone,
                    onCheckedChange = { viewModel.updateStyle(viewModel.style.copy(quietZone = it)) },
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SectionSubtitle(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun PresetsRow(applied: LogoPreset, onSelect: (LogoPreset) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            PresetChip(
                logo = LogoPreset.NONE,
                label = stringResource(R.string.preset_none),
                tint = MaterialTheme.colorScheme.primary.toArgb(),
                selected = applied == LogoPreset.NONE,
                onSelect = onSelect,
            )
        }
        for (preset in QrPresets.all) {
            item {
                PresetChip(
                    logo = preset.id,
                    label = preset.name,
                    tint = preset.style.color1,
                    selected = applied == preset.id,
                    onSelect = onSelect,
                )
            }
        }
    }
}

@Composable
private fun PresetChip(
    logo: LogoPreset,
    label: String,
    tint: Int,
    selected: Boolean,
    onSelect: (LogoPreset) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable { onSelect(logo) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        if (logo == LogoPreset.NONE) {
            Icon(
                Icons.Rounded.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
        } else {
            BrandIconCanvas(
                logo = logo,
                tint = tint,
                knockout = 0xFFFFFFFF.toInt(),
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

/** Мини-превью формы точек: 2×2 модуля. */
@Composable
private fun DotStylePreview(
    style: DotStyle,
    color: Int,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onSelect),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(30.dp)) {
            drawIntoCanvas { c ->
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
                val native: AndroidCanvas = c.nativeCanvas
                val m = size.width * 0.42f
                val gap = size.width * 0.08f
                for (row in 0..1) for (col in 0..1) {
                    QrRenderer.drawModuleShape(
                        native,
                        col * (m + gap),
                        row * (m + gap),
                        m,
                        style,
                        paint,
                    )
                }
            }
        }
    }
}

/** Мини-превью формы «глаза». */
@Composable
private fun EyeStylePreview(
    style: EyeStyle,
    color: Int,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onSelect),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(34.dp)) {
            drawIntoCanvas { c ->
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
                QrRenderer.drawEye(
                    c.nativeCanvas,
                    0f,
                    0f,
                    size.width / 7f,
                    style,
                    paint,
                )
            }
        }
    }
}

@Composable
private fun SwatchRow(
    colors: List<Int>,
    selected: Int,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        for (color in colors) {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (selected == color) 3.dp else 1.dp,
                        color = if (selected == color) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = CircleShape,
                    )
                    .clickable { onPick(color) },
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawIntoCanvas { c ->
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
                        c.nativeCanvas.drawCircle(size.width / 2f, size.height / 2f, size.width / 2f, paint)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogoRow(
    style: com.kizqr.app.qr.QrStyle,
    logoBitmap: android.graphics.Bitmap?,
    onSelectPreset: (LogoPreset) -> Unit,
    onClear: () -> Unit,
    onPickPhoto: () -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            LogoChip(
                label = stringResource(R.string.style_logo_none),
                selected = style.logo == LogoPreset.NONE && style.logoUri == null,
                onClick = onClear,
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        for (preset in QrPresets.all) {
            item {
                LogoChip(
                    label = preset.name,
                    selected = style.logo == preset.id && style.logoUri == null,
                    onClick = { onSelectPreset(preset.id) },
                ) {
                    BrandIconCanvas(
                        logo = preset.id,
                        tint = preset.style.color1,
                        knockout = 0xFFFFFFFF.toInt(),
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
        item {
            LogoChip(
                label = stringResource(R.string.style_logo_photo),
                selected = style.logoUri != null,
                onClick = onPickPhoto,
            ) {
                if (logoBitmap != null && style.logoUri != null) {
                    Image(
                        bitmap = logoBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)),
                    )
                } else {
                    Icon(
                        Icons.Rounded.Image,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun LogoChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        content()
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

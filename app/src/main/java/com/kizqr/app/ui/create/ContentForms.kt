package com.kizqr.app.ui.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.kizqr.app.R
import com.kizqr.app.qr.QrContent

@Composable
private fun FieldSpacer() = Spacer(Modifier.height(8.dp))

@Composable
fun TextForm(vm: CreateViewModel) {
    OutlinedTextField(
        value = vm.text,
        onValueChange = vm::onTextChange,
        label = { Text(stringResource(R.string.hint_text)) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun UrlForm(vm: CreateViewModel) {
    OutlinedTextField(
        value = vm.url,
        onValueChange = vm::onUrlChange,
        label = { Text(stringResource(R.string.hint_url)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun WifiForm(vm: CreateViewModel) {
    OutlinedTextField(
        value = vm.wifiSsid,
        onValueChange = { vm.onWifiChange(ssid = it) },
        label = { Text(stringResource(R.string.hint_wifi_ssid)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    FieldSpacer()
    if (vm.wifiEncryption != QrContent.WifiEncryption.NOPASS) {
        OutlinedTextField(
            value = vm.wifiPassword,
            onValueChange = { vm.onWifiChange(password = it) },
            label = { Text(stringResource(R.string.hint_wifi_password)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        FieldSpacer()
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (enc in QrContent.WifiEncryption.entries) {
            FilterChip(
                selected = vm.wifiEncryption == enc,
                onClick = { vm.onWifiEncryptionChange(enc) },
                label = { Text(enc.label) },
                modifier = Modifier.weight(1f),
            )
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.wifi_hidden), modifier = Modifier.weight(1f))
        Switch(checked = vm.wifiHidden, onCheckedChange = vm::onWifiHiddenChange)
    }
}

@Composable
fun ContactForm(vm: CreateViewModel) {
    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = vm.contactFirst,
            onValueChange = { vm.onContactChange(first = it) },
            label = { Text(stringResource(R.string.hint_first_name)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = vm.contactLast,
            onValueChange = { vm.onContactChange(last = it) },
            label = { Text(stringResource(R.string.hint_last_name)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
    }
    FieldSpacer()
    OutlinedTextField(
        value = vm.contactOrg,
        onValueChange = { vm.onContactChange(org = it) },
        label = { Text(stringResource(R.string.hint_org)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    FieldSpacer()
    OutlinedTextField(
        value = vm.contactPhone,
        onValueChange = { vm.onContactChange(phone = it) },
        label = { Text(stringResource(R.string.hint_phone)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    FieldSpacer()
    OutlinedTextField(
        value = vm.contactEmail,
        onValueChange = { vm.onContactChange(email = it) },
        label = { Text(stringResource(R.string.hint_email)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    FieldSpacer()
    OutlinedTextField(
        value = vm.contactWebsite,
        onValueChange = { vm.onContactChange(website = it) },
        label = { Text(stringResource(R.string.hint_website)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    FieldSpacer()
    OutlinedTextField(
        value = vm.contactNote,
        onValueChange = { vm.onContactChange(note = it) },
        label = { Text(stringResource(R.string.hint_note)) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun SmsForm(vm: CreateViewModel) {
    OutlinedTextField(
        value = vm.smsPhone,
        onValueChange = { vm.onSmsChange(phone = it) },
        label = { Text(stringResource(R.string.hint_sms_to)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    FieldSpacer()
    OutlinedTextField(
        value = vm.smsMessage,
        onValueChange = { vm.onSmsChange(message = it) },
        label = { Text(stringResource(R.string.hint_sms_text)) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun PhoneForm(vm: CreateViewModel) {
    OutlinedTextField(
        value = vm.phoneNumber,
        onValueChange = vm::onPhoneChange,
        label = { Text(stringResource(R.string.hint_phone_number)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun GeoForm(vm: CreateViewModel) {
    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = vm.geoLat,
            onValueChange = { vm.onGeoChange(lat = it) },
            label = { Text(stringResource(R.string.hint_lat)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = vm.geoLng,
            onValueChange = { vm.onGeoChange(lng = it) },
            label = { Text(stringResource(R.string.hint_lng)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
    }
}

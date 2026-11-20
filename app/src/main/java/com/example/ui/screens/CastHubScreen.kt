package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CastDevice
import com.example.data.model.CastPlaybackState
import com.example.ui.components.LiquidGlassActionButton
import com.example.ui.components.LiquidGlassBadge
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.GlassTextMuted
import com.example.ui.theme.GlassTextPrimary
import com.example.ui.theme.GlassTextSecondary
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.theme.StatusLiveEmerald
import com.example.viewmodel.DevicesUiState

@Composable
fun CastHubScreen(
    devicesState: DevicesUiState,
    onScanDevices: () -> Unit,
    onSelectDevice: (CastDevice) -> Unit,
    onPickGalleryVideo: () -> Unit,
    onPickLocalFile: () -> Unit,
    onCastDirectUrl: (url: String, title: String) -> Unit,
    onSaveStream: (title: String, url: String) -> Unit,
    onImportM3u: (String) -> Int,
    onTogglePlayPause: () -> Unit,
    onStopCast: () -> Unit,
    onVolumeChange: (Int) -> Unit,
    contentBottomPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    var directUrl by remember { mutableStateOf("") }
    var directTitle by remember { mutableStateOf("") }
    var showM3uImporter by remember { mutableStateOf(false) }
    var m3uInput by remember { mutableStateOf("") }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = palette.primaryAccent,
        unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
        focusedContainerColor = Color.Black.copy(alpha = 0.30f),
        unfocusedContainerColor = Color.Black.copy(alpha = 0.24f),
        focusedLabelColor = palette.primaryAccent,
        unfocusedLabelColor = GlassTextSecondary
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("cast_hub_list"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = contentBottomPadding.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "الأجهزة والوسائط",
                        style = MaterialTheme.typography.headlineMedium,
                        color = GlassTextPrimary
                    )
                    Text(
                        text = "خادم البث المحلي: ${devicesState.receiverUrl}",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                LiquidGlassBadge(
                    text = if (devicesState.isWifiConnected) "LAN نشط" else "غير متصل",
                    accentColor = if (devicesState.isWifiConnected) StatusLiveEmerald else palette.primaryAccent,
                    showPulseDot = devicesState.isWifiConnected
                )
            }
        }

        // Status Banner
        if (!devicesState.statusBanner.isNullOrBlank()) {
            item {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 18.dp,
                    highlightAccent = true,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Text(
                        text = devicesState.statusBanner,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
        }

        // Active TV Remote Control Card
        devicesState.activeSession?.let { session ->
            item {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    highlightAccent = true
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "جهاز التحكم بالبث النشط",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.secondaryAccent
                                )
                                Text(
                                    text = session.mediaTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = session.targetDevice.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GlassTextSecondary
                                )
                            }
                            LiquidGlassBadge(
                                text = session.formatTag,
                                accentColor = StatusLiveEmerald,
                                showPulseDot = true
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!session.isMirrorMode && !session.targetDevice.isWebReceiver) {
                                LiquidGlassActionButton(
                                    text = if (session.playbackState == CastPlaybackState.PLAYING) "إيقاف مؤقت" else "تشغيل",
                                    icon = if (session.playbackState == CastPlaybackState.PLAYING) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    onClick = onTogglePlayPause,
                                    testTag = "remote_play_pause_button",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            LiquidGlassActionButton(
                                text = "إنهاء البث",
                                icon = Icons.Rounded.Stop,
                                isPrimary = false,
                                isDanger = true,
                                onClick = onStopCast,
                                testTag = "remote_stop_button",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (!session.targetDevice.isWebReceiver) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                    contentDescription = "مستوى الصوت",
                                    tint = GlassTextPrimary
                                )
                                Slider(
                                    value = session.volume.toFloat(),
                                    onValueChange = { onVolumeChange(it.toInt()) },
                                    valueRange = 0f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = palette.secondaryAccent,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.16f)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${session.volume}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // SSDP Smart TV Scanner & Devices List
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "أجهزة التلفاز والمستقبلات",
                                style = MaterialTheme.typography.titleMedium,
                                color = GlassTextPrimary
                            )
                            Text(
                                text = "يدعم DLNA / UPnP ومستقبل الويب على نفس الشبكة",
                                style = MaterialTheme.typography.bodySmall,
                                color = GlassTextMuted
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(palette.secondaryAccent.copy(alpha = 0.22f))
                                .border(1.dp, palette.secondaryAccent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .testTag("scan_tv_devices_button")
                                .clickable(enabled = !devicesState.isScanning, onClick = onScanDevices)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (devicesState.isScanning) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Radar,
                                        contentDescription = "البحث عن التلفاز",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "فحص الشبكة",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    devicesState.devices.forEach { device ->
                        val isSelected = devicesState.selectedDevice?.id == device.id
                        DeviceGlassRow(
                            device = device,
                            selected = isSelected,
                            onClick = { onSelectDevice(device) }
                        )
                    }
                }
            }
        }

        // Local Phone Media Casting (Zero-Permission Photo Picker & SAF)
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "بث الوسائط من ذاكرة الهاتف",
                        style = MaterialTheme.typography.titleMedium,
                        color = GlassTextPrimary
                    )
                    Text(
                        text = "اختر فيديو أو ملفاً صوتياً لبثه مباشرة عبر الخادم المحلي بدون أذونات تخزين شاملة",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LiquidGlassActionButton(
                            text = "فيديو المعرض",
                            icon = Icons.Rounded.VideoLibrary,
                            onClick = onPickGalleryVideo,
                            testTag = "pick_local_video_button",
                            modifier = Modifier.weight(1f)
                        )
                        LiquidGlassActionButton(
                            text = "ملف فيديو/صوت",
                            icon = Icons.Rounded.FolderOpen,
                            isPrimary = false,
                            onClick = onPickLocalFile,
                            testTag = "pick_local_file_button",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Direct Stream URL & IPTV M3U Importer
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Link,
                                contentDescription = null,
                                tint = palette.primaryAccent
                            )
                            Text(
                                text = "بث رابط مباشر أو IPTV",
                                style = MaterialTheme.typography.titleMedium,
                                color = GlassTextPrimary
                            )
                        }
                        Text(
                            text = if (showM3uImporter) "إغلاق M3U" else "استيراد M3U",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.secondaryAccent,
                            modifier = Modifier
                                .testTag("toggle_m3u_importer")
                                .clickable { showM3uImporter = !showM3uImporter }
                                .padding(6.dp)
                        )
                    }

                    OutlinedTextField(
                        value = directTitle,
                        onValueChange = { directTitle = it },
                        label = { Text("اسم الفيديو أو القناة (اختياري)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("direct_title_input")
                    )

                    OutlinedTextField(
                        value = directUrl,
                        onValueChange = { directUrl = it },
                        label = { Text("رابط البث (.m3u8 / .mp4 / .mpd)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("direct_url_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LiquidGlassActionButton(
                            text = "بث الرابط الآن",
                            icon = Icons.Rounded.Cast,
                            enabled = directUrl.isNotBlank(),
                            onClick = { onCastDirectUrl(directUrl, directTitle) },
                            testTag = "cast_direct_url_button",
                            modifier = Modifier.weight(1f)
                        )
                        LiquidGlassActionButton(
                            text = "حفظ",
                            icon = Icons.Rounded.BookmarkAdd,
                            isPrimary = false,
                            enabled = directUrl.isNotBlank(),
                            onClick = {
                                onSaveStream(directTitle.ifBlank { "رابط بث مباشر" }, directUrl)
                            },
                            testTag = "save_direct_url_button",
                            modifier = Modifier.width(110.dp)
                        )
                    }

                    AnimatedVisibility(visible = showM3uImporter) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = m3uInput,
                                onValueChange = { m3uInput = it },
                                label = { Text("الصق محتوى قائمة M3U هنا (#EXTINF...)") },
                                minLines = 3,
                                maxLines = 5,
                                shape = RoundedCornerShape(16.dp),
                                colors = textFieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("m3u_playlist_input")
                            )
                            LiquidGlassActionButton(
                                text = "استيراد القنوات إلى المكتبة",
                                icon = Icons.Rounded.PlaylistAdd,
                                enabled = m3uInput.isNotBlank(),
                                onClick = {
                                    val count = onImportM3u(m3uInput)
                                    if (count > 0) {
                                        m3uInput = ""
                                        showM3uImporter = false
                                    }
                                },
                                testTag = "import_m3u_button",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceGlassRow(
    device: CastDevice,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalGlassPalette.current
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) {
                    Brush.horizontalGradient(
                        listOf(
                            palette.primaryAccent.copy(alpha = 0.24f),
                            palette.secondaryAccent.copy(alpha = 0.20f)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.03f))
                    )
                }
            )
            .border(
                width = 1.dp,
                color = if (selected) palette.primaryAccent.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.12f),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (device.isWebReceiver) palette.secondaryAccent.copy(alpha = 0.26f)
                    else palette.primaryAccent.copy(alpha = 0.26f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (device.isWebReceiver) Icons.Rounded.Language else Icons.Rounded.Tv,
                contentDescription = device.name,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${device.manufacturer} • ${device.modelName}",
                style = MaterialTheme.typography.bodySmall,
                color = GlassTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "محدد",
                tint = StatusLiveEmerald,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

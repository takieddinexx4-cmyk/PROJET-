package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.ScreenShare
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.MirrorConfig
import com.example.data.model.MirrorQualityPreset
import com.example.service.ScreenMirrorLiveState
import com.example.ui.components.LiquidGlassActionButton
import com.example.ui.components.LiquidGlassBadge
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassSelectablePill
import com.example.ui.theme.GlassTextMuted
import com.example.ui.theme.GlassTextPrimary
import com.example.ui.theme.GlassTextSecondary
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.theme.StatusLiveEmerald
import com.example.ui.theme.StatusWarningAmber
import java.util.Locale

@Composable
fun ScreenCastScreen(
    mirrorConfig: MirrorConfig,
    liveState: ScreenMirrorLiveState,
    receiverUrl: String,
    isWifiConnected: Boolean,
    selectedDeviceName: String,
    isDlnaSelected: Boolean,
    onSelectPreset: (MirrorQualityPreset) -> Unit,
    onSelectFps: (Int) -> Unit,
    onChangeJpegQuality: (Int) -> Unit,
    onToggleKeepScreenOn: (Boolean) -> Unit,
    onStartMirrorRequest: () -> Unit,
    onStopMirrorRequest: () -> Unit,
    onPushToDlnaTv: () -> Unit,
    onOpenDevicesTab: () -> Unit,
    contentBottomPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalGlassPalette.current
    var copiedNotice by remember { mutableStateOf(false) }

    val statusColor by animateColorAsState(
        targetValue = if (liveState.isMirroring) StatusLiveEmerald else palette.secondaryAccent,
        label = "mirror_status_color"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_cast_list"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = contentBottomPadding.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DZ Cast",
                        style = MaterialTheme.typography.headlineMedium,
                        color = GlassTextPrimary
                    )
                    Text(
                        text = "أول مشروع جزائري لبث الشاشة والويب • بدون إعلانات",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTextSecondary
                    )
                }

                LiquidGlassBadge(
                    text = if (liveState.isMirroring) "مباشر LIVE" else "جاهز",
                    accentColor = statusColor,
                    showPulseDot = liveState.isMirroring
                )
            }
        }

        // Hero Liquid Glass Studio Card with generated artwork
        item {
            val cardShape = RoundedCornerShape(28.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.32f),
                                palette.primaryAccent.copy(alpha = 0.35f),
                                palette.secondaryAccent.copy(alpha = 0.32f)
                            )
                        ),
                        shape = cardShape
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_cast_hero_1795183132852),
                    contentDescription = "خلفية استوديو بث الشاشة الزجاجي",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF06070B).copy(alpha = 0.42f),
                                    Color(0xFF06070B).copy(alpha = 0.88f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            palette.primaryAccent.copy(alpha = 0.50f),
                                            palette.secondaryAccent.copy(alpha = 0.50f)
                                        )
                                    )
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ScreenShare,
                                contentDescription = "بث شاشة الهاتف",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "استوديو بث شاشة الهاتف",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = if (liveState.isMirroring) {
                                    "يتم بث شاشتك الآن بدقة ${liveState.activeConfig.preset.label}"
                                } else {
                                    "اعرض شاشة هاتفك على أي تلفاز ذكي أو متصفح بجودة عالية"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = GlassTextSecondary
                            )
                        }
                    }

                    // Live Telemetry Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TelemetryChip(
                            icon = Icons.Rounded.HighQuality,
                            label = mirrorConfig.preset.label,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryChip(
                            icon = Icons.Rounded.Speed,
                            label = if (liveState.isMirroring) "${liveState.measuredFps} FPS" else "${mirrorConfig.targetFps} FPS",
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryChip(
                            icon = Icons.Rounded.People,
                            label = "${liveState.activeViewers} متصل",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!liveState.errorMessage.isNullOrBlank()) {
                        Text(
                            text = liveState.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF8A9B)
                        )
                    }

                    // Primary Start / Stop CTA
                    if (liveState.isMirroring) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LiquidGlassActionButton(
                                text = "إيقاف بث الشاشة",
                                icon = Icons.Rounded.StopCircle,
                                onClick = onStopMirrorRequest,
                                isPrimary = false,
                                isDanger = true,
                                testTag = "stop_mirror_button",
                                modifier = Modifier.weight(1f)
                            )
                            if (isDlnaSelected) {
                                LiquidGlassActionButton(
                                    text = "إرسال للتلفاز",
                                    icon = Icons.Rounded.Tv,
                                    onClick = onPushToDlnaTv,
                                    isPrimary = true,
                                    testTag = "push_mirror_dlna_button",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        LiquidGlassActionButton(
                            text = "بدء بث شاشة الهاتف الآن",
                            icon = Icons.Rounded.ScreenShare,
                            onClick = onStartMirrorRequest,
                            isPrimary = true,
                            testTag = "start_mirror_button",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Receiver LAN URL Card
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                highlightAccent = liveState.isMirroring
            ) {
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
                                imageVector = Icons.Rounded.Wifi,
                                contentDescription = "الشبكة المحلية",
                                tint = if (isWifiConnected) StatusLiveEmerald else StatusWarningAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "رابط المستقبل على التلفاز",
                                style = MaterialTheme.typography.titleMedium,
                                color = GlassTextPrimary
                            )
                        }
                        Text(
                            text = if (isWifiConnected) "Wi-Fi متصل" else "تحقق من الواي فاي",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isWifiConnected) StatusLiveEmerald else StatusWarningAmber
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.40f))
                            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = receiverUrl,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "افتح هذا الرابط في متصفح التلفاز الذكي أو الحاسوب على نفس شبكة الواي فاي",
                                style = MaterialTheme.typography.bodySmall,
                                color = GlassTextSecondary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LiquidGlassActionButton(
                            text = if (copiedNotice) "تم النسخ ✓" else "نسخ الرابط",
                            icon = Icons.Rounded.ContentCopy,
                            isPrimary = false,
                            testTag = "copy_receiver_url_button",
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("DZ Cast URL", receiverUrl))
                                copiedNotice = true
                            },
                            modifier = Modifier.weight(1f)
                        )
                        LiquidGlassActionButton(
                            text = "مشاركة",
                            icon = Icons.Rounded.Share,
                            isPrimary = false,
                            testTag = "share_receiver_url_button",
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "شاهد بث DZ Cast المباشر على الرابط:\n$receiverUrl")
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "مشاركة رابط DZ Cast"))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Active target shortcut
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CastConnected,
                                contentDescription = null,
                                tint = palette.secondaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "الهدف: $selectedDeviceName",
                                style = MaterialTheme.typography.bodySmall,
                                color = GlassTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        LiquidGlassSelectablePill(
                            label = "تغيير الجهاز",
                            selected = false,
                            onClick = onOpenDevicesTab,
                            testTag = "switch_device_shortcut"
                        )
                    }
                }
            }
        }

        // Screen Quality & Bitrate Controls Card
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "اختيار جودة البث المناسبة",
                            style = MaterialTheme.typography.titleMedium,
                            color = GlassTextPrimary
                        )
                        LiquidGlassBadge(
                            text = String.format(Locale.US, "~%.1f Mbps", mirrorConfig.estimatedMbps()),
                            accentColor = palette.primaryAccent
                        )
                    }

                    // Resolution presets
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "دقة الشاشة (Resolution)",
                            style = MaterialTheme.typography.labelMedium,
                            color = GlassTextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MirrorQualityPreset.entries.forEach { preset ->
                                LiquidGlassSelectablePill(
                                    label = preset.label,
                                    selected = mirrorConfig.preset == preset,
                                    onClick = { onSelectPreset(preset) },
                                    testTag = "quality_preset_${preset.id}",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Text(
                            text = mirrorConfig.preset.subtitleAr,
                            style = MaterialTheme.typography.bodySmall,
                            color = GlassTextMuted
                        )
                    }

                    // Frame Rate pills
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "معدل الإطارات (FPS)",
                            style = MaterialTheme.typography.labelMedium,
                            color = GlassTextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(20, 30, 60).forEach { fps ->
                                val hint = when (fps) {
                                    20 -> "توفير"
                                    30 -> "سلس"
                                    else -> "ألعاب"
                                }
                                LiquidGlassSelectablePill(
                                    label = "$fps FPS",
                                    sublabel = hint,
                                    selected = mirrorConfig.targetFps == fps,
                                    onClick = { onSelectFps(fps) },
                                    testTag = "quality_fps_$fps",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Compression Quality Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "نقاء الصورة والضغط",
                                style = MaterialTheme.typography.labelMedium,
                                color = GlassTextSecondary
                            )
                            Text(
                                text = "${mirrorConfig.jpegQuality}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.primaryAccent
                            )
                        }
                        Slider(
                            value = mirrorConfig.jpegQuality.toFloat(),
                            onValueChange = { onChangeJpegQuality(it.toInt()) },
                            valueRange = 45f..92f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = palette.primaryAccent,
                                inactiveTrackColor = Color.White.copy(alpha = 0.16f)
                            ),
                            modifier = Modifier.testTag("quality_compression_slider")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "استجابة أسرع",
                                style = MaterialTheme.typography.labelSmall,
                                color = GlassTextMuted
                            )
                            Text(
                                text = "أعلى وضوح",
                                style = MaterialTheme.typography.labelSmall,
                                color = GlassTextMuted
                            )
                        }
                    }

                    // Keep Screen Awake
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "إبقاء الشاشة مضاءة أثناء البث",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GlassTextPrimary
                            )
                            Text(
                                text = "يمنع انطفاء الشاشة تلقائياً أثناء المشاركة",
                                style = MaterialTheme.typography.bodySmall,
                                color = GlassTextMuted
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = mirrorConfig.keepScreenOn,
                            onCheckedChange = onToggleKeepScreenOn,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = palette.primaryAccent
                            ),
                            modifier = Modifier.testTag("keep_screen_on_switch")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

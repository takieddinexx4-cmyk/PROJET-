package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.local.BookmarkEntity
import com.example.data.local.CastHistoryEntity
import com.example.data.local.SavedStreamEntity
import com.example.data.local.UserSettings
import com.example.ui.components.LiquidGlassBadge
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassSelectablePill
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonMuted
import com.example.ui.theme.GlassTextMuted
import com.example.ui.theme.GlassTextPrimary
import com.example.ui.theme.GlassTextSecondary
import com.example.ui.theme.GlassThemeMode
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.theme.SapphireElectric
import com.example.ui.theme.SapphireGlow
import com.example.ui.theme.StatusLiveEmerald

@Composable
fun LibrarySettingsScreen(
    settings: UserSettings,
    bookmarks: List<BookmarkEntity>,
    history: List<CastHistoryEntity>,
    savedStreams: List<SavedStreamEntity>,
    onOpenBookmark: (String) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onCastUrl: (url: String, title: String) -> Unit,
    onDeleteHistory: (Long) -> Unit,
    onClearHistory: () -> Unit,
    onDeleteSavedStream: (Long) -> Unit,
    onSelectThemeMode: (GlassThemeMode) -> Unit,
    onChangeFrostIntensity: (Float) -> Unit,
    onToggleAdBlock: (Boolean) -> Unit,
    onToggleDesktopUa: (Boolean) -> Unit,
    contentBottomPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    var selectedSection by remember { mutableIntStateOf(0) } // 0=المظهر, 1=قنواتي, 2=المواقع, 3=السجل

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("library_settings_list"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = contentBottomPadding.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "المكتبة والإعدادات",
                    style = MaterialTheme.typography.headlineMedium,
                    color = GlassTextPrimary
                )
                Text(
                    text = "تخصيص مظهر الزجاج السائل، القنوات المحفوظة، وسجل البث",
                    style = MaterialTheme.typography.bodySmall,
                    color = GlassTextSecondary
                )
            }
        }

        // Segmented Liquid Glass Tabs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    0 to "المظهر",
                    1 to "قنواتي",
                    2 to "المواقع",
                    3 to "السجل"
                ).forEach { (idx, title) ->
                    LiquidGlassSelectablePill(
                        label = title,
                        selected = selectedSection == idx,
                        onClick = { selectedSection = idx },
                        testTag = "library_segment_$idx",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        when (selectedSection) {
            0 -> {
                // Appearance & Glass Settings
                item {
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Palette,
                                    contentDescription = null,
                                    tint = palette.primaryAccent
                                )
                                Text(
                                    text = "ألوان الزجاج السائل (iOS 27 Glass)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = GlassTextPrimary
                                )
                            }

                            GlassThemeMode.entries.forEach { mode ->
                                val isSelected = settings.themeMode == mode
                                val previewColors = when (mode) {
                                    GlassThemeMode.DUAL_PRISM -> listOf(CrimsonGlow, SapphireElectric)
                                    GlassThemeMode.CRIMSON_OBSIDIAN -> listOf(CrimsonGlow, CrimsonMuted)
                                    GlassThemeMode.SAPPHIRE_OBSIDIAN -> listOf(SapphireElectric, SapphireGlow)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(
                                            if (isSelected) Color.White.copy(alpha = 0.12f)
                                            else Color.White.copy(alpha = 0.04f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) previewColors.first().copy(alpha = 0.75f)
                                            else Color.White.copy(alpha = 0.10f),
                                            RoundedCornerShape(18.dp)
                                        )
                                        .testTag("theme_mode_${mode.name.lowercase()}")
                                        .clickable { onSelectThemeMode(mode) }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Brush.linearGradient(previewColors))
                                            .border(1.5.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mode.labelAr,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White
                                        )
                                        Text(
                                            text = mode.subtitleAr,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = GlassTextSecondary
                                        )
                                    }
                                    if (isSelected) {
                                        LiquidGlassBadge(text = "مفعّل", accentColor = StatusLiveEmerald)
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "كثافة التعتيم الزجاجي",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = GlassTextSecondary
                                    )
                                    Text(
                                        text = "${(settings.frostIntensity * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = palette.secondaryAccent
                                    )
                                }
                                Slider(
                                    value = settings.frostIntensity,
                                    onValueChange = onChangeFrostIntensity,
                                    valueRange = 0.35f..1.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = palette.secondaryAccent,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.16f)
                                    ),
                                    modifier = Modifier.testTag("frost_intensity_slider")
                                )
                            }
                        }
                    }
                }

                // Privacy & Ad-Free Controls
                item {
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shield,
                                    contentDescription = null,
                                    tint = StatusLiveEmerald
                                )
                                Text(
                                    text = "الحماية وتجربة التصفح النظيف",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = GlassTextPrimary
                                )
                            }

                            SettingSwitchRow(
                                title = "مانع إعلانات المتصفح المدمج",
                                subtitle = "حجب شبكات الإعلانات والنوافذ المنبثقة أثناء البحث عن الفيديوهات",
                                checked = settings.adBlockEnabled,
                                onCheckedChange = onToggleAdBlock,
                                testTag = "settings_adblock_switch"
                            )

                            SettingSwitchRow(
                                title = "وضع سطح المكتب افتراضياً",
                                subtitle = "يساعد في إظهار مشغلات الفيديو عالية الجودة في بعض المواقع",
                                checked = settings.desktopUserAgent,
                                onCheckedChange = onToggleDesktopUa,
                                testTag = "settings_desktop_ua_switch"
                            )
                        }
                    }
                }

                // Algerian Project Pride Card
                item {
                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        highlightAccent = true
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Verified,
                                    contentDescription = null,
                                    tint = StatusLiveEmerald
                                )
                                Text(
                                    text = "DZ Cast — أول مشروع جزائري لبث الويب والشاشة",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "تطبيق خاص خالٍ تماماً من الإعلانات أو التتبع، مبني بتصميم زجاجي حديث مع دعم كامل لبث شاشة الهاتف بجودات متعددة (480p / 720p / 1080p) ومهيأ للبناء التلقائي على GitHub Actions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = GlassTextSecondary
                            )
                        }
                    }
                }
            }

            1 -> {
                // Saved Streams / IPTV
                if (savedStreams.isEmpty()) {
                    item {
                        EmptyLibraryCard(
                            icon = Icons.Rounded.LiveTv,
                            title = "لا توجد روابط أو قنوات محفوظة",
                            subtitle = "احفظ أي رابط فيديو من المتصفح أو استورد قائمة M3U من تبويب الأجهزة"
                        )
                    }
                } else {
                    items(savedStreams, key = { it.id }) { stream ->
                        LiquidGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp,
                            contentPadding = PaddingValues(14.dp),
                            onClick = { onCastUrl(stream.streamUrl, stream.title) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LiveTv,
                                    contentDescription = null,
                                    tint = palette.secondaryAccent
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = stream.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        LiquidGlassBadge(
                                            text = stream.formatTag,
                                            accentColor = palette.primaryAccent
                                        )
                                    }
                                    Text(
                                        text = "${stream.category} • ${stream.streamUrl}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GlassTextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { onCastUrl(stream.streamUrl, stream.title) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Cast,
                                        contentDescription = "بث القناة",
                                        tint = Color.White
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteSavedStream(stream.id) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "حذف",
                                        tint = GlassTextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Bookmarks
                if (bookmarks.isEmpty()) {
                    item {
                        EmptyLibraryCard(
                            icon = Icons.Rounded.Bookmark,
                            title = "لا توجد مواقع محفوظة بعد",
                            subtitle = "اضغط على أيقونة الحفظ داخل المتصفح لإضافة مواقعك المفضلة هنا"
                        )
                    }
                } else {
                    items(bookmarks, key = { it.id }) { bm ->
                        LiquidGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp,
                            contentPadding = PaddingValues(14.dp),
                            onClick = { onOpenBookmark(bm.url) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Public,
                                    contentDescription = null,
                                    tint = palette.primaryAccent
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bm.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = bm.url,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GlassTextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteBookmark(bm.id) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "حذف الموقع",
                                        tint = GlassTextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                // Cast History
                if (history.isEmpty()) {
                    item {
                        EmptyLibraryCard(
                            icon = Icons.Rounded.History,
                            title = "سجل البث فارغ",
                            subtitle = "كل فيديو تقوم ببثه إلى التلفاز أو مستقبل الويب سيظهر هنا لإعادة تشغيله بضغطة واحدة"
                        )
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "مسح السجل بالكامل",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.primaryAccent,
                                modifier = Modifier
                                    .clickable(onClick = onClearHistory)
                                    .padding(6.dp)
                            )
                        }
                    }
                    items(history, key = { it.id }) { item ->
                        LiquidGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp,
                            contentPadding = PaddingValues(14.dp),
                            onClick = { onCastUrl(item.mediaUrl, item.title) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = null,
                                    tint = palette.secondaryAccent
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.formatTag} • ${item.targetDeviceName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GlassTextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteHistory(item.id) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "حذف من السجل",
                                        tint = GlassTextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val palette = LocalGlassPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = GlassTextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = GlassTextMuted
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = palette.primaryAccent
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun EmptyLibraryCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GlassTextMuted,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = GlassTextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = GlassTextSecondary
            )
        }
    }
}

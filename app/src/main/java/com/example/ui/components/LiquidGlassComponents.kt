package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.ScreenShare
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ScreenShare
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.ActiveCastSession
import com.example.data.model.AppTab
import com.example.data.model.CastPlaybackState
import com.example.ui.theme.GlassTextMuted
import com.example.ui.theme.GlassTextPrimary
import com.example.ui.theme.GlassTextSecondary
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.StatusLiveEmerald

@Composable
fun LiquidGlassBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalGlassPalette.current
    val infiniteTransition = rememberInfiniteTransition(label = "glass_ambient")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_drift"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDeep)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Top-start muted crimson/primary ambient refraction orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.orbTopStart, Color.Transparent),
                    center = Offset(w * (0.18f + 0.08f * drift), h * (0.14f + 0.04f * drift)),
                    radius = w * 0.78f
                ),
                radius = w * 0.78f,
                center = Offset(w * (0.18f + 0.08f * drift), h * (0.14f + 0.04f * drift))
            )

            // Bottom-end sapphire/secondary refractive orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.orbBottomEnd, Color.Transparent),
                    center = Offset(w * (0.85f - 0.10f * drift), h * (0.78f - 0.05f * drift)),
                    radius = w * 0.85f
                ),
                radius = w * 0.85f,
                center = Offset(w * (0.85f - 0.10f * drift), h * (0.78f - 0.05f * drift))
            )

            // Center subtle specular glass glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.orbCenter, Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.45f),
                    radius = w * 0.65f
                ),
                radius = w * 0.65f,
                center = Offset(w * 0.5f, h * 0.45f)
            )
        }

        content()
    }
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    highlightAccent: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalGlassPalette.current
    val shape: Shape = RoundedCornerShape(cornerRadius)

    val fillBrush = if (highlightAccent) {
        Brush.linearGradient(
            colors = listOf(
                palette.primaryAccent.copy(alpha = 0.22f),
                palette.secondaryAccent.copy(alpha = 0.14f),
                palette.glassFillBottom
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                palette.glassFillTop,
                palette.glassFillBottom
            )
        )
    }

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            palette.glassBorderStart,
            if (highlightAccent) palette.primaryAccent.copy(alpha = 0.45f) else palette.glassBorderMid,
            palette.glassBorderEnd
        )
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(fillBrush)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun LiquidGlassActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = true,
    isDanger: Boolean = false,
    enabled: Boolean = true,
    testTag: String = ""
) {
    val palette = LocalGlassPalette.current
    val shape = RoundedCornerShape(20.dp)

    val gradient = when {
        !enabled -> Brush.horizontalGradient(
            listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.05f))
        )
        isDanger -> Brush.horizontalGradient(
            listOf(Color(0xFFB91C3B), Color(0xFFE11D48))
        )
        isPrimary -> Brush.horizontalGradient(
            listOf(palette.primaryAccent, palette.secondaryAccent)
        )
        else -> Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.13f), Color.White.copy(alpha = 0.05f))
        )
    }

    val border = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = if (isPrimary || isDanger) 0.40f else 0.24f),
            Color.White.copy(alpha = 0.08f)
        )
    )

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .clip(shape)
            .background(gradient)
            .border(1.dp, border, shape)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) Color.White else GlassTextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) Color.White else GlassTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LiquidGlassSelectablePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sublabel: String? = null,
    testTag: String = ""
) {
    val palette = LocalGlassPalette.current
    val shape = RoundedCornerShape(18.dp)
    val borderColor by animateColorAsState(
        targetValue = if (selected) palette.primaryAccent.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.16f),
        label = "pill_border"
    )
    val fillBrush = if (selected) {
        Brush.linearGradient(
            listOf(
                palette.primaryAccent.copy(alpha = 0.34f),
                palette.secondaryAccent.copy(alpha = 0.26f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.03f))
        )
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .background(fillBrush)
            .border(1.dp, borderColor, shape)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Color.White else GlassTextSecondary,
                maxLines = 1
            )
            if (!sublabel.isNullOrBlank()) {
                Text(
                    text = sublabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) Color.White.copy(alpha = 0.82f) else GlassTextMuted,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun LiquidGlassBadge(
    text: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    showPulseDot: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = accentColor.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (showPulseDot) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = GlassTextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MiniCastFloatingBar(
    session: ActiveCastSession,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    onOpenControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        cornerRadius = 22.dp,
        highlightAccent = true,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        onClick = onOpenControls
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(palette.primaryAccent.copy(alpha = 0.45f), palette.secondaryAccent.copy(alpha = 0.45f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (session.isMirrorMode) Icons.Rounded.ScreenShare else Icons.Rounded.CastConnected,
                    contentDescription = "جلسة بث نشطة",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(StatusLiveEmerald)
                    )
                    Text(
                        text = session.mediaTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = GlassTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = session.targetDevice.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = GlassTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!session.isMirrorMode && !session.targetDevice.isWebReceiver) {
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("mini_cast_play_pause")
                ) {
                    Icon(
                        imageVector = if (session.playbackState == CastPlaybackState.PLAYING) {
                            Icons.Rounded.Pause
                        } else {
                            Icons.Rounded.PlayArrow
                        },
                        contentDescription = "تشغيل أو إيقاف مؤقت",
                        tint = Color.White
                    )
                }
            }

            IconButton(
                onClick = onStop,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("mini_cast_stop")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Stop,
                    contentDescription = "إيقاف البث",
                    tint = Color(0xFFFF6B81)
                )
            }
        }
    }
}

@Composable
fun LiquidGlassFloatingDock(
    selectedTab: AppTab,
    detectedVideoCount: Int,
    isMirrorLive: Boolean,
    onSelectTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    val dockShape = RoundedCornerShape(30.dp)

    val items = listOf(
        Triple(AppTab.BROWSER, "المتصفح", Icons.Rounded.Language to Icons.Outlined.Language),
        Triple(AppTab.MIRROR, "بث الشاشة", Icons.Rounded.ScreenShare to Icons.Outlined.ScreenShare),
        Triple(AppTab.DEVICES, "الأجهزة", Icons.Rounded.CastConnected to Icons.Outlined.Cast),
        Triple(AppTab.LIBRARY, "المكتبة", Icons.Rounded.Bookmarks to Icons.Outlined.Bookmarks)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(dockShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1A1E2E).copy(alpha = 0.86f),
                            Color(0xFF0A0C14).copy(alpha = 0.94f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.28f),
                            palette.secondaryAccent.copy(alpha = 0.26f),
                            palette.primaryAccent.copy(alpha = 0.28f)
                        )
                    ),
                    shape = dockShape
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (tab, label, icons) ->
                val isSelected = selectedTab == tab
                val pillPadding by animateDpAsState(
                    targetValue = if (isSelected) 12.dp else 8.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "dock_padding"
                )
                val pillShape = RoundedCornerShape(22.dp)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 52.dp)
                        .clip(pillShape)
                        .background(
                            if (isSelected) {
                                Brush.linearGradient(
                                    listOf(
                                        palette.primaryAccent.copy(alpha = 0.34f),
                                        palette.secondaryAccent.copy(alpha = 0.28f)
                                    )
                                )
                            } else {
                                Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            }
                        )
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.22f),
                                    pillShape
                                )
                            } else {
                                Modifier
                            }
                        )
                        .testTag("nav_tab_${tab.route}")
                        .clickable { onSelectTab(tab) }
                        .padding(horizontal = pillPadding, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box {
                            Icon(
                                imageVector = if (isSelected) icons.first else icons.second,
                                contentDescription = label,
                                tint = if (isSelected) Color.White else GlassTextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                            if (tab == AppTab.BROWSER && detectedVideoCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(palette.primaryAccent)
                                )
                            }
                            if (tab == AppTab.MIRROR && isMirrorLive) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(StatusLiveEmerald)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color.White else GlassTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

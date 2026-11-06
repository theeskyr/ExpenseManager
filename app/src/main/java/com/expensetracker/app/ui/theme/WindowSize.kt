package com.expensetracker.app.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowWidthType {
    COMPACT, // < 600.dp (phones in portrait)
    MEDIUM,  // 600.dp .. 839.dp (foldables, small tablets, phones in landscape)
    EXPANDED // >= 840.dp (large tablets, desktop / landscape tablets)
}

data class WindowSizeInfo(
    val widthType: WindowWidthType,
    val widthDp: Dp,
    val heightDp: Dp,
    val isLandscape: Boolean
) {
    val isWideScreen: Boolean get() = widthType != WindowWidthType.COMPACT
    val isExpanded: Boolean get() = widthType == WindowWidthType.EXPANDED
    val isMedium: Boolean get() = widthType == WindowWidthType.MEDIUM
    val isCompact: Boolean get() = widthType == WindowWidthType.COMPACT
}

val LocalWindowSizeInfo = staticCompositionLocalOf {
    WindowSizeInfo(
        widthType = WindowWidthType.COMPACT,
        widthDp = 360.dp,
        heightDp = 640.dp,
        isLandscape = false
    )
}

@Composable
fun rememberWindowSizeInfo(): WindowSizeInfo {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp.dp
    val heightDp = configuration.screenHeightDp.dp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val widthType = when {
        widthDp < 600.dp -> WindowWidthType.COMPACT
        widthDp < 840.dp -> WindowWidthType.MEDIUM
        else -> WindowWidthType.EXPANDED
    }
    return remember(widthDp, heightDp, isLandscape) {
        WindowSizeInfo(
            widthType = widthType,
            widthDp = widthDp,
            heightDp = heightDp,
            isLandscape = isLandscape
        )
    }
}

/**
 * Container tối ưu chiều rộng hiển thị cho các màn hình kích thước lớn (tablets/landscape)
 * giúp nội dung không bị kéo dãn quá mức, giữ bố cục cân đối và dễ đọc.
 */
@Composable
fun ResponsiveContentContainer(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 840.dp,
    contentAlignment: Alignment = Alignment.TopCenter,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = contentAlignment
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = maxWidth)
                .fillMaxWidth(),
            content = content
        )
    }
}

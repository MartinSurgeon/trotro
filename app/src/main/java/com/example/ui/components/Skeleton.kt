package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val LocalShimmer = staticCompositionLocalOf<Brush?> { null }

/**
 * Shares one moving highlight across every skeleton block inside it, so the whole placeholder
 * shimmers in sync like in popular apps, instead of each block animating on its own.
 */
@Composable
fun ShimmerHost(content: @Composable () -> Unit) {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerLowest
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1600f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX"
    )
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(x, 0f),
        end = Offset(x + 600f, 300f)
    )
    CompositionLocalProvider(LocalShimmer provides brush) {
        Box(modifier = Modifier.semantics { contentDescription = "Loading" }) { content() }
    }
}

/** A grey placeholder block that shimmers when inside a [ShimmerHost]. */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = 14.dp,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    val brush = LocalShimmer.current
    val sized = if (width != null) modifier.size(width, height) else modifier.fillMaxWidth().height(height)
    Box(
        modifier = sized
            .clip(shape)
            .then(
                if (brush != null) Modifier.background(brush)
                else Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
            )
    )
}

/** Placeholder for a list row: circle avatar, two lines of text, and a pill on the right. */
@Composable
fun SkeletonListRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonBlock(width = 44.dp, height = 44.dp, shape = CircleShape)
        Spacer(Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.6f), height = 14.dp)
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.4f), height = 12.dp)
        }
        Spacer(Modifier.size(12.dp))
        SkeletonBlock(width = 64.dp, height = 24.dp, shape = RoundedCornerShape(50))
    }
}

@Composable
fun SkeletonList(rows: Int = 6, modifier: Modifier = Modifier) {
    ShimmerHost {
        Column(modifier = modifier) {
            repeat(rows) { SkeletonListRow() }
        }
    }
}

/** Placeholder for a message card: date line, three text lines, and a progress bar. */
@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row {
            SkeletonBlock(modifier = Modifier.weight(1f).padding(end = 80.dp), height = 12.dp)
            SkeletonBlock(width = 72.dp, height = 22.dp, shape = RoundedCornerShape(50))
        }
        SkeletonBlock(height = 14.dp)
        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.8f), height = 14.dp)
        SkeletonBlock(height = 6.dp, shape = RoundedCornerShape(50))
    }
}

/** Full-screen placeholder shown while the app's data loads for the first time. */
@Composable
fun AppSkeleton() {
    ShimmerHost {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBlock(width = 52.dp, height = 52.dp, shape = CircleShape)
                Spacer(Modifier.size(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonBlock(width = 110.dp, height = 12.dp)
                    SkeletonBlock(width = 180.dp, height = 20.dp)
                }
            }
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SkeletonBlock(height = 56.dp, shape = RoundedCornerShape(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(4) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SkeletonBlock(width = 52.dp, height = 52.dp, shape = RoundedCornerShape(16.dp))
                            SkeletonBlock(width = 48.dp, height = 10.dp)
                        }
                    }
                }
                SkeletonBlock(height = 88.dp, shape = RoundedCornerShape(20.dp))
                SkeletonCard()
                SkeletonCard()
            }
        }
    }
}

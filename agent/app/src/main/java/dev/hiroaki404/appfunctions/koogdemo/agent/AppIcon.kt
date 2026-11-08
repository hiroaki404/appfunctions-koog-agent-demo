package dev.hiroaki404.appfunctions.koogdemo.agent

import android.content.Context
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The launcher icon of [packageName], read from PackageManager at runtime (the agent knows no tool
 * app at build time). Takes no space without a package or in previews, and stays an empty slot while
 * loading or if the icon can't be read.
 */
@Composable
fun AppIcon(packageName: String?, size: Dp, modifier: Modifier = Modifier) {
    if (packageName == null || LocalInspectionMode.current) return
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val icon by produceState(AppIconCache.peek(packageName, sizePx), packageName, sizePx) {
        if (value == null) value = AppIconCache.load(context, packageName, sizePx)
    }
    // Keep the slot while loading so the label next to it doesn't shift.
    Box(modifier = modifier.size(size).clip(CircleShape)) {
        icon?.let {
            // The app name is always shown next to the icon.
            Image(it, contentDescription = null, modifier = Modifier.size(size))
        }
    }
}

/** Rasterized icons per package and pixel size, so scrolling past cards doesn't decode them again. */
private object AppIconCache {
    private val cache = LruCache<String, ImageBitmap>(32)

    private fun key(packageName: String, sizePx: Int) = "$packageName@$sizePx"

    fun peek(packageName: String, sizePx: Int): ImageBitmap? = cache.get(key(packageName, sizePx))

    suspend fun load(context: Context, packageName: String, sizePx: Int): ImageBitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                // An adaptive icon draws through the device's mask, so it comes out in the launcher's shape.
                context.packageManager.getApplicationIcon(packageName).toBitmap(sizePx, sizePx).asImageBitmap()
            }.getOrNull()?.also { cache.put(key(packageName, sizePx), it) }
        }
}

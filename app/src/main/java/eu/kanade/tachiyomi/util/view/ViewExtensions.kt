@file:Suppress("NOTHING_TO_INLINE")

package eu.kanade.tachiyomi.util.view

import android.content.res.Resources
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.inputmethod.EditorInfoCompat
import app.luma.di.appGraph
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.theme.TachiyomiTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Attaches Compose content to the activity.
 *
 * This deliberately does NOT use androidx's [setContent] extension. That helper
 * starts with:
 *
 *     window.decorView.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
 *
 * On HONOR / Android 17 the strict lookup can return null - the window has a
 * decor view but the content container is not resolvable at that moment - and
 * the chained getChildAt throws:
 *
 *     NullPointerException: ViewGroup.getChildAt() on a null object reference
 *         at ComponentActivityKt.setContent
 *
 * Building the ComposeView ourselves and passing it to setContentView avoids
 * that lookup entirely: AppCompat creates the container if it does not exist,
 * then attaches our view to it.
 */
inline fun ComponentActivity.setComposeContent(
    parent: CompositionContext? = null,
    crossinline overlay: @Composable BoxScope.() -> Unit = {},
    crossinline content: @Composable () -> Unit,
) {
    val composeView = ComposeView(this).apply {
        // Build the composition before attaching, so the first layout already
        // has content and there is no blank frame.
        setParentCompositionContext(parent)
        setContent {
            TachiyomiTheme {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.bodySmall,
                    LocalContentColor provides MaterialTheme.colorScheme.onBackground,
                    LocalMetroViewModelFactory provides appGraph.viewModelFactory,
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        content()
                        overlay()
                    }
                }
            }
        }
    }
    attachContentView(composeView)
}

/**
 * Puts [view] on screen, cheapest option first.
 *
 * 1. `Activity.setContentView` - the normal path.
 * 2. `Window.setContentView` - bypasses AppCompat, which here fails to build
 *    its sub-decor and throws out of `ensureSubDecor()`:
 *
 *        NullPointerException: Attempt to read from field
 *        'android.graphics.Rect ContentFrameLayout.mDecorPadding'
 *        on a null object reference
 *
 * 3. Attach straight to the decor view - the window has no content slot at all.
 */
@PublishedApi
internal fun ComponentActivity.attachContentView(view: View) {
    try {
        setContentView(view)
        return
    } catch (e: RuntimeException) {
        // Fall through to the window.
    }
    try {
        window.setContentView(view)
        return
    } catch (windowFailure: RuntimeException) {
        val decor = window.decorView as? ViewGroup ?: throw windowFailure
        decor.removeAllViews()
        decor.addView(
            view,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
    }
}

fun ComposeView.setComposeContent(
    content: @Composable () -> Unit,
) {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
        TachiyomiTheme {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.bodySmall,
                LocalContentColor provides MaterialTheme.colorScheme.onBackground,
            ) {
                content()
            }
        }
    }
}

fun View?.isVisibleOnScreen(): Boolean {
    if (this == null) {
        return false
    }
    if (!this.isShown) {
        return false
    }
    val actualPosition = Rect()
    this.getGlobalVisibleRect(actualPosition)
    val screen =
        Rect(0, 0, Resources.getSystem().displayMetrics.widthPixels, Resources.getSystem().displayMetrics.heightPixels)
    return actualPosition.intersect(screen)
}

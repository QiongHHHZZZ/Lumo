package eu.kanade.presentation.more

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R

/**
 * 更多页顶部的标志。
 *
 * 这里原来用的是 `Icon` 加 `Icon` 的 tint。`Icon` 会把图片染成单一颜色，
 * 用于矢量图标是对的，但换成插画之后会把画面压成一块剪影，颜色全丢。
 * 所以改用 `Image`，并给一层圆角——和桌面图标的形状保持一致。
 */
@Composable
fun LogoHeader(
    iconPadding: PaddingValues = PaddingValues(),
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_lumo),
            contentDescription = null,
            modifier = Modifier
                .padding(iconPadding)
                .size(72.dp)
                .clip(RoundedCornerShape(18.dp)),
        )

        HorizontalDivider()
    }
}

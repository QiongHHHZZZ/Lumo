package eu.kanade.tachiyomi.ui.more

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.more.NewUpdateScreen
import eu.kanade.presentation.util.Screen

class NewUpdateScreen(
    private val versionName: String,
    private val changelogInfo: String,
    private val downloadLink: String,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel =
            assistedMetroViewModel<NewUpdateScreenModel, NewUpdateScreenModel.Factory> {
                create(changelogInfo = changelogInfo, downloadLink = downloadLink)
            }

        val state by viewModel.state.collectAsState()

        NewUpdateScreen(
            versionName = versionName,
            stage = state.stage,
            downloadProgress = { state.downloadProgress },
            changelogInfo = state.changelogInfo,
            onAcceptUpdate = {
                when (state.stage) {
                    NewUpdateScreenModel.Stage.Available, NewUpdateScreenModel.Stage.Failed -> viewModel.startDownload()
                    NewUpdateScreenModel.Stage.Downloaded -> viewModel.installUpdate()
                    else -> Unit
                }
            },
            onRejectUpdate = navigator::pop,
            onSkipUpdate = {
                viewModel.skipUpdate(versionName)
                navigator.pop()
            },
        )
    }
}

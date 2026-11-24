package mihon.gradle

import org.gradle.api.Project

interface BuildConfig {
    val enableUpdater: Boolean
    val includeDependencyInfo: Boolean
}

private enum class Distribution {
    LOCAL,
    CI,
    GITHUB,
    FOSS,
}

val Project.Config: BuildConfig get() = object : BuildConfig {
    private val distribution: Distribution = project.providers.gradleProperty("dist").orNull
        ?.let { name ->
            Distribution.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?: error("Unknown -Pdist=$name, expected one of $distributionNames")
        }
        ?: Distribution.LOCAL

    /**
     * 更新检查。默认打开。
     *
     * 检查指向本项目自己的 GitHub 仓库（见 AppUpdateChecker.GITHUB_REPO），
     * 不会把用户引回上游。安装包走加速站下载，见 ReleaseServiceImpl.DOWNLOAD_MIRROR。
     *
     * 发布新版本时需在 GitHub 上打形如 v0.0.1 的标签并附上安装包，否则查不到更新。
     */
    override val enableUpdater: Boolean = project.flag("enable-updater") ?: true

    override val includeDependencyInfo: Boolean = project.flag("include-dependency-info") ?: false
}

private val distributionNames = Distribution.entries.joinToString { it.name.lowercase() }

private fun Project.flag(name: String): Boolean? = providers.gradleProperty(name).orNull
    ?.let { it.isEmpty() || it.toBoolean() }

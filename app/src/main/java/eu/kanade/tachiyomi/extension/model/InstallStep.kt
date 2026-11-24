package eu.kanade.tachiyomi.extension.model

sealed interface InstallStep {
    data object Idle : InstallStep
    data object Pending : InstallStep
    data object Downloading : InstallStep
    data object Installing : InstallStep
    data object Installed : InstallStep

    /**
     * 安装失败。
     *
     * [reason] 是给人看的一句话，说明失败在哪一步、为什么。之前这个状态没带原因，
     * 界面只知道"失败了"却说不清是什么，于是干脆什么都不显示——用户点了安装，
     * 什么都没发生，看起来像按钮坏了。
     */
    data class Error(val reason: String = "") : InstallStep

    fun isCompleted(): Boolean {
        return this == Installed || this is Error || this == Idle
    }
}

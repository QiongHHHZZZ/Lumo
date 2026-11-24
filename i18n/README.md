# i18n

这个模块放着界面文案和各语言的翻译。

英文原文在 `src/commonMain/moko-resources/base/`，其他语言在对应目录下（如 `zh-rCN`）。

改文案时改 `base` 里的英文，再同步到各语言文件。键名用点号分隔（如 `pref_reader_page_read`），构建时会自动转成下划线。
# Lint 警告清理

基线：`c8809f8`，Debug Lint 共 112 条警告。按根因修复并分别提交；没有增加警告基线、关闭检查或批量忽略警告。

## 原始分类

| 类别 | 条数 | 处理方式 |
| --- | ---: | --- |
| UnusedResources | 55 | 删除无引用的旧资源及对应译文 |
| VectorPath / VectorRaster | 6 | 随未使用的旧矢量图删除 |
| Overdraw | 4 | 使用主题窗口背景，避免根布局重复绘制 |
| UselessParent | 4 | 移除单子项容器，保留宽屏居中 |
| DisableBaselineAlignment | 1 | 时间表单行关闭不需要的基线计算 |
| CutPasteId | 2 | 复用已经查找到的控件 |
| AlwaysShowAction | 1 | 设置操作在空间不足时进入菜单 |
| UnsafeProtectedBroadcastReceiver | 1 | 明确接受系统广播动作，忽略无效动作 |
| SetTextI18n | 3 | 使用资源格式化课程信息和分钟数 |
| PluralsCandidate | 3 | 删除两项无用字符串，导入数量改为 plurals |
| UseTomlInstead | 4 | 库和插件版本集中到版本目录 |
| AndroidGradlePluginVersion | 3 | 升级兼容的 Android 构建工具链 |
| GradleDependency | 24 | 核对官方仓库后更新稳定依赖 |
| OldTargetApi | 1 | 升级编译与目标 SDK，验证新版运行行为 |

布局清理后出现的 NestedWeights 也单独修复：用相对位置确定首页内容区域，避免两层权重重复测量。

## 验证与兼容依据

- 广播回归测试先复现错误，再确认无动作和无关动作不会启动后台工作。
- 每类修复后运行对应 Lint 或测试；工具链升级后重新运行完整测试与构建。
- [AGP 9.5 官方兼容表](https://developer.android.google.cn/build/releases/agp-9-5-0-release-notes?hl=en)列出最低 Gradle 9.6、JDK 17 与 API 37 支持。
- [内置 Kotlin 迁移说明](https://developer.android.com/build/migrate-to-built-in-kotlin)：移除旧 Kotlin Android 插件和 kotlinOptions，使用内置编译支持。
- [Android 17 SDK 设置](https://developer.android.com/about/versions/17/setup-sdk)与[目标版本行为变化](https://developer.android.com/about/versions/17/behavior-changes-17)用于核对平台升级。
- [Room 发布记录](https://developer.android.com/jetpack/androidx/releases/room)用于核对数据库运行库升级；保留原数据库结构和身份校验。


## 逐项提交

| 根因或后续检查发现 | 提交 |
| --- | --- |
| 无用字符串、颜色和复杂矢量图 | `7a985d2` |
| 根布局重复绘制背景 | `b2d2e18` |
| 多余的单子项布局容器 | `259876e` |
| 不需要的表单基线计算 | `dbb7f2d` |
| 重复查找同一控件 | `1d1edf4` |
| 操作项强制显示，未适配窄屏菜单 | `cf0773f` |
| 广播动作未校验及其回归测试 | `3218a75` |
| 显示文字和数字本地化 | `29a5ce4` |
| 导入课程数量的单复数文案 | `3dcf475` |
| 依赖和插件版本声明分散 | `00c35bf` |
| 布局简化后暴露的嵌套权重 | `55b613b` |
| Android 构建工具链与 Room/KSP 配套升级、旧数据库兼容测试 | `4ea43db` |
| 应用和测试依赖过旧、旧生命周期 Lint 检查失效 | `a1f9157` |
| 新检查器发现的 7 项 KTX 建议 | `aa4d690` |
| 新 SDK 弃用的重复无障碍朗读调用 | `237498d` |
| 新模拟器动画与自动点击时序 | `90c85bd` |
| 目标 SDK 过旧 | `2310467` |
| 移除旧的特殊闹钟权限误报屏蔽 | `469628a` |
| 将新增 Lint 警告作为检查失败 | `4a5ca96` |

## 本轮 Lint 清理时的版本

| 组件 | 版本 |
| --- | --- |
| Gradle / Android Gradle Plugin | 9.8.2 / 9.5.0 |
| Kotlin / KSP | AGP 内置 Kotlin / 2.3.13 |
| 编译、目标 SDK / 最低 SDK | 37 / 33 |
| Room / Gson | 2.8.5 / 2.15.0 |
| Core KTX / Lifecycle | 1.19.1 / 2.11.0 |
| AppCompat / Material | 1.8.0 / 1.14.0 |
| AndroidX Test Core / JUnit 扩展 / Espresso | 1.7.0 / 1.3.0 / 3.7.0 |
| Robolectric | 4.17 |

Room 升级时保留 `courses.db`、版本 1、完整导出结构以及身份值 `39ac5a2ae51cb3b9219ac22c2c748728`。兼容测试使用原始 SQLite 表定义、身份值和逗号分隔的星期字段，确认旧课程的中英文内容可完整读取，继续保存课程后旧记录仍保持不变。

## 最终验证

本节记录 2026-10-31 的 Lint 清理轮次，代码版本为 `4a5ca96`。后续 Kotlin 插件修复及最新验证见 [KOTLIN_PLUGIN_FIX.md](KOTLIN_PLUGIN_FIX.md)。

从 `git archive` 创建仅含已提交文件的全新目录，使用 JDK 17 和 Android SDK 37.0，不复制 `local.properties` 或 IDE 配置：

```sh
./gradlew --offline :app:testDebugUnitTest :app:assembleDebug \
  :app:assembleRelease :app:lintDebug :app:lintRelease
```

结果为 `BUILD SUCCESSFUL`，102 项任务全部执行。

| 验证 | 结果 | 原始记录 |
| --- | --- | --- |
| 本地测试 | 41 通过、0 失败、0 跳过 | [summary.json](verification/2026-10-31/summary.json) |
| Debug Lint | 0 错误、0 警告 | [lint-debug.xml](verification/2026-10-31/lint-debug.xml) |
| Release Lint | 0 错误、0 警告 | [lint-release.xml](verification/2026-10-31/lint-release.xml) |
| Debug / Release APK | 均构建成功 | [summary.json](verification/2026-10-31/summary.json) |
| Android 13 / API 33 | 7 通过、0 失败、0 跳过 | [android-api33.xml](verification/2026-10-31/android-api33.xml) |
| Android 17 / API 37 | 7 通过、0 失败、0 跳过 | [android-api37.xml](verification/2026-10-31/android-api37.xml) |
| Android 17 提醒开关 | 开启登记 2 个任务；关闭后为 0 | [alarm-api37.json](verification/2026-10-31/alarm-api37.json) |

两套系统运行同样的七项操作测试，覆盖周末草稿恢复与保存、单击详情及删除撤销、清空后返回更新、搜索空状态、横屏及键盘布局、通知入口删除撤销和应用上下文。所有设备均使用本任务独立的临时 AVD 数据目录。测试统一关闭系统动画，保留原本的可见性检查和所有功能断言。

实际检查了升级后的[中文首页](screenshots/lint-cleanup/home-api37.png)、[深色首页](screenshots/lint-cleanup/home-dark-api37.png)、[1.3 倍字体表单](screenshots/lint-cleanup/editor-large-api37.png)和[宽屏](screenshots/lint-cleanup/home-wide-api37.png)。课程均为虚构测试数据。

`warningsAsErrors = true` 已生效；没有设置警告基线、关闭检查或添加忽略规则。原有的特殊闹钟权限误报屏蔽也已移除，最终两个 Lint 报告均没有任何问题。

本轮验证时曾有 AGP 默认 Kotlin 插件调用 `Configuration.setVisible` 的弃用提示；现已通过[后续 Kotlin 插件升级](KOTLIN_PLUGIN_FIX.md)消除。实体机厂商省电策略和长期待机提醒仍不在本次模拟器验证范围内。

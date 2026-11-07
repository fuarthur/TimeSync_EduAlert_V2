# Kotlin 插件弃用提示修复

日期：2026-10-31。修改基线：`7654a93`。

## 原因与修复

AGP 9.5 默认依赖 Kotlin Gradle Plugin（KGP）2.2.10，其中 `DefaultKotlinBasePlugin` 调用已弃用的 `Configuration.setVisible`。修改前，`./gradlew help --warning-mode=fail` 可以稳定复现失败。

按 [Android 官方的 KGP 运行时依赖升级说明](https://developer.android.com/build/releases/agp-9-0-0-release-notes)，在根项目的 `buildscript` classpath 中显式选择 KGP 2.4.21，继续使用 AGP 内置 Kotlin 集成。版本及依赖别名集中在版本目录中，没有重新应用旧 Kotlin Android 插件，也没有修改应用代码或数据库结构。

依赖解析确认 AGP 的 `2.2.10` 已选择为 `2.4.21`，见 [实际依赖解析结果](verification/2026-10-31/kotlin-plugin/kgp-resolution.txt)。升级后，相同的严格 `help` 检查通过。`gradle.properties` 设置 `org.gradle.warning.mode=fail`，让以后出现的 Gradle 弃用提示直接使验证失败；原有 Lint 警告阻断规则继续保留。

## 实际验证

使用 JDK 17、Gradle 9.8.2、Android SDK 37.0，在最终代码上运行：

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease \
  :app:lintDebug :app:lintRelease --warning-mode=fail
```

结果：`BUILD SUCCESSFUL`，102 项任务全部执行。41 项本地测试通过，0 失败、0 跳过；Debug 和 Release APK 均构建成功，两个 Lint 报告均没有问题。构建日志没有 Kotlin 编译警告或 Gradle 弃用提示。

- [测试与构建汇总](verification/2026-10-31/kotlin-plugin/summary.json)
- [Debug Lint 原始报告](verification/2026-10-31/kotlin-plugin/lint-debug.xml)
- [Release Lint 原始报告](verification/2026-10-31/kotlin-plugin/lint-release.xml)

设备测试也使用 `--warning-mode=fail`，保留全部操作断言和固定的无动画测试设置，运行于本任务的独立临时 AVD 数据目录。Android 17 / API 37 的 7 项测试全部通过，见 [原始结果](verification/2026-10-31/kotlin-plugin/android-api37.xml)。

Android 13 / API 33 的相同 7 项测试也全部通过，见 [原始结果](verification/2026-10-31/kotlin-plugin/android-api33.xml)。两套系统均为 0 失败、0 跳过。上述构建和两次设备测试都在严格弃用模式下成功，未屏蔽任何提示。

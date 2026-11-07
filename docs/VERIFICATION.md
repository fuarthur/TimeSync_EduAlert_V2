# 验证记录

Kotlin 插件弃用提示的后续修复及严格验证见 [KOTLIN_PLUGIN_FIX.md](KOTLIN_PLUGIN_FIX.md)。

2026-10-31 最新结果：Debug 和 Release 均为 0 条 Lint 问题，41 项本地测试、Android 13/17 各 7 项设备测试，以及干净检出下的两个构建均通过，见 [Lint 清理验证](LINT_CLEANUP.md)。下文保留 2026-10-30 首轮功能验证记录。

验证对象：`codex/timetable-refresh`，代码提交 `3f0fca3`。结果记录于 2026-10-30（America/Chicago）。截图使用虚构课程，没有用户的真实课表。

## 构建与自动化测试

在最终代码上执行：

```sh
ANDROID_SERIAL=emulator-5580 ./gradlew --offline \
  :app:testDebugUnitTest :app:assembleDebug :app:lintDebug \
  :app:connectedDebugAndroidTest \
  -Pandroid.injected.device.serial=emulator-5580 --console=plain
```

结果：`BUILD SUCCESSFUL`。

| 检查 | 实际结果 |
| --- | --- |
| JUnit / Robolectric | 39 项通过，0 失败、0 跳过 |
| Android 模拟器测试 | 7 项通过，0 失败、0 跳过 |
| Debug APK | 构建成功 |
| Android Lint | 0 错误、112 警告 |

本地回归覆盖导入取消与失败保留数据、格式校验与版本兼容、重叠/并发写入、ID 溢出、时间边界与夏令时、提醒开关和过时回调、草稿恢复、列表更新、复制和撤销等。发现问题时记录的先失败后通过证据见 [实施记录](IMPLEMENTATION.md) 和 [独立复审](REVIEW.md)。

模拟器在独立的临时 AVD 数据目录运行，Android API 33。六项完整操作流程覆盖：

1. 周末草稿旋转恢复，通过真实时间选择器保存。
2. 单击详情、删除并撤销。
3. 设置清空后用系统返回键回到已更新列表。
4. 周末搜索和无结果状态。
5. 长地点文本在横屏、键盘打开时仍保留课表空间。
6. 从通知入口打开课程、删除并撤销。

第七项为包上下文检查。界面测试等待异步列表更新及可见控件，不使用旧视图作为成功依据。

Lint 警告主要为未使用的旧资源（55 项）和依赖版本建议（24 项），其余包含矢量图、布局、字符串及目标 SDK 建议。本轮没有宣称无警告，也没有为消除版本提示而进行未经验证的 Android 工具链大版本升级。

## 系统提醒登记

在独立模拟器中准备两门周六课程，通过设置页开启提醒，并通过测试工具授予通知权限。Android `dumpsys alarm` 显示两个未来的 `RTC_WAKEUP` 任务，目标均为本应用的 `CourseReminderReceiver`；关闭提醒后，同一系统列表中的应用任务数量变为 0。这项检查没有等待真实上课时间，也没有模拟厂商长期休眠。

## 干净检出

仅用 `git archive` 导出已提交文件，使用 JDK 17 和 Android SDK 34，通过环境变量指定 SDK。目录中不含原工作区的 `local.properties`、IDE 配置、忽略的构建输出或未提交文件。

执行 `./gradlew --offline :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --console=plain`，结果为 `BUILD SUCCESSFUL`，53 项任务全部执行；39 项测试通过，0 失败。由此确认 Gradle 启动文件、版本目录、源码和测试均在提交中。

## 界面截图

截图来自实际运行的模拟器。手机为 1080×2400、420 dpi；大字体为系统字体比例 1.3；宽屏为 1600×1200、240 dpi。并核对键盘、返回操作标签与切换屏幕宽度后的星期可见性。

| 场景 | 截图 |
| --- | --- |
| 修改前首页 | [before-home.png](screenshots/before-home.png) |
| 浅色首页 | [home-light.png](screenshots/home-light.png) |
| 深色首页 | [home-dark.png](screenshots/home-dark.png) |
| 大字体 | [home-large-text.png](screenshots/home-large-text.png) |
| 中文首页 | [home-chinese.png](screenshots/home-chinese.png) |
| 宽屏 | [home-tablet.png](screenshots/home-tablet.png) |
| 课程表单 | [editor-light.png](screenshots/editor-light.png) |
| 大字体课程表单 | [editor-large-text.png](screenshots/editor-large-text.png) |
| 设置 | [settings-light.png](screenshots/settings-light.png) |

## 验证范围

已验证本地数据行为、系统任务接口和模拟器操作。尚未在实体手机上验证厂商省电策略、长期待机提醒准点率和无障碍用户实际操作；模拟器结果不能保证各厂商的后台调度表现。精确提醒未获系统允许时使用非精确任务，并在设置页说明状态。

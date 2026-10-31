# TimeSync EduAlert 课表

[English](README.md) | 中文

一款离线 Android 课表，用来安排课程、查看下一节课并设置提醒。

## 功能

- 周一至周日课表，一门课程可选择多天重复；保存时检查时间冲突，支持选填地点、备注和课程颜色。
- 首页显示正在进行或下一节课程，可在所选日期内按课程名称或地点搜索。
- 添加、编辑、复制和删除课程；删除后可以撤销，从通知进入课程详情也支持撤销。
- 屏幕旋转后保留草稿，编辑、导入或清空后列表自动更新。
- 提前 1–1440 分钟的每周提醒，支持通知权限管理，以及重启、修改时间或时区后的重新安排。
- 备份导出、导入预览和事务替换。取消导入、文件无效或替换失败时保留原课表。
- Material 浅色与深色主题，简体中文和英文界面，适配键盘、大字体和较宽屏幕。

## 提醒设置

在设置页开启提醒，并允许通知。精确提醒按钮会打开 Android 的“闹钟和提醒”权限页面；未获允许时使用非精确系统任务，Android 可能延迟提醒，应用会显示当前状态。机制见 [Android 官方说明](https://developer.android.com/develop/background-work/services/alarms)。

课程按设备本地时区每周重复，所选的每一天都会分别安排。提醒触发时重新检查当前课程和触发时间，避免过时任务覆盖新的安排。

## 备份与旧数据

继续使用原有 `courses.db` 和 Room 第 1 版数据库结构。兼容旧版课程数组 JSON，新导出格式包含 `schemaVersion: 1`、`format` 和 `courses`。

导入上限为 1 MiB、1000 门课程。星期和 `HH:mm` 时间必须有效，课程名称不能为空，结束时间必须晚于当天的开始时间。重复的正数标识或重叠课程会在预览前被拒绝。确认预览中的课程数量后才替换现有课表，也可以导出空课表。

## 构建与检查

需要 Android SDK 34，建议使用 JDK 17。设置 `ANDROID_HOME`，或由 Android Studio 生成指向 SDK 的 `local.properties`。Gradle 启动文件和依赖版本目录均已提交。

```sh
./gradlew testDebugUnitTest assembleDebug lintDebug
```

连接测试模拟器后运行完整操作流程测试：

```sh
./gradlew connectedDebugAndroidTest
```

调试安装包位置：`app/build/outputs/apk/debug/app-debug.apk`。

技术栈为 Kotlin、Android Views/Material、Room、协程和 Gson。无需账号、云端服务或网络权限。

## 维护记录

- [问题与提交对应表](docs/CHANGES.md)
- [实际验证结果与截图](docs/VERIFICATION.md)
- [实施计划](docs/superpowers/plans/2026-10-30-timetable-refresh.md)
- [独立审阅的后续修复](docs/REVIEW.md)

## 开源许可

[MIT](LICENSE)。前身：[TimeSync_EduAlert](https://github.com/fuarthur/TimeSync_EduAlert)。

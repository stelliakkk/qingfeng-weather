# 清风天气

清风天气是一款轻量、无广告的 Android 天气应用。它独立于荣耀手机自带天气，不需要改动或破解系统应用。主天气服务使用和风天气，备用服务使用 Open-Meteo。

## 项目状态

| 项目 | 信息 |
| --- | --- |
| 当前版本 | 1.3.0（versionCode 4） |
| Android 要求 | Android 8.0（API 26）及以上 |
| 主天气服务 | 和风天气 |
| 备用天气服务 | Open-Meteo |
| 发布包 | [`release/清风天气-v1.3.0.apk`](release/清风天气-v1.3.0.apk) |

详细安装与配置步骤见[使用说明](使用说明.md)。

## 功能

- **天气实况**：温度、天气现象、体感温度、湿度、风速和降水。
- **逐小时预报**：未来 24 小时的天气、温度和降水概率。
- **7 天预报**：每天的天气情况和高低温。
- **天气预警**：首页显示预警摘要，点击可查看详情和防御建议。
- **空气质量**：AQI、等级、首要污染物、PM2.5/PM10 和健康提示；中国地区优先使用中国本地 AQI 标准。
- **生活指数**：首页摘要及穿衣、运动、洗车等指数详情。
- **区县城市搜索**：优先使用和风 GeoAPI，显示区县、所属城市和省份，合并行政路径相同的重复结果；和风搜索失败时使用 Open-Meteo 备用搜索。
- **圆角界面**：主天气卡片、城市选择、天气服务设置和信息弹窗采用圆角样式。
- **动态天气背景**：晴天柔和暖光、多云缓慢云层、雾天轻雾、雨天雨丝、雷雨云雨和雪天飘雪；效果保持浅色低对比度，可在设置中关闭。
- **快速打开**：先展示本机天气快照，再检查是否需要联网更新。
- **手动刷新**：在天气主页下拉即可刷新；前台运行时每 30 分钟自动更新一次。

## 天气服务与隐私

- 未配置和风天气时，应用使用 Open-Meteo 获取实况、逐小时和 7 天预报。备用服务不提供本应用的和风预警、空气质量和生活指数。
- 配置和风天气后，应用优先获取实况、7 天预报、24 小时预报、预警、空气质量和生活指数；个别接口失败时，不会阻止实况和预报显示。
- 应用不申请定位权限。默认城市为北京；城市搜索后，只保存用户选中的城市，不保存 GeoAPI 搜索结果列表。
- 应用没有广告或统计 SDK。API Key 不会打包进 APK；它保存在应用私有数据中并由 Android Keystore 管理的 AES-GCM 密钥加密。
- 卸载应用会清除本机密钥、API 配置和天气缓存。直接安装新版覆盖旧版通常会保留这些数据。

## 和风天气 API 配置

在和风天气控制台创建项目并添加 API KEY 凭据，再从控制台“设置”复制 API Host。打开应用的“设置”，填写 Host（不要加 `https://`）和 API Key。

API 限制建议启用以下类别：**天气预报、天气预警、空气质量、天气指数、GeoAPI**。控制台里的“不限制”表示 Key 可以调用所有 API 类别，不表示请求量不限或永久免费。

和风天气官网当前的“天气和基础服务”按月累计请求：前 50,000 次为 0 元，之后每次按阶梯计费；当前下一档为每次 0.0007 元。天气、预警、指数、空气质量和 GeoAPI 属于这个合并计价组。持续在前台运行时，本应用单城市约每月 8,640 次天气请求，另加城市搜索；费用规则以[和风天气价格页](https://dev.qweather.com/docs/finance/pricing/)及控制台为准。

不要把 API Key 提交到仓库、截图或聊天中。虽然应用会加密保存，个人设备上的客户端密钥仍不同于服务器端保密凭据。

## 本地构建

需要 Java 17 或更新版本、Android SDK 36 和 Android Build Tools 36。Windows PowerShell 示例：

```powershell
$env:ANDROID_HOME = 'F:\path\to\android-sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:JAVA_HOME = 'F:\path\to\jdk'
$env:GRADLE_USER_HOME = 'F:\path\to\gradle-cache'
.\gradlew.bat clean testDebugUnitTest assembleDebug
```

APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`。仓库中的 `release/清风天气-v1.3.0.apk` 是供个人试装的 Debug 签名版本，不是应用商店发布签名。

## 自动测试

18 项 JVM 单元测试覆盖和风与 Open-Meteo 天气解析、逐小时降水概率换算、预警/AQI/生活指数解析、城市行政路径与重复项处理、缓存往返、主备数据源切换，以及天气背景分类（含雨、雷雨、雪和未知描述）。当前没有连接模拟器或手机执行自动化 UI 测试；安装后的页面和和风账号联调仍需在设备上检查。

## 数据来源与文档

- [和风天气实况](https://dev.qweather.com/docs/api/weather/weather-current/)、[逐小时预报](https://dev.qweather.com/docs/api/weather/weather-hourly-forecast/)、[预警](https://dev.qweather.com/docs/api/warning/weather-alert/)、[空气质量](https://dev.qweather.com/docs/api/air-quality/air-current/)、[生活指数](https://dev.qweather.com/docs/api/indices/indices-forecast/)、[GeoAPI 城市搜索](https://dev.qweather.com/docs/api/geoapi/city-lookup/)。应用会展示天气响应提供的数据归因。
- Open-Meteo：应用注明来源，并遵循 [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)；使用规则见 [Open-Meteo Terms](https://open-meteo.com/en/terms)。

# 清风天气官网

清风天气的静态产品网站。官网使用 Vite、HTML、CSS 和原生 JavaScript，运行时无需服务器、天气 API 或 Key。Android 源码与构建流程位于上一级目录。

## 本地开发

使用 Node.js 24。在 `website/` 内运行以下命令。

```sh
npm ci
npm run dev
```

打开 Vite 输出的地址。默认路径为 `/qingfeng-weather/`。

```sh
npm run build
npm run preview
```

生产文件位于 `dist/`。预览时访问终端输出的完整地址，包括 `/qingfeng-weather/`。

## GitHub Pages

[官网地址](https://stelliakkk.github.io/qingfeng-weather/)

在仓库 [Settings → Pages](https://github.com/stelliakkk/qingfeng-weather/settings/pages) 中将 Source 设为 **GitHub Actions**。官网或工作流变更推送到 `main` 后，`.github/workflows/website.yml` 会构建并部署 `website/dist`。也可从 Actions 手动运行工作流。针对官网的 Pull Request 只执行构建检查。

Vite 的 `base`、页面 canonical 与 Open Graph 地址使用仓库的 Pages 子路径。迁移域名或仓库时，应同步更新这些值并检查分享封面的绝对 URL。

## 功能与数据口径

- 核对基准为 Android 项目提交 `eb97dff`。实际版本为 `1.3.1`（versionCode 5），最低 Android 8.0 / API 26。仓库 APK 是 Debug 签名的个人测试包。
- Open-Meteo 提供备用实况、24 小时与 7 天预报。天气预警、空气质量和生活指数依赖已配置并可用的和风天气服务。Android 应用不申请定位权限。
- 官网页面不读取本地天气。“天气效果演示”只切换预置的场景图片和动效。功能区温度与趋势均有示例标记。
- 上游功能、版本或数据源变化时，核对根目录 README、`app/build.gradle` 与 Android 实现，再更新官网相应文案。

## 素材与版权记录

风景图由 ImageGen 为本官网原创生成，采用摄影风格。图片不是实地摄影，未使用第三方图库或远程图片热链接。桌面与移动版均为本地 WebP，OG 分享图由雨后背景和官网文字合成。背景作为装饰使用空 `alt`，天气切换的文字描述独立提供。分享图与应用截图有对应的替代文字。

| 文件 | 视觉说明 |
| --- | --- |
| `dawn*.webp` | 雨后的山湖、松林、贴水薄雾和金色晨光 |
| `sun*.webp` | 晴日湖面、暖色自然光与浅云 |
| `cloud*.webp` | 多层灰白云与浅灰绿山湖 |
| `rain*.webp` | 潮湿的山林、低云与雨中的湖 |
| `fog*.webp` | 湖面雾气与渐隐的松林和远山 |
| `dusk*.webp` | 暮色湖面、层叠远山与暖色天际线 |
| `share.jpg` | 雨后山湖背景上的清风天气品牌与主文案 |
| `app-*.webp` | 仓库 v1.3.1 APK 在 Android 10 模拟器中的真实截屏。天气、AQI、预警和指数来自注入应用本地缓存的示例数据。设置使用空白配置，未采集 API Host 或 Key。 |
| `brand.svg` | 官网原创风线与日光标志 |
| `fonts/noto-serif-sc-headings.woff2` | [Noto Serif SC](https://github.com/google/fonts/tree/main/ofl/notoserifsc) 标题字符子集，使用 650 字重。保留 [SIL OFL 1.1](public/fonts/OFL.txt)。 |

截图保持原始 432 × 960 构图，页面用 CSS 呈现设备外框和预览裁切，放大时展示全图。更新截图时请使用当前 APK，检查系统通知栏、显示内容和配置字段，防止个人信息或密钥进入图片。

## 资源命名

自然背景为 `public/media/<scene>.webp` 与 `<scene>-mobile.webp`。桌面首屏为 1920 × 1080，其余背景为 1600 × 1000。移动端使用独立竖向裁切。修改天气文件名时，需同步更新 JavaScript 的模式键和默认晴天画面。更新标题字符时，重新制作 WOFF2 子集或交由本地宋体回退。

## 已执行的验证

- Node.js 24 下的 `npm ci` 与生产构建。
- Edge Chromium 在 320、360、390、768、1440 px 的视口中检查横向溢出、锚点位置、响应式背景、四种天气模式、键盘控制、截图放大和关闭后的焦点恢复。
- 390 与 1440 px 的浏览器截图视觉检查和 axe WCAG 2.1 A/AA 自动检查。验证默认雨丝和雾层动效、离屏暂停及减少动态效果下的交互。
- 在基准提交实跑 Android `testDebugUnitTest`，18 项通过。Android 10 模拟器用于运行 APK 和截取界面，示例数据未用于验证和风天气服务联调。

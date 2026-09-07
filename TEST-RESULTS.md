# 实机验证记录 — 2026-09-07

## 域名丢失复现

在 `https://fill.dev/form/login-simple` 聚焦输入框，兼容结构有 122 个节点、3 个输入节点，webDomain=0。Bitwarden 按 Chrome 包名匹配。这是 0.1 的缺陷；它的“出现候选并填入”验证不足以证明网站匹配正确。

## 根因定位

1. Android View.populateVirtualStructure 复制输入类型、值与虚拟 ID，遗漏网页来源。
2. Bitwarden 当前 URL_BARS 表移除了 Chrome。
3. 给文档根节点设置域名后，webDomain=1，深层字段仍回退到包名。Bitwarden traverse 向下传递当前节点 website，深层节点需要各自的来源元数据。

## 修复验证

| 场景 | 观察 | 结果 |
|---|---|---|
| fill.dev 登录表单 | 122 节点、19 个节点有域名；Bitwarden 显示“fill.dev用のアイテム” | 通过 |
| 同一 tab 从 fill.dev 导航到 example.com，临时添加合成登录表单 | Bitwarden 显示“example.com用のアイテム” | 通过 |
| 顶层 HTTPS 页中加入 sandbox=allow-scripts 的 srcdoc iframe | 结构保留顶层输入、排除 iframe 输入；聚焦 iframe 后 Gboard 无 Bitwarden 候选 | 通过 |
| Chrome 模式 | ContentProvider 的 autofill_third_party_state=0；Google 原生密码/卡片/地址入口仍显示 | 通过 |
| API 与产物 | API 102 框架成功加载 API 101 模块；编译和 APK 签名校验通过 | 通过 |

合成测试只改变当前测试 tab 的 DOM，不向服务器提交表单。没有创建测试凭据。测试结束通过导航清除合成页面。

## 尚待验证

- 0.2 来源绑定后，在有对应 Bitwarden 凭据的网站上点选填入。
- Google 信用卡完整填入、TOTP，以及 Google/Bitwarden 两家的 passkey 完整登录。
- 所有重定向/同源换页时序、无痕、多 Activity、其他版本的回归。

原始诊断代码已移除；统计值与 UI 的域名标题是本记录的证据。跨来源 iframe 已采取排除策略；同源 iframe 暂时也被排除。

最终去诊断日志的 0.2 安装后，再次自动断言 Bitwarden 标题包含“fill.dev用のアイテム”：通过。测试时开启的保持亮屏设置已恢复为原值 0；临时 ADB DevTools 转发已移除。

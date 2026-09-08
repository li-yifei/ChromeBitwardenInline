# How it works / 实现

The module enables Android's Autofill compatibility bridge inside Chrome while Chrome keeps Google Autofill selected. Requests go to the Android profile's selected third-party AutofillService.

模块在 Chrome 保持 Google 自动填充时启用 Android Autofill 兼容桥，请求交给当前 Android 资料中选中的第三方服务。

The compatibility structure can omit website metadata. The module reads visible and committed URLs from the ContentView's WebContents, requires matching HTTPS origins, and attaches the origin to the document's autofill nodes. It checks the origin again when filling.

兼容结构可能遗漏网页来源。模块读取 WebContents 的可见 URL 和已提交 URL，要求 HTTPS origin 一致，将来源写入节点，并在填入时再次检查。

Native Chrome fields and nested documents are excluded. Credential Manager and WebAuthn routing remain unchanged.

Chrome 原生输入框与嵌套文档会被排除。Credential Manager 和 WebAuthn 沿用原有路由。

See [test results](../TEST-RESULTS.md) for observed behavior.

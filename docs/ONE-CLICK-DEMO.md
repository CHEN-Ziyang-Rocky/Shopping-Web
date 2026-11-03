> Archived: the portfolio now links to the public storefront. Former demo pages redirect there and the separate API is disabled by default (`DEMO_ENABLED=false`). The notes below describe the previous release.

# 一键演示（2026-11-02）

状态：已于 2026-11-03 部署并完成公网接口验收。本轮按用户授权发布，保留原有未提交工作；未执行数据库 reset，未修改 Nginx、端口或真实用户数据。

## 会话与权限

前端 `/demo` 只展示说明；点击 Try Demo 才 POST 创建上下文。浏览器生成 256 位幂等键，服务器产生独立 256 位不透明凭证。凭证仅保存在该标签页的 sessionStorage，不写 URL、日志或正常账号存储；服务端不接受客户端角色升级。正常账号凭证保留；Exit 会撤销服务器凭证，再清除演示存储并回入口。Reset 先显示行内确认，再仅恢复当前上下文；刷新恢复有效状态。关闭标签页不主动发出退出请求，未撤销状态按 TTL 回收。

每次受保护请求同时验证凭证、闲置期限和绝对期限；清理滞后不延长访问权。仅命中当前凭证的状态，公开样本之外不读取正常用户数据。写请求要求 JSON；有 Origin 时必须精确匹配前端来源。演示使用 Authorization，不依赖普通账号 Cookie，不把演示身份赋予普通用户或管理员权限。生产必须通过 HTTPS；不要将凭证存入可共享链接。

状态为有上限的进程内存，重启后全部演示失效，不写 MySQL。适用于当前单实例作品集；必须让演示请求稳定进入同一后端进程。启用前检查实际 PM2/容器副本数；若多进程或多副本，先保持 `DEMO_ENABLED=false`，另行审查共享存储或进程路由方案。此限制不是已经完成的多实例能力。

## 配置

| 变量 | 示例/默认 | 用途 |
| --- | --- | --- |
| DEMO_ENABLED | true | false 时拒绝新旧演示请求，保留正常业务 |
| DEMO_IDLE_SECONDS | 3600 | 闲置期限 |
| DEMO_MAX_SECONDS | 86400 | 绝对保留上限 |
| DEMO_MAX_SESSIONS | 200 | 活跃上下文上限 |
| DEMO_CREATE_PER_HOUR | 20 | 每个连接来源每小时新建上限；重复幂等请求复用原会话 |
| DEMO_REQUESTS_PER_MINUTE | 180 | 每个演示会话每分钟请求上限 |

创建限流采用连接的 remote address，**不信任客户端 X-Forwarded-For**。反向代理后可能共用一个限流桶，应按实际访客量调整上限；未改动 Nginx 的信任链或访问日志。计数表也有 10,000 个来源上限，防止无限增长。以上限制是单进程限制，重启会清空计数。

## 部署与回退

1. 保存当前可回退的应用构建和本次源码清单；不要用 `git reset --hard` 清理现有工作。
2. 在独立发布目录安装锁定依赖、运行本页检查，保留服务器现有秘密配置；不要把本地 `.env`、密钥、数据库或上传目录打包进去。
3. 先部署后端与公开样本，核对单进程、来源、TTL/上限和 API 转发；再部署前端。没有新增数据库迁移，不运行 schema sync、db:init、seed:admin 或全库 seed。
4. 经批准后用已有应用管理器重启对应服务。没有授权时停在本地构建结果，不能直接重启线上 PM2/systemd、改 Nginx 或 Compose。
5. 在线复测两个隔离会话：核心操作、跨会话 ID、正常账号/管理员拒绝、刷新、确认重置、退出旧凭证 401。检查正常账号还能登录/读写自己的数据，检查手机布局。TTL 可在独立验收环境缩短，不在生产制造大量会话。
6. 三个应用线上验收通过后，最后发布 homepage 的 Try Demo 链接，并去掉待部署说明。只有此时简历才可将新地址作为无需注册的 Live Demo。
7. 回退先恢复旧前端和主页，再恢复旧后端/样本及原配置。也可 `DEMO_ENABLED=false` 暂停入口。无需回滚数据库或删除真实数据；服务重启会终止现有演示。

## 验证边界

自动化测试覆盖服务器身份与归属、到期/退出、限流、重置和正常业务回归，不仅检查渲染。截图位于 `docs/demo-screenshots/`，全部来自本地样本会话。未验证线上域名部署、真实用户数据回归、真实 Stripe 端到端支付、多实例与高负载行为。浏览器隔离检查使用独立标签页的 sessionStorage 上下文及 HTTP 独立凭证；不声称做过两个独立浏览器 profile 的完整自动化。

## Shopping 的复用与改动

原认证：Spring Security + HttpOnly JSESSIONID + 会话 CSRF；原购物车在 localStorage。演示独立 `/api/demo/**`，凭证前缀 `demo_`，不创建普通 Spring principal。正常 Cookie 不变；前端演示请求使用 credentials:omit，普通接口看到 demo_ Authorization 时即使同时携带正常 Cookie 也会 403。

复用现有 ProductCard、ProductImage、ProductDetailActions、购物袋、VoucherOffers 和 OrdersView。演示目录固定为已有 everyday-edit 定义中的 8 个示例商品，图片沿用 public/catalog；公开普通商品目录仍免登录。演示目录、库存副本、购物车、优惠券使用状态和订单全在当前上下文。示例券 DEMO10 减 HK$10（不超过小计），每会话重置前一次；服务器重算价格、数量及库存，最多 20 个购物车行、每行 99 件、10 张订单。订单 ID 由服务端随机产生，另一会话访问该 ID 返回 404。

**本次交付为独立模拟结账，未声称已验证 Stripe 实际测试支付。** 没有读取支付密钥，DemoController 不依赖 CheckoutService、PaymentGateway、仓库、邮件或履约组件；其订单状态为 SIMULATED。重复 checkout 幂等键不重复下单。正常 Stripe 签名校验和状态验证继续由原接口执行，没有绕过 webhook。官方测试方式见 [Stripe Testing](https://docs.stripe.com/testing)：应使用测试密钥和官方测试资料，不能输入真实卡资料；当前模拟路径不需要卡号。

入口 `/demo` → `/demo/shop` → `/demo/products/:id`，订单 `/user/orders`。Reset 有行内二次确认，恢复本上下文的商品库存、券、购物车和订单，不操作商品表。

清理：已启用 Spring scheduling，`DEMO_CLEANUP_MILLIS=60000` 定期删除到期内存上下文和旧计数；每次请求仍先校验到期。`frontend.url` 复用原前端来源配置。没有新增 Flyway 迁移。

正常本地启动沿用 README；演示单独预览可用不连接数据库的测试 harness：

```sh
cd backend
./mvnw test-compile dependency:build-classpath -Dmdep.outputFile=/tmp/shop-demo-classpath
java -cp "$(cat /tmp/shop-demo-classpath):target/classes:target/test-classes" cc.iecuhk.iems5718.s57.demo.DemoPreview --spring.config.location=optional:file:/tmp/empty-shop-demo.properties --server.port=5131 --server.address=127.0.0.1 --frontend.url=http://localhost:5130
# 另一终端 frontend
BACKEND_URL=http://127.0.0.1:5131 NEXT_PUBLIC_API_URL=/api npm run dev -- --port 5130 --hostname 127.0.0.1
```

`/tmp/empty-shop-demo.properties` 应不存在或为空，不放秘密；harness 在测试源码中、仅作本地验收，使用真实安全链与替身用户仓库，不包含正常账号业务。生产使用原应用入口和外部配置。

检查：Java 17；`./mvnw -Dtest=DemoTests,MoneyTests,StripePaymentGatewayTests test` 13 项通过；`ShopIntegrationTests` 既有 71 项使用独立 Testcontainers MySQL 通过，另加 demoKeepsNormalSessionAndCannotUseItsPrivileges 定向测试通过（含正常 Cookie 保留、普通支付和管理员接口拒绝）。前端类型与 `next build --webpack` 通过。临时副本默认 Turbopack 不能解析跨目录 node_modules 软链接，故验收采用 Webpack；未改依赖或生产构建配置。

浏览器完成示例详情→数量 2→购物袋→DEMO10→模拟结账→SIMULATED 订单 HK$788；刷新后订单仍在，第二会话无订单。

最终补充：`node --test frontend/test/demo-account-boundary.test.cjs` 通过，验证 demo 上下文调用账号/管理 API 时不会发送保存的普通账号 Cookie；退出 demo 后普通账号 API 行为保留。正常接口的 demo bearer 拒绝为直接 JSON 403，避免 Servlet 错误转发改变状态码。

## 本轮线上发布结果（2026-11-03）

已部署到原有域名的 `/demo`，公网会话、隔离、越权拒绝与核心接口通过；正常登录入口保留。Shopping `/register` 与 Finance `/signup` 提供明显 Register 按钮。Commercial 保留原本默认关闭的公开注册策略。上线后的浏览器工具连接超时，未声称完成本轮线上视觉或注册新账号的全流程；已有本地桌面/手机截图保留。

Finance JavaScript CI 在定向修复 follow-redirects 后通过；本轮只将该间接依赖固定到 1.16.2，没有升级框架。后台 34 项、前端 28 项测试通过，公开 Python CI 81 项（其中需要未公开模型的 1 项跳过）；服务器模型验证另外 3 项通过。

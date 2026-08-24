# wallet 钱包工程

多模块 Spring Boot 钱包：用户资产（余额 / 积分 / 优惠券 / 支付密码）+ 三方支付内核 + **拆分支付**编排。
由 pay-channel-sdk 扩展而来——原来只是一个纯 Java 的三方支付编排 SDK，现在补全为完整的钱包系统，
并贯彻一个核心设计：**同一支付单的所有状态变更共用同一把分布式锁**。

## 模块划分

```
wallet（父 pom：版本管理、模块聚合）
├── wallet-common    通用件：ApiResult / BizException / ErrorCode / IdMaker / MoneyUtil
├── wallet-channel   三方支付内核（纯 Java，无 Spring）：渠道×动作注册表、支付/退款状态机、渠道调用编排
├── wallet-asset     用户资产：余额 money / 积分 point / 优惠券 coupon / 支付密码 password（全部 CAS 条件更新）
├── wallet-pay       支付编排：支付主单 + 分段（券/积分/余额/三方）拆分支付、单锁、退款分摊、超时关单、mock 渠道
└── wallet-app       启动类 + REST 接口 + 装配配置
```

SQL 脚本统一放根目录 `sql/`，命名 `日期-中文说明.sql`（如 `2026-08-24-钱包建表.sql`）。

依赖方向：`app → pay → (asset, channel, common)`；`asset → common`；`channel` 不依赖任何内部模块与 Spring。

## 核心设计

### 拆分支付
一笔支付单可同时用 **优惠券抵扣 + 积分 + 余额 + 三方渠道** 拆成多段：

- `pay_order` 主单记录总额与状态；`pay_part` 分段记录每段金额与来源（COUPON / POINT / MONEY / CHANNEL）。
- 校验 `sum(段金额) == 总金额` 通过后才能创建。
- 资产段（券/积分/余额）在**同一个本地事务**内按顺序 CAS 扣减，任一段失败整体回滚；
  三方段异步等回调确认，期间主单停在 PAYING。
- 纯资产支付（无三方段）当场完成；渠道下单失败时已扣资产段自动补偿回滚。

### 同一把锁
分布式锁统一用 lock4j 注解声明：`@Lock4j(name = "order", keys = "#orderNo")`，
实际 key 为 `wallet:lock:order#{orderNo}`（前缀由 `lock4j.lock-key-prefix` 配置）。
提交支付、回调、主动查询、退款、取消、超时关单入口全部持同一把锁，串行执行。
底层是 Redisson 可重入锁（`RedissonLockExecutor` 复用手动装配的 `RedissonClient`）：
看门狗续期、等锁 3 秒快速失败（`LockFailureException` 由全局异常处理映射为 `LOCK_FAILED`）。
内核 `wallet-channel` 不加锁，由编排层入口方法持锁。

### 状态机（转换表式）
- 主单：`INIT --SUBMIT--> PAYING --FINISH--> SUCCESS`；`PAYING --FAIL--> FAIL`；`INIT/PAYING --CLOSE--> CLOSED`
- 分段：`INIT --START--> PAYING --DONE--> SUCCESS`；`--FAIL--> FAIL`；`--CLOSE--> CLOSED`；`SUCCESS --ROLLBACK--> ROLLBACK`（未完成支付时的补偿返还）
- 退款单：`INIT --REFUND_REQUEST--> REFUNDING --REFUND_SUCCESS/FAIL--> SUCCESS/FAIL`

实体状态与分段类型全部用枚举（`OrderState`/`PartState`/`RefundOrderState`/`PayType`，DB 存 name()，
服务层无字符串比较）；可否关单等转换判定走 `OrderStateMachine`/`PartStateMachine.canTransition`。
结单（全部分段成功→主单 SUCCESS + 发布事件）唯一入口是 `OrderFinisher`；
资产扣减/回滚/退款的事务边界在独立 Bean `AssetPartService`（经代理调用，@Transactional 才生效）。

### 资产扣减全部条件更新（CAS）
- 扣余额：`UPDATE wallet_account SET money = money - x WHERE user_id=? AND money >= x`（影响行数=1 才算成功）
- 核销券：`UPDATE user_coupon SET status=1 WHERE id=? AND status=0 AND expire_time > NOW()`
- 状态推进：`UPDATE ... SET state=:to WHERE xx_no=:no AND state=:from`

### 支付密码
`wallet-asset` 的 password 模块：BCrypt 慢哈希（强度 12）+ 连续错 5 次 / 当日 10 次锁 10 分钟 +
校验通过签发一次性授权票据（TTL 300 秒），提交支付时原子消费并复核用户/订单/金额。

## 技术栈

Java 21 LTS · Maven 3.9+ · Spring Boot 3.5.x · Spring Cloud 2025.0.x（BOM 预置，组件按需引入）·
MyBatis-Plus 3.5.x（`mybatis-plus-spring-boot3-starter` + 分页插件 `PaginationInnerInterceptor`）·
Redis 7.x：`spring-boot-starter-data-redis`（排除 Lettuce）+ Redisson 4.7 底层（手动装配 `RedissonClient`，
经 `RedissonConnectionFactory` 桥接 Spring Data Redis，Redisson 值序列化用 `JsonJacksonCodec`，
`RedisTemplate` 值序列化用 Jackson JSON）·
lock4j 2.2.7（`@Lock4j` 声明式分布式锁）· XXL-Job 3.4.2（分布式定时任务，执行器内嵌，见 `xxl.job.*` 配置）·
MySQL 8.0+ · springdoc-openapi 2.8（Swagger 3 / OpenAPI 3）· spring-boot-starter-validation（`@Valid` 参数校验）·
Actuator + Prometheus（`/actuator/health`、`/actuator/prometheus`，JVM/CPU/GC 自动上报，QPS/RT 看
`http_server_requests_seconds`，Grafana 面板服务端配）· JUnit 5 + Mockito + Testcontainers（无 Docker 自动跳过）。
RabbitMQ 3.13+ / Elasticsearch 8.x 规划内，按需引入。
金额统一用 **long（分）**，积分数量独立 long 字段。

约定（架构层统一，业务代码不用重复处理）：
- **序列化**：Jackson 全局配置（`JacksonConfig`）——`LocalDateTime ⇄ yyyy-MM-dd HH:mm:ss`、
  `LocalDate ⇄ yyyy-MM-dd`、`LocalTime ⇄ HH:mm:ss`、Date 东八区同格式；枚举入参大小写不敏感；
  查询串/表单参数格式在 `spring.mvc.format`，与 JSON 一致。
- **统一返回体**：`ApiResult{code, message, data, traceId, timestamp, success}`，traceId 自动携带。
- **TraceId 链路追踪**：`TraceIdFilter` 透传/生成 `X-Trace-Id` → MDC → 日志与响应头；
  异步/定时入口用 `TraceIds.seed()` 播种。日志格式统一在 `logback-spring.xml`
  （控制台 + 按天/100MB 滚动 + ERROR 单独文件 + 异步输出）。
- **领域事件**：主单支付成功/关闭、退款成功发布 `OrderPaidEvent`/`OrderClosedEvent`/`RefundSuccessEvent`
  （CAS 推进成功才发布，至多一次），联动逻辑写 `@EventListener` 订阅，服务间不互相调用；
  事件在持锁内同步派发，监听器只做轻量动作。
- **日志**：统一 SLF4J，Lombok 仅用于 `@Slf4j` 注解（entity 仍手写 getter/setter，model 用 record）；
  error 日志必须带异常堆栈。

## 常用命令

```bash
# 全量构建 + 测试（支付密码单测需本机 127.0.0.1:6379 Redis，Redis 不可用时自动跳过）
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test

# 只编译
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile
```

## REST 接口（wallet-app，默认端口 8080）

用户识别用请求头 `X-Uid: <数字>`，无鉴权体系（联调用）。

| 方法+路径 | 说明 |
|---|---|
| GET `/api/asset/summary` | 资产总览（余额/积分/可用券） |
| POST `/api/asset/recharge` | 模拟充值余额 |
| POST `/api/asset/point/add` | 模拟发积分 |
| POST `/api/asset/coupon/take` | 领券 |
| POST `/api/password/set` | 设置/重置支付密码 |
| POST `/api/password/verify` | 校验密码并签发一次性票据 |
| POST `/api/pay/create` | 创建拆分支付单 |
| POST `/api/pay/submit` | 提交支付（扣资产+发起三方） |
| POST `/api/pay/callback/{channel}/{orderNo}/{partNo}` | 渠道异步回调 |
| GET `/api/pay/order/{orderNo}` | 查主单+分段 |
| POST `/api/pay/query/{orderNo}` | 主动向渠道查证 |
| POST `/api/pay/cancel/{orderNo}` | 取消支付 |
| POST `/api/refund/create` | 发起退款 |
| GET `/api/refund/{refundNo}` | 查退款单+分段 |

## 联调环境

```bash
# 本机需 MySQL 8（库 wallet，root 无密码）与 Redis（127.0.0.1:6379）
/opt/homebrew/opt/mysql/bin/mysql.server start
/opt/homebrew/opt/redis/bin/redis-server --daemonize yes
cd wallet-app
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn spring-boot:run
# 建表：mysql -uroot wallet < sql/2026-08-24-钱包建表.sql
# swagger-ui: http://localhost:8080/swagger-ui.html
```

Mock 渠道配置（`config/mock.yml` 的 `wallet.mock.*`）：总开关 enabled、下单/查询延迟、下单失败率、下单后 N 秒自动回调（0=手工触发）、退款强制失败开关。mock 自动回调只对 MOCK 渠道的分段生效。

配置文件按主题拆分（`spring.config.import` 装配，按环境的差异用 `application-{env}.yml` 覆盖，两者正交）：

```
resources/
├── application.yml      # 主入口：应用名、端口、import 清单
├── logback-spring.xml   # 日志格式/滚动/traceId
└── config/
    ├── infra.yml        # 基础能力：数据源/Redis/MyBatis-Plus/lock4j/XXL-Job/Web 格式/接口文档
    ├── biz.yml          # 业务参数：wallet.*（支付/密码/Antom）
    ├── mock.yml         # Mock 渠道（仅联调；wallet.mock.enabled=false 时 MockChannel 不注册）
    └── monitor.yml      # 监控（actuator/prometheus）与日志级别
```

## 接入真实渠道

在 `wallet-pay` 的 `mock/` 旁实现一个类，实现 `com.wallet.channel.action.*` 的动作接口
（`PayAction` 必选，`QueryAction` / `RefundAction` / `CancelAction` / `CallbackAction` / `ConfirmAction` 按渠道能力选实现），
在装配配置里 `ChannelKit.builder().addChannel(...)` 注册即可。参见 `antom/` 目录的 Antom 渠道示范。

# wallet 钱包工程

多模块 Spring Boot 钱包：用户资产（余额 / 积分 / 优惠券 / 支付密码）+ 三方支付内核 + **拆分支付**编排。
由 pay-channel-sdk 扩展而来——原来只是一个纯 Java 的三方支付编排 SDK，现在补全为完整的钱包系统，
并贯彻一个核心设计：**同一支付单的所有状态变更共用同一把分布式锁**。

## 模块划分

```
wallet（父 pom：版本管理、模块聚合）
├── wallet-common    通用件：ApiResult / BizException / ErrorCode / IdMaker / LockService（Redisson 单锁）/ MoneyUtil
├── wallet-channel   三方支付内核（纯 Java，无 Spring）：渠道×动作注册表、支付/退款状态机、渠道调用编排
├── wallet-asset     用户资产：余额 money / 积分 point / 优惠券 coupon / 支付密码 password（全部 CAS 条件更新）
├── wallet-pay       支付编排：支付主单 + 分段（券/积分/余额/三方）拆分支付、单锁、退款分摊、超时关单、mock 渠道
└── wallet-app       启动类 + REST 接口 + 装配配置 + 建表 SQL
```

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
`wallet-common` 的 `LockService` 提供唯一锁语义：`wallet:lock:order:{orderNo}`。
提交支付、回调、主动查询、退款、取消、超时关单全部持同一把锁，串行执行。
内核 `wallet-channel` 不加锁，由调用方在编排层外层持锁（可重入、看门狗续期、等锁 3 秒快速失败）。

### 状态机（转换表式）
- 主单：`INIT --SUBMIT--> PAYING --FINISH--> SUCCESS`；`PAYING --FAIL--> FAIL`；`INIT/PAYING --CLOSE--> CLOSED`
- 分段：`INIT --START--> PAYING --DONE--> SUCCESS`；`--FAIL--> FAIL`；`--CLOSE--> CLOSED`；`SUCCESS --ROLLBACK--> ROLLBACK`（未完成支付时的补偿返还）
- 退款单：`INIT --REFUND_REQUEST--> REFUNDING --REFUND_SUCCESS/FAIL--> SUCCESS/FAIL`

### 资产扣减全部条件更新（CAS）
- 扣余额：`UPDATE wallet_account SET money = money - x WHERE user_id=? AND money >= x`（影响行数=1 才算成功）
- 核销券：`UPDATE user_coupon SET status=1 WHERE id=? AND status=0 AND expire_time > NOW()`
- 状态推进：`UPDATE ... SET state=:to WHERE xx_no=:no AND state=:from`

### 支付密码
`wallet-asset` 的 password 模块：BCrypt 慢哈希（强度 12）+ 连续错 5 次 / 当日 10 次锁 10 分钟 +
校验通过签发一次性授权票据（TTL 300 秒），提交支付时原子消费并复核用户/订单/金额。

## 技术栈

Java 25 · Spring Boot 4.0.x · MyBatis-Plus 3.5.x（`mybatis-plus-spring-boot4-starter`）·
Redisson 4.7（不用 starter，手动装配 `RedissonClient`）· MySQL 8 · springdoc-openapi · JUnit 5。
金额统一用 **long（分）**，积分数量独立 long 字段。全工程不引 Lombok（entity 手写 getter/setter，model 用 record）。

## 常用命令

```bash
# 全量构建 + 测试（需本机 127.0.0.1:6379 Redis 用于锁单测，Redis 不可用时自动跳过）
JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn test

# 只编译
JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn compile
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
JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn spring-boot:run
# 建表：mysql -uroot wallet < wallet-app/src/main/resources/sql/schema.sql
# swagger-ui: http://localhost:8080/swagger-ui.html
```

Mock 渠道配置（`application.yml` 的 `wallet.mock.*`）：下单/查询延迟、下单失败率、下单后 N 秒自动回调（0=手工触发）、退款强制失败开关。

## 接入真实渠道

在 `wallet-pay` 的 `mock/` 旁实现一个类，实现 `com.wallet.channel.action.*` 的动作接口
（`PayAction` 必选，`QueryAction` / `RefundAction` / `CancelAction` / `CallbackAction` / `ConfirmAction` 按渠道能力选实现），
在装配配置里 `ChannelKit.builder().addChannel(...)` 注册即可。参见 `antom/` 目录的 Antom 渠道示范。

# pay-channel-sdk 多渠道支付编排 SDK 接入指南

多渠道支付的**编排内核**：渠道×动作注册表、支付/退款状态机、幂等编排（发起/回调/查询/退款/取消/二段扣款）与支付日志钩子。
纯 Java jar，零 Spring/Servlet/第三方支付 SDK 依赖，可被任意 Java 8+ 宿主引入。
提炼自 BIZ-CRMEB-JAVA 的 `crmeb-pay-service` 模块，并修正了原模块的四处已知缺陷（见第 13 节）。

## 1. 能力与边界

**SDK 负责**：
- 支付全流程编排：发起支付（含上一笔未支付交易的查证与关闭）、异步回调、主动查询、退款、取消、二段式扣款确认；
- 状态一致性：支付/退款状态机（转换表式）+ 条件更新（CAS）+ 分布式锁，保证支付成功事件对同一交易**至多发布一次**；
- 渠道能力路由：按 (渠道, 动作) 注册表分发，缺失组合抛明确异常；
- 手续费策略挂载点、渠道调用日志采集（请求/响应/耗时/异常）。

**宿主负责**：
- 渠道协议实现（下单报文、验签、退款接口——即渠道 Provider）；
- 交易单/退款单持久化（含条件更新 SQL）、分布式锁、支付成功后的业务履约；
- HTTP 层（Controller）、登录态与鉴权、轮询任务调度、错误码文案（i18n）。

## 2. 架构总览

```
宿主 Controller / 任务
        │
        ▼
PayChannelKernel ──► PaymentOrchestrator（编排：锁 + 状态机 + CAS + 日志）
        │                    │
        │                    ├──► ProviderRegistry ──► 渠道 Provider（宿主实现，策略模式）
        │                    │         (channelCode × action，构建期校验完整性)
        │                    ├──► PayStateMachine / RefundStateMachine（转换表状态机）
        │                    └──► 宿主 SPI：PayOrderRepository / RefundOrderRepository /
        │                              PayLockManager / PayEventListener / PayLogSink / FeePolicy
```

支付状态机：`INIT --PAY_REQUEST--> PAYING --PAY_SUCCESS--> SUCCESS`；`PAYING --PAY_FAIL--> FAIL`；`INIT/PAYING --CLOSE--> CLOSED`。
退款状态机：`INIT --REFUND_REQUEST--> REFUNDING --REFUND_SUCCESS/REFUND_FAIL--> SUCCESS/FAIL`。
终态无出边，`canTransition` 是全部幂等判断的基础。

## 3. Maven 坐标与依赖

```xml
<dependency>
    <groupId>com.zbkj</groupId>
    <artifactId>pay-channel-sdk</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

传递依赖仅：`slf4j-api`、`jackson-databind`（日志序列化）。Lombok 为 provided，不传递。

## 4. 核心概念

- **channelCode**：渠道编码字符串（如 `"JKOPAY"`、`"ANTOM"`）。SDK 不内置渠道枚举——各商城（大陆/港澳台/海外）渠道集合不同，枚举由宿主自定义，保证与 Provider 的 `channelCode()` 一致即可。
- **动作（PayActionEnum）**：PAY / QUERY / REFUND / CANCEL / CALLBACK / EXECUTE_PAYMENT。渠道通过"实现对应 Provider 接口"声明能力，**PAY 必选**，其余按渠道实际能力选实现；一个类可同时实现多个动作接口。
- **能力查询**：`kernel.supports(channelCode, action)`；`PayResult.queryable` 告知宿主该交易是否应进轮询队列。

## 5. 新宿主接入清单

1. pom 引入本 SDK；
2. 建表或复用现有表：交易单表（对应 CRMEB `eb_pay_order`）、退款单表（`eb_pay_refund_order`）、支付日志表（`eb_pay_info`，可选）；
3. 实现 4 个必选宿主 SPI（第 6 节）+ 按需实现 2 个可选 SPI；
4. 为每个渠道实现 Provider（第 7 节；CRMEB 系宿主可参考 `crmeb-pay-service` 的 `service/impl/payment/{渠道}` 目录改写）；
5. Spring 装配 Kernel 单例（第 8 节）；
6. 编写回调 Controller 与轮询任务（第 9、10 节）;
7. 补齐错误码文案（第 12 节，12 个 i18n 键）。

## 6. 宿主 SPI 契约（4 必选 + 2 可选）

| SPI | 必选 | 语义 | 关键契约 |
|---|---|---|---|
| `PayOrderRepository` | ✅ | 交易单持久化 | `create` 生成并回填 outTradeNo，初始 INIT；**`transitionState` 必须实现为条件更新**（`UPDATE ... WHERE out_trade_no=? AND pay_status=:from`，返回影响行数==1），这是幂等与并发安全的基石；`applyRefund` 原子扣减可退金额 |
| `RefundOrderRepository` | ✅ | 退款单持久化 | `transitionState` 契约同上 |
| `PayLockManager` | ✅ | 分布式锁 | 阻塞获取，失败抛异常；宿主适配 Lock4j / Redisson 均可 |
| `PayEventListener` | ✅ | 支付/退款成功事件 | `onPaySuccess` 对同一交易至多回调一次；实现内抛异常会向上传播——重业务建议转投 MQ |
| `PayLogSink` | 可选 | 渠道调用日志出口 | SDK 已 try/catch 兜底；建议独立事务或异步落库；缺省仅打 slf4j |
| `FeePolicy` | 可选 | 渠道手续费 | 按币种最小单位取整（勿硬编码 `setScale(0)`）；缺省不加费 |

## 7. 渠道 Provider SPI 契约

| 接口 | 动作 | 契约 |
|---|---|---|
| `PayProvider` | PAY（必选） | 渠道下单，返回前端拉起支付参数（类型渠道自定）；失败抛 `CHANNEL_INVOKE_ERROR` |
| `QueryProvider` | QUERY | 实现了才参与：上一笔交易查证、回调二次查证、轮询兜底 |
| `RefundProvider` | REFUND | 业务性拒绝用 `success=false + failReason`，仅系统异常才抛异常 |
| `CancelProvider` | CANCEL | 渠道侧本就无此交易也返回 true；`pay` 携带 lastOutTradeNo 时会用到 |
| `CallbackProvider` | CALLBACK | **必须在方法内完成验签**（基于原始 body），失败抛 `CALLBACK_VERIFY_FAILED`；`ackBody` 必填；不信任回调报文的渠道设 `reQueryRequired=true`（须同时实现 QueryProvider） |
| `ExecutePaymentProvider` | EXECUTE_PAYMENT | 二段式扣款（PayPal 类）；编排层已做状态幂等，不会重复调用 |

构建期校验（`PayChannelKernel.build()`）：渠道缺 PAY、(渠道,动作) 重复注册、Provider 未实现任何动作接口——全部启动失败，错误信息含渠道与类名。

## 8. Spring 装配示例（宿主 @Configuration）

```java
@Configuration
public class PayChannelConfiguration {

    /** 所有渠道 Provider 声明为 Spring Bean，容器收集后一次性注册 */
    @Bean
    public PayChannelKernel payChannelKernel(List<ChannelProvider> providers,
        PayOrderRepositoryAdapter payOrderRepository, RefundOrderRepositoryAdapter refundOrderRepository,
        Lock4jLockManagerAdapter lockManager, PayEventListenerAdapter eventListener,
        PayLogSinkAdapter logSink, ConfigCenterFeePolicy feePolicy) {
        return PayChannelKernel.builder()
            .providers(providers)
            .payOrderRepository(payOrderRepository)
            .refundOrderRepository(refundOrderRepository)
            .lockManager(lockManager)
            .eventListener(eventListener)
            .logSink(logSink)
            .feePolicy(feePolicy)
            .build();   // 配置错误（渠道缺动作/重复注册）在启动期失败
    }
}
```

锁适配示例（Redisson）：

```java
@Component
public class RedissonLockManagerAdapter implements PayLockManager {
    @Resource private RedissonClient redisson;

    @Override
    public <T> T withLock(String lockKey, Supplier<T> action) {
        RLock lock = redisson.getLock(lockKey);
        if (!tryLock(lock)) {
            throw new IllegalStateException("获取支付锁失败: " + lockKey);
        }
        try { return action.get(); } finally { lock.unlock(); }
    }
}
```

## 9. 渠道 Provider 示例（JKO 风格骨架）

```java
@Component
public class JkoPayProvider implements PayProvider, QueryProvider, RefundProvider, CancelProvider, CallbackProvider {

    @Override
    public String channelCode() { return "JKOPAY"; }

    @Override
    public Object pay(PayCommand command, PayOrderSnapshot payOrder) {
        JkoOrderRequest req = new JkoOrderRequest();
        req.setPlatformOrderId(payOrder.getOutTradeNo());
        req.setTotalPrice(command.getPayAmount());
        // ... 组装 result_url（回调）与 result_display_url（跳转）
        JkoOrderResponse resp = jkoClient.createOrder(req);   // HMAC 签名在 http client 层
        if (!resp.isSuccess()) {
            throw new PayChannelException(PayErrorCode.CHANNEL_INVOKE_ERROR, resp.getMessage());
        }
        return new JkoPayPayload(resp.getPaymentUrl(), resp.getQrImg(), resp.getQrTimeout());
    }

    @Override
    public CallbackResult handleCallback(CallbackCommand command) {
        if (!JkoSign.verify(command.getBody(), command.getHeaders().get("digest"), secretKey)) {
            throw new PayChannelException(PayErrorCode.CALLBACK_VERIFY_FAILED, "JKO digest mismatch");
        }
        JkoCallback cb = parse(command.getBody());
        return CallbackResult.builder()
            .paid(cb.isSuccess())
            .thirdOutTradeNo(cb.getTradeNo())
            .reQueryRequired(true)              // JKO 回调建议以主动查询为准
            .ackBody("{\"result\":\"success\"}")
            .build();
    }

    // query / refund / cancel 同理，按 JKO 协议实现
}
```

## 10. 回调 Controller 与轮询任务示例

```java
@RestController
@RequestMapping("api/pay/callback")
public class PayCallbackController {

    @Resource private PayChannelKernel kernel;

    @PostMapping("/{channel}/{orderNo}/{outTradeNo}")
    public String callback(@PathVariable String channel, @PathVariable String orderNo,
        @PathVariable String outTradeNo, @RequestBody String rawBody, HttpServletRequest request) {
        return kernel.payment().handleCallback(CallbackCommand.builder()
            .channelCode(channel).orderNo(orderNo).outTradeNo(outTradeNo)
            .httpMethod(request.getMethod()).requestUri(request.getRequestURI())
            .headers(extractHeaders(request)).body(rawBody)
            .build());
    }
}
```

轮询兜底（对应 crmeb 的 QueryPayResultTask）：

```java
// 发起支付后入队——只对 queryable 的交易入队（PayPal 类渠道不入队，避免无效轮询）
PayResult result = kernel.payment().pay(payCommand);
if (result.isQueryable()) {
    queue.push(new QueryTask(channel, orderNo, result.getOutTradeNo(), expireAt(15, MINUTES)));
}

// 任务消费：true=已支付或已终态（出队），false=未支付（重新入队直至过期）
boolean done = kernel.payment().queryPayResult(QueryCommand.builder()
    .channelCode(task.channel).orderNo(task.orderNo).outTradeNo(task.outTradeNo).build());
```

## 11. 幂等与并发语义（宿主可依赖的保证）

- 回调、查询、扣款确认共用同一把结果锁（`paychannel:result:{channel}:{orderNo}`），三条路径互斥；
- 状态推进 = 锁 + `canTransition` + 仓储条件更新（CAS）三重防护：重复回调 / 回调与轮询并发，`onPaySuccess` 只触发一次；
- 已终态订单：回调幂等返回 ackBody，查询返回 true 且不再调渠道，扣款确认按终态返回且不再调渠道；
- 退款失败（渠道拒绝或抛异常）：退款单必落 FAIL 留痕，可退金额不扣减；
- SDK 内不开数据库事务、渠道 HTTP 调用不在任何事务中——宿主 SPI 的每次写入都应是独立短事务，**不要**把 `pay()`/`refund()` 整体包进 `@Transactional`。

## 12. 错误码（12 个，值即宿主 i18n 资源键）

`PayChannelException.getErrorCode().getCode()` 返回错误码字符串；与 CRMEB 存量资源键同名的可直接复用文案：

`PAY_PARAM_INVALID` / `PAYMENT_ACTION_UNSUPPORTED` / `ORDER_HAS_PAID` / `ORDER_DOES_NOT_EXIST` /
`ORDER_NOT_PAID` / `ORDER_REFUND_FINISH` / `ORDER_PAYING_WAITE_REFUND` / `ILLEGAL_CHANGE_STATUS` /
`CALLBACK_VERIFY_FAILED` / `CALLBACK_QUERY_UNPAID` / `REFUND_AMOUNT_INVALID` / `CHANNEL_INVOKE_ERROR`

## 13. 从 crmeb-pay-service 迁移映射与设计修正

| crmeb-pay-service | 本 SDK | 说明 |
|---|---|---|
| `PaymentServiceImpl` | `PaymentOrchestrator` | 同名方法一一对应（callbackCheck→handleCallback） |
| `PaymentServiceFactory` | `ProviderRegistry` | **修正：构建期完整性校验 + 缺失组合抛类型化异常**（原为运行时 NPE，PayPal 订单曾因缺 QUERY 在轮询里 NPE 循环） |
| `PayEnum` | 宿主自定义 channelCode | SDK 不内置渠道枚举，多商城渠道集合互相独立 |
| `PayAction` 等三段式模板 + `@PayLogAnnotation` 切面 | Provider 单方法 + 编排层日志采集 | **修正：日志不再依赖跨模块切面与 ThreadLocal**，模板步骤职责越位问题（验签写在 buildParams 等）从接口设计上消除 |
| `PayStatusMachine`/`RefundStatusMachine` | `PayStateMachine`/`RefundStateMachine` | **修正：退款失败分支源状态误用 INIT 的缺陷**（原失败路径必抛"非法状态变更"且无痕回滚） |
| `executePayment` 直写状态 | 状态机幂等 + CAS | **修正：重复扣款确认不再重复调渠道、重复发事件** |
| `@Transactional` 包渠道 HTTP | 无事务 + 条件更新 | **修正：消除长事务占连接、渠道异常连交易单一起回滚的问题** |
| `setPayFee` if/else | `FeePolicy` SPI | 费率与取整规则宿主自定，支持按币种小数位 |
| `FrontLocalVar.PAID` ThreadLocal | `ORDER_HAS_PAID` 异常 | 取消时渠道侧已支付改为显式异常表达 |

状态与错误码枚举名和 CRMEB 存量库表值保持一致（INIT/PAYING/SUCCESS/FAIL/CLOSED 等），存量数据可直接映射。

## 14. 构建与测试

```bash
mvn compile test-compile        # Java 8+，依赖版本与 CRMEB 宿主 BOM 对齐
```

单元测试 31 个（状态机、注册表校验、编排器幂等/退款失败留痕/渠道能力缺失等回归用例），
IDE 内直接运行，或命令行 `java -cp ... org.junit.runner.JUnitCore <测试类>`。
注意：当前公司 Nexus 镜像（repo.rebornnet.cn）对 maven-surefire-plugin 等部分公共构件返回 403，
`mvn test` 需先解决镜像授权或临时切换公共镜像。

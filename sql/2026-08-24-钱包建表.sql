-- wallet 钱包工程建表脚本（MySQL 8，utf8mb4）
-- 金额一律 BIGINT 单位分；状态一律 VARCHAR 存枚举名。

-- 1. 钱包账户（余额 + 积分合一）
CREATE TABLE IF NOT EXISTS wallet_account (
  id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id      BIGINT       NOT NULL COMMENT '用户ID（header X-Uid）',
  money        BIGINT       NOT NULL DEFAULT 0 COMMENT '余额，单位分',
  point        BIGINT       NOT NULL DEFAULT 0 COMMENT '积分数量',
  status       TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0冻结',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='钱包账户';

-- 2. 余额流水（biz_no + type 幂等）
CREATE TABLE IF NOT EXISTS money_log (
  id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id       BIGINT      NOT NULL,
  biz_no        VARCHAR(40) NOT NULL COMMENT '业务唯一号：分段号/退款分段号/充值流水号',
  type          VARCHAR(20) NOT NULL COMMENT 'RECHARGE/PAY/ROLLBACK/REFUND',
  change_amount BIGINT      NOT NULL COMMENT '变动金额分，支出为负',
  after_amount  BIGINT      NOT NULL COMMENT '变动后余额快照',
  order_no      VARCHAR(32)          COMMENT '关联支付单号',
  remark        VARCHAR(255),
  create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_biz (biz_no, type),
  KEY idx_user (user_id, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='余额流水';

-- 3. 积分流水（biz_no + type 幂等）
CREATE TABLE IF NOT EXISTS point_log (
  id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id      BIGINT      NOT NULL,
  biz_no       VARCHAR(40) NOT NULL,
  type         VARCHAR(20) NOT NULL COMMENT 'ADD/PAY/ROLLBACK/REFUND',
  change_count BIGINT      NOT NULL COMMENT '变动积分，支出为负',
  after_count  BIGINT      NOT NULL COMMENT '变动后积分快照',
  order_no     VARCHAR(32),
  remark       VARCHAR(255),
  create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_biz (biz_no, type),
  KEY idx_user (user_id, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='积分流水';

-- 4. 优惠券模板（满减券）
CREATE TABLE IF NOT EXISTS coupon (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(64) NOT NULL,
  face_amount BIGINT      NOT NULL COMMENT '面额分',
  min_amount  BIGINT      NOT NULL DEFAULT 0 COMMENT '使用门槛分，0不限',
  total_count INT         NOT NULL DEFAULT 0 COMMENT '发行量，0不限',
  taken_count INT         NOT NULL DEFAULT 0 COMMENT '已领取量',
  expire_time DATETIME    NOT NULL,
  status      TINYINT     NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='券模板';

-- 5. 用户券
CREATE TABLE IF NOT EXISTS user_coupon (
  id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id      BIGINT      NOT NULL,
  coupon_id    BIGINT      NOT NULL,
  name         VARCHAR(64) NOT NULL COMMENT '快照',
  face_amount  BIGINT      NOT NULL COMMENT '快照',
  min_amount   BIGINT      NOT NULL COMMENT '快照',
  status       TINYINT     NOT NULL DEFAULT 0 COMMENT '0未用 1已用 2失效',
  use_order_no VARCHAR(32)          COMMENT '核销支付单号',
  use_time     DATETIME,
  expire_time  DATETIME    NOT NULL,
  create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user (user_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户券';

-- 6. 支付密码（无记录 = 未设置）
CREATE TABLE IF NOT EXISTS pay_password (
  id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id       BIGINT      NOT NULL,
  password_hash VARCHAR(60) NOT NULL COMMENT 'BCrypt 强度12',
  status        VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  version       INT         NOT NULL DEFAULT 1 COMMENT '改密自增',
  create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='支付密码';

-- 7. 支付主单
CREATE TABLE IF NOT EXISTS pay_order (
  id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  order_no          VARCHAR(32) NOT NULL COMMENT '钱包支付单号',
  app_id            VARCHAR(32) NOT NULL DEFAULT 'DEFAULT' COMMENT '来源商城/接入方（多商城预留）',
  biz_order_no      VARCHAR(64) NOT NULL COMMENT '外部业务单号',
  user_id           BIGINT      NOT NULL,
  total_amount      BIGINT      NOT NULL COMMENT '应付总额分 = sum(分段金额)',
  currency          VARCHAR(8)  NOT NULL DEFAULT 'TWD',
  state             VARCHAR(16) NOT NULL DEFAULT 'INIT' COMMENT 'INIT/PAYING/SUCCESS/FAIL/CLOSED',
  expire_time       DATETIME    NOT NULL COMMENT '超时关单时间',
  pay_time          DATETIME,
  close_time        DATETIME,
  refundable_amount BIGINT      NOT NULL DEFAULT 0 COMMENT '剩余可退分',
  refunded_amount   BIGINT      NOT NULL DEFAULT 0,
  fail_reason       VARCHAR(255),
  create_time       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_order (order_no),
  UNIQUE KEY uk_app_biz (app_id, biz_order_no) COMMENT '同接入方业务单号防重复建单',
  KEY idx_user (user_id, id),
  KEY idx_close (state, expire_time) COMMENT '关单任务扫描'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='支付主单';

-- 8. 支付分段（拆分支付核心；三方段 part_no 即渠道 outTradeNo）
CREATE TABLE IF NOT EXISTS pay_part (
  id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  part_no         VARCHAR(32) NOT NULL COMMENT '分段号',
  order_no        VARCHAR(32) NOT NULL,
  user_id         BIGINT      NOT NULL,
  pay_type        VARCHAR(16) NOT NULL COMMENT 'COUPON/POINT/MONEY/CHANNEL',
  amount          BIGINT      NOT NULL COMMENT '本段抵扣金额分',
  point_count     BIGINT               COMMENT '积分段消耗积分数',
  user_coupon_id  BIGINT               COMMENT '券段：用户券ID',
  channel_code    VARCHAR(32)          COMMENT '三方段：渠道码',
  third_no        VARCHAR(64)          COMMENT '三方段：渠道侧交易号',
  channel_payload TEXT                 COMMENT '三方段：渠道支付参数（JSON）',
  state           VARCHAR(16) NOT NULL DEFAULT 'INIT'
                  COMMENT 'INIT/PAYING/SUCCESS/FAIL/CLOSED/ROLLBACK',
  refunded_amount BIGINT      NOT NULL DEFAULT 0 COMMENT '本段已退分',
  pay_time        DATETIME,
  create_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_part (part_no),
  KEY idx_order (order_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='支付分段';

-- 9. 退款主单
CREATE TABLE IF NOT EXISTS refund_order (
  id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  refund_no     VARCHAR(32) NOT NULL,
  order_no      VARCHAR(32) NOT NULL,
  user_id       BIGINT      NOT NULL,
  refund_amount BIGINT      NOT NULL COMMENT '申请退款总额分',
  refund_point  BIGINT      NOT NULL DEFAULT 0 COMMENT '实际返还积分数',
  coupon_back   TINYINT     NOT NULL DEFAULT 0 COMMENT '是否返还了券（仅整单全退）',
  state         VARCHAR(16) NOT NULL DEFAULT 'INIT' COMMENT 'INIT/SUCCESS/FAIL',
  reason        VARCHAR(255),
  finish_time   DATETIME,
  create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_refund (refund_no),
  KEY idx_order (order_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='退款单';

-- 10. 退款分段明细
CREATE TABLE IF NOT EXISTS refund_part (
  id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  refund_part_no    VARCHAR(40) NOT NULL COMMENT '退款分段号，兼资产流水幂等 biz_no',
  refund_no         VARCHAR(32) NOT NULL,
  part_no           VARCHAR(32) NOT NULL COMMENT '对应支付分段',
  pay_type          VARCHAR(16) NOT NULL,
  amount            BIGINT      NOT NULL COMMENT '本段退款分',
  point_count       BIGINT               COMMENT '积分段返还积分数',
  channel_refund_no VARCHAR(64)          COMMENT '三方段：渠道退款流水号',
  state             VARCHAR(16) NOT NULL DEFAULT 'INIT' COMMENT 'INIT/REFUNDING/SUCCESS/FAIL',
  create_time       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_no (refund_part_no),
  KEY idx_refund (refund_no),
  KEY idx_part (part_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='退款分段明细';

-- 11. 渠道调用日志
CREATE TABLE IF NOT EXISTS channel_log (
  id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  channel_code  VARCHAR(32) NOT NULL,
  action        VARCHAR(16) NOT NULL COMMENT 'PAY/QUERY/REFUND/CANCEL/CALLBACK/CONFIRM',
  order_no      VARCHAR(32),
  out_trade_no  VARCHAR(32),
  request_json  TEXT,
  response_json TEXT,
  error_msg     VARCHAR(500),
  cost_ms       INT         NOT NULL DEFAULT 0,
  trace_id      VARCHAR(64)          COMMENT '链路追踪ID，与应用日志/响应头 X-Trace-Id 对应',
  create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_order (order_no),
  KEY idx_trace (trace_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='渠道调用日志';

-- 支付渠道配置表：渠道商户密钥等敏感配置落库，改库即生效（服务侧缓存 TTL 30 秒），无需改 yml 重启。
-- 生产建议对 config_json 做列级加密或接 KMS 后再写入。
CREATE TABLE IF NOT EXISTS channel_config (
  id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  channel_code VARCHAR(32)  NOT NULL COMMENT '渠道编码：ANTOM 等（MOCK 联调渠道走 yml 不入库）',
  enabled      TINYINT      NOT NULL DEFAULT 0 COMMENT '1启用 0停用',
  config_json  TEXT         NOT NULL COMMENT '渠道自定义配置 JSON（字段由各渠道的 *Config record 定义）',
  remark       VARCHAR(255)          COMMENT '备注',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_channel (channel_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付渠道配置';

-- Antom（支付宝国际）配置模板：填好密钥后把 enabled 置 1 即启用
INSERT IGNORE INTO channel_config (channel_code, enabled, config_json, remark) VALUES
('ANTOM', 0, JSON_OBJECT(
  'gateway', 'https://globalapi.alipay.com',
  'clientId', '',
  'merchantPrivateKey', '',
  'alipayPublicKey', '',
  'baseUrl', 'http://localhost:8080',
  'expiryMinutes', 10
), 'Antom 支付宝国际，填好商户密钥后 enabled 置 1');

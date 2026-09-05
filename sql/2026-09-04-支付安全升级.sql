-- 现有 wallet 数据库升级：在执行应用发布前运行。
ALTER TABLE pay_password
  ADD COLUMN security_version INT NOT NULL DEFAULT 1 COMMENT '支付安全全局版本' AFTER version,
  ADD COLUMN password_set_at DATETIME NULL COMMENT '首次设置时间' AFTER security_version,
  ADD COLUMN password_updated_at DATETIME NULL COMMENT '最近修改或重置时间' AFTER password_set_at;

UPDATE pay_password
SET password_set_at = COALESCE(password_set_at, create_time),
    password_updated_at = COALESCE(password_updated_at, update_time)
WHERE password_set_at IS NULL OR password_updated_at IS NULL;

CREATE TABLE IF NOT EXISTS pay_biometric_credential (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  credential_id VARCHAR(64) NOT NULL,
  registration_id VARCHAR(64) NOT NULL,
  uid BIGINT NOT NULL,
  platform VARCHAR(16) NOT NULL,
  password_version INT NOT NULL,
  security_version INT NOT NULL,
  public_key TEXT NOT NULL,
  algorithm VARCHAR(32) NOT NULL DEFAULT 'EC_P256_SHA256',
  key_attestation_status VARCHAR(16) NOT NULL DEFAULT 'UNVERIFIED',
  app_integrity_status VARCHAR(16) NOT NULL DEFAULT 'UNVERIFIED',
  status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
  disabled_reason VARCHAR(32),
  registered_at DATETIME,
  last_used_at DATETIME,
  disabled_at DATETIME,
  UNIQUE KEY uk_pay_bio_credential (credential_id),
  UNIQUE KEY uk_pay_bio_registration (registration_id),
  KEY idx_pay_bio_uid_status (uid, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生物支付公钥凭证';

CREATE TABLE IF NOT EXISTS pay_security_audit (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  event_type VARCHAR(64) NOT NULL,
  uid BIGINT NOT NULL,
  order_no VARCHAR(64),
  credential_id VARCHAR(64),
  client_type VARCHAR(16),
  app_version VARCHAR(32),
  result VARCHAR(16) NOT NULL,
  reason_code VARCHAR(64),
  amount DECIMAL(18,2),
  currency VARCHAR(8),
  ip VARCHAR(64),
  user_agent_digest VARCHAR(64),
  occurred_at DATETIME NOT NULL,
  KEY idx_pay_security_uid_time (uid, occurred_at),
  KEY idx_pay_security_order (order_no),
  KEY idx_pay_security_credential (credential_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付安全审计';

package com.wallet.account.service.password;

import com.wallet.account.config.PasswordProperties;
import com.wallet.account.entity.PayPassword;
import com.wallet.account.error.AccountError;
import com.wallet.common.error.BizException;
import com.wallet.contract.account.PasswordService;
import org.redisson.api.RBucket;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 支付密码服务，实现 {@link PasswordService} 契约。
 *
 * <p>三要素：</p>
 * <ul>
 *   <li><b>BCrypt 慢哈希</b>（强度默认 12），库表不存明文；</li>
 *   <li><b>错误锁定</b>（Redis 计数）：连续错 N 次或当日错 M 次锁定 10 分钟；</li>
 *   <li><b>一次性授权票据</b>：校验通过签发票据（默认 TTL 300 秒），提交支付时原子消费并复核用户/订单/金额。</li>
 * </ul>
 */
@Service
public class PasswordServiceImpl implements PasswordService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PasswordStore store;
    private final RedissonClient redisson;
    private final PasswordProperties props;
    private final BCryptPasswordEncoder encoder;

    public PasswordServiceImpl(PasswordStore store, RedissonClient redisson, PasswordProperties props) {
        this.store = store;
        this.redisson = redisson;
        this.props = props;
        this.encoder = new BCryptPasswordEncoder(props.getBcryptStrength());
    }

    /** 设置/重置支付密码。已设置时必须校验旧密码。 */
    @Override
    @Transactional
    public void set(Long userId, String password, String oldPassword) {
        PayPassword exist = store.findByUserId(userId);
        if (exist == null) {
            PayPassword passwordRow = new PayPassword();
            passwordRow.setUserId(userId);
            passwordRow.setPasswordHash(encoder.encode(password));
            passwordRow.setStatus("ENABLED");
            passwordRow.setVersion(1);
            store.insert(passwordRow);
            return;
        }
        if (oldPassword == null || !encoder.matches(oldPassword, exist.getPasswordHash())) {
            throw new BizException(AccountError.PASSWORD_WRONG, "旧密码不正确");
        }
        store.updateHash(userId, encoder.encode(password), exist.getVersion() + 1);
    }

    /**
     * 校验支付密码并签发一次性授权票据。
     *
     * @return 票据
     */
    @Override
    public String verifyAndIssue(Long userId, String password, String orderNo, long amount) {
        if (isLocked(userId)) {
            throw new BizException(AccountError.PASSWORD_LOCKED, "userId=" + userId);
        }
        PayPassword passwordRow = store.findByUserId(userId);
        if (passwordRow == null || !"ENABLED".equals(passwordRow.getStatus())) {
            throw new BizException(AccountError.PASSWORD_NOT_SET, "userId=" + userId);
        }
        if (!encoder.matches(password, passwordRow.getPasswordHash())) {
            recordFail(userId);
            throw new BizException(AccountError.PASSWORD_WRONG, "userId=" + userId);
        }
        clearFail(userId);
        // 票据是安全凭证，必须高熵不可预测（UUID 底层 SecureRandom）；
        // 不能用 IdMaker（时间戳+4位随机，可被窗口内枚举）
        String ticket = "TK" + UUID.randomUUID().toString().replace("-", "");
        String value = userId + ":" + orderNo + ":" + amount;
        redisson.getBucket(ticketKey(ticket))
            .set(value, Duration.ofSeconds(props.getTicketTtlSeconds()));
        return ticket;
    }

    /**
     * 原子消费一次性票据并复核用户/订单/金额三者一致。
     */
    @Override
    public void consumeTicket(String ticket, Long userId, String orderNo, long amount) {
        RBucket<String> bucket = redisson.getBucket(ticketKey(ticket));
        String value = bucket.getAndDelete(); // 原子消费：并发只有一个能拿到
        if (value == null) {
            throw new BizException(AccountError.TICKET_INVALID, "票据不存在或已被使用");
        }
        String[] parts = value.split(":");
        boolean matches = parts.length == 3
            && parts[0].equals(String.valueOf(userId))
            && parts[1].equals(orderNo)
            && Long.parseLong(parts[2]) == amount;
        if (!matches) {
            throw new BizException(AccountError.TICKET_INVALID, "票据与本次支付不匹配");
        }
    }

    private boolean isLocked(Long userId) {
        return redisson.getBucket(lockKey(userId)).isExists();
    }

    private void recordFail(Long userId) {
        RAtomicLong continuous = redisson.getAtomicLong(continuousKey(userId));
        long c = continuous.incrementAndGet();
        continuous.expire(Duration.ofMinutes(props.getLockMinutes())); // 滑动窗口
        RAtomicLong daily = redisson.getAtomicLong(dailyKey(userId));
        long d = daily.incrementAndGet();
        daily.expire(Duration.ofDays(1));
        if (c >= props.getMaxContinuousFail() || d >= props.getMaxDailyFail()) {
            redisson.getBucket(lockKey(userId)).set("1", Duration.ofMinutes(props.getLockMinutes()));
            continuous.delete();
        }
    }

    private void clearFail(Long userId) {
        redisson.getAtomicLong(continuousKey(userId)).delete();
        redisson.getAtomicLong(dailyKey(userId)).delete();
    }

    private String continuousKey(Long userId) {
        return "wallet:pwd:fail:continuous:" + userId;
    }

    private String dailyKey(Long userId) {
        return "wallet:pwd:fail:daily:" + LocalDate.now().format(DAY) + ":" + userId;
    }

    private String lockKey(Long userId) {
        return "wallet:pwd:lock:" + userId;
    }

    private String ticketKey(String ticket) {
        return "wallet:ticket:" + ticket;
    }
}

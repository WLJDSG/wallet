package com.wallet.security.spi;

/**
 * 纯字符串键值（Key-Value）存储端口（通常由 Redis 实现），TTL 一律为秒。
 *
 * <p>实现契约：</p>
 * <ul>
 *   <li>{@link #set} 与 {@link #setIfAbsent} 遇存储异常时返回 false，不向上抛异常；
 *       支付安全内核 会把 set 失败转为 PAY_SECURITY_UNAVAILABLE，把 setIfAbsent 失败按“占用失败”处理。</li>
 *   <li>{@link #increment} 对不存在的 key 从 0 开始累加并返回累加后的值（Redis INCRBY 语义），
 *       累加与设置过期必须在同一原子操作内完成（Redis 实现应使用 Lua 脚本）。</li>
 *   <li>值一律为原始字符串，实现不得做任何 JSON 包装或引号转义（票据序列化由 支付安全内核 完成）。</li>
 * </ul>
 */
public interface PaySecurityKeyValueStore {

    /**
     * 读取字符串值。
     *
     * @param key 键
     * @return 值，不存在时返回 null
     */
    String get(String key);

    /**
     * 写入字符串值并设置过期时间。
     *
     * @param key 键
     * @param value 值
     * @param ttlSeconds 有效秒数
     * @return 写入成功返回 true，存储异常返回 false
     */
    boolean set(String key, String value, long ttlSeconds);

    /**
     * 不存在时原子写入并设置过期时间（一次性票据与冷却占位的核心原语）。
     *
     * @param key 键
     * @param value 值
     * @param ttlSeconds 有效秒数
     * @return 占用成功返回 true；已存在或存储异常返回 false
     */
    boolean setIfAbsent(String key, String value, long ttlSeconds);

    /**
     * 删除键。
     *
     * <p>返回值是"一次性值原子消费"的原语（Redis DEL 语义）：并发删除同一键时
     * 只有一个调用方得到 true，支付安全内核 以此保证短信验证码一码一用。</p>
     *
     * @param key 键
     * @return 键存在且本次删除成功返回 true；键不存在返回 false
     */
    boolean delete(String key);

    /**
     * 原子递增并同时设置过期时间，返回递增后的值。
     *
     * <p>递增与设置过期必须原子完成：若两步分离，中途故障会留下无过期时间的计数键，
     * 频控与失败锁定计数将永不清零。</p>
     *
     * @param key 键
     * @param delta 增量，必须大于 0
     * @param ttlSeconds 有效秒数，仅大于 0 时生效
     * @return 递增后的值
     */
    long increment(String key, long delta, long ttlSeconds);
}

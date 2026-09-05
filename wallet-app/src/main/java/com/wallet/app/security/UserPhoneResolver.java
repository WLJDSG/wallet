package com.wallet.app.security;

/**
 * 从服务端可信用户资料中解析手机号。
 *
 * <p>实际接入方应用账号服务实现此接口，不得从请求头或请求体信任客户端传入的手机号。</p>
 */
@FunctionalInterface
public interface UserPhoneResolver {

    String resolve(Long userId);
}

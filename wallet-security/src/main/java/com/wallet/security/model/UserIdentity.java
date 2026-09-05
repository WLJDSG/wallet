package com.wallet.security.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前操作用户的最小身份快照。
 *
 * <p>支付安全内核 不接受客户端传入 uid，本对象必须由宿主从服务端登录态构造；
 * {@code active} 表示用户状态正常（未禁用）。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserIdentity {

    /** 用户ID。 */
    private Long uid;

    /** 用户手机号，短信身份验证使用。 */
    private String phone;

    /** 用户状态是否正常。 */
    private boolean active;

    public static UserIdentity of(Long uid, String phone, boolean active) {
        return new UserIdentity(uid, phone, active);
    }
}

package com.wallet.app.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 审计字段自动填充：createTime（插入）/ updateTime（插入+更新），一律 UTC。
 * 配合 {@code BaseEntity} 的 {@code @TableField(fill)}；已显式赋值的不覆盖（strict）。
 */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class,
            LocalDateTime.now(ZoneOffset.UTC));
    }
}

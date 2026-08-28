package com.wallet.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类：审计字段。createTime/updateTime 由 MetaObjectHandler 自动填充（UTC），
 * deleted 为逻辑删除（MyBatis-Plus {@link TableLogic}）。所有持久化实体必须继承。
 */
@Getter
@Setter
public abstract class BaseEntity implements Serializable {

    /** 创建时间（UTC，插入时自动填充） */
    @Schema(description = "创建时间（UTC）")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（UTC，插入/更新时自动填充） */
    @Schema(description = "更新时间（UTC）")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0 未删 / 1 已删 */
    @Schema(description = "逻辑删除：0 未删 / 1 已删")
    @TableLogic
    private Integer deleted;

    /** 创建人 */
    @Schema(description = "创建人")
    private String createBy;

    /** 更新人 */
    @Schema(description = "更新人")
    private String updateBy;
}

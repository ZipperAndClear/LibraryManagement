package com.zipper.librarymanagement.dto;

import lombok.Data;

import java.util.List;

/**
 * 编辑用户数据传输对象
 * <p>管理员编辑用户时提交的数据，不含密码字段（密码有单独的修改接口）</p>
 */
@Data
public class UpdateUserDTO {

    /** 用户 ID（必填，用于定位要更新的用户） */
    private Long id;

    /** 真实姓名 */
    private String realName;

    /** 电子邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 角色 ID 列表（传入 null 表示不修改角色，传入空列表表示清空角色） */
    private List<Long> roleIds;
}

package com.elink.evco.platform.iam.vo;

/**
 * 可授予角色选项；仅返回当前操作者可授予的角色。
 *
 * @param id 角色 ID。
 * @param name 角色名称。
 * @param scope 角色层级：platform/tenant。
 * @param description 角色描述。
 */
public record RoleOptionVO(String id, String name, String scope, String description) {}

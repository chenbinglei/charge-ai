package com.elink.evco.platform.iam.vo;

import java.util.List;

/**
 * 当前用户档案响应；菜单树由后端按权限推导，前端仅渲染。
 *
 * @param userId 用户 ID。
 * @param username 登录名。
 * @param displayName 展示姓名。
 * @param roles 角色名称集合。
 * @param permissions 全部权限码（resource:read/write）。
 * @param deploymentMode 部署模式：IOT_COLLABORATIVE/STANDALONE。
 * @param menus 可见菜单树。
 */
public record ProfileVO(
        String userId,
        String username,
        String displayName,
        List<String> roles,
        List<String> permissions,
        String deploymentMode,
        List<MenuNodeVO> menus) {}

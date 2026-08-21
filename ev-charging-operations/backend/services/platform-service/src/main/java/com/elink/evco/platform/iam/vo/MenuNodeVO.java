package com.elink.evco.platform.iam.vo;

import java.util.List;

/**
 * 可见菜单树节点；父级菜单仅在其下存在可见叶子时返回（不出现空菜单）。
 *
 * @param key 菜单标识（kebab-case）。
 * @param title 菜单中文名；与《充电运营平台菜单架构》一致。
 * @param path 叶子菜单路由；父级菜单为 null。
 * @param permission 叶子菜单入口权限码（resource:read）；父级菜单为 null。
 * @param children 子菜单集合；叶子菜单为空集合。
 */
public record MenuNodeVO(
        String key, String title, String path, String permission, List<MenuNodeVO> children) {}

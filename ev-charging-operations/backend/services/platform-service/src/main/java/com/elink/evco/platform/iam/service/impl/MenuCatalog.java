package com.elink.evco.platform.iam.service.impl;

import java.util.List;

/**
 * 菜单目录（W2 范围）：与《充电运营平台菜单架构 v4.1》一二级三级命名完全对齐。
 *
 * <p>叶子菜单携带入口权限码（resource:read）；W3-W11 交付周页面按周追加， 待前端骨架页确定后评估是否迁移入 iam_menu 表。
 */
public final class MenuCatalog {

    /** 工具类禁止实例化。 */
    private MenuCatalog() {}

    /** 菜单定义节点；叶子（permission 非空）无子级，父级节点无路由。 */
    record MenuDef(
            String key, String title, String path, String permission, List<MenuDef> children) {}

    /** W2 菜单树（系统与配置、客户与权益）。 */
    static final List<MenuDef> TREE =
            List.of(
                    group(
                            "system-config",
                            "系统与配置",
                            group(
                                    "permission-management",
                                    "权限管理",
                                    leaf(
                                            "platform-admin",
                                            "平台管理员",
                                            "/system-config/permission-management/platform-admin",
                                            "iam_user:read"),
                                    leaf(
                                            "tenant-permission",
                                            "租户权限分配",
                                            "/system-config/permission-management/tenant-permission",
                                            "tenant_permission:read"),
                                    leaf(
                                            "marketing-permission",
                                            "营销权限",
                                            "/system-config/permission-management/marketing-permission",
                                            "mkt_permission:read"),
                                    leaf(
                                            "auth-audit",
                                            "授权审计",
                                            "/system-config/permission-management/auth-audit",
                                            "auth_audit:read")),
                            group(
                                    "app-content",
                                    "应用与内容",
                                    leaf(
                                            "mini-program",
                                            "小程序管理",
                                            "/system-config/app-content/mini-program",
                                            "mini_program:read")),
                            group(
                                    "trade-config",
                                    "交易配置",
                                    leaf(
                                            "payment-channel",
                                            "支付渠道与路由",
                                            "/system-config/trade-config/payment-channel",
                                            "payment_channel:read"),
                                    leaf(
                                            "settlement-channel",
                                            "结算渠道配置",
                                            "/system-config/trade-config/settlement-channel",
                                            "settlement_channel:read")),
                            group(
                                    "base-setting",
                                    "基础设置",
                                    leaf(
                                            "platform-setting",
                                            "基础设置",
                                            "/system-config/base-setting/platform-setting",
                                            "platform_setting:read"))),
                    group(
                            "customer-equity",
                            "客户与权益",
                            group(
                                    "personal-customer",
                                    "个人客户",
                                    leaf(
                                            "personal-user",
                                            "个人用户",
                                            "/customer-equity/personal-customer/personal-user",
                                            "personal_user:read"))));

    /**
     * 构造叶子菜单定义。
     *
     * @param key 菜单标识。
     * @param title 菜单中文名。
     * @param path 三级路由。
     * @param permission 入口权限码。
     * @return 叶子菜单定义。
     */
    private static MenuDef leaf(String key, String title, String path, String permission) {
        return new MenuDef(key, title, path, permission, List.of());
    }

    /**
     * 构造父级菜单定义。
     *
     * @param key 菜单标识。
     * @param title 菜单中文名。
     * @param children 子菜单集合。
     * @return 父级菜单定义。
     */
    private static MenuDef group(String key, String title, MenuDef... children) {
        return new MenuDef(key, title, null, null, List.of(children));
    }
}

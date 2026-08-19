package com.elink.evco.platform.iam.service.impl;

import com.elink.evco.platform.iam.service.MenuService;
import com.elink.evco.platform.iam.vo.MenuNodeVO;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

/** 菜单推导实现：基于 MenuCatalog 静态目录按权限码递归裁剪可见树。 */
@Service
public class MenuServiceImpl implements MenuService {

    /** 构造菜单推导实现。 */
    public MenuServiceImpl() {}

    /**
     * 按权限集合推导可见菜单树。
     *
     * <p>步骤：逐棵一级菜单递归裁剪，父级无可见叶子时整棵丢弃。
     *
     * @param permissions 当前用户权限码集合。
     * @return 可见菜单树；无任何可见页面时为空列表。
     */
    @Override
    public List<MenuNodeVO> visibleMenus(Set<String> permissions) {
        // 1. 遍历目录树，逐节点按权限裁剪；不可见节点返回 null 由 filter 剔除。
        return MenuCatalog.TREE.stream()
                .map(def -> toVisibleNode(def, permissions))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 递归推导单个节点的可见性。
     *
     * @param def 菜单定义。
     * @param permissions 权限码集合。
     * @return 可见节点；不可见返回 null。
     */
    private MenuNodeVO toVisibleNode(MenuCatalog.MenuDef def, Set<String> permissions) {
        // 2. 叶子节点：持有入口权限码即可见。
        if (def.permission() != null) {
            return permissions.contains(def.permission())
                    ? new MenuNodeVO(
                            def.key(), def.title(), def.path(), def.permission(), List.of())
                    : null;
        }
        // 3. 父级节点：子级逐个裁剪，全部不可见则父级隐藏。
        List<MenuNodeVO> children =
                def.children().stream()
                        .map(child -> toVisibleNode(child, permissions))
                        .filter(Objects::nonNull)
                        .toList();
        if (children.isEmpty()) {
            return null;
        }
        return new MenuNodeVO(def.key(), def.title(), null, null, children);
    }
}

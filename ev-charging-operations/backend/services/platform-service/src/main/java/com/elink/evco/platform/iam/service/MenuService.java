package com.elink.evco.platform.iam.service;

import com.elink.evco.platform.iam.vo.MenuNodeVO;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 菜单推导服务：叶子可见=拥有 resource:read；父级可见=其下存在任意可见叶子，
 * 全部不可见则父级整棵隐藏（不出现空菜单）；菜单树由后端推导，前端仅渲染。
 */
@Service
public class MenuService {

    /** 构造菜单推导服务。 */
    public MenuService() {}

    /**
     * 按权限集合推导可见菜单树。
     *
     * @param permissions 当前用户权限码集合。
     * @return 可见菜单树；无任何可见页面时为空列表。
     */
    public List<MenuNodeVO> visibleMenus(Set<String> permissions) {
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
        if (def.permission() != null) {
            return permissions.contains(def.permission())
                    ? new MenuNodeVO(def.key(), def.title(), def.path(), def.permission(), List.of())
                    : null;
        }
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

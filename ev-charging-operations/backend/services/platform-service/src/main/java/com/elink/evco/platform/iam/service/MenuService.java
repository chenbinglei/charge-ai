package com.elink.evco.platform.iam.service;

import com.elink.evco.platform.iam.vo.MenuNodeVO;
import java.util.List;
import java.util.Set;

/** 菜单推导服务：叶子可见=拥有 resource:read；父级可见=其下存在任意可见叶子， 全部不可见则父级整棵隐藏（不出现空菜单）；菜单树由后端推导，前端仅渲染。 */
public interface MenuService {

    /**
     * 按权限集合推导可见菜单树。
     *
     * @param permissions 当前用户权限码集合。
     * @return 可见菜单树；无任何可见页面时为空列表。
     */
    List<MenuNodeVO> visibleMenus(Set<String> permissions);
}

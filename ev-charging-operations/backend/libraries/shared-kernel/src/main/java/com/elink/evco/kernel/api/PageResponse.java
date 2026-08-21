package com.elink.evco.kernel.api;

import java.util.List;
import java.util.Objects;

/**
 * 偏移量分页的稳定数据结构；高频流式查询须在其独立契约中声明游标规则。
 *
 * @param <T> 已登记的列表元素类型。
 * @param items 当前页元素。
 * @param pageNo 从 1 开始的页码。
 * @param pageSize 当前页大小。
 * @param total 符合过滤条件的总量。
 */
public record PageResponse<T>(List<T> items, int pageNo, int pageSize, long total) {
    /** 防止未校验的分页参数和可变列表泄漏到调用链外。 */
    public PageResponse {
        items = List.copyOf(Objects.requireNonNull(items, "items 不能为空"));
        if (pageNo < 1) {
            throw new IllegalArgumentException("pageNo 必须从 1 开始");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize 必须大于 0");
        }
        if (total < 0) {
            throw new IllegalArgumentException("total 不能为负数");
        }
    }
}

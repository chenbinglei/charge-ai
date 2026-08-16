/** API 统一响应外观；具体业务数据类型只能在已审核模块中追加。 */
export interface ApiResponse<TData> {
  code: string;
  data: TData | null;
  message: string;
  timestamp: string;
  traceId: string;
}

/** 通用分页外观，不表达任何业务实体。 */
export interface PageResponse<TItem> {
  items: TItem[];
  page: number;
  pageSize: number;
  total: number;
}

/** 三种已批准主题的稳定标识。 */
export type ThemeName = "energy-green" | "graphite-night" | "command-deep";

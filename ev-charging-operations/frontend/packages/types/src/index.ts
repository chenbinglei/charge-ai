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

/** 部署模式（W2 技术设计包 §8）：IOT 协同（IOT 维护数据域只读）/ 独立部署（全量读写）。 */
export type DeploymentMode = "IOT_COLLABORATIVE" | "STANDALONE";

/** 图形验证码响应（auth-v1.yaml v1.3.0）。 */
export interface CaptchaVO {
  captchaId: string;
  imageBase64: string;
  expiresIn: number;
}

/** 登录请求（auth-v1.yaml v1.3.0）。 */
export interface LoginRequest {
  username: string;
  password: string;
  captchaId: string;
  captchaCode: string;
}

/** 登录/换会话/刷新共用令牌响应（auth-v1.yaml v1.3.0）。 */
export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
  userId: string;
  displayName: string;
  roles: string[];
  deploymentMode: DeploymentMode;
}

/** 后端推导的可见菜单节点（profile.menus）；父级仅分组无权限码。 */
export interface MenuNodeVO {
  key: string;
  title: string;
  path: string;
  permission: string | null;
  children: MenuNodeVO[] | null;
}

/** 当前用户档案（GET /api/v1/auth/profile）。 */
export interface ProfileVO {
  userId: string;
  username: string;
  displayName: string;
  roles: string[];
  permissions: string[];
  deploymentMode: DeploymentMode;
  menus: MenuNodeVO[];
}

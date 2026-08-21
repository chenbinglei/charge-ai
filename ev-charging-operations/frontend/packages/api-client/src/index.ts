import type {
  ApiResponse,
  CaptchaVO,
  LoginRequest,
  LoginResponse,
  ProfileVO,
} from "@evco/types";

/** 已登记 API 请求必须携带的技术头；不得在此层加入业务端点。 */
export interface ApiRequestOptions {
  idempotencyKey?: string;
  signal?: AbortSignal;
  traceId?: string;
}

/** 请求体 JSON 序列化后的内容类型头。 */
const JSON_CONTENT_TYPE = "application/json";

/**
 * 类型安全 HTTP 客户端边界：统一注入鉴权头与技术头；
 * 401 时不在此层做副作用（由应用的响应拦截回调决定刷新或登出）。
 */
export class ApiClient {
  private accessToken: string | null = null;
  private onUnauthorized?: () => void;

  public constructor(private readonly baseUrl: string) {}

  /** 登录成功后注入访问令牌；登出时传 null 清除。 */
  public setAccessToken(token: string | null): void {
    this.accessToken = token;
  }

  /** 应用装配期注入 401 统一处理（刷新令牌或跳登录）；避免客户端层产生副作用。 */
  public setOnUnauthorized(handler: () => void): void {
    this.onUnauthorized = handler;
  }

  public async get<TData>(
    path: `/api/v${number}${string}`,
    options: ApiRequestOptions = {},
  ): Promise<ApiResponse<TData>> {
    return this.request<TData>("GET", path, undefined, options);
  }

  public async post<TData>(
    path: `/api/v${number}${string}`,
    body?: unknown,
    options: ApiRequestOptions = {},
  ): Promise<ApiResponse<TData>> {
    return this.request<TData>("POST", path, body, options);
  }

  private async request<TData>(
    method: "GET" | "POST",
    path: `/api/v${number}${string}`,
    body: unknown,
    options: ApiRequestOptions,
  ): Promise<ApiResponse<TData>> {
    const headers = this.createHeaders(options);
    if (body !== undefined) {
      headers.set("Content-Type", JSON_CONTENT_TYPE);
    }
    const response = await fetch(new URL(path, this.baseUrl), {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: options.signal,
    });
    // 401 统一交由应用层处理（刷新令牌或跳登录），此处只回调不吞异常。
    if (response.status === 401) {
      this.onUnauthorized?.();
    }
    return (await response.json()) as ApiResponse<TData>;
  }

  private createHeaders(options: ApiRequestOptions): Headers {
    const headers = new Headers({ Accept: JSON_CONTENT_TYPE });
    if (this.accessToken !== null) {
      headers.set("Authorization", `Bearer ${this.accessToken}`);
    }
    if (options.idempotencyKey !== undefined) {
      headers.set("X-Idempotency-Key", options.idempotencyKey);
    }
    if (options.traceId !== undefined) {
      headers.set("X-Trace-Id", options.traceId);
    }
    return headers;
  }
}

/** 认证域端点（auth-v1.yaml v1.3.0）；仅管理端登录链路使用。 */
export class AuthApi {
  public constructor(private readonly client: ApiClient) {}

  /** 获取图形验证码（Base64 PNG，Redis TTL 120 秒一次性校验）。 */
  public captcha(): Promise<ApiResponse<CaptchaVO>> {
    return this.client.get<CaptchaVO>("/api/v1/auth/captcha");
  }

  /** 账号密码登录（图形验证码校验）。 */
  public login(request: LoginRequest): Promise<ApiResponse<LoginResponse>> {
    return this.client.post<LoginResponse>("/api/v1/auth/login", request);
  }

  /** IOT 协同模式 SSO 免登录：一次性 ticket 换会话（独立部署模式入口关闭返回 403）。 */
  public ssoLogin(ticket: string): Promise<ApiResponse<LoginResponse>> {
    return this.client.post<LoginResponse>("/api/v1/auth/sso/login", { ticket });
  }

  /** 当前用户档案：权限码集合、部署模式与后端推导的可见菜单树。 */
  public profile(): Promise<ApiResponse<ProfileVO>> {
    return this.client.get<ProfileVO>("/api/v1/auth/profile");
  }

  /** 登出并撤销当前会话。 */
  public logout(refreshToken: string): Promise<ApiResponse<void>> {
    return this.client.post<void>("/api/v1/auth/logout", { refreshToken });
  }

  /** 刷新令牌轮换（一次性 refreshToken）。 */
  public refresh(refreshToken: string): Promise<ApiResponse<LoginResponse>> {
    return this.client.post<LoginResponse>("/api/v1/auth/refresh", {
      refreshToken,
    });
  }
}

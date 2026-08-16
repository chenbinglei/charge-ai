import type { ApiResponse } from "@evco/types";

/** 已登记 API 请求必须携带的技术头；不得在此层加入业务端点。 */
export interface ApiRequestOptions {
  idempotencyKey?: string;
  signal?: AbortSignal;
  traceId?: string;
}

/** 无业务端点的类型安全 HTTP 客户端边界。 */
export class ApiClient {
  public constructor(private readonly baseUrl: string) {}

  public async get<TData>(
    path: `/api/v${number}${string}`,
    options: ApiRequestOptions = {},
  ): Promise<ApiResponse<TData>> {
    const response = await fetch(new URL(path, this.baseUrl), {
      headers: this.createHeaders(options),
      signal: options.signal,
    });

    return (await response.json()) as ApiResponse<TData>;
  }

  private createHeaders(options: ApiRequestOptions): Headers {
    const headers = new Headers({ Accept: "application/json" });
    if (options.idempotencyKey !== undefined) {
      headers.set("X-Idempotency-Key", options.idempotencyKey);
    }
    if (options.traceId !== undefined) {
      headers.set("X-Trace-Id", options.traceId);
    }
    return headers;
  }
}

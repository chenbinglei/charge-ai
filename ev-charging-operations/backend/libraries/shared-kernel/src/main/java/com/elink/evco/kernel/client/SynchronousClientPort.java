package com.elink.evco.kernel.client;

/**
 * 本服务拥有的同步客户端端口标记。
 *
 * <p>实现必须留在服务内且可替换；该标记不授权服务间直接耦合，也不允许绕开版本化契约。
 */
public interface SynchronousClientPort {}

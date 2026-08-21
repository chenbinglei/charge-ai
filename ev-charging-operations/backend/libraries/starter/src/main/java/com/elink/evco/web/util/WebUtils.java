package com.elink.evco.web.util;

import jakarta.servlet.http.HttpServletRequest;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

/**
 * Web 请求工具：从当前请求提取客户端信息的静态方法集合。
 *
 * <p>仅供控制器/服务薄层使用；不含业务规则，避免控制器内堆积私有封装方法。
 *
 * <p>IP 基线（DEC-20260820-017）：平台仅记录 IPv4——IPv6 环回（`::1`/`0:0:0:0:0:0:0:1`）
 * 归一化为 `127.0.0.1`，IPv6 映射地址 `::ffff:x.x.x.x` 剥离取 IPv4，
 * 其余纯 IPv6 来源返回 null（页面展示 —）。
 */
public final class WebUtils {

    /** 代理转发链路头；取第一跳作为客户端真实 IP。 */
    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    /** IPv4 点分十进制格式（四段 0-255；预检用，避免 InetAddress 对非 IP 串做 DNS 解析）。 */
    private static final Pattern IPV4_PATTERN =
            Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");

    /** 工具类禁止实例化。 */
    private WebUtils() {}

    /**
     * 提取客户端 IPv4；优先取代理链第一跳，头缺失时回退远端地址。
     *
     * @param request 当前请求。
     * @return IPv4 字符串；来源为纯 IPv6 或无法识别时返回 null。
     */
    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader(FORWARDED_FOR_HEADER);
        String candidate =
                (forwarded != null && !forwarded.isBlank())
                        ? forwarded.split(",")[0].trim()
                        : request.getRemoteAddr();
        return toIpv4OrNull(candidate);
    }

    /**
     * 将候选地址归一化为 IPv4。
     *
     * @param candidate 候选地址（可能为 IPv4/IPv6 环回/IPv6 映射/纯 IPv6）。
     * @return IPv4 字符串；纯 IPv6 或无法识别时返回 null。
     */
    private static String toIpv4OrNull(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        String trimmed = candidate.trim();
        // 快速路径：IPv4 点分十进制（含代理头直传 IPv4 的常见场景）。
        if (IPV4_PATTERN.matcher(trimmed).matches()) {
            return isIpv4(trimmed) ? trimmed : null;
        }
        // 含冒号视为 IPv6 字面量（含环回/映射/zone id 变体），交给 InetAddress 解析；
        // 其余非 IP 形态（如伪造主机名）直接丢弃，避免触发 DNS 解析。
        if (!trimmed.contains(":")) {
            return null;
        }
        try {
            // 截掉 IPv6 zone id（如 fe80::1%lo0）避免解析失败。
            String literal = trimmed.split("%", 2)[0];
            InetAddress address = InetAddress.getByName(literal);
            // IPv6 环回（::1 及其完整形式）归一化为 IPv4 环回。
            if (address.isLoopbackAddress()) {
                return "127.0.0.1";
            }
            if (address instanceof Inet4Address) {
                return address.getHostAddress();
            }
            // IPv6 映射 IPv4（::ffff:x.x.x.x）：低 4 字节即 IPv4。
            byte[] bytes = address.getAddress();
            if (bytes.length == 16 && isMappedIpv4(bytes)) {
                return (bytes[12] & 0xFF) + "." + (bytes[13] & 0xFF) + "."
                        + (bytes[14] & 0xFF) + "." + (bytes[15] & 0xFF);
            }
            // 纯 IPv6 不落库（字段仅 15 字符），存 NULL 由页面展示 —。
            return null;
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /**
     * 判定 16 字节 IPv6 地址是否为 IPv4 映射形式（前 10 字节为 0、第 11/12 字节为 0xFF）。
     *
     * @param bytes 16 字节地址。
     * @return 是否映射 IPv4。
     */
    private static boolean isMappedIpv4(byte[] bytes) {
        for (int i = 0; i < 10; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return bytes[10] == (byte) 0xFF && bytes[11] == (byte) 0xFF;
    }

    /**
     * 判定字符串是否为合法 IPv4（四段 0-255 点分十进制；格式已由正则预检）。
     *
     * @param value 待判定字符串。
     * @return 是否 IPv4。
     */
    private static boolean isIpv4(String value) {
        String[] parts = value.split("\\.", -1);
        for (String part : parts) {
            int octet = Integer.parseInt(part);
            if (octet > 255 || (part.length() > 1 && part.startsWith("0"))) {
                return false;
            }
        }
        return true;
    }
}

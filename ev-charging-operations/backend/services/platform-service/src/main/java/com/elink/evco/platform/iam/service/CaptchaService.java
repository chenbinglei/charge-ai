package com.elink.evco.platform.iam.service;

import com.elink.evco.platform.common.cache.RedisKeys;
import com.elink.evco.platform.common.config.AppProperties;
import com.elink.evco.platform.iam.vo.CaptchaVO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 图形验证码服务：4-6 位字母数字（剔除易混淆字符），Redis 存储 120 秒，
 * GETDEL 一次性消费；大小写不敏感。
 */
@Service
public class CaptchaService {

    /** 候选字符集；剔除 0/O、1/I 等易混淆字符。 */
    private static final char[] CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    /** 图片宽度（像素）。 */
    private static final int IMAGE_WIDTH = 160;

    /** 图片高度（像素）。 */
    private static final int IMAGE_HEIGHT = 48;

    /** 字体大小（像素）。 */
    private static final int FONT_SIZE = 30;

    /** 干扰线数量。 */
    private static final int NOISE_LINES = 6;

    /** 安全随机源；验证码内容必须不可预测。 */
    private final SecureRandom random = new SecureRandom();

    /** Redis 客户端。 */
    private final StringRedisTemplate redis;

    /** Redis 键规则。 */
    private final RedisKeys keys;

    /** 验证码长度。 */
    private final int length;

    /** 验证码有效期（秒）。 */
    private final int ttlSeconds;

    /**
     * 构造验证码服务。
     *
     * @param redis Redis 客户端。
     * @param keys Redis 键规则。
     * @param appProperties 平台业务配置。
     */
    public CaptchaService(StringRedisTemplate redis, RedisKeys keys, AppProperties appProperties) {
        this.redis = redis;
        this.keys = keys;
        this.length = appProperties.getAuth().getCaptcha().getLength();
        this.ttlSeconds = appProperties.getAuth().getCaptcha().getTtlSeconds();
    }

    /**
     * 生成图形验证码：返回标识与 Base64 PNG 图片，内容小写落 Redis。
     *
     * @return 验证码响应（captchaId、图片、有效秒数）。
     */
    public CaptchaVO generate() {
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(CHARSET[random.nextInt(CHARSET.length)]);
        }
        redis.opsForValue().set(keys.captcha(captchaId), code.toString().toLowerCase(), Duration.ofSeconds(ttlSeconds));
        return new CaptchaVO(captchaId, renderPng(code.toString()), ttlSeconds);
    }

    /**
     * 一次性校验验证码；GETDEL 原子读取并删除，比对大小写不敏感。
     *
     * @param captchaId 验证码标识。
     * @param input 用户输入内容。
     * @return true 表示校验通过（验证码同时被消费）。
     */
    public boolean verify(String captchaId, String input) {
        if (captchaId == null || input == null || input.isBlank()) {
            return false;
        }
        String stored = redis.opsForValue().getAndDelete(keys.captcha(captchaId));
        return stored != null && stored.equalsIgnoreCase(input.trim());
    }

    /**
     * 渲染验证码 PNG 图片（Base64）；含字符旋转与干扰线。
     *
     * @param code 验证码明文。
     * @return data 无前缀的 Base64 PNG 字符串。
     */
    private String renderPng(String code) {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
            graphics.setStroke(new BasicStroke(1.4f));
            for (int i = 0; i < NOISE_LINES; i++) {
                graphics.setColor(randomColor(180, 255));
                graphics.drawLine(
                        random.nextInt(IMAGE_WIDTH),
                        random.nextInt(IMAGE_HEIGHT),
                        random.nextInt(IMAGE_WIDTH),
                        random.nextInt(IMAGE_HEIGHT));
            }
            Font font = new Font("SansSerif", Font.BOLD, FONT_SIZE);
            graphics.setFont(font);
            int step = (IMAGE_WIDTH - 20) / code.length();
            for (int i = 0; i < code.length(); i++) {
                int x = 12 + i * step + random.nextInt(6);
                int y = IMAGE_HEIGHT / 2 + FONT_SIZE / 3;
                double rotate = (random.nextInt(40) - 20) / 100.0;
                graphics.rotate(rotate, x, y);
                graphics.setColor(randomColor(20, 120));
                graphics.drawString(String.valueOf(code.charAt(i)), x, y);
                graphics.rotate(-rotate, x, y);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException ex) {
            // PNG 为内存编码，IOException 仅在流异常时出现；直接失败防止无图验证码上线。
            throw new IllegalStateException("验证码图片渲染失败", ex);
        } finally {
            graphics.dispose();
        }
    }

    /**
     * 生成柔和范围内的随机颜色。
     *
     * @param minRGB 最小通道值。
     * @param maxRGB 最大通道值。
     * @return 随机颜色。
     */
    private Color randomColor(int minRGB, int maxRGB) {
        int span = maxRGB - minRGB;
        return new Color(
                minRGB + random.nextInt(span),
                minRGB + random.nextInt(span),
                minRGB + random.nextInt(span));
    }
}

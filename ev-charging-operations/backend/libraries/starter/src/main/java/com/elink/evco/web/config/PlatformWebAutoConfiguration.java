package com.elink.evco.web.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.elink.evco.web.error.GlobalExceptionHandler;
import com.elink.evco.web.security.BearerAuthFilter;
import com.elink.evco.web.security.JwtTokenProvider;
import com.elink.evco.web.security.PermissionCheckPort;
import com.elink.evco.web.security.PermissionInterceptor;
import com.elink.evco.web.security.SessionValidationPort;
import com.elink.evco.web.trace.TraceIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 通用组件自动装配，分两层：
 *
 * <p><b>基础层（所有服务可用，含个人小程序等无权限服务）</b>：链路追踪过滤器、 全局异常处理器、MyBatis-Plus 分页/乐观锁拦截器与审计时间填充。
 *
 * <p><b>管理端层（evco.web.security.enabled=true 时装配，默认开启）</b>： Bearer 统一鉴权过滤器与两档权限拦截器；且分别要求宿主服务实现
 * SessionValidationPort / PermissionCheckPort，未实现时对应组件不装配。
 *
 * <p>全部 Bean 均可被宿主服务同名 Bean 覆盖。
 */
@AutoConfiguration
@EnableConfigurationProperties({AppProperties.class, WebSecurityProperties.class})
public class PlatformWebAutoConfiguration {

    /** 平台统一业务时区（DEC-20260820-016）：北京时间；落库与 API 输出均用本时区。 */
    public static final ZoneId PLATFORM_ZONE = ZoneId.of("Asia/Shanghai");

    static {
        // 类加载即固定 JVM 默认时区，业务代码直接 LocalDateTime.now() 即北京时间，
        // 不依赖部署环境（容器默认 UTC 等）的时区设置。
        TimeZone.setDefault(TimeZone.getTimeZone(PLATFORM_ZONE));
    }

    /**
     * Jackson 时间格式定制：LocalDateTime 统一「yyyy-MM-dd HH:mm:ss」秒级序列化，
     * 与库内 DATETIME 秒级精度一致（DEC-20260820-016）。
     *
     * @return Jackson 定制器。
     */
    @Bean
    @ConditionalOnMissingBean(name = "evcoLocalDateTimeCustomizer")
    public Jackson2ObjectMapperBuilderCustomizer evcoLocalDateTimeCustomizer() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return builder ->
                builder.serializers(new LocalDateTimeSerializer(formatter))
                        .deserializers(new LocalDateTimeDeserializer(formatter));
    }

    /**
     * 链路追踪过滤器注册；最高优先级，先于鉴权建立 traceId。
     *
     * @return 链路追踪过滤器注册器。
     */
    @Bean
    @ConditionalOnMissingBean
    public TraceIdFilterRegistration traceIdFilterRegistration() {
        return new TraceIdFilterRegistration(new TraceIdFilter());
    }

    /** 链路追踪过滤器注册器；固定最高优先级。 */
    public static class TraceIdFilterRegistration
            extends org.springframework.boot.web.servlet.FilterRegistrationBean<TraceIdFilter> {

        /**
         * 以固定顺序注册链路追踪过滤器。
         *
         * @param filter 链路追踪过滤器。
         */
        public TraceIdFilterRegistration(TraceIdFilter filter) {
            super(filter);
            setOrder(Ordered.HIGHEST_PRECEDENCE);
        }
    }

    /**
     * JWT 签发器；构造时校验密钥长度防止弱密钥上线。
     *
     * @param appProperties 平台业务配置。
     * @return JWT 签发器。
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtTokenProvider jwtTokenProvider(AppProperties appProperties) {
        return new JwtTokenProvider(appProperties);
    }

    /**
     * BCrypt 密码编码器；强度默认 10 轮，哈希结果含盐自校验。
     *
     * @return 密码编码器。
     */
    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 全局异常处理器；把失败路径收敛为稳定 ApiResponse 外观。
     *
     * @return 全局异常处理器。
     */
    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    /**
     * MyBatis-Plus 拦截器链：分页 + 乐观锁；乐观锁依赖实体 version 字段， 更新 0 行由业务层翻译为 VERSION_CONFLICT。
     *
     * @return MyBatis-Plus 拦截器链。
     */
    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }

    /**
     * 审计时间自动填充：insert 填 created_at/updated_at，update 刷新 updated_at（北京时间）。
     *
     * @return 元对象填充处理器。
     */
    @Bean
    @ConditionalOnMissingBean
    public MetaObjectHandler auditMetaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(org.apache.ibatis.reflection.MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
                strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
            }

            @Override
            public void updateFill(org.apache.ibatis.reflection.MetaObject metaObject) {
                strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }

    /**
     * Bearer 令牌过滤器；管理端层，开关开启且宿主服务实现会话校验端口时装配。
     *
     * @param jwtTokenProvider JWT 签发器。
     * @param sessionValidation 会话校验端口实现。
     * @param securityProperties 免认证端点配置。
     * @param objectMapperProvider JSON 序列化器提供者。
     * @return Bearer 令牌过滤器注册。
     */
    @Bean
    @ConditionalOnBean(SessionValidationPort.class)
    @ConditionalOnProperty(
            prefix = "evco.web.security",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public BearerAuthFilterRegistration bearerAuthFilterRegistration(
            JwtTokenProvider jwtTokenProvider,
            SessionValidationPort sessionValidation,
            WebSecurityProperties securityProperties,
            ObjectProvider<ObjectMapper> objectMapperProvider) {
        BearerAuthFilter filter =
                new BearerAuthFilter(
                        jwtTokenProvider,
                        sessionValidation,
                        securityProperties,
                        objectMapperProvider.getIfAvailable(ObjectMapper::new));
        return new BearerAuthFilterRegistration(filter);
    }

    /**
     * 两档权限拦截器；管理端层，开关开启且宿主服务实现权限读取端口时装配。
     *
     * @param permissionCheck 权限读取端口实现。
     * @param appProperties 平台业务配置。
     * @return 两档权限拦截器。
     */
    @Bean
    @ConditionalOnBean(PermissionCheckPort.class)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "evco.web.security",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public PermissionInterceptor permissionInterceptor(
            PermissionCheckPort permissionCheck, AppProperties appProperties) {
        return new PermissionInterceptor(permissionCheck, appProperties);
    }

    /**
     * MVC 配置：注册两档权限拦截器到全部 /api/** 路由。
     *
     * @param permissionInterceptor 两档权限拦截器。
     * @return MVC 配置器。
     */
    @Bean
    @ConditionalOnBean(PermissionInterceptor.class)
    public WebMvcConfigurer permissionWebMvcConfigurer(
            PermissionInterceptor permissionInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(permissionInterceptor).addPathPatterns("/api/**");
            }
        };
    }

    /** Bearer 过滤器注册器；固定次高优先级（链路追踪之后）。 */
    public static class BearerAuthFilterRegistration
            extends org.springframework.boot.web.servlet.FilterRegistrationBean<BearerAuthFilter> {

        /**
         * 以固定顺序注册鉴权过滤器。
         *
         * @param filter Bearer 令牌过滤器。
         */
        public BearerAuthFilterRegistration(BearerAuthFilter filter) {
            super(filter);
            setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        }
    }
}

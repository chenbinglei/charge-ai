/*
 Navicat Premium Data Transfer

 Source Server         : 192.168.2.137_3306
 Source Server Type    : MySQL
 Source Server Version : 80036
 Source Host           : 192.168.2.137:3306
 Source Schema         : linkos

 Target Server Type    : MySQL
 Target Server Version : 80036
 File Encoding         : 65001

 Date: 19/08/2026 10:29:06
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for config_change_log
-- ----------------------------
DROP TABLE IF EXISTS `config_change_log`;
CREATE TABLE `config_change_log`  (
  `id` bigint(0) NOT NULL COMMENT '雪花ID',
  `tenant_id` bigint(0) NOT NULL COMMENT '所属租户',
  `device_id` bigint(0) NOT NULL COMMENT '根设备 t_device.id',
  `component_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '组件实例 component_id；NULL 表示设备根实例',
  `point_key` varchar(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '实例属性标识',
  `old_value` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '变更前值',
  `new_value` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '变更后值',
  `operator` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作人用户名',
  `changed_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '变更时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_config_change_device_component`(`device_id`, `component_id`) USING BTREE,
  INDEX `idx_config_change_changed_at`(`changed_at`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备与组件实例属性配置变更日志' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for flyway_schema_history
-- ----------------------------
DROP TABLE IF EXISTS `flyway_schema_history`;
CREATE TABLE `flyway_schema_history`  (
  `installed_rank` int(0) NOT NULL,
  `version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `script` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `checksum` int(0) NULL DEFAULT NULL,
  `installed_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `installed_on` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `execution_time` int(0) NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`) USING BTREE,
  INDEX `flyway_schema_history_s_idx`(`success`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for shedlock
-- ----------------------------
DROP TABLE IF EXISTS `shedlock`;
CREATE TABLE `shedlock`  (
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `lock_until` timestamp(3) NOT NULL,
  `locked_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `locked_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_access_channel
-- ----------------------------
DROP TABLE IF EXISTS `t_access_channel`;
CREATE TABLE `t_access_channel`  (
  `id` bigint(0) NOT NULL COMMENT '雪花ID；access_channel_id',
  `device_id` bigint(0) NOT NULL,
  `channel_code` varchar(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备内稳定通道编码',
  `endpoint_id` bigint(0) NULL DEFAULT NULL COMMENT 'SERVER_PASSIVE 必填；CLIENT_POLL 可空',
  `template_revision_id` bigint(0) NOT NULL COMMENT '精确不可变 revision',
  `executor` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'PLATFORM/GATEWAY',
  `comm_method` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '链路介质，不是协议名',
  `match_rule` json NULL COMMENT '入站时仅在可信 Device 内选通道',
  `conn_params` json NOT NULL COMMENT '按连接角色分型的通道参数；不得含秘密',
  `auth_scheme_ref` varchar(192) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '认证方案引用；凭证型方案内含 slot_key，不得含秘密',
  `enabled` tinyint(0) NOT NULL DEFAULT 0,
  `failover_group` varchar(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `failover_priority` int(0) NULL DEFAULT NULL COMMENT '数值越小优先级越高',
  `config_version` bigint(0) NOT NULL DEFAULT 0 COMMENT '设备级串行化版本的快照',
  `row_version` bigint(0) NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `created_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `updated_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_access_channel_device_code`(`device_id`, `channel_code`) USING BTREE,
  INDEX `idx_access_channel_endpoint`(`endpoint_id`) USING BTREE,
  INDEX `idx_access_channel_revision`(`template_revision_id`) USING BTREE,
  INDEX `idx_access_channel_failover`(`device_id`, `failover_group`, `failover_priority`) USING BTREE,
  CONSTRAINT `fk_access_channel_device` FOREIGN KEY (`device_id`) REFERENCES `t_device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_access_channel_endpoint` FOREIGN KEY (`endpoint_id`) REFERENCES `t_protocol_endpoint` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_access_channel_revision` FOREIGN KEY (`template_revision_id`) REFERENCES `t_mapping_template_revision` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备 AccessChannel 语义单元' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_access_element_owner
-- ----------------------------
DROP TABLE IF EXISTS `t_access_element_owner`;
CREATE TABLE `t_access_element_owner`  (
  `id` bigint(0) NOT NULL COMMENT '雪花ID',
  `device_id` bigint(0) NOT NULL,
  `component_path` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '根为空串',
  `element_kind` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'TELEMETRY_POINT/COMMAND_SERVICE',
  `element_identifier` varchar(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `direction` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'UPLINK/DOWNLINK',
  `access_channel_id` bigint(0) NOT NULL COMMENT '当前唯一 owner',
  `candidate_channel_id` bigint(0) NULL DEFAULT NULL COMMENT 'SWITCHING 候选',
  `owner_state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/SWITCHING',
  `failover_group` varchar(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `access_config_version` bigint(0) NOT NULL,
  `fencing_epoch` bigint(0) NOT NULL,
  `row_version` bigint(0) NOT NULL DEFAULT 0,
  `created_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `updated_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  `active_device_id` bigint(0) GENERATED ALWAYS AS ((case when (`is_deleted` = 0) then `device_id` else NULL end)) STORED NULL,
  `active_component_path` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS ((case when (`is_deleted` = 0) then `component_path` else NULL end)) STORED NULL,
  `active_element_kind` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS ((case when (`is_deleted` = 0) then `element_kind` else NULL end)) STORED NULL,
  `active_element_identifier` varchar(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS ((case when (`is_deleted` = 0) then `element_identifier` else NULL end)) STORED NULL,
  `active_direction` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS ((case when (`is_deleted` = 0) then `direction` else NULL end)) STORED NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_access_element_owner`(`active_device_id`, `active_component_path`, `active_element_kind`, `active_element_identifier`, `active_direction`) USING BTREE,
  INDEX `idx_access_element_owner_channel`(`access_channel_id`) USING BTREE,
  INDEX `idx_access_element_owner_candidate`(`candidate_channel_id`) USING BTREE,
  INDEX `idx_access_element_owner_device_version`(`device_id`, `access_config_version`) USING BTREE,
  CONSTRAINT `fk_access_element_owner_candidate` FOREIGN KEY (`candidate_channel_id`) REFERENCES `t_access_channel` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_access_element_owner_channel` FOREIGN KEY (`access_channel_id`) REFERENCES `t_access_channel` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_access_element_owner_device` FOREIGN KEY (`device_id`) REFERENCES `t_device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '接入元素唯一有效 owner 物化占用' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_alarm_rule
-- ----------------------------
DROP TABLE IF EXISTS `t_alarm_rule`;
CREATE TABLE `t_alarm_rule`  (
  `id` bigint(0) NOT NULL COMMENT '雪花ID',
  `tenant_id` bigint(0) NOT NULL COMMENT '所属租户',
  `model_id` bigint(0) NULL DEFAULT NULL COMMENT '设备模型ID,空表示租户级通用规则',
  `scope` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'MODEL' COMMENT 'MODEL',
  `identifier` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发事件identifier',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '规则名称',
  `point_key` varchar(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '测点标识,表达式规则可为空',
  `rule_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'THRESHOLD/DEADBAND/EXPRESSION',
  `operator` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '比较操作符',
  `threshold` decimal(20, 6) NULL DEFAULT NULL,
  `expression` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `severity` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'WARNING',
  `duration_sec` int(0) NOT NULL DEFAULT 0,
  `enabled` tinyint(0) NOT NULL DEFAULT 1,
  `auto_actions` json NULL,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_alarm_rule_tenant_model`(`tenant_id`, `model_id`) USING BTREE,
  INDEX `idx_alarm_rule_point`(`point_key`) USING BTREE,
  INDEX `idx_alarm_rule_enabled`(`enabled`) USING BTREE,
  INDEX `idx_alarm_rule_scope_enabled`(`scope`, `enabled`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '告警规则' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_alarm_silence
-- ----------------------------
DROP TABLE IF EXISTS `t_alarm_silence`;
CREATE TABLE `t_alarm_silence`  (
  `id` bigint(0) NOT NULL COMMENT '雪花ID',
  `tenant_id` bigint(0) NOT NULL COMMENT '所属租户',
  `scope_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'DEVICE/RULE',
  `device_id` bigint(0) NULL DEFAULT NULL COMMENT '设备维度静默',
  `station_id` bigint(0) NULL DEFAULT NULL COMMENT '场站维度静默',
  `rule_id` bigint(0) NULL DEFAULT NULL COMMENT '规则维度静默',
  `start_time` datetime(0) NOT NULL COMMENT '静默开始时间',
  `end_time` datetime(0) NOT NULL COMMENT '静默结束时间',
  `reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '静默原因',
  `enabled` tinyint(0) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_alarm_silence_tenant_time`(`tenant_id`, `enabled`, `start_time`, `end_time`) USING BTREE,
  INDEX `idx_alarm_silence_device`(`device_id`, `start_time`, `end_time`) USING BTREE,
  INDEX `idx_alarm_silence_station`(`station_id`, `start_time`, `end_time`) USING BTREE,
  INDEX `idx_alarm_silence_rule`(`rule_id`, `start_time`, `end_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '告警静默窗' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_audit_log
-- ----------------------------
DROP TABLE IF EXISTS `t_audit_log`;
CREATE TABLE `t_audit_log`  (
  `id` bigint(0) NOT NULL,
  `tenant_id` bigint(0) NULL DEFAULT NULL,
  `actor_tenant_id` bigint(0) NULL DEFAULT NULL COMMENT '操作者真实租户',
  `target_tenant_id` bigint(0) NULL DEFAULT NULL COMMENT '目标租户',
  `user_id` bigint(0) NULL DEFAULT NULL,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `action` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作类型',
  `resource` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '资源类型',
  `resource_id` varchar(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `detail` json NULL COMMENT '操作细节/参数',
  `result` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS/FAILURE',
  `ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `trace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_audit_user`(`user_id`) USING BTREE,
  INDEX `idx_audit_created`(`created_at`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '审计日志' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_config_sync_log
-- ----------------------------
DROP TABLE IF EXISTS `t_config_sync_log`;
CREATE TABLE `t_config_sync_log`  (
  `id` bigint(0) NOT NULL COMMENT '雪花ID',
  `tenant_id` bigint(0) NOT NULL COMMENT '所属租户,跟随网关',
  `gateway_id` bigint(0) NOT NULL COMMENT '网关设备 t_device.id',
  `gateway_sn` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '网关SN',
  `from_version` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `to_version` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `result` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'SUCCESS/FAILURE',
  `message` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `payload` json NULL COMMENT '下发配置摘要',
  `sync_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_config_sync_gateway`(`gateway_id`, `sync_time`) USING BTREE,
  INDEX `idx_config_sync_tenant`(`tenant_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '云边配置同步日志' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_device
-- ----------------------------
DROP TABLE IF EXISTS `t_device`;
CREATE TABLE `t_device`  (
  `id` bigint(0) NOT NULL,
  `tenant_id` bigint(0) NOT NULL DEFAULT 2069700000000000001,
  `device_id` varchar(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备唯一标识/SN,用作 TDengine 子表名',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `model_id` bigint(0) NOT NULL COMMENT '引用的设备模型(须 PUBLISHED)',
  `station_id` bigint(0) NOT NULL,
  `access_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DIRECT' COMMENT 'DIRECT/GATEWAY/SUB_DEVICE',
  `parent_id` bigint(0) NULL DEFAULT NULL COMMENT '子设备指向其网关 t_device.id',
  `credential` json NULL COMMENT '接入凭证(加密)',
  `credential_username_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'MQTT凭证用户名hash',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'UNREGISTERED' COMMENT 'ONLINE/OFFLINE/FAULT/UNREGISTERED',
  `last_online_at` datetime(0) NULL DEFAULT NULL,
  `is_demo` tinyint(0) NOT NULL DEFAULT 0 COMMENT '演示数据标记',
  `created_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `updated_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最后修改人',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  `gateway_device_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '子设备在网关侧标识',
  `relation_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'GATEWAY_PROXY/COMPONENT',
  `access_config_version` bigint(0) NOT NULL DEFAULT 0 COMMENT '接入配置设备级串行化版本',
  `access_fencing_epoch` bigint(0) NOT NULL DEFAULT 0 COMMENT '接入执行 owner 的单调 fencing epoch',
  `resolver_mode` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'LEGACY' COMMENT 'LEGACY/SHADOW/CHANNEL 写权威',
  `resolver_epoch` bigint(0) NOT NULL DEFAULT 0 COMMENT '写权威转换单调 epoch',
  `sync_checkpoint` bigint(0) NOT NULL DEFAULT 0 COMMENT '双写/反向投影已完成的 access_config_version',
  `sync_status` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'HEALTHY' COMMENT 'HEALTHY/FAILED',
  `sync_error` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `sync_failed_at` datetime(0) NULL DEFAULT NULL,
  `parity_status` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NOT_RUN' COMMENT 'NOT_RUN/PASS/FAIL/BLOCKED',
  `parity_checkpoint` bigint(0) NULL DEFAULT NULL,
  `legacy_config_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `channel_config_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `parity_checked_at` datetime(0) NULL DEFAULT NULL,
  `migration_boundary` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'REVERSIBLE' COMMENT 'REVERSIBLE/IRREVERSIBLE',
  `irreversible_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `irreversible_at` datetime(0) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_device_device_id`(`device_id`) USING BTREE,
  UNIQUE INDEX `uk_device_tenant_id`(`tenant_id`, `id`) USING BTREE,
  INDEX `idx_device_model`(`model_id`) USING BTREE,
  INDEX `idx_device_station`(`station_id`) USING BTREE,
  INDEX `idx_device_parent`(`parent_id`) USING BTREE,
  INDEX `idx_device_tenant_station`(`tenant_id`, `station_id`) USING BTREE,
  INDEX `idx_device_tenant_parent`(`tenant_id`, `parent_id`) USING BTREE,
  INDEX `idx_device_gw`(`parent_id`, `gateway_device_id`) USING BTREE,
  INDEX `idx_device_credential_username_hash`(`credential_username_hash`) USING BTREE,
  INDEX `idx_device_resolver_mode`(`resolver_mode`, `migration_boundary`, `sync_status`, `parity_status`) USING BTREE,
  CONSTRAINT `fk_device_model` FOREIGN KEY (`model_id`) REFERENCES `t_device_model` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_device_parent_tenant` FOREIGN KEY (`tenant_id`, `parent_id`) REFERENCES `t_device` (`tenant_id`, `id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_device_station_tenant` FOREIGN KEY (`tenant_id`, `station_id`) REFERENCES `t_station` (`tenant_id`, `id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_device_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `t_tenant` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备(模型实例)' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_device_command
-- ----------------------------
DROP TABLE IF EXISTS `t_device_command`;
CREATE TABLE `t_device_command`  (
  `id` bigint(0) NOT NULL COMMENT '雪花ID',
  `tenant_id` bigint(0) NOT NULL COMMENT '所属租户,跟随设备',
  `device_id` bigint(0) NOT NULL COMMENT 't_device.id',
  `component_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '组件路径，空表示整机根服务',
  `service_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '服务标识符',
  `command_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '幂等ID(UUID)',
  `priority` tinyint(0) NOT NULL DEFAULT 3 COMMENT '1紧急/2高/3普通/4低',
  `request_body` json NOT NULL COMMENT '请求体',
  `response_body` json NULL COMMENT '响应体',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SENT/SUCCESS/FAILED/TIMEOUT/CANCELLED',
  `sent_time` datetime(0) NULL DEFAULT NULL,
  `ack_time` datetime(0) NULL DEFAULT NULL,
  `cost_ms` int(0) NULL DEFAULT NULL,
  `user_id` bigint(0) NULL DEFAULT NULL,
  `source` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'API/SCHEDULE/RULE_ENGINE',
  `route_source_device_id` bigint(0) NULL DEFAULT NULL COMMENT '发送时可信网关/直连会话设备 t_device.id',
  `route_gun_no` smallint(0) UNSIGNED NULL DEFAULT NULL COMMENT '发送时可信枪号，非枪级命令为空',
  `owner_access_fencing_epoch` bigint(0) NULL DEFAULT NULL COMMENT 'DOWNLINK owner 接入 epoch 快照；不是 t_protocol_endpoint.fencing_epoch',
  `access_channel_id` bigint(0) NULL DEFAULT NULL COMMENT '命令发送时唯一 DOWNLINK owner 的 t_access_channel.id',
  `template_revision_id` bigint(0) NULL DEFAULT NULL COMMENT '命令编码使用的 t_mapping_template_revision.id',
  `driver_id` bigint(0) NULL DEFAULT NULL COMMENT '命令执行使用的精确 t_protocol_driver.id',
  `params_canonical_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '实际绑定 params canonical JSON 的 SHA-256 小写 hex',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_command_tenant`(`tenant_id`, `command_id`) USING BTREE,
  INDEX `idx_cmd_device`(`device_id`) USING BTREE,
  INDEX `idx_cmd_status`(`status`) USING BTREE,
  INDEX `idx_cmd_tenant`(`tenant_id`) USING BTREE,
  INDEX `idx_cmd_route_source`(`tenant_id`, `route_source_device_id`, `status`) USING BTREE,
  INDEX `idx_cmd_owner_route_inflight`(`device_id`, `status`, `owner_access_fencing_epoch`, `access_channel_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备指令记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_device_component
-- ----------------------------
DROP TABLE IF EXISTS `t_device_component`;
CREATE TABLE `t_device_component`  (
  `id` bigint(0) NOT NULL COMMENT '雪花ID',
  `device_id` bigint(0) NOT NULL COMMENT '所属根设备 t_device.id',
  `component_ref_id` bigint(0) NOT NULL COMMENT '对应组件引用',
  `parent_component_id` bigint(0) NULL DEFAULT NULL COMMENT '上级组件实例；一级组件为空',
  `component_model_id` bigint(0) NOT NULL COMMENT '组件模型',
  `component_model_version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '实例锚定的组件模型版本',
  `component_no` int(0) NOT NULL COMMENT '组件序号 1..N',
  `component_id` varchar(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备内唯一寻址ID',
  `component_path` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '根到组件的完整寻址路径',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '组件业务状态',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  `active_device_id` bigint(0) GENERATED ALWAYS AS ((case when (`is_deleted` = 0) then `device_id` else NULL end)) STORED NULL,
  `active_component_id` varchar(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS ((case when (`is_deleted` = 0) then `component_id` else NULL end)) STORED NULL,
  `active_component_path` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS ((case when (`is_deleted` = 0) then `component_path` else NULL end)) STORED NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_device_component_id`(`active_device_id`, `active_component_id`) USING BTREE,
  UNIQUE INDEX `uk_device_component_path`(`active_device_id`, `active_component_path`) USING BTREE,
  INDEX `idx_device_component_device`(`device_id`) USING BTREE,
  INDEX `idx_device_component_ref`(`component_ref_id`) USING BTREE,
  INDEX `idx_device_component_parent`(`parent_component_id`) USING BTREE,
  INDEX `idx_device_component_model`(`component_model_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备组件实例台账' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_device_event
-- ----------------------------
DROP TABLE IF EXISTS `t_device_event`;
CREATE TABLE `t_device_event`  (
  `id` bigint(0) NOT NULL COMMENT '雪花ID',
  `tenant_id` bigint(0) NOT NULL COMMENT '所属租户,跟随设备',
  `device_id` bigint(0) NULL DEFAULT NULL COMMENT 't_device.id,站点级规则可为空',
  `component_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '触发告警的组件路径，空表示根设备',
  `station_id` bigint(0) NULL DEFAULT NULL COMMENT 't_station.id,站点级告警或设备归属场站',
  `rule_id` bigint(0) NULL DEFAULT NULL COMMENT '触发规则ID,设备主动事件为空',
  `identifier` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '事件identifier',
  `severity` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'TRIGGERED' COMMENT 'TRIGGERED/ACKED/RESOLVED/CLEARED',
  `trigger_time` datetime(0) NOT NULL,
  `clear_time` datetime(0) NULL DEFAULT NULL,
  `ack_time` datetime(0) NULL DEFAULT NULL,
  `ack_user_id` bigint(0) NULL DEFAULT NULL,
  `comment` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '处理备注',
  `resolve_user_id` bigint(0) NULL DEFAULT NULL COMMENT '人工关闭人',
  `resolve_time` datetime(0) NULL DEFAULT NULL COMMENT '人工关闭时间',
  `payload` json NULL COMMENT '事件携带数据',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_event_device_status`(`device_id`, `status`) USING BTREE,
  INDEX `idx_event_rule_status`(`rule_id`, `status`) USING BTREE,
  INDEX `idx_event_trigger`(`trigger_time`) USING BTREE,
  INDEX `idx_event_tenant`(`tenant_id`) USING BTREE,
  INDEX `idx_event_status_trigger`(`status`, `trigger_time`) USING BTREE,
  INDEX `idx_event_station_status`(`station_id`, `status`) USING BTREE,
  INDEX `idx_event_device_component_status`(`device_id`, `component_path`, `status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备告警实例' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_device_mapping_override
-- ----------------------------
DROP TABLE IF EXISTS `t_device_mapping_override`;
CREATE TABLE `t_device_mapping_override`  (
  `id` bigint(0) NOT NULL COMMENT '雪花ID',
  `access_channel_id` bigint(0) NOT NULL,
  `item_key` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '跨 revision 稳定元素身份',
  `component_path` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '具体实例；根为空串',
  `action` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'ADD/REPLACE/DISABLE',
  `element_kind` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '从 base item 复制或 ADD 指定，用于审计/owner',
  `element_identifier` varchar(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `addr_expr_override` json NULL,
  `transform_rule_override` json NULL,
  `collect_policy_override` json NULL,
  `row_version` bigint(0) NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `created_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `updated_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_device_mapping_override`(`access_channel_id`, `item_key`, `component_path`) USING BTREE,
  INDEX `idx_device_mapping_override_element`(`access_channel_id`, `component_path`, `element_kind`, `element_identifier`) USING BTREE,
  CONSTRAINT `fk_device_mapping_override_channel` FOREIGN KEY (`access_channel_id`) REFERENCES `t_access_channel` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备映射覆盖' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_device_model
-- ----------------------------
DROP TABLE IF EXISTS `t_device_model`;
CREATE TABLE `t_device_model`  (
  `id` bigint(0) NOT NULL,
  `code` varchar(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模型编码,用作 TDengine 超级表名后缀',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `device_type_id` bigint(0) NOT NULL,
  `manufacturer` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `model_kind` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DEVICE' COMMENT 'DEVICE 可实例化 / COMPONENT 仅被嵌入',
  `brand` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `model_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `node_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'DIRECT/GATEWAY/SUB_DEVICE',
  `comm_method` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'WIFI/ETHERNET/CELLULAR_4G/CELLULAR_5G;SUB_DEVICE 为空',
  `protocol_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '接入协议:MQTT/MODBUS_TCP/IEC104/...',
  `protocol_config` json NULL COMMENT '协议级配置',
  `version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '1.0.0',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/DEPRECATED',
  `tdengine_stable_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'TDengine 超级表名,由模型编码转义生成',
  `created_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `updated_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最后修改人',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  `spec_params` json NULL COMMENT '模型级规格属性值',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_device_model_code`(`code`) USING BTREE,
  INDEX `idx_model_type`(`device_type_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备模型' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_device_model_component_ref
-- ----------------------------
DROP TABLE IF EXISTS `t_device_model_component_ref`;
CREATE TABLE `t_device_model_component_ref`  (
  `id` bigint(0) NOT NULL COMMENT '雪花ID',
  `parent_model_id` bigint(0) NOT NULL COMMENT '宿主设备模型',
  `parent_ref_id` bigint(0) NULL DEFAULT NULL COMMENT '上级组件引用；一级组件为空',
  `child_model_id` bigint(0) NOT NULL COMMENT '被引用的 COMPONENT 模型',
  `child_model_version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '嵌入时固定的精确版本',
  `component_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组件在宿主内的唯一角色编码',
  `min_count` int(0) NOT NULL DEFAULT 1 COMMENT '最小实例数',
  `max_count` int(0) NOT NULL DEFAULT 1 COMMENT '最大实例数',
  `sort_order` int(0) NOT NULL DEFAULT 0,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  `active_parent_model_id` bigint(0) GENERATED ALWAYS AS ((case when (`is_deleted` = 0) then `parent_model_id` else NULL end)) STORED NULL,
  `active_component_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS ((case when (`is_deleted` = 0) then `component_code` else NULL end)) STORED NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_model_component_code`(`active_parent_model_id`, `active_component_code`) USING BTREE,
  INDEX `idx_component_ref_parent_model`(`parent_model_id`) USING BTREE,
  INDEX `idx_component_ref_parent_ref`(`parent_ref_id`) USING BTREE,
  INDEX `idx_component_ref_child_model`(`child_model_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备模型组件引用' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_device_model_event
-- ----------------------------
DROP TABLE IF EXISTS `t_device_model_event`;
CREATE TABLE `t_device_model_event`  (
  `id` bigint(0) NOT NULL COMMENT '雪花ID',
  `model_id` bigint(0) NOT NULL COMMENT '所属设备模型',
  `identifier` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '事件标识',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `severity` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'WARNING' COMMENT 'INFO/WARNING/CRITICAL/EMERGENCY',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `output_params` json NULL COMMENT '事件输出参数定义',
  `auto_actions` json NULL COMMENT '自动动作定义',
  `catalog_id` bigint(0) NULL DEFAULT NULL COMMENT '引用 t_model_element_catalog.id',
  `sort_order` int(0) NOT NULL DEFAULT 0,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  `active_model_id` bigint(0) GENERATED ALWAYS AS ((case when (`is_deleted` = 0) then `model_id` else NULL end)) STORED NULL,
  `active_identifier` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS ((case when (`is_deleted` = 0) then `identifier` else NULL end)) STORED NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_model_event`(`active_model_id`, `active_identifier`) USING BTREE,
  INDEX `idx_event_model`(`model_id`) USING BTREE,
  INDEX `idx_event_catalog`(`catalog_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备事件定义' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_device_model_point
-- ----------------------------
DROP TABLE IF EXISTS `t_device_model_point`;
CREATE TABLE `t_device_model_point`  (
  `id` bigint(0) NOT NULL,
  `model_id` bigint(0) NOT NULL,
  `point_key` varchar(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '测点标识(模型内唯一)',
  `point_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `point_kind` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'TELEMETRY' COMMENT 'TELEMETRY/ATTRIBUTE/COMMAND',
  `data_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'FLOAT/INT/BOOL/STRING/ENUM',
  `unit` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `read_write` varchar(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'R' COMMENT 'R/W/RW',
  `range_min` decimal(20, 6) NULL DEFAULT NULL,
  `range_max` decimal(20, 6) NULL DEFAULT NULL,
  `scale` decimal(20, 6) NOT NULL DEFAULT 1.000000,
  `offset` decimal(20, 6) NOT NULL DEFAULT 0.000000,
  `protocol_addr` json NULL COMMENT '协议相关映射,如 {\"jsonPath\":\"$.activePower\"}',
  `enum_dict` json NULL,
  `sort_order` int(0) NOT NULL DEFAULT 0,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  `catalog_id` bigint(0) NULL DEFAULT NULL COMMENT '引用 t_model_element_catalog.id',
  `value_scope` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'ATTRIBUTE 取值层级:MODEL/DEVICE',
  `tag_indexed` tinyint(0) NOT NULL DEFAULT 0 COMMENT '仅 ATTRIBUTE + value_scope=DEVICE 可标记为低频静态 TAG',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_model_point`(`model_id`, `point_key`) USING BTREE,
  INDEX `idx_point_model`(`model_id`) USING BTREE,
  INDEX `idx_model_point_catalog`(`catalog_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备模型测点定义' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_device_point_mapping
-- ----------------------------
DROP TABLE IF EXISTS `t_device_point_mapping`;
CREATE TABLE `t_device_point_mapping`  (
  `id` bigint(0) NOT NULL COMMENT '雪花ID',
  `device_id` bigint(0) NOT NULL COMMENT '子设备 t_device.id',
  `channel_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '网关通道chId,空=匹配所有通道',
  `point_id` int(0) NOT NULL COMMENT '网关侧点号pId',
  `point_key` varchar(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '物模型测点标识',
  `data_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `scale` decimal(20, 6) NOT NULL DEFAULT 1.000000,
  `offset` decimal(20, 6) NOT NULL DEFAULT 0.000000,
  `collect_interval_seconds` int(0) NULL DEFAULT NULL COMMENT '映射级采集周期覆盖,秒',
  `enabled` tinyint(0) NOT NULL DEFAULT 1,
  `sort_order` int(0) NOT NULL DEFAULT 0,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  `active_device_id` bigint(0) GENERATED ALWAYS AS ((case when (`is_deleted` = 0) then `device_id` else NULL end)) STORED NULL,
  `active_channel_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS ((case when (`is_deleted` = 0) then `channel_id` else NULL end)) STORED NULL,
  `active_point_id` int(0) GENERATED ALWAYS AS ((case when (`is_deleted` = 0) then `point_id` else NULL end)) STORED NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_device_point`(`active_device_id`, `active_channel_id`, `active_point_id`) USING BTREE,
  INDEX `idx_mapping_device`(`device_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备点号映射(模式B/云边同步)' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_device_point_value
-- ----------------------------
DROP TABLE IF EXISTS `t_device_point_value`;
CREATE TABLE `t_device_point_value`  (
  `id` bigint(0) NOT NULL,
  `tenant_id` bigint(0) NOT NULL DEFAULT 2069700000000000001 COMMENT '所属租户,跟随设备',
  `device_id` bigint(0) NOT NULL COMMENT '关联 t_device.id(设备主键,非 SN)',
  `component_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '组件实例 component_id；NULL 表示设备根实例',
  `point_id` bigint(0) NULL DEFAULT NULL COMMENT '属性定义 t_device_model_point.id 快照(模型升版本可能变,允许空)',
  `point_key` varchar(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '属性标识(模型内唯一,稳定锚点)',
  `point_value` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '属性值文本表示,类型由模型测点 data_type 解释',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_dpv_device_component_point`(`device_id`, `component_id`, `point_key`) USING BTREE,
  INDEX `idx_dpv_device`(`device_id`) USING BTREE,
  INDEX `idx_dpv_tenant`(`tenant_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备属性值(ATTRIBUTE 测点的 per-device 取值)' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_device_shadow
-- ----------------------------
DROP TABLE IF EXISTS `t_device_shadow`;
CREATE TABLE `t_device_shadow`  (
  `device_id` bigint(0) NOT NULL COMMENT 't_device.id',
  `component_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '组件ID，空字符串表示根设备',
  `tenant_id` bigint(0) NOT NULL COMMENT '所属租户,跟随设备',
  `reported` json NULL COMMENT '最近上报属性值(物模型格式)',
  `reported_version` bigint(0) NOT NULL DEFAULT 0,
  `reported_time` datetime(0) NULL DEFAULT NULL,
  `desired` json NULL COMMENT '云端期望属性值',
  `desired_version` bigint(0) NOT NULL DEFAULT 0,
  `desired_time` datetime(0) NULL DEFAULT NULL,
  `version` bigint(0) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`device_id`, `component_id`) USING BTREE,
  INDEX `idx_shadow_tenant`(`tenant_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备影子' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_device_type
-- ----------------------------
DROP TABLE IF EXISTS `t_device_type`;
CREATE TABLE `t_device_type`  (
  `id` bigint(0) NOT NULL,
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型编码',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `category` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'deprecated:由 level=1 领域取代',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `parent_id` bigint(0) NULL DEFAULT NULL COMMENT '上级设备类型节点;一级领域为空',
  `level` tinyint(0) NOT NULL DEFAULT 3 COMMENT '层级:1领域/2场景/3设备类型',
  `sort_order` int(0) NOT NULL DEFAULT 0 COMMENT '同级排序',
  `icon_url` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '类型示意图/产品图:data-URI(base64)或URL',
  `is_builtin` tinyint(0) NOT NULL DEFAULT 0 COMMENT '平台内置节点:1是/0否',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_device_type_code`(`code`) USING BTREE,
  INDEX `idx_device_type_parent`(`parent_id`) USING BTREE,
  INDEX `idx_device_type_level`(`level`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备类型' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_gateway
-- ----------------------------
DROP TABLE IF EXISTS `t_gateway`;
CREATE TABLE `t_gateway`  (
  `device_id` bigint(0) NOT NULL COMMENT '网关设备 t_device.id',
  `tenant_id` bigint(0) NOT NULL COMMENT '所属租户,跟随设备',
  `gateway_sn` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '网关SN(契约标识,全局唯一)',
  `software_version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `config_version` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '网关上报配置版本',
  `expected_config_version` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '平台期望配置版本',
  `last_heartbeat_time` datetime(0) NULL DEFAULT NULL,
  `heartbeat_interval` int(0) NOT NULL DEFAULT 30,
  `sub_device_count` int(0) NOT NULL DEFAULT 0,
  `extra` json NULL,
  `version` bigint(0) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`device_id`) USING BTREE,
  UNIQUE INDEX `uk_gateway_sn`(`gateway_sn`) USING BTREE,
  INDEX `idx_gateway_tenant`(`tenant_id`) USING BTREE,
  INDEX `idx_gateway_heartbeat`(`last_heartbeat_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '网关运行时' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_mapping_template
-- ----------------------------
DROP TABLE IF EXISTS `t_mapping_template`;
CREATE TABLE `t_mapping_template`  (
  `id` bigint(0) NOT NULL COMMENT '雪花ID',
  `template_code` varchar(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'owner/driver/model 内稳定模板编码',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `driver_key` varchar(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '稳定驱动键，不是 driver_id',
  `model_id` bigint(0) NOT NULL COMMENT '语义设备模型',
  `owner_scope` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'PLATFORM/TENANT',
  `owner_tenant_id` bigint(0) NOT NULL,
  `visibility` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'PUBLIC/PRIVATE',
  `created_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `updated_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_mapping_template_owner_driver_model_code`(`owner_scope`, `owner_tenant_id`, `driver_key`, `model_id`, `template_code`) USING BTREE,
  INDEX `idx_mapping_template_model`(`model_id`) USING BTREE,
  INDEX `idx_mapping_template_owner`(`owner_scope`, `owner_tenant_id`, `visibility`) USING BTREE,
  INDEX `fk_mapping_template_owner`(`owner_tenant_id`) USING BTREE,
  CONSTRAINT `fk_mapping_template_model` FOREIGN KEY (`model_id`) REFERENCES `t_device_model` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_mapping_template_owner` FOREIGN KEY (`owner_tenant_id`) REFERENCES `t_tenant` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '映射模板主体' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_mapping_template_item
-- ----------------------------
DROP TABLE IF EXISTS `t_mapping_template_item`;
CREATE TABLE `t_mapping_template_item`  (
  `id` bigint(0) NOT NULL COMMENT '雪花ID',
  `revision_id` bigint(0) NOT NULL,
  `item_key` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '跨 revision 稳定元素身份',
  `element_kind` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'TELEMETRY_POINT/COMMAND_SERVICE',
  `element_identifier` varchar(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'point_key 或 serviceIdentifier',
  `component_ref_selector` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '$root',
  `path_pattern` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '根为空；如 gun-{no}',
  `requiredness` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'OPTIONAL' COMMENT 'REQUIRED/OPTIONAL',
  `addr_expr` json NULL COMMENT '协议寻址或编码字段绑定',
  `transform_rule` json NULL COMMENT 'scale/offset/字节序等唯一转换归属',
  `collect_policy` json NULL COMMENT '采集周期/超时/重试等',
  `params_schema` json NULL COMMENT 'COMMAND_SERVICE 参数 JSON Schema',
  `param_bindings` json NULL COMMENT 'COMMAND_SERVICE 参数到协议字段绑定',
  `sort_order` int(0) NOT NULL DEFAULT 0,
  `created_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `updated_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_mapping_item_key`(`revision_id`, `item_key`) USING BTREE,
  INDEX `idx_mapping_item_element`(`revision_id`, `component_ref_selector`, `element_kind`, `element_identifier`) USING BTREE,
  INDEX `idx_mapping_item_revision_order`(`revision_id`, `sort_order`, `id`) USING BTREE,
  CONSTRAINT `fk_mapping_item_revision` FOREIGN KEY (`revision_id`) REFERENCES `t_mapping_template_revision` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '映射模板 revision 元素' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_mapping_template_revision
-- ----------------------------
DROP TABLE IF EXISTS `t_mapping_template_revision`;
CREATE TABLE `t_mapping_template_revision`  (
  `id` bigint(0) NOT NULL COMMENT '雪花ID',
  `template_id` bigint(0) NOT NULL,
  `driver_id` bigint(0) NOT NULL COMMENT '精确驱动版本',
  `revision_no` int(0) NOT NULL COMMENT '模板主体内单调递增',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/DEPRECATED',
  `source` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'USER_DEFINED/DRIVER_BUILTIN',
  `template_config` json NOT NULL COMMENT '消息级解析或编码配置',
  `contract_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'BUILT_IN revision 固定契约 hash',
  `published_at` datetime(0) NULL DEFAULT NULL,
  `created_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `updated_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_mapping_template_revision`(`template_id`, `revision_no`) USING BTREE,
  INDEX `idx_mapping_revision_driver`(`driver_id`) USING BTREE,
  INDEX `idx_mapping_revision_status`(`template_id`, `status`) USING BTREE,
  CONSTRAINT `fk_mapping_revision_driver` FOREIGN KEY (`driver_id`) REFERENCES `t_protocol_driver` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_mapping_revision_template` FOREIGN KEY (`template_id`) REFERENCES `t_mapping_template` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '映射模板不可变 revision' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_model_element_alias
-- ----------------------------
DROP TABLE IF EXISTS `t_model_element_alias`;
CREATE TABLE `t_model_element_alias`  (
  `id` bigint(0) NOT NULL COMMENT '雪花ID',
  `kind` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'POINT/ATTRIBUTE/COMMAND/EVENT',
  `alias` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '历史/厂商标识',
  `canonical_identifier` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'canonical identifier',
  `source` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  `active_kind` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS ((case when (`is_deleted` = 0) then `kind` else NULL end)) STORED NULL,
  `active_alias` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS ((case when (`is_deleted` = 0) then `alias` else NULL end)) STORED NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_alias`(`active_kind`, `active_alias`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '物模型元素别名' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_model_element_catalog
-- ----------------------------
DROP TABLE IF EXISTS `t_model_element_catalog`;
CREATE TABLE `t_model_element_catalog`  (
  `id` bigint(0) NOT NULL COMMENT '雪花ID',
  `kind` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'POINT/ATTRIBUTE/COMMAND/EVENT',
  `domain` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'DEVICE/STATION/SHARED',
  `identifier` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'canonical identifier',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `data_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `unit` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `range_min` decimal(20, 6) NULL DEFAULT NULL,
  `range_max` decimal(20, 6) NULL DEFAULT NULL,
  `enum_dict` json NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `tags` json NULL,
  `value_shape` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SCALAR' COMMENT 'SCALAR/INDEXED_SERIES',
  `series_config` json NULL COMMENT '索引序列配置',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  `active_kind` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS ((case when (`is_deleted` = 0) then `kind` else NULL end)) STORED NULL,
  `active_identifier` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS ((case when (`is_deleted` = 0) then `identifier` else NULL end)) STORED NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_catalog_kind_identifier`(`active_kind`, `active_identifier`) USING BTREE,
  INDEX `idx_catalog_kind_domain`(`kind`, `domain`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '物模型元素字典库' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_notification_channel
-- ----------------------------
DROP TABLE IF EXISTS `t_notification_channel`;
CREATE TABLE `t_notification_channel`  (
  `id` bigint(0) NOT NULL COMMENT '雪花ID',
  `tenant_id` bigint(0) NOT NULL COMMENT '所属租户',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '渠道名称',
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'WEBHOOK/EMAIL/DINGTALK/WECOM/SMS',
  `config` json NOT NULL COMMENT '渠道配置JSON',
  `enabled` tinyint(0) NOT NULL DEFAULT 1,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_notification_channel_tenant`(`tenant_id`, `enabled`) USING BTREE,
  INDEX `idx_notification_channel_type`(`type`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '告警通知渠道' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_notification_log
-- ----------------------------
DROP TABLE IF EXISTS `t_notification_log`;
CREATE TABLE `t_notification_log`  (
  `id` bigint(0) NOT NULL COMMENT '雪花ID',
  `tenant_id` bigint(0) NOT NULL COMMENT '所属租户',
  `event_id` bigint(0) NOT NULL COMMENT 't_device_event.id',
  `channel_id` bigint(0) NOT NULL COMMENT 't_notification_channel.id',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'SUCCESS/FAILED/RETRYING',
  `attempts` int(0) NOT NULL DEFAULT 0 COMMENT '发送尝试次数',
  `last_error` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最近一次错误',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_notification_event_channel`(`event_id`, `channel_id`) USING BTREE,
  INDEX `idx_notification_log_tenant`(`tenant_id`) USING BTREE,
  INDEX `idx_notification_log_channel`(`channel_id`, `status`) USING BTREE,
  INDEX `idx_notification_log_event`(`event_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '告警通知发送记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_org
-- ----------------------------
DROP TABLE IF EXISTS `t_org`;
CREATE TABLE `t_org`  (
  `id` bigint(0) NOT NULL,
  `tenant_id` bigint(0) NOT NULL,
  `parent_id` bigint(0) NULL DEFAULT NULL,
  `level` tinyint(0) NOT NULL DEFAULT 0,
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `sort` int(0) NOT NULL DEFAULT 0,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  `active_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS ((case when (`is_deleted` = 0) then `code` else NULL end)) STORED COMMENT '未删除组织编码' NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_org_tenant_active_code`(`tenant_id`, `active_code`) USING BTREE,
  INDEX `idx_org_parent`(`parent_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '组织架构' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_org_data_scope
-- ----------------------------
DROP TABLE IF EXISTS `t_org_data_scope`;
CREATE TABLE `t_org_data_scope`  (
  `id` bigint(0) NOT NULL,
  `org_id` bigint(0) NOT NULL,
  `station_id` bigint(0) NOT NULL,
  `access` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'READ/WRITE',
  `include_sub_org` tinyint(0) NOT NULL DEFAULT 0,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_org_data_scope`(`org_id`, `station_id`) USING BTREE,
  INDEX `idx_org_data_scope_station`(`station_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '组织数据权限' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_permission
-- ----------------------------
DROP TABLE IF EXISTS `t_permission`;
CREATE TABLE `t_permission`  (
  `id` bigint(0) NOT NULL,
  `code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权限编码,如 device:write',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `resource` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '资源,如 device',
  `action` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '动作,如 read/write',
  `scope` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'TENANT' COMMENT 'PLATFORM/TENANT',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_permission_code`(`code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '权限' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_permission_policy
-- ----------------------------
DROP TABLE IF EXISTS `t_permission_policy`;
CREATE TABLE `t_permission_policy`  (
  `id` bigint(0) NOT NULL,
  `tenant_id` bigint(0) NOT NULL COMMENT '归属租户;系统租户=平台级策略',
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '策略编码(租户内唯一,软删后可复用)',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `builtin` tinyint(0) NOT NULL DEFAULT 0 COMMENT '预置核心策略,不可删',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  `active_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS ((case when (`is_deleted` = 0) then `code` else NULL end)) STORED COMMENT '未删除策略编码,软删后唯一约束自动释放' NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_policy_tenant_active_code`(`tenant_id`, `active_code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '权限策略' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_permission_policy_item
-- ----------------------------
DROP TABLE IF EXISTS `t_permission_policy_item`;
CREATE TABLE `t_permission_policy_item`  (
  `id` bigint(0) NOT NULL,
  `policy_id` bigint(0) NOT NULL,
  `permission_id` bigint(0) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_policy_item`(`policy_id`, `permission_id`) USING BTREE,
  INDEX `idx_ppi_permission`(`permission_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '策略-权限明细' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_protocol_driver
-- ----------------------------
DROP TABLE IF EXISTS `t_protocol_driver`;
CREATE TABLE `t_protocol_driver`  (
  `id` bigint(0) NOT NULL COMMENT '雪花ID；精确驱动版本 driver_id',
  `driver_key` varchar(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '跨版本稳定驱动键',
  `version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '不可变语义版本',
  `driver_family` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '凭证分槽与运行时家族',
  `driver_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'MESSAGE/SESSION/POLLING',
  `mapping_mode` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'CONFIGURABLE/BUILT_IN',
  `conn_params_schema` json NOT NULL COMMENT '冻结 JSON Schema',
  `point_contract` json NULL COMMENT 'BUILT_IN 必填的组件维度契约',
  `contract_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'point_contract canonical SHA-256',
  `owner_scope` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'PLATFORM/TENANT；S1 仅开放 PLATFORM driver',
  `owner_tenant_id` bigint(0) NOT NULL COMMENT '真实租户行；平台资产引用系统租户',
  `created_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `updated_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_protocol_driver_version`(`driver_key`, `version`) USING BTREE,
  INDEX `idx_protocol_driver_owner`(`owner_scope`, `owner_tenant_id`) USING BTREE,
  INDEX `fk_protocol_driver_owner`(`owner_tenant_id`) USING BTREE,
  CONSTRAINT `fk_protocol_driver_owner` FOREIGN KEY (`owner_tenant_id`) REFERENCES `t_tenant` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '协议驱动不可变版本' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_protocol_endpoint
-- ----------------------------
DROP TABLE IF EXISTS `t_protocol_endpoint`;
CREATE TABLE `t_protocol_endpoint`  (
  `id` bigint(0) NOT NULL COMMENT '雪花ID',
  `endpoint_code` varchar(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'owner 内稳定端点编码',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `driver_id` bigint(0) NOT NULL COMMENT '精确驱动版本',
  `connection_role` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'SERVER_PASSIVE/CLIENT_POLL',
  `executor` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'PLATFORM/GATEWAY',
  `conn_params` json NOT NULL COMMENT '端点级监听或共享连接参数',
  `desired_state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'STOPPED' COMMENT 'STOPPED/RUNNING',
  `effective_state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'STOPPED' COMMENT 'STOPPED/STARTING/RUNNING/DRAINING/FAILED/CONFLICT/LEADER_WAIT',
  `effective_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `runtime_generation` bigint(0) NOT NULL DEFAULT 0 COMMENT '配置重载世代',
  `effective_generation` bigint(0) NOT NULL DEFAULT 0 COMMENT '当前实际服务世代',
  `fencing_epoch` bigint(0) NOT NULL DEFAULT 0 COMMENT '端点执行 lease holder 的持久化单调 fencing',
  `bound_gateway_id` bigint(0) NULL DEFAULT NULL COMMENT 'executor=GATEWAY 时必填 t_device.id',
  `owner_scope` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'PLATFORM/TENANT',
  `owner_tenant_id` bigint(0) NOT NULL,
  `row_version` bigint(0) NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `created_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `updated_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_protocol_endpoint_owner_code`(`owner_scope`, `owner_tenant_id`, `endpoint_code`) USING BTREE,
  INDEX `idx_protocol_endpoint_driver`(`driver_id`) USING BTREE,
  INDEX `idx_protocol_endpoint_gateway`(`bound_gateway_id`) USING BTREE,
  INDEX `fk_protocol_endpoint_owner`(`owner_tenant_id`) USING BTREE,
  CONSTRAINT `fk_protocol_endpoint_driver` FOREIGN KEY (`driver_id`) REFERENCES `t_protocol_driver` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_protocol_endpoint_gateway` FOREIGN KEY (`bound_gateway_id`) REFERENCES `t_device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_protocol_endpoint_owner` FOREIGN KEY (`owner_tenant_id`) REFERENCES `t_tenant` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '共享协议端点' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_role
-- ----------------------------
DROP TABLE IF EXISTS `t_role`;
CREATE TABLE `t_role`  (
  `id` bigint(0) NOT NULL,
  `tenant_id` bigint(0) NOT NULL DEFAULT 2069700000000000001,
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色编码',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_tenant_code`(`tenant_id`, `code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `t_role_permission`;
CREATE TABLE `t_role_permission`  (
  `id` bigint(0) NOT NULL,
  `role_id` bigint(0) NOT NULL,
  `permission_id` bigint(0) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_permission`(`role_id`, `permission_id`) USING BTREE,
  INDEX `idx_rp_permission`(`permission_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色-权限' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_role_policy
-- ----------------------------
DROP TABLE IF EXISTS `t_role_policy`;
CREATE TABLE `t_role_policy`  (
  `id` bigint(0) NOT NULL,
  `role_id` bigint(0) NOT NULL,
  `policy_id` bigint(0) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_policy`(`role_id`, `policy_id`) USING BTREE,
  INDEX `idx_rp_policy`(`policy_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色-策略绑定' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_station
-- ----------------------------
DROP TABLE IF EXISTS `t_station`;
CREATE TABLE `t_station`  (
  `id` bigint(0) NOT NULL,
  `tenant_id` bigint(0) NOT NULL DEFAULT 2069700000000000001,
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '电站编码',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `longitude` decimal(17, 14) NULL DEFAULT NULL,
  `latitude` decimal(17, 14) NULL DEFAULT NULL,
  `coordinate_system` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'WGS84' COMMENT '原始坐标系：WGS84/GCJ02/BD09',
  `map_longitude` decimal(17, 14) NULL DEFAULT NULL COMMENT '地图查询统一经度（WGS84）',
  `map_latitude` decimal(17, 14) NULL DEFAULT NULL COMMENT '地图查询统一纬度（WGS84）',
  `country` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '国家',
  `province` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '省/直辖市/自治区',
  `city` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地市',
  `district` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '区/县',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '详细地址',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ACTIVE',
  `is_demo` tinyint(0) NOT NULL DEFAULT 0 COMMENT '演示数据标记',
  `created_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `updated_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最后修改人',
  `model_id` bigint(0) NULL DEFAULT NULL COMMENT '引用场站模型;NULL 兼容存量',
  `attributes` json NULL COMMENT '逐站属性值 {attr_key: value}',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_station_tenant_code`(`tenant_id`, `code`) USING BTREE,
  UNIQUE INDEX `uk_station_tenant_id`(`tenant_id`, `id`) USING BTREE,
  INDEX `idx_station_model`(`model_id`) USING BTREE,
  INDEX `idx_station_map_location`(`tenant_id`, `map_longitude`, `map_latitude`, `is_deleted`) USING BTREE,
  CONSTRAINT `fk_station_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `t_tenant` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '电站' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_station_attribute_change_log
-- ----------------------------
DROP TABLE IF EXISTS `t_station_attribute_change_log`;
CREATE TABLE `t_station_attribute_change_log`  (
  `id` bigint(0) NOT NULL COMMENT '雪花ID',
  `tenant_id` bigint(0) NOT NULL COMMENT '所属租户',
  `station_id` bigint(0) NOT NULL COMMENT '场站ID',
  `attr_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '场站模型属性标识',
  `old_value` json NULL COMMENT '变更前值',
  `new_value` json NULL COMMENT '变更后值',
  `operator` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作人用户名',
  `changed_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '变更时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_station_attr_history`(`station_id`, `attr_key`, `changed_at`, `id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '场站实例属性值变更历史' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_station_model
-- ----------------------------
DROP TABLE IF EXISTS `t_station_model`;
CREATE TABLE `t_station_model`  (
  `id` bigint(0) NOT NULL,
  `owner_scope` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PLATFORM' COMMENT 'PLATFORM/TENANT',
  `owner_tenant_id` bigint(0) NOT NULL DEFAULT 2069700000000000001 COMMENT '系统租户雪花ID',
  `visibility` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PUBLIC' COMMENT 'PUBLIC/PRIVATE',
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `energy_scenes` json NULL COMMENT '[\"pv\",\"ess\",\"charging\",\"microgrid\",\"load\",\"swap\"]',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  `active_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS ((case when (`is_deleted` = 0) then `code` else NULL end)) STORED COMMENT '未删除模型编码,软删后唯一约束自动释放' NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_station_model_owner_active_code`(`owner_scope`, `owner_tenant_id`, `active_code`) USING BTREE,
  INDEX `idx_station_model_owner`(`owner_scope`, `owner_tenant_id`) USING BTREE,
  INDEX `idx_station_model_visibility`(`visibility`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '场站模型' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_station_model_attr
-- ----------------------------
DROP TABLE IF EXISTS `t_station_model_attr`;
CREATE TABLE `t_station_model_attr`  (
  `id` bigint(0) NOT NULL,
  `model_id` bigint(0) NOT NULL,
  `attr_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `attr_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `data_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'NUMBER/STRING/DATE/BOOL/ENUM',
  `access_mode` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'READ_WRITE' COMMENT '实例值访问模式：READ_ONLY/READ_WRITE',
  `unit` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `enum_dict` json NULL,
  `required` tinyint(0) NOT NULL DEFAULT 0,
  `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `sort` int(0) NOT NULL DEFAULT 0,
  `default_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  `active_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS ((case when (`is_deleted` = 0) then `attr_key` else NULL end)) STORED COMMENT '未删除属性标识,软删后唯一约束自动释放' NULL,
  `catalog_id` bigint(0) NULL DEFAULT NULL COMMENT '引用 t_model_element_catalog.id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_station_model_attr_active_key`(`model_id`, `active_key`) USING BTREE,
  INDEX `idx_attr_model`(`model_id`) USING BTREE,
  INDEX `idx_station_attr_catalog`(`catalog_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '场站模型属性定义' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_tenant
-- ----------------------------
DROP TABLE IF EXISTS `t_tenant`;
CREATE TABLE `t_tenant`  (
  `id` bigint(0) NOT NULL,
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户编码',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户名称',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
  `admin_user_id` bigint(0) NULL DEFAULT NULL COMMENT '租户管理员主账号',
  `edition` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'STANDARD' COMMENT '版本',
  `expire_at` datetime(0) NULL DEFAULT NULL COMMENT '租户到期时间',
  `parent_id` bigint(0) NULL DEFAULT NULL COMMENT '父租户/集团预留',
  `contact_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `contact_email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系邮箱',
  `created_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `updated_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最后修改人',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  `active_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS ((case when (`is_deleted` = 0) then `code` else NULL end)) STORED COMMENT '未删除租户编码' NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_active_code`(`active_code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租户' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_tenant_branding
-- ----------------------------
DROP TABLE IF EXISTS `t_tenant_branding`;
CREATE TABLE `t_tenant_branding`  (
  `id` bigint(0) NOT NULL,
  `tenant_id` bigint(0) NOT NULL COMMENT '所属租户',
  `platform_name_i18n` json NULL COMMENT '平台名称多语言覆盖,如 {\"zh-CN\":\"..\",\"en-US\":\"..\"}',
  `primary_color` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '品牌主色,#hex',
  `accent_color` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '品牌辅助色,#hex',
  `brand_text_color` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '品牌文字色,#hex',
  `page_background_color` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '页面背景色,#hex',
  `header_background_color` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '顶部栏背景色,#hex',
  `sidebar_background_color` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '侧边栏背景色,#hex',
  `dark_primary_color` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '深色主题品牌主色,#hex',
  `dark_accent_color` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '深色主题品牌辅助色,#hex',
  `dark_brand_text_color` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '深色主题品牌文字色,#hex',
  `dark_page_background_color` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '深色主题页面背景色,#hex',
  `dark_header_background_color` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '深色主题顶部栏背景色,#hex',
  `dark_sidebar_background_color` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '深色主题侧边栏背景色,#hex',
  `logo_light` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '亮色 logo,data URL(base64)',
  `logo_dark` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '暗色 logo,data URL(base64)',
  `favicon` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '浏览器图标,data URL(base64)',
  `login_background` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '登录背景,data URL(base64)',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant`(`tenant_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租户品牌配置' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_tenant_data_scope
-- ----------------------------
DROP TABLE IF EXISTS `t_tenant_data_scope`;
CREATE TABLE `t_tenant_data_scope`  (
  `id` bigint(0) NOT NULL,
  `tenant_id` bigint(0) NOT NULL,
  `station_id` bigint(0) NOT NULL,
  `access` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'READ/WRITE',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_data_scope`(`tenant_id`, `station_id`) USING BTREE,
  INDEX `idx_tenant_data_scope_station`(`station_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租户数据权限上限' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_tenant_grant
-- ----------------------------
DROP TABLE IF EXISTS `t_tenant_grant`;
CREATE TABLE `t_tenant_grant`  (
  `id` bigint(0) NOT NULL,
  `tenant_id` bigint(0) NOT NULL,
  `permission_id` bigint(0) NOT NULL,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_grant`(`tenant_id`, `permission_id`) USING BTREE,
  INDEX `idx_tenant_grant_permission`(`permission_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租户功能授权上限' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_tenant_grant_policy
-- ----------------------------
DROP TABLE IF EXISTS `t_tenant_grant_policy`;
CREATE TABLE `t_tenant_grant_policy`  (
  `id` bigint(0) NOT NULL,
  `tenant_id` bigint(0) NOT NULL COMMENT '被授权的业务租户',
  `policy_id` bigint(0) NOT NULL COMMENT '指向系统租户的平台级策略',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_grant_policy`(`tenant_id`, `policy_id`) USING BTREE,
  INDEX `idx_tgp_policy`(`policy_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租户上限-策略绑定' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_tenant_license
-- ----------------------------
DROP TABLE IF EXISTS `t_tenant_license`;
CREATE TABLE `t_tenant_license`  (
  `id` bigint(0) NOT NULL,
  `tenant_id` bigint(0) NOT NULL,
  `edition` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'STANDARD',
  `features` json NULL COMMENT '功能码集合',
  `max_stations` int(0) NOT NULL DEFAULT -1,
  `max_devices` int(0) NOT NULL DEFAULT -1,
  `max_users` int(0) NOT NULL DEFAULT -1,
  `issued_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `expire_at` datetime(0) NULL DEFAULT NULL,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_license`(`tenant_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租户授权与配额' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_user
-- ----------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user`  (
  `id` bigint(0) NOT NULL,
  `tenant_id` bigint(0) NOT NULL DEFAULT 2069700000000000001 COMMENT '所属租户',
  `org_id` bigint(0) NULL DEFAULT NULL COMMENT '所属组织',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'BCrypt 加密',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
  `user_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NORMAL' COMMENT 'PLATFORM_SUPER/TENANT_ADMIN/NORMAL',
  `must_change_password` tinyint(0) NOT NULL DEFAULT 0 COMMENT '是否必须改密',
  `last_login_at` datetime(0) NULL DEFAULT NULL,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `is_deleted` tinyint(0) NOT NULL DEFAULT 0,
  `active_username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS ((case when (`is_deleted` = 0) then `username` else NULL end)) STORED COMMENT '未删除用户账号' NULL,
  `active_email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS ((case when (`is_deleted` = 0) then `email` else NULL end)) STORED COMMENT '未删除用户邮箱' NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_active_username`(`active_username`) USING BTREE,
  UNIQUE INDEX `uk_user_active_email`(`active_email`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_user_role
-- ----------------------------
DROP TABLE IF EXISTS `t_user_role`;
CREATE TABLE `t_user_role`  (
  `id` bigint(0) NOT NULL,
  `user_id` bigint(0) NOT NULL,
  `role_id` bigint(0) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_role`(`user_id`, `role_id`) USING BTREE,
  INDEX `idx_ur_role`(`role_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户-角色' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;

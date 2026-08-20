-- ============================================================
-- 迁移：W3-W11 全量权限初始化（DEC-20260820-015，用户批准）
-- Schema: evco_iam
-- 关联设计：docs/requirements/充电运营平台菜单架构-v4.1.md（权限推导唯一源）
--           delivery/execution/02-变更与审批日志.md DEC-20260820-014/015
-- 策略变更：原「权限按交付周分阶段初始化」改为 W2 一次全量入库（ACTIVE），
--           phase 字段标注各自交付周；超级管理员（is_super_admin=1）自动拥有
--           全部 ACTIVE 权限 → 登录即见全部管理端菜单（前端骨架占位页）。
-- 范围：P1 64 菜单 ＋ P2 9 菜单 = 73 资源 141 条权限
--       （5 个纯查看资源仅 read；68 个 read/write）
--       加 V202608240005 已有 10 资源 18 条，合计 83 资源 159 条。
-- 不设菜单权限的端：M1（公众开放）、M2/M3/M4（复用对应域权限）、D1（独立展示应用）。
-- 约定：UTF-8、前向 only、无 down migration、雪花 ID、脚本幂等；
--       menu_path 按菜单架构 v4.1 推导，各周页面清单（Step 2 产物）为路由
--       终审权威，出入时以校准迁移 UPDATE，不得直接改库；
--       ID 使用保留段 1000000000000000019+（19-159），不与运行时雪花 ID 冲突。
-- ============================================================

SET @now := NOW();

INSERT INTO `iam_permission`
    (`id`, `code`, `name`, `resource`, `action`, `scope`, `menu_path`, `description`, `is_builtin`, `status`, `phase`, `source`, `created_at`, `updated_at`)
VALUES
-- ============================================================
-- W3（5 资源 10 条）：资产域 + 租户管理拆分首期（DEC-20260820-014）
-- ============================================================
-- station 场站管理（P1 场站与运维>资产管理）
(1000000000000000019, 'station:read',              '场站管理-查看',     'station',              'read',  'TENANT',   '/station-maintenance/asset-management/station',              '查看场站列表、详情与变更记录',                             1, 'ACTIVE', 'W3', 'PLATFORM', @now, @now),
(1000000000000000020, 'station:write',             '场站管理-管理',     'station',              'write', 'TENANT',   '/station-maintenance/asset-management/station',              '场站资料/经营属性/收益模型/开放范围等写操作',               1, 'ACTIVE', 'W3', 'PLATFORM', @now, @now),
-- device 设备管理（P1 场站与运维>资产管理；含充电桩/计量设备/设备关系）
(1000000000000000021, 'device:read',               '设备管理-查看',     'device',               'read',  'TENANT',   '/station-maintenance/asset-management/device',               '查看充电设备/计量设备/运行状态/数据接入状态',               1, 'ACTIVE', 'W3', 'PLATFORM', @now, @now),
(1000000000000000022, 'device:write',              '设备管理-管理',     'device',               'write', 'TENANT',   '/station-maintenance/asset-management/device',               '设备档案/设备关系/重卡V2G属性等写操作',                     1, 'ACTIVE', 'W3', 'PLATFORM', @now, @now),
-- asset_field_template 资产字段模板（P1 场站与运维>资产管理）
(1000000000000000023, 'asset_field_template:read', '资产字段模板-查看', 'asset_field_template', 'read',  'TENANT',   '/station-maintenance/asset-management/asset-field-template', '查看资产字段模板/字段定义/适用范围/版本',                   1, 'ACTIVE', 'W3', 'PLATFORM', @now, @now),
(1000000000000000024, 'asset_field_template:write','资产字段模板-管理', 'asset_field_template', 'write', 'TENANT',   '/station-maintenance/asset-management/asset-field-template', '模板/字段定义/适用范围/授权等写操作',                       1, 'ACTIVE', 'W3', 'PLATFORM', @now, @now),
-- service_config 服务配置（P1 场站与运维>场站服务）
(1000000000000000025, 'service_config:read',       '服务配置-查看',     'service_config',       'read',  'TENANT',   '/station-maintenance/station-service/service-config',        '查看营业状态/开放对象/营业时间/服务标签',                   1, 'ACTIVE', 'W3', 'PLATFORM', @now, @now),
(1000000000000000026, 'service_config:write',      '服务配置-管理',     'service_config',       'write', 'TENANT',   '/station-maintenance/station-service/service-config',        '营业状态/开放规则/占位提醒时长等写操作',                     1, 'ACTIVE', 'W3', 'PLATFORM', @now, @now),
-- tenant 租户管理（P1 客户与权益>合作运营；DEC-20260820-014 拆分：W3 租户资料+权限闭环，W7 组织/角色/用户，W8 结算主体）
(1000000000000000027, 'tenant:read',               '租户管理-查看',     'tenant',               'read',  'TENANT',   '/customer-equity/partner-operation/tenant',                  '查看租户资料/组织架构/角色/用户/授权范围',                   1, 'ACTIVE', 'W3', 'PLATFORM', @now, @now),
(1000000000000000028, 'tenant:write',              '租户管理-管理',     'tenant',               'write', 'TENANT',   '/customer-equity/partner-operation/tenant',                  '租户资料/组织/角色/用户/服务范围等写操作（按交付周分批可用）', 1, 'ACTIVE', 'W3', 'PLATFORM', @now, @now),

-- ============================================================
-- W4（6 资源 11 条）：监控与分析域
-- ============================================================
-- real_time_monitor 实时监控（P1 场站与运维>运行维护）
(1000000000000000029, 'real_time_monitor:read',    '实时监控-查看',     'real_time_monitor',    'read',  'TENANT',   '/station-maintenance/operation-maintenance/real-time-monitor', '查看场站/设备/枪口实时状态与实时会话',                       1, 'ACTIVE', 'W4', 'PLATFORM', @now, @now),
(1000000000000000030, 'real_time_monitor:write',   '实时监控-处置',     'real_time_monitor',    'write', 'TENANT',   '/station-maintenance/operation-maintenance/real-time-monitor', '实时监控页发起的远程操作等写操作（执行由设备接入服务校验）',   1, 'ACTIVE', 'W4', 'PLATFORM', @now, @now),
-- station_data_analysis 场站数据分析（P1 场站与运维>运行维护）
(1000000000000000031, 'station_data_analysis:read','场站数据分析-查看', 'station_data_analysis','read',  'TENANT',   '/station-maintenance/operation-maintenance/station-data-analysis', '多场站多数据点曲线查询与下钻',                           1, 'ACTIVE', 'W4', 'PLATFORM', @now, @now),
(1000000000000000032, 'station_data_analysis:write','场站数据分析-导出', 'station_data_analysis','write', 'TENANT',   '/station-maintenance/operation-maintenance/station-data-analysis', '分析明细导出等写操作',                                   1, 'ACTIVE', 'W4', 'PLATFORM', @now, @now),
-- device_data_analysis 设备数据分析（P1 场站与运维>运行维护）
(1000000000000000033, 'device_data_analysis:read', '设备数据分析-查看', 'device_data_analysis', 'read',  'TENANT',   '/station-maintenance/operation-maintenance/device-data-analysis', '多设备多功能点曲线查询与下钻',                           1, 'ACTIVE', 'W4', 'PLATFORM', @now, @now),
(1000000000000000034, 'device_data_analysis:write','设备数据分析-导出', 'device_data_analysis', 'write', 'TENANT',   '/station-maintenance/operation-maintenance/device-data-analysis', '分析明细导出等写操作',                                   1, 'ACTIVE', 'W4', 'PLATFORM', @now, @now),
-- device_protocol_trace 设备协议追溯（菜单架构明确「仅平台级协议排障/审计权限可用」）
(1000000000000000035, 'device_protocol_trace:read','设备协议追溯-查看', 'device_protocol_trace','read',  'PLATFORM', '/station-maintenance/operation-maintenance/device-protocol-trace', '按设备/事件/命令/订单追溯证据链（原文默认脱敏）',           1, 'ACTIVE', 'W4', 'PLATFORM', @now, @now),
(1000000000000000036, 'device_protocol_trace:write','设备协议追溯-原文', 'device_protocol_trace','write', 'PLATFORM', '/station-maintenance/operation-maintenance/device-protocol-trace', '下载/查看完整原文（二次审批+审计）等写操作',               1, 'ACTIVE', 'W4', 'PLATFORM', @now, @now),
-- alarm 告警管理（P1 场站与运维>运行维护）
(1000000000000000037, 'alarm:read',                '告警管理-查看',     'alarm',                'read',  'TENANT',   '/station-maintenance/operation-maintenance/alarm',            '查看告警与统计',                                           1, 'ACTIVE', 'W4', 'PLATFORM', @now, @now),
(1000000000000000038, 'alarm:write',               '告警管理-处置',     'alarm',                'write', 'TENANT',   '/station-maintenance/operation-maintenance/alarm',            '告警确认/升级/屏蔽/恢复等写操作',                           1, 'ACTIVE', 'W4', 'PLATFORM', @now, @now),
-- remote_operation_record 远程操作记录（纯查看）
(1000000000000000039, 'remote_operation_record:read','远程操作记录-查看','remote_operation_record','read', 'TENANT',  '/station-maintenance/operation-maintenance/remote-operation-record', '查看启停/复位操作结果与审计记录',                         1, 'ACTIVE', 'W4', 'PLATFORM', @now, @now),

-- ============================================================
-- W5（3 资源 6 条）：交易与计费域
-- ============================================================
-- charging_order 充电订单（P1 交易与售后>充电交易）
(1000000000000000040, 'charging_order:read',       '充电订单-查看',     'charging_order',       'read',  'TENANT',   '/transaction-after-sales/charging-transaction/charging-order', '查看个人/企业/电卡充电订单',                                1, 'ACTIVE', 'W5', 'PLATFORM', @now, @now),
(1000000000000000041, 'charging_order:write',      '充电订单-管理',     'charging_order',       'write', 'TENANT',   '/transaction-after-sales/charging-transaction/charging-order', '异常处理/人工结算/订单调整等写操作',                         1, 'ACTIVE', 'W5', 'PLATFORM', @now, @now),
-- occupancy_reminder 占位提醒（P1 交易与售后>充电交易）
(1000000000000000042, 'occupancy_reminder:read',   '占位提醒-查看',     'occupancy_reminder',   'read',  'TENANT',   '/transaction-after-sales/charging-transaction/occupancy-reminder', '查看待提醒/提醒记录/已结束',                              1, 'ACTIVE', 'W5', 'PLATFORM', @now, @now),
(1000000000000000043, 'occupancy_reminder:write',  '占位提醒-管理',     'occupancy_reminder',   'write', 'TENANT',   '/transaction-after-sales/charging-transaction/occupancy-reminder', '提醒发送/处理等写操作',                                   1, 'ACTIVE', 'W5', 'PLATFORM', @now, @now),
-- charging_billing 充电计费管理（P1 计费与营销>计费管理）
(1000000000000000044, 'charging_billing:read',     '充电计费管理-查看', 'charging_billing',     'read',  'TENANT',   '/billing-marketing/billing-management/charging-billing',      '查看计费规则/服务费规则/时段价格/版本',                     1, 'ACTIVE', 'W5', 'PLATFORM', @now, @now),
(1000000000000000045, 'charging_billing:write',    '充电计费管理-管理', 'charging_billing',     'write', 'TENANT',   '/billing-marketing/billing-management/charging-billing',      '计费规则/价格版本/发布等写操作',                             1, 'ACTIVE', 'W5', 'PLATFORM', @now, @now),

-- ============================================================
-- W6（4 资源 7 条）：售后与个人资金域
-- ============================================================
-- refund_difference 退款与补差（P1 交易与售后>售后服务）
(1000000000000000046, 'refund_difference:read',    '退款与补差-查看',   'refund_difference',    'read',  'TENANT',   '/transaction-after-sales/after-sales/refund-difference',      '查看退款/补差记录',                                         1, 'ACTIVE', 'W6', 'PLATFORM', @now, @now),
(1000000000000000047, 'refund_difference:write',   '退款与补差-处理',   'refund_difference',    'write', 'TENANT',   '/transaction-after-sales/after-sales/refund-difference',      '退款/补收/原路退款等写操作',                                 1, 'ACTIVE', 'W6', 'PLATFORM', @now, @now),
-- customer_service_appeal 客服与申诉（P1 交易与售后>售后服务）
(1000000000000000048, 'customer_service_appeal:read',    '客服与申诉-查看', 'customer_service_appeal', 'read',  'TENANT', '/transaction-after-sales/after-sales/customer-service-appeal', '查看咨询/投诉/申诉/评价/回访',                           1, 'ACTIVE', 'W6', 'PLATFORM', @now, @now),
(1000000000000000049, 'customer_service_appeal:write',   '客服与申诉-处理', 'customer_service_appeal', 'write', 'TENANT', '/transaction-after-sales/after-sales/customer-service-appeal', '投诉处理/申诉处置/回访等写操作',                         1, 'ACTIVE', 'W6', 'PLATFORM', @now, @now),
-- personal_fund 个人资金管理（P1 资金与结算>个人资金；平台统一收款，平台维护）
(1000000000000000050, 'personal_fund:read',        '个人资金管理-查看', 'personal_fund',        'read',  'PLATFORM', '/fund-settlement/personal-fund/personal-fund-management',     '查看钱包余额/充值批次/预付冻结/退款/支付流水',               1, 'ACTIVE', 'W6', 'PLATFORM', @now, @now),
(1000000000000000051, 'personal_fund:write',       '个人资金管理-管理', 'personal_fund',        'write', 'PLATFORM', '/fund-settlement/personal-fund/personal-fund-management',     '个人预付资金账务处理等写操作',                               1, 'ACTIVE', 'W6', 'PLATFORM', @now, @now),
-- personal_transaction 个人交易明细（纯查看汇总）
(1000000000000000052, 'personal_transaction:read', '个人交易明细-查看', 'personal_transaction', 'read',  'PLATFORM', '/fund-settlement/personal-fund/personal-transaction',         '只读汇总个人交易及渠道结果',                                 1, 'ACTIVE', 'W6', 'PLATFORM', @now, @now),

-- ============================================================
-- W7（6 P1 资源 12 条 + 5 P2 资源 9 条）：企业域（P1 代维护视角 + P2 企业端）
-- ============================================================
-- enterprise 企业管理（P1 客户与权益>企业客户；平台/租户创建与代维护企业）
(1000000000000000053, 'enterprise:read',           '企业管理-查看',     'enterprise',           'read',  'TENANT',   '/customer-equity/enterprise-customer/enterprise',             '查看企业资料/合同/组织架构/角色/成员/权限',                 1, 'ACTIVE', 'W7', 'PLATFORM', @now, @now),
(1000000000000000054, 'enterprise:write',          '企业管理-管理',     'enterprise',           'write', 'TENANT',   '/customer-equity/enterprise-customer/enterprise',             '企业资料/合同/服务状态等写操作',                             1, 'ACTIVE', 'W7', 'PLATFORM', @now, @now),
-- enterprise_member 企业成员账户（P1 客户与权益>企业客户）
(1000000000000000055, 'enterprise_member:read',    '企业成员账户-查看', 'enterprise_member',    'read',  'TENANT',   '/customer-equity/enterprise-customer/enterprise-member',      '查看员工/司机账号/状态/绑定关系',                           1, 'ACTIVE', 'W7', 'PLATFORM', @now, @now),
(1000000000000000056, 'enterprise_member:write',   '企业成员账户-管理', 'enterprise_member',    'write', 'TENANT',   '/customer-equity/enterprise-customer/enterprise-member',      '成员业务资料与绑定关系维护等写操作',                         1, 'ACTIVE', 'W7', 'PLATFORM', @now, @now),
-- enterprise_vehicle 企业车辆管理（P1 客户与权益>企业客户）
(1000000000000000057, 'enterprise_vehicle:read',   '企业车辆管理-查看', 'enterprise_vehicle',   'read',  'TENANT',   '/customer-equity/enterprise-customer/enterprise-vehicle',     '查看企业车辆/分组/可用场站/额度',                           1, 'ACTIVE', 'W7', 'PLATFORM', @now, @now),
(1000000000000000058, 'enterprise_vehicle:write',  '企业车辆管理-管理', 'enterprise_vehicle',   'write', 'TENANT',   '/customer-equity/enterprise-customer/enterprise-vehicle',     '车辆/分组/额度等写操作',                                     1, 'ACTIVE', 'W7', 'PLATFORM', @now, @now),
-- enterprise_evcard 企业电卡管理（P1 客户与权益>企业客户）
(1000000000000000059, 'enterprise_evcard:read',    '企业电卡管理-查看', 'enterprise_evcard',    'read',  'TENANT',   '/customer-equity/enterprise-customer/enterprise-evcard',      '查看企业电卡/分组/卡余额/冻结/绑卡',                         1, 'ACTIVE', 'W7', 'PLATFORM', @now, @now),
(1000000000000000060, 'enterprise_evcard:write',   '企业电卡管理-管理', 'enterprise_evcard',    'write', 'TENANT',   '/customer-equity/enterprise-customer/enterprise-evcard',      '电卡/分组/绑卡/挂失等写操作',                               1, 'ACTIVE', 'W7', 'PLATFORM', @now, @now),
-- partner 合作方管理（P1 客户与权益>合作运营；非租户类渠道合作，平台维护）
(1000000000000000061, 'partner:read',              '合作方管理-查看',   'partner',              'read',  'PLATFORM', '/customer-equity/partner-operation/partner',                   '查看合作协议/渠道合作/合作站点',                             1, 'ACTIVE', 'W7', 'PLATFORM', @now, @now),
(1000000000000000062, 'partner:write',             '合作方管理-管理',   'partner',              'write', 'PLATFORM', '/customer-equity/partner-operation/partner',                   '合作协议/授权范围等写操作',                                 1, 'ACTIVE', 'W7', 'PLATFORM', @now, @now),
-- enterprise_agreement_price 企业协议价（P1 计费与营销>计费管理）
(1000000000000000063, 'enterprise_agreement_price:read',  '企业协议价-查看', 'enterprise_agreement_price', 'read',  'TENANT', '/billing-marketing/billing-management/enterprise-agreement-price', '查看协议主体/适用场站/价格/版本',                     1, 'ACTIVE', 'W7', 'PLATFORM', @now, @now),
(1000000000000000064, 'enterprise_agreement_price:write', '企业协议价-管理', 'enterprise_agreement_price', 'write', 'TENANT', '/billing-marketing/billing-management/enterprise-agreement-price', '协议价配置/版本/生效失效等写操作',                    1, 'ACTIVE', 'W7', 'PLATFORM', @now, @now),
-- P2 企业概览（纯查看）
(1000000000000000065, 'enterprise_overview:read',  '企业概览-查看',     'enterprise_overview',  'read',  'ENTERPRISE', '/enterprise-home/enterprise-overview',                     '查看本企业余额预警/异常订单/待办/经营摘要',                 1, 'ACTIVE', 'W7', 'PLATFORM', @now, @now),
-- P2 企业组织管理
(1000000000000000066, 'enterprise_org:read',       '企业组织管理-查看', 'enterprise_org',       'read',  'ENTERPRISE', '/enterprise-management/enterprise-org/enterprise-org-management', '查看企业资料/部门/成员/授权摘要',                     1, 'ACTIVE', 'W7', 'PLATFORM', @now, @now),
(1000000000000000067, 'enterprise_org:write',      '企业组织管理-管理', 'enterprise_org',       'write', 'ENTERPRISE', '/enterprise-management/enterprise-org/enterprise-org-management', '企业资料/部门/成员账号等写操作',                       1, 'ACTIVE', 'W7', 'PLATFORM', @now, @now),
-- P2 车辆管理
(1000000000000000068, 'vehicle:read',              '车辆管理-查看',     'vehicle',              'read',  'ENTERPRISE', '/vehicle-evcard/vehicle-management/vehicle',               '查看企业车辆/分组/可用场站/额度/变更记录',                 1, 'ACTIVE', 'W7', 'PLATFORM', @now, @now),
(1000000000000000069, 'vehicle:write',             '车辆管理-管理',     'vehicle',              'write', 'ENTERPRISE', '/vehicle-evcard/vehicle-management/vehicle',               '企业车辆/分组维护等写操作',                                 1, 'ACTIVE', 'W7', 'PLATFORM', @now, @now),
-- P2 电卡管理
(1000000000000000070, 'evcard:read',               '电卡管理-查看',     'evcard',               'read',  'ENTERPRISE', '/vehicle-evcard/evcard-management/evcard',                 '查看企业电卡/分组/绑卡/余额/冻结',                         1, 'ACTIVE', 'W7', 'PLATFORM', @now, @now),
(1000000000000000071, 'evcard:write',              '电卡管理-管理',     'evcard',               'write', 'ENTERPRISE', '/vehicle-evcard/evcard-management/evcard',                 '电卡/分组/绑卡等写操作',                                     1, 'ACTIVE', 'W7', 'PLATFORM', @now, @now),
-- P2 用车额度与授权
(1000000000000000072, 'vehicle_quota:read',        '用车额度与授权-查看','vehicle_quota',       'read',  'ENTERPRISE', '/fund-authorization/vehicle-control/vehicle-quota',        '查看分组额度/日月限制/可用场站/成员可见范围',               1, 'ACTIVE', 'W7', 'PLATFORM', @now, @now),
(1000000000000000073, 'vehicle_quota:write',       '用车额度与授权-管理','vehicle_quota',       'write', 'ENTERPRISE', '/fund-authorization/vehicle-control/vehicle-quota',        '额度/限制/授权范围等写操作',                                 1, 'ACTIVE', 'W7', 'PLATFORM', @now, @now),

-- ============================================================
-- W8（6 P1 资源 12 条 + 2 P2 资源 4 条）：企业资金与租户结算域
-- ============================================================
-- enterprise_fund_account 企业资金账户（平台统一管理企业资金台账）
(1000000000000000074, 'enterprise_fund_account:read',  '企业资金账户-查看', 'enterprise_fund_account', 'read',  'PLATFORM', '/fund-settlement/enterprise-fund/enterprise-fund-account', '查看企业预存余额/授信/划拨/分组资金/冻结',               1, 'ACTIVE', 'W8', 'PLATFORM', @now, @now),
(1000000000000000075, 'enterprise_fund_account:write', '企业资金账户-管理', 'enterprise_fund_account', 'write', 'PLATFORM', '/fund-settlement/enterprise-fund/enterprise-fund-account', '资金划拨/冻结/扣款等写操作',                             1, 'ACTIVE', 'W8', 'PLATFORM', @now, @now),
-- enterprise_recharge 企业充值明细
(1000000000000000076, 'enterprise_recharge:read',  '企业充值明细-查看', 'enterprise_recharge',  'read',  'PLATFORM', '/fund-settlement/enterprise-fund/enterprise-recharge',        '查看企业总资金账户充值/到账/失败/退款',                     1, 'ACTIVE', 'W8', 'PLATFORM', @now, @now),
(1000000000000000077, 'enterprise_recharge:write', '企业充值明细-管理', 'enterprise_recharge',  'write', 'PLATFORM', '/fund-settlement/enterprise-fund/enterprise-recharge',        '充值退款处理等写操作',                                       1, 'ACTIVE', 'W8', 'PLATFORM', @now, @now),
-- settlement_account 结算账户管理（平台维护租户收款账户及审核）
(1000000000000000078, 'settlement_account:read',   '结算账户管理-查看', 'settlement_account',   'read',  'PLATFORM', '/fund-settlement/tenant-settlement/settlement-account',       '查看对公/银联/第三方账户/审核状态',                         1, 'ACTIVE', 'W8', 'PLATFORM', @now, @now),
(1000000000000000079, 'settlement_account:write',  '结算账户管理-管理', 'settlement_account',   'write', 'PLATFORM', '/fund-settlement/tenant-settlement/settlement-account',       '账户登记/审核/启停/变更等写操作',                           1, 'ACTIVE', 'W8', 'PLATFORM', @now, @now),
-- station_settlement_plan 场站结算方案（租户可被授权）
(1000000000000000080, 'station_settlement_plan:read',  '场站结算方案-查看', 'station_settlement_plan', 'read',  'TENANT', '/fund-settlement/tenant-settlement/station-settlement-plan', '查看方案版本/场站绑定/分成/费率/生效记录',               1, 'ACTIVE', 'W8', 'PLATFORM', @now, @now),
(1000000000000000081, 'station_settlement_plan:write', '场站结算方案-管理', 'station_settlement_plan', 'write', 'TENANT', '/fund-settlement/tenant-settlement/station-settlement-plan', '方案配置/版本/场站绑定等写操作',                         1, 'ACTIVE', 'W8', 'PLATFORM', @now, @now),
-- tenant_settlement_bill 租户结算账单（租户确认/锁单参与）
(1000000000000000082, 'tenant_settlement_bill:read',  '租户结算账单-查看', 'tenant_settlement_bill', 'read',  'TENANT', '/fund-settlement/tenant-settlement/tenant-settlement-bill', '查看月度账单/差异/调账/锁单状态',                         1, 'ACTIVE', 'W8', 'PLATFORM', @now, @now),
(1000000000000000083, 'tenant_settlement_bill:write', '租户结算账单-管理', 'tenant_settlement_bill', 'write', 'TENANT', '/fund-settlement/tenant-settlement/tenant-settlement-bill', '差异调账/账单确认/锁单等写操作',                         1, 'ACTIVE', 'W8', 'PLATFORM', @now, @now),
-- payment_receipt 付款与回单（平台执行付款）
(1000000000000000084, 'payment_receipt:read',      '付款与回单-查看',   'payment_receipt',      'read',  'PLATFORM', '/fund-settlement/tenant-settlement/payment-receipt',           '查看转账执行/失败重试/撤销补付/回单',                       1, 'ACTIVE', 'W8', 'PLATFORM', @now, @now),
(1000000000000000085, 'payment_receipt:write',     '付款与回单-管理',   'payment_receipt',      'write', 'PLATFORM', '/fund-settlement/tenant-settlement/payment-receipt',           '转账执行/重试/撤销/补付等写操作',                           1, 'ACTIVE', 'W8', 'PLATFORM', @now, @now),
-- P2 资金账户（企业查看并获授权操作企业资金）
(1000000000000000086, 'fund_account:read',         '资金账户-查看',     'fund_account',         'read',  'ENTERPRISE', '/fund-authorization/fund-management/fund-account',          '查看预存余额/授信/充值/划拨/流水',                         1, 'ACTIVE', 'W8', 'PLATFORM', @now, @now),
(1000000000000000087, 'fund_account:write',        '资金账户-管理',     'fund_account',         'write', 'ENTERPRISE', '/fund-authorization/fund-management/fund-account',          '获授权的资金操作等写操作',                                   1, 'ACTIVE', 'W8', 'PLATFORM', @now, @now),
-- P2 充电订单与对账
(1000000000000000088, 'enterprise_order:read',     '充电订单与对账-查看','enterprise_order',    'read',  'ENTERPRISE', '/order-service/order-management/enterprise-order',          '查看企业订单/异常/对账单/差异',                             1, 'ACTIVE', 'W8', 'PLATFORM', @now, @now),
(1000000000000000089, 'enterprise_order:write',    '充电订单与对账-处理','enterprise_order',    'write', 'ENTERPRISE', '/order-service/order-management/enterprise-order',          '异常订单按授权人工处理等写操作',                             1, 'ACTIVE', 'W8', 'PLATFORM', @now, @now),

-- ============================================================
-- W9（17 资源 33 条）：总览/运维协同/营销/内容配置域
-- ============================================================
-- operation_overview 运营概览（纯查看，钻取跳转）
(1000000000000000090, 'operation_overview:read',   '运营概览-查看',     'operation_overview',   'read',  'TENANT',   '/operation-overview/operation-overview',                       '查看平台经营/运营/风险指标与钻取',                           1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
-- todo_warning 待办与预警（纯查看，处理跳转对应业务菜单）
(1000000000000000091, 'todo_warning:read',         '待办与预警-查看',   'todo_warning',         'read',  'TENANT',   '/operation-overview/todo-warning',                             '查看审批待办/异常订单/资金预警/监管失败/工单超时',           1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
-- parking_service 停车服务协同
(1000000000000000092, 'parking_service:read',      '停车服务协同-查看', 'parking_service',      'read',  'TENANT',   '/station-maintenance/station-service/parking-service',         '查看停车场映射/充电减免/核销记录',                           1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
(1000000000000000093, 'parking_service:write',     '停车服务协同-管理', 'parking_service',      'write', 'TENANT',   '/station-maintenance/station-service/parking-service',         '停车场映射/减免规则/核销撤销等写操作',                       1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
-- access_parking 出入口与车位协同
(1000000000000000094, 'access_parking:read',       '出入口与车位协同-查看','access_parking',    'read',  'TENANT',   '/station-maintenance/station-service/access-parking',          '查看道闸映射/车位闸机映射/进出场记录',                       1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
(1000000000000000095, 'access_parking:write',      '出入口与车位协同-管理','access_parking',    'write', 'TENANT',   '/station-maintenance/station-service/access-parking',          '道闸/车位映射维护等写操作',                                 1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
-- campus_service 园区便民服务协同
(1000000000000000096, 'campus_service:read',       '园区便民服务协同-查看','campus_service',    'read',  'TENANT',   '/station-maintenance/station-service/campus-service',          '查看门禁服务/权益授权/使用记录',                             1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
(1000000000000000097, 'campus_service:write',      '园区便民服务协同-管理','campus_service',    'write', 'TENANT',   '/station-maintenance/station-service/campus-service',          '园区服务授权等写操作',                                       1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
-- scheduled_charging 场站定时充电策略（仅平台或获授权租户配置）
(1000000000000000098, 'scheduled_charging:read',   '定时充电策略-查看', 'scheduled_charging',   'read',  'TENANT',   '/station-maintenance/station-service/scheduled-charging',      '查看策略/目标/启停计划/执行记录',                           1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
(1000000000000000099, 'scheduled_charging:write',  '定时充电策略-管理', 'scheduled_charging',   'write', 'TENANT',   '/station-maintenance/station-service/scheduled-charging',      '策略配置/启停计划等写操作',                                 1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
-- ops_ticket 运维工单
(1000000000000000100, 'ops_ticket:read',           '运维工单-查看',     'ops_ticket',           'read',  'TENANT',   '/station-maintenance/operation-maintenance/ops-ticket',        '查看工单/巡检/派工/验收/SLA',                               1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
(1000000000000000101, 'ops_ticket:write',          '运维工单-管理',     'ops_ticket',           'write', 'TENANT',   '/station-maintenance/operation-maintenance/ops-ticket',        '工单/巡检/派工/验收等写操作',                               1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
-- member_benefit 会员与权益
(1000000000000000102, 'member_benefit:read',       '会员与权益-查看',   'member_benefit',       'read',  'TENANT',   '/customer-equity/personal-customer/member-benefit',            '查看会员规则/等级/权益包/发放核销',                         1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
(1000000000000000103, 'member_benefit:write',      '会员与权益-管理',   'member_benefit',       'write', 'TENANT',   '/customer-equity/personal-customer/member-benefit',            '会员规则/等级/权益包等写操作',                               1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
-- marketing_campaign 营销活动
(1000000000000000104, 'marketing_campaign:read',   '营销活动-查看',     'marketing_campaign',   'read',  'TENANT',   '/billing-marketing/marketing-equity/marketing-campaign',       '查看活动介绍/规则/预算/效果分析',                           1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
(1000000000000000105, 'marketing_campaign:write',  '营销活动-管理',     'marketing_campaign',   'write', 'TENANT',   '/billing-marketing/marketing-equity/marketing-campaign',       '活动配置/预算/发布等写操作',                                 1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
-- coupon_campaign 发券活动
(1000000000000000106, 'coupon_campaign:read',      '发券活动-查看',     'coupon_campaign',      'read',  'TENANT',   '/billing-marketing/marketing-equity/coupon-campaign',          '查看发券活动/发放结果',                                     1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
(1000000000000000107, 'coupon_campaign:write',     '发券活动-管理',     'coupon_campaign',      'write', 'TENANT',   '/billing-marketing/marketing-equity/coupon-campaign',          '券规则/发放对象/执行等写操作',                               1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
-- recharge_campaign 充值活动
(1000000000000000108, 'recharge_campaign:read',    '充值活动-查看',     'recharge_campaign',    'read',  'TENANT',   '/billing-marketing/marketing-equity/recharge-campaign',        '查看充值活动/赠送规则/活动订单',                             1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
(1000000000000000109, 'recharge_campaign:write',   '充值活动-管理',     'recharge_campaign',    'write', 'TENANT',   '/billing-marketing/marketing-equity/recharge-campaign',        '充值活动/赠送规则等写操作',                                 1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
-- charging_campaign 充电活动
(1000000000000000110, 'charging_campaign:read',    '充电活动-查看',     'charging_campaign',    'read',  'TENANT',   '/billing-marketing/marketing-equity/charging-campaign',        '查看充电有礼/场站活动/效果',                                 1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
(1000000000000000111, 'charging_campaign:write',   '充电活动-管理',     'charging_campaign',    'write', 'TENANT',   '/billing-marketing/marketing-equity/charging-campaign',        '充电活动配置等写操作',                                       1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
-- point_campaign 积分活动
(1000000000000000112, 'point_campaign:read',       '积分活动-查看',     'point_campaign',       'read',  'TENANT',   '/billing-marketing/marketing-equity/point-campaign',           '查看积分兑换/抽奖/规则/奖品/库存',                           1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
(1000000000000000113, 'point_campaign:write',      '积分活动-管理',     'point_campaign',       'write', 'TENANT',   '/billing-marketing/marketing-equity/point-campaign',           '积分活动/奖品/库存等写操作',                                 1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
-- precision_marketing 精准营销
(1000000000000000114, 'precision_marketing:read',  '精准营销-查看',     'precision_marketing',  'read',  'TENANT',   '/billing-marketing/marketing-equity/precision-marketing',      '查看客群筛选/触达记录/效果分析',                             1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
(1000000000000000115, 'precision_marketing:write', '精准营销-管理',     'precision_marketing',  'write', 'TENANT',   '/billing-marketing/marketing-equity/precision-marketing',      '客群圈选/营销触达/客情关怀等写操作',                         1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
-- coupon_asset 优惠资产与核销
(1000000000000000116, 'coupon_asset:read',         '优惠资产与核销-查看','coupon_asset',        'read',  'TENANT',   '/billing-marketing/marketing-equity/coupon-asset',             '查看卡券/红包/积分/兑换/冻结/核销',                         1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
(1000000000000000117, 'coupon_asset:write',        '优惠资产与核销-管理','coupon_asset',        'write', 'TENANT',   '/billing-marketing/marketing-equity/coupon-asset',             '资产冻结/兑换/核销处理等写操作',                             1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
-- mini_program_content 小程序内容配置（平台维护）
(1000000000000000118, 'mini_program_content:read', '小程序内容配置-查看','mini_program_content','read',  'PLATFORM', '/system-config/app-content/mini-program-content',              '查看首页内容/功能入口/活动位/业务开关',                     1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
(1000000000000000119, 'mini_program_content:write','小程序内容配置-管理','mini_program_content','write', 'PLATFORM', '/system-config/app-content/mini-program-content',              '内容配置/活动位/业务开关等写操作',                           1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
-- message_notice 消息与公告配置（平台维护）
(1000000000000000120, 'message_notice:read',       '消息与公告配置-查看','message_notice',      'read',  'PLATFORM', '/system-config/app-content/message-notice',                    '查看模板/触达渠道/公告/发送记录',                           1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),
(1000000000000000121, 'message_notice:write',      '消息与公告配置-管理','message_notice',      'write', 'PLATFORM', '/system-config/app-content/message-notice',                    '模板/公告/失败处理等写操作',                                 1, 'ACTIVE', 'W9', 'PLATFORM', @now, @now),

-- ============================================================
-- W10（12 P1 资源 24 条 + 2 P2 资源 4 条）：价格/分析/大屏/对账/发票/监管域
-- ============================================================
-- purchase_sale_price 购售电价
(1000000000000000122, 'purchase_sale_price:read',  '购售电价-查看',     'purchase_sale_price',  'read',  'TENANT',   '/billing-marketing/billing-management/purchase-sale-price',    '查看购/售电价模板/尖峰平谷深/场站绑定',                     1, 'ACTIVE', 'W10', 'PLATFORM', @now, @now),
(1000000000000000123, 'purchase_sale_price:write', '购售电价-管理',     'purchase_sale_price',  'write', 'TENANT',   '/billing-marketing/billing-management/purchase-sale-price',    '电价模板/日期范围/场站绑定等写操作',                         1, 'ACTIVE', 'W10', 'PLATFORM', @now, @now),
-- business_analysis_report 经营分析与报表
(1000000000000000124, 'business_analysis_report:read',  '经营分析与报表-查看','business_analysis_report', 'read',  'TENANT', '/data-analysis/business-analysis/business-analysis-report', '查看收入/订单/站点/用户/企业/营销分析',                 1, 'ACTIVE', 'W10', 'PLATFORM', @now, @now),
(1000000000000000125, 'business_analysis_report:write', '经营分析与报表-导出','business_analysis_report', 'write', 'TENANT', '/data-analysis/business-analysis/business-analysis-report', '报表导出/订阅等写操作',                                 1, 'ACTIVE', 'W10', 'PLATFORM', @now, @now),
-- revenue_analysis 收益分析
(1000000000000000126, 'revenue_analysis:read',     '收益分析-查看',     'revenue_analysis',     'read',  'TENANT',   '/data-analysis/business-analysis/revenue-analysis',            '查看电量/损耗/收益/补贴/成本/收益率（三级下钻）',           1, 'ACTIVE', 'W10', 'PLATFORM', @now, @now),
(1000000000000000127, 'revenue_analysis:write',    '收益分析-重算导出', 'revenue_analysis',     'write', 'TENANT',   '/data-analysis/business-analysis/revenue-analysis',            '按查询日期重新计算/导出等写操作',                           1, 'ACTIVE', 'W10', 'PLATFORM', @now, @now),
-- asset_ops_analysis 资产与运维分析
(1000000000000000128, 'asset_ops_analysis:read',   '资产与运维分析-查看','asset_ops_analysis',  'read',  'TENANT',   '/data-analysis/asset-analysis/asset-ops-analysis',              '查看利用率/故障率/服务质量/设备健康/专题',                   1, 'ACTIVE', 'W10', 'PLATFORM', @now, @now),
(1000000000000000129, 'asset_ops_analysis:write',  '资产与运维分析-导出','asset_ops_analysis',  'write', 'TENANT',   '/data-analysis/asset-analysis/asset-ops-analysis',              '专题分析导出等写操作',                                       1, 'ACTIVE', 'W10', 'PLATFORM', @now, @now),
-- dashboard_config 运营大屏配置与发布（平台维护）
(1000000000000000130, 'dashboard_config:read',     '大屏配置与发布-查看','dashboard_config',    'read',  'PLATFORM', '/system-config/app-content/dashboard-config',                  '查看 D1 布局/指标/专题/发布版本/发布记录',                   1, 'ACTIVE', 'W10', 'PLATFORM', @now, @now),
(1000000000000000131, 'dashboard_config:write',    '大屏配置与发布-管理','dashboard_config',    'write', 'PLATFORM', '/system-config/app-content/dashboard-config',                  '布局/指标/专题/发布等写操作',                               1, 'ACTIVE', 'W10', 'PLATFORM', @now, @now),
-- channel_reconciliation 渠道与账务对账（平台四账对账）
(1000000000000000132, 'channel_reconciliation:read',  '渠道与账务对账-查看','channel_reconciliation', 'read',  'PLATFORM', '/fund-settlement/accounting-invoice/channel-reconciliation', '查看支付/订单/资金账/结算账/发票账差异',                 1, 'ACTIVE', 'W10', 'PLATFORM', @now, @now),
(1000000000000000133, 'channel_reconciliation:write', '渠道与账务对账-处理','channel_reconciliation', 'write', 'PLATFORM', '/fund-settlement/accounting-invoice/channel-reconciliation', '差异调账处理等写操作',                                 1, 'ACTIVE', 'W10', 'PLATFORM', @now, @now),
-- user_charging_invoice 用户充电发票（平台统一开票主体）
(1000000000000000134, 'user_charging_invoice:read',  '用户充电发票-查看','user_charging_invoice','read',  'PLATFORM', '/fund-settlement/accounting-invoice/user-charging-invoice',  '查看个人/企业发票/申请/红冲/下载/异常',                   1, 'ACTIVE', 'W10', 'PLATFORM', @now, @now),
(1000000000000000135, 'user_charging_invoice:write', '用户充电发票-管理','user_charging_invoice','write', 'PLATFORM', '/fund-settlement/accounting-invoice/user-charging-invoice',  '开票/红冲/异常处理等写操作',                             1, 'ACTIVE', 'W10', 'PLATFORM', @now, @now),
-- tenant_incoming_invoice 租户来票管理
(1000000000000000136, 'tenant_incoming_invoice:read',  '租户来票管理-查看','tenant_incoming_invoice','read',  'PLATFORM', '/fund-settlement/accounting-invoice/tenant-incoming-invoice', '查看租户结算/履约发票/验票/关联付款',                   1, 'ACTIVE', 'W10', 'PLATFORM', @now, @now),
(1000000000000000137, 'tenant_incoming_invoice:write', '租户来票管理-管理','tenant_incoming_invoice','write', 'PLATFORM', '/fund-settlement/accounting-invoice/tenant-incoming-invoice', '验票/关联付款等写操作',                                 1, 'ACTIVE', 'W10', 'PLATFORM', @now, @now),
-- platform_service_invoice 平台服务费发票
(1000000000000000138, 'platform_service_invoice:read',  '平台服务费发票-查看','platform_service_invoice','read',  'PLATFORM', '/fund-settlement/accounting-invoice/platform-service-invoice', '查看服务费计费/开票/红冲/关联结算',                   1, 'ACTIVE', 'W10', 'PLATFORM', @now, @now),
(1000000000000000139, 'platform_service_invoice:write', '平台服务费发票-管理','platform_service_invoice','write', 'PLATFORM', '/fund-settlement/accounting-invoice/platform-service-invoice', '服务费开票/红冲等写操作',                             1, 'ACTIVE', 'W10', 'PLATFORM', @now, @now),
-- regulatory_archive 监管业务档案（W10 ecosystem-service，平台专属）
(1000000000000000140, 'regulatory_archive:read',   '监管业务档案-查看', 'regulatory_archive',   'read',  'PLATFORM', '/regulatory-access/regulatory-operation/regulatory-archive',   '查看运营商/站点/设备/业务参数映射',                         1, 'ACTIVE', 'W10', 'PLATFORM', @now, @now),
(1000000000000000141, 'regulatory_archive:write',  '监管业务档案-管理', 'regulatory_archive',   'write', 'PLATFORM', '/regulatory-access/regulatory-operation/regulatory-archive',   '监管档案与参数映射维护等写操作',                             1, 'ACTIVE', 'W10', 'PLATFORM', @now, @now),
-- regulatory_report 监管报送与补推
(1000000000000000142, 'regulatory_report:read',    '监管报送与补推-查看','regulatory_report',   'read',  'PLATFORM', '/regulatory-access/regulatory-operation/regulatory-report',    '查看报送记录/漏推/补推/回执',                               1, 'ACTIVE', 'W10', 'PLATFORM', @now, @now),
(1000000000000000143, 'regulatory_report:write',   '监管报送与补推-管理','regulatory_report',   'write', 'PLATFORM', '/regulatory-access/regulatory-operation/regulatory-report',    '报送/补推触发等写操作',                                     1, 'ACTIVE', 'W10', 'PLATFORM', @now, @now),
-- regulatory_reconciliation 监管对账与异常
(1000000000000000144, 'regulatory_reconciliation:read',  '监管对账与异常-查看','regulatory_reconciliation','read',  'PLATFORM', '/regulatory-access/regulatory-operation/regulatory-reconciliation', '查看订单/金额/状态差异/处置记录',                   1, 'ACTIVE', 'W10', 'PLATFORM', @now, @now),
(1000000000000000145, 'regulatory_reconciliation:write', '监管对账与异常-管理','regulatory_reconciliation','write', 'PLATFORM', '/regulatory-access/regulatory-operation/regulatory-reconciliation', '差异处置等写操作',                                   1, 'ACTIVE', 'W10', 'PLATFORM', @now, @now),
-- P2 发票管理
(1000000000000000146, 'enterprise_invoice:read',   '发票管理-查看',     'enterprise_invoice',   'read',  'ENTERPRISE', '/order-service/invoice-service/enterprise-invoice',        '查看发票抬头/申请/下载/红冲状态',                           1, 'ACTIVE', 'W10', 'PLATFORM', @now, @now),
(1000000000000000147, 'enterprise_invoice:write',  '发票管理-申请',     'enterprise_invoice',   'write', 'ENTERPRISE', '/order-service/invoice-service/enterprise-invoice',        '发票申请/红冲等写操作',                                     1, 'ACTIVE', 'W10', 'PLATFORM', @now, @now),
-- P2 数据报表
(1000000000000000148, 'enterprise_report:read',    '数据报表-查看',     'enterprise_report',    'read',  'ENTERPRISE', '/data-analysis/enterprise-analysis/enterprise-report',     '查看车辆分组/成员/站点/费用/能耗分析',                     1, 'ACTIVE', 'W10', 'PLATFORM', @now, @now),
(1000000000000000149, 'enterprise_report:write',   '数据报表-导出',     'enterprise_report',    'write', 'ENTERPRISE', '/data-analysis/enterprise-analysis/enterprise-report',     '报表导出等写操作',                                           1, 'ACTIVE', 'W10', 'PLATFORM', @now, @now),

-- ============================================================
-- W11（5 资源 10 条）：V2G 域（W11 交付模拟功能；真实放电执行为业务层关闭态，
--       由放电授权与策略页的授权/开关数据控制，非权限层状态，见 DEC-20260820-015）
-- ============================================================
-- discharge_authorization 放电授权与策略
(1000000000000000150, 'discharge_authorization:read',  '放电授权与策略-查看','discharge_authorization', 'read',  'TENANT', '/v2g-operation/discharge-operation/discharge-authorization', '查看主体授权/策略/计划/审批/审计',                     1, 'ACTIVE', 'W11', 'PLATFORM', @now, @now),
(1000000000000000151, 'discharge_authorization:write', '放电授权与策略-管理','discharge_authorization', 'write', 'TENANT', '/v2g-operation/discharge-operation/discharge-authorization', '授权/策略/计划配置等写操作',                           1, 'ACTIVE', 'W11', 'PLATFORM', @now, @now),
-- discharge_order 放电订单
(1000000000000000152, 'discharge_order:read',      '放电订单-查看',     'discharge_order',      'read',  'TENANT',   '/v2g-operation/discharge-operation/discharge-order',           '查看放电订单/异常/计量明细',                                 1, 'ACTIVE', 'W11', 'PLATFORM', @now, @now),
(1000000000000000153, 'discharge_order:write',     '放电订单-管理',     'discharge_order',      'write', 'TENANT',   '/v2g-operation/discharge-operation/discharge-order',           '放电订单异常处理等写操作',                                   1, 'ACTIVE', 'W11', 'PLATFORM', @now, @now),
-- discharge_pricing 放电价格与计费
(1000000000000000154, 'discharge_pricing:read',    '放电价格与计费-查看','discharge_pricing',   'read',  'TENANT',   '/v2g-operation/discharge-settlement/discharge-pricing',        '查看放电价格/计费规则/版本',                                 1, 'ACTIVE', 'W11', 'PLATFORM', @now, @now),
(1000000000000000155, 'discharge_pricing:write',   '放电价格与计费-管理','discharge_pricing',   'write', 'TENANT',   '/v2g-operation/discharge-settlement/discharge-pricing',        '放电价格/计费规则/发布等写操作',                             1, 'ACTIVE', 'W11', 'PLATFORM', @now, @now),
-- discharge_settlement 放电结算与对账
(1000000000000000156, 'discharge_settlement:read', '放电结算与对账-查看','discharge_settlement', 'read',  'TENANT',   '/v2g-operation/discharge-settlement/discharge-settlement',     '查看收益结算/账单/对账/调账',                               1, 'ACTIVE', 'W11', 'PLATFORM', @now, @now),
(1000000000000000157, 'discharge_settlement:write','放电结算与对账-管理','discharge_settlement', 'write', 'TENANT',   '/v2g-operation/discharge-settlement/discharge-settlement',     '结算/调账等写操作',                                         1, 'ACTIVE', 'W11', 'PLATFORM', @now, @now),
-- discharge_monitor_analysis 放电监控与分析
(1000000000000000158, 'discharge_monitor_analysis:read',  '放电监控与分析-查看','discharge_monitor_analysis','read', 'TENANT', '/v2g-operation/discharge-monitor/discharge-monitor-analysis', '查看 V2G 实时态势/经营分析/告警/异常',                 1, 'ACTIVE', 'W11', 'PLATFORM', @now, @now),
(1000000000000000159, 'discharge_monitor_analysis:write', '放电监控与分析-处置','discharge_monitor_analysis','write','TENANT', '/v2g-operation/discharge-monitor/discharge-monitor-analysis', '异常处置等写操作',                                     1, 'ACTIVE', 'W11', 'PLATFORM', @now, @now)
ON DUPLICATE KEY UPDATE `id` = `id`;

-- ============================================================
-- 校验（执行后人工/自动化核对）
-- ============================================================
-- 预期：SELECT COUNT(*) FROM iam_permission;                                          => 159（W2 18 + 本脚本 141）
-- 预期：SELECT COUNT(DISTINCT resource) FROM iam_permission;                          => 83
-- 预期：按 phase 统计 => W2=18, W3=10, W4=11, W5=6, W6=7, W7=21, W8=16, W9=32, W10=28, W11=10
--       （W7=6 P1 资源 12 条 + 5 P2 资源 9 条；W8=6 P1 12 条 + 2 P2 4 条；W9=17 资源 32 条；W10=12 P1 24 条 + 2 P2 4 条）
-- 预期：按 scope 统计 => PLATFORM=53, TENANT=89, ENTERPRISE=17（W2 已有 18 条全 PLATFORM）
-- 预期：SELECT COUNT(*) FROM iam_permission WHERE status = 'ACTIVE';                  => 159
--
-- 后续约定：
-- - 各周页面清单（Step 2 产物）为菜单路由终审权威；与 menu_path 不一致时以校准迁移 UPDATE，不得直接改库
-- - 预置策略（POLICY_ASSET_FULL 等）仍由各交付周迁移追加，本脚本只初始化权限主数据
-- - IOT 协同模式下同编码权限由 IOT 推送覆盖维护（source=IOT_PUSH）；独立部署模式由平台维护

/**
 * P1 管理端 73 个叶子菜单「路由 → 计划交付周」映射。
 *
 * 来源：迁移 V202608240006（iam_permission.menu_path/phase，DEC-20260820-015 全量初始化）。
 * 骨架占位页据此展示「本功能计划 Wx 交付」；各周页面清单（Step 2 产物）为路由终审权威，
 * 不一致时以后者为准并登记校准。
 */
export const MENU_PHASE: Readonly<Record<string, string>> = {
  // 运营总览
  "/operation-overview/operation-overview": "W9",
  "/operation-overview/todo-warning": "W9",
  // 场站与运维
  "/station-maintenance/asset-management/station": "W3",
  "/station-maintenance/asset-management/device": "W3",
  "/station-maintenance/asset-management/asset-field-template": "W3",
  "/station-maintenance/station-service/service-config": "W3",
  "/station-maintenance/station-service/parking-service": "W9",
  "/station-maintenance/station-service/access-parking": "W9",
  "/station-maintenance/station-service/campus-service": "W9",
  "/station-maintenance/station-service/scheduled-charging": "W9",
  "/station-maintenance/operation-maintenance/real-time-monitor": "W4",
  "/station-maintenance/operation-maintenance/station-data-analysis": "W4",
  "/station-maintenance/operation-maintenance/device-data-analysis": "W4",
  "/station-maintenance/operation-maintenance/device-protocol-trace": "W4",
  "/station-maintenance/operation-maintenance/alarm": "W4",
  "/station-maintenance/operation-maintenance/ops-ticket": "W9",
  "/station-maintenance/operation-maintenance/remote-operation-record": "W4",
  // 客户与权益
  "/customer-equity/personal-customer/personal-user": "W2",
  "/customer-equity/personal-customer/member-benefit": "W9",
  "/customer-equity/enterprise-customer/enterprise": "W7",
  "/customer-equity/enterprise-customer/enterprise-member": "W7",
  "/customer-equity/enterprise-customer/enterprise-vehicle": "W7",
  "/customer-equity/enterprise-customer/enterprise-evcard": "W7",
  "/customer-equity/partner-operation/tenant": "W3",
  "/customer-equity/partner-operation/partner": "W7",
  // 交易与售后
  "/transaction-after-sales/charging-transaction/charging-order": "W5",
  "/transaction-after-sales/charging-transaction/occupancy-reminder": "W5",
  "/transaction-after-sales/after-sales/refund-difference": "W6",
  "/transaction-after-sales/after-sales/customer-service-appeal": "W6",
  // 计费与营销
  "/billing-marketing/billing-management/charging-billing": "W5",
  "/billing-marketing/billing-management/enterprise-agreement-price": "W7",
  "/billing-marketing/billing-management/purchase-sale-price": "W10",
  "/billing-marketing/marketing-equity/marketing-campaign": "W9",
  "/billing-marketing/marketing-equity/coupon-campaign": "W9",
  "/billing-marketing/marketing-equity/recharge-campaign": "W9",
  "/billing-marketing/marketing-equity/charging-campaign": "W9",
  "/billing-marketing/marketing-equity/point-campaign": "W9",
  "/billing-marketing/marketing-equity/precision-marketing": "W9",
  "/billing-marketing/marketing-equity/coupon-asset": "W9",
  // 资金与结算
  "/fund-settlement/personal-fund/personal-fund-management": "W6",
  "/fund-settlement/personal-fund/personal-transaction": "W6",
  "/fund-settlement/enterprise-fund/enterprise-fund-account": "W8",
  "/fund-settlement/enterprise-fund/enterprise-recharge": "W8",
  "/fund-settlement/tenant-settlement/settlement-account": "W8",
  "/fund-settlement/tenant-settlement/station-settlement-plan": "W8",
  "/fund-settlement/tenant-settlement/tenant-settlement-bill": "W8",
  "/fund-settlement/tenant-settlement/payment-receipt": "W8",
  "/fund-settlement/accounting-invoice/channel-reconciliation": "W10",
  "/fund-settlement/accounting-invoice/user-charging-invoice": "W10",
  "/fund-settlement/accounting-invoice/tenant-incoming-invoice": "W10",
  "/fund-settlement/accounting-invoice/platform-service-invoice": "W10",
  // V2G 运营
  "/v2g-operation/discharge-operation/discharge-authorization": "W11",
  "/v2g-operation/discharge-operation/discharge-order": "W11",
  "/v2g-operation/discharge-settlement/discharge-pricing": "W11",
  "/v2g-operation/discharge-settlement/discharge-settlement": "W11",
  "/v2g-operation/discharge-monitor/discharge-monitor-analysis": "W11",
  // 数据分析
  "/data-analysis/business-analysis/business-analysis-report": "W10",
  "/data-analysis/business-analysis/revenue-analysis": "W10",
  "/data-analysis/asset-analysis/asset-ops-analysis": "W10",
  // 监管接入
  "/regulatory-access/regulatory-operation/regulatory-archive": "W10",
  "/regulatory-access/regulatory-operation/regulatory-report": "W10",
  "/regulatory-access/regulatory-operation/regulatory-reconciliation": "W10",
  // 系统与配置
  "/system-config/permission-management/platform-admin": "W2",
  "/system-config/permission-management/tenant-permission": "W2",
  "/system-config/permission-management/marketing-permission": "W2",
  "/system-config/permission-management/auth-audit": "W2",
  "/system-config/app-content/mini-program": "W2",
  "/system-config/app-content/mini-program-content": "W9",
  "/system-config/app-content/dashboard-config": "W10",
  "/system-config/app-content/message-notice": "W9",
  "/system-config/trade-config/payment-channel": "W2",
  "/system-config/trade-config/settlement-channel": "W2",
  "/system-config/base-setting/platform-setting": "W2",
};

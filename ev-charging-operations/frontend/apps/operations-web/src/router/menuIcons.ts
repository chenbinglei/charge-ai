/**
 * 菜单语义图标映射表（DEC-20260820-020 菜单图标系统）。
 *
 * 叶子菜单 key 为完整 path（后端 MenuNodeVO.path）；
 * 一级/二级分组节点 path 为 null，key 为其后端 MenuDef.key（如 asset-management）；
 * 图标选自 @element-plus/icons-vue（线性 2px 风格，全库统一）；
 * 映射缺失时兜底 Menu 图标，保证任意菜单项都有图标。
 * 语义优先，允许跨分组少量复用同义图标；同一分组内父子不重复。
 */
import type { Component } from "vue";
import {
  Aim,
  AlarmClock,
  Avatar,
  Back,
  Bell,
  Calendar,
  Cellphone,
  ChatDotRound,
  CircleCheck,
  CirclePlus,
  Coin,
  Collection,
  Compass,
  Cpu,
  CreditCard,
  DataAnalysis,
  DataBoard,
  DataLine,
  Discount,
  Document,
  DocumentAdd,
  DocumentChecked,
  DocumentCopy,
  EditPen,
  Files,
  Filter,
  Flag,
  Folder,
  FolderOpened,
  FullScreen,
  Goods,
  Grid,
  Guide,
  Headset,
  Histogram,
  Iphone,
  Key,
  Lightning,
  Link,
  List,
  Location,
  Lock,
  MapLocation,
  Medal,
  Memo,
  Menu,
  Message,
  Money,
  Monitor,
  Notebook,
  Odometer,
  OfficeBuilding,
  Operation,
  Picture,
  PieChart,
  Position,
  Postcard,
  Present,
  PriceTag,
  Promotion,
  Reading,
  RefreshLeft,
  Search,
  Sell,
  Service,
  Setting,
  Share,
  ShoppingBag,
  ShoppingCart,
  Stamp,
  Star,
  Suitcase,
  Switch,
  SwitchButton,
  Ticket,
  Timer,
  Tools,
  TrendCharts,
  Trophy,
  Upload,
  User,
  UserFilled,
  Van,
  VideoCamera,
  Wallet,
  Warning,
} from "@element-plus/icons-vue";

/** 兜底图标：映射缺失时使用，避免空白。 */
export const DEFAULT_MENU_ICON: Component = Menu;

/** 一级/二级分组（按后端 key）+ 全部叶子菜单（按 path）的图标映射。 */
export const MENU_ICONS: Readonly<Record<string, Component>> = {
  /* ---------- 一级菜单（10 组，key = 一级标识） ---------- */
  "operation-overview": Compass, // 运营总览
  "station-maintenance": MapLocation, // 场站与运维
  "customer-equity": User, // 客户与权益
  "transaction-after-sales": Ticket, // 交易与售后
  "billing-marketing": Sell, // 计费与营销
  "fund-settlement": Wallet, // 资金与结算
  "v2g-operation": Lightning, // V2G 运营
  "data-analysis": TrendCharts, // 数据分析
  "regulatory-access": Link, // 监管接入
  "system-config": Setting, // 系统与配置

  /* ---------- 二级分组（24 组，key = 二级标识） ---------- */
  "asset-management": FolderOpened, // 资产管理
  "station-service": Service, // 场站服务
  "operation-maintenance": Monitor, // 运维监测
  "personal-customer": Avatar, // 个人客户
  "enterprise-customer": Suitcase, // 企业客户
  "partner-operation": Share, // 合伙人运营
  "charging-transaction": List, // 充电交易
  "after-sales": ChatDotRound, // 售后处理
  "billing-management": PriceTag, // 计费管理
  "marketing-equity": Present, // 营销权益
  "personal-fund": CreditCard, // 个人资金
  "enterprise-fund": Coin, // 企业资金
  "tenant-settlement": DocumentChecked, // 租户结算
  "accounting-invoice": Document, // 账务发票
  "discharge-operation": SwitchButton, // 放电运营
  "discharge-settlement": Money, // 放电结算
  "discharge-monitor": DataLine, // 放电监测
  "business-analysis": PieChart, // 经营分析
  "asset-analysis": Histogram, // 资产分析
  "regulatory-operation": Upload, // 监管运营
  "permission-management": Lock, // 权限管理
  "app-content": Iphone, // 应用内容
  "trade-config": ShoppingCart, // 交易配置
  "base-setting": Tools, // 基础设置

  /* ---------- 叶子菜单（73 项，按菜单架构 v4.1） ---------- */
  // 运营总览
  "/operation-overview/operation-overview": Odometer, // 运营总览
  "/operation-overview/todo-warning": Bell, // 待办预警
  // 场站与运维 / 资产管理
  "/station-maintenance/asset-management/station": Location, // 场站管理
  "/station-maintenance/asset-management/device": Cpu, // 设备管理
  "/station-maintenance/asset-management/asset-field-template": DocumentCopy, // 资产字段模板
  // 场站与运维 / 场站服务
  "/station-maintenance/station-service/service-config": Operation, // 服务配置
  "/station-maintenance/station-service/parking-service": MapLocation, // 停车服务
  "/station-maintenance/station-service/access-parking": Aim, // 出入停车
  "/station-maintenance/station-service/campus-service": Reading, // 校园服务
  "/station-maintenance/station-service/scheduled-charging": Timer, // 定时充电
  // 场站与运维 / 运维监测
  "/station-maintenance/operation-maintenance/real-time-monitor": VideoCamera, // 实时监控
  "/station-maintenance/operation-maintenance/station-data-analysis": DataAnalysis, // 场站数据分析
  "/station-maintenance/operation-maintenance/device-data-analysis": DataBoard, // 设备数据分析
  "/station-maintenance/operation-maintenance/device-protocol-trace": Guide, // 设备协议追踪
  "/station-maintenance/operation-maintenance/alarm": Warning, // 告警管理
  "/station-maintenance/operation-maintenance/ops-ticket": EditPen, // 运维工单
  "/station-maintenance/operation-maintenance/remote-operation-record": Promotion, // 远程操作记录
  // 客户与权益 / 个人客户
  "/customer-equity/personal-customer/personal-user": UserFilled, // 个人用户
  "/customer-equity/personal-customer/member-benefit": Star, // 会员权益
  // 客户与权益 / 企业客户
  "/customer-equity/enterprise-customer/enterprise": OfficeBuilding, // 企业管理
  "/customer-equity/enterprise-customer/enterprise-member": User, // 企业成员管理
  "/customer-equity/enterprise-customer/enterprise-vehicle": Van, // 企业车辆
  "/customer-equity/enterprise-customer/enterprise-evcard": Postcard, // 企业充电卡
  // 客户与权益 / 合伙人运营
  "/customer-equity/partner-operation/tenant": Key, // 租户管理
  "/customer-equity/partner-operation/partner": Share, // 合伙人管理
  // 交易与售后 / 充电交易
  "/transaction-after-sales/charging-transaction/charging-order": Lightning, // 充电订单
  "/transaction-after-sales/charging-transaction/occupancy-reminder": AlarmClock, // 占位提醒
  // 交易与售后 / 售后处理
  "/transaction-after-sales/after-sales/refund-difference": RefreshLeft, // 退费补差
  "/transaction-after-sales/after-sales/customer-service-appeal": Headset, // 客服申诉
  // 计费与营销 / 计费管理
  "/billing-marketing/billing-management/charging-billing": Coin, // 充电计费
  "/billing-marketing/billing-management/enterprise-agreement-price": Files, // 企业协议价
  "/billing-marketing/billing-management/purchase-sale-price": ShoppingBag, // 购销电价
  // 计费与营销 / 营销权益
  "/billing-marketing/marketing-equity/marketing-campaign": Flag, // 营销活动
  "/billing-marketing/marketing-equity/coupon-campaign": Discount, // 优惠券活动
  "/billing-marketing/marketing-equity/recharge-campaign": Goods, // 充值活动
  "/billing-marketing/marketing-equity/charging-campaign": Medal, // 充电活动
  "/billing-marketing/marketing-equity/point-campaign": Trophy, // 积分活动
  "/billing-marketing/marketing-equity/precision-marketing": Filter, // 精准营销
  "/billing-marketing/marketing-equity/coupon-asset": Ticket, // 优惠券资产
  // 资金与结算 / 个人资金
  "/fund-settlement/personal-fund/personal-fund-management": Money, // 个人资金管理
  "/fund-settlement/personal-fund/personal-transaction": Memo, // 个人交易流水
  // 资金与结算 / 企业资金
  "/fund-settlement/enterprise-fund/enterprise-fund-account": Wallet, // 企业资金账户
  "/fund-settlement/enterprise-fund/enterprise-recharge": CirclePlus, // 企业充值
  // 资金与结算 / 租户结算
  "/fund-settlement/tenant-settlement/settlement-account": CreditCard, // 结算账户
  "/fund-settlement/tenant-settlement/station-settlement-plan": Calendar, // 场站结算方案
  "/fund-settlement/tenant-settlement/tenant-settlement-bill": DocumentAdd, // 租户结算账单
  "/fund-settlement/tenant-settlement/payment-receipt": Money, // 收款单
  // 资金与结算 / 账务发票
  "/fund-settlement/accounting-invoice/channel-reconciliation": Switch, // 渠道对账
  "/fund-settlement/accounting-invoice/user-charging-invoice": Ticket, // 用户充电发票
  "/fund-settlement/accounting-invoice/tenant-incoming-invoice": DocumentCopy, // 租户进项发票
  "/fund-settlement/accounting-invoice/platform-service-invoice": Collection, // 平台服务发票
  // V2G 运营 / 放电运营
  "/v2g-operation/discharge-operation/discharge-authorization": CircleCheck, // 放电授权
  "/v2g-operation/discharge-operation/discharge-order": Back, // 放电订单
  // V2G 运营 / 放电结算
  "/v2g-operation/discharge-settlement/discharge-pricing": PriceTag, // 放电定价
  "/v2g-operation/discharge-settlement/discharge-settlement": Coin, // 放电结算
  // V2G 运营 / 放电监测
  "/v2g-operation/discharge-monitor/discharge-monitor-analysis": TrendCharts, // 放电监测分析
  // 数据分析 / 经营分析
  "/data-analysis/business-analysis/business-analysis-report": Notebook, // 经营分析报告
  "/data-analysis/business-analysis/revenue-analysis": TrendCharts, // 收入分析
  // 数据分析 / 资产分析
  "/data-analysis/asset-analysis/asset-ops-analysis": Grid, // 资产运营分析
  // 监管接入 / 监管运营
  "/regulatory-access/regulatory-operation/regulatory-archive": Folder, // 监管档案
  "/regulatory-access/regulatory-operation/regulatory-report": Position, // 监管上报
  "/regulatory-access/regulatory-operation/regulatory-reconciliation": CircleCheck, // 监管对账
  // 系统与配置 / 权限管理
  "/system-config/permission-management/platform-admin": UserFilled, // 平台管理员
  "/system-config/permission-management/tenant-permission": Lock, // 租户权限分配
  "/system-config/permission-management/marketing-permission": Stamp, // 营销权限
  "/system-config/permission-management/auth-audit": Search, // 授权审计
  // 系统与配置 / 应用内容
  "/system-config/app-content/mini-program": Cellphone, // 小程序管理
  "/system-config/app-content/mini-program-content": Picture, // 小程序内容
  "/system-config/app-content/dashboard-config": FullScreen, // 大屏配置
  "/system-config/app-content/message-notice": Message, // 消息通知
  // 系统与配置 / 交易配置
  "/system-config/trade-config/payment-channel": CreditCard, // 支付渠道
  "/system-config/trade-config/settlement-channel": Link, // 结算渠道
  // 系统与配置 / 基础设置
  "/system-config/base-setting/platform-setting": Setting, // 平台设置
};

/**
 * 按菜单节点取语义图标：叶子优先按 path 匹配，分组（path 为 null）按 key 匹配；
 * 均未命中时返回兜底图标。
 */
export function menuIconOf(path: string | null, key: string): Component {
  if (path !== null && path !== "" && MENU_ICONS[path] !== undefined) {
    return MENU_ICONS[path];
  }
  return MENU_ICONS[key] ?? DEFAULT_MENU_ICON;
}

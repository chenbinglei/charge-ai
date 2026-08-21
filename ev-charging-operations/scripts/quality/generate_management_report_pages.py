"""Regenerate leadership-report page images and replace DOCX embedded media.

The reports intentionally use one designed PNG per A4 page.  Keeping this
generator makes future service-boundary and release-gate corrections repeatable.
Run with the bundled workspace Python (Pillow required).
"""

from __future__ import annotations

import io
import shutil
import tempfile
import zipfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[2]
REPORTS = ROOT / "delivery" / "leadership-reports"
FONT = "/System/Library/Fonts/PingFang.ttc"
WIDTH, HEIGHT = 1600, 2263
NAVY = "#143A62"
BLUE = "#2774B8"
PALE_BLUE = "#EAF3FB"
GREEN = "#4E8B6A"
PALE_GREEN = "#EDF7F1"
ORANGE = "#C97A17"
PALE_ORANGE = "#FFF5E5"
RED = "#B54343"
PALE_RED = "#FFF0F0"
INK = "#1D2D3D"
MUTED = "#62768A"
LINE = "#C9D8E6"


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(FONT, size, index=1 if bold else 0)


def page(title: str, eyebrow: str, subtitle: str = "") -> tuple[Image.Image, ImageDraw.ImageDraw]:
    image = Image.new("RGB", (WIDTH, HEIGHT), "#FFFFFF")
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, WIDTH, 26), fill=BLUE)
    draw.text((96, 86), eyebrow, font=font(28, True), fill=BLUE)
    draw.text((96, 132), title, font=font(52, True), fill=NAVY)
    if subtitle:
        draw.multiline_text((96, 210), subtitle, font=font(25), fill=MUTED, spacing=8)
    draw.line((96, 302, WIDTH - 96, 302), fill=LINE, width=2)
    draw.text((96, HEIGHT - 72), "充电运营平台｜管理汇报摘要｜以现役 Markdown 与版本化契约为准", font=font(20), fill=MUTED)
    draw.text((WIDTH - 250, HEIGHT - 72), "2026-08-14", font=font(20), fill=MUTED)
    return image, draw


def wrap(draw: ImageDraw.ImageDraw, text: str, fnt: ImageFont.FreeTypeFont, width: int) -> list[str]:
    lines: list[str] = []
    for paragraph in text.split("\n"):
        line = ""
        for char in paragraph:
            candidate = line + char
            if draw.textlength(candidate, font=fnt) <= width:
                line = candidate
            else:
                if line:
                    lines.append(line)
                line = char
        if line:
            lines.append(line)
    return lines


def card(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, h: int, heading: str, body: str,
         color: str = BLUE, fill: str = PALE_BLUE) -> None:
    draw.rounded_rectangle((x, y, x + w, y + h), radius=18, fill=fill, outline=color, width=3)
    draw.text((x + 28, y + 24), heading, font=font(28, True), fill=color)
    lines = wrap(draw, body, font(23), w - 56)
    draw.multiline_text((x + 28, y + 76), "\n".join(lines), font=font(23), fill=INK, spacing=10)


def pill(draw: ImageDraw.ImageDraw, x: int, y: int, text: str, color: str = BLUE, fill: str = PALE_BLUE) -> int:
    w = int(draw.textlength(text, font=font(24, True))) + 44
    draw.rounded_rectangle((x, y, x + w, y + 52), radius=26, fill=fill, outline=color, width=2)
    draw.text((x + 22, y + 11), text, font=font(24, True), fill=color)
    return w


def cover(title: str, subtitle: str, tag: str) -> Image.Image:
    image = Image.new("RGB", (WIDTH, HEIGHT), "#F7FBFF")
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, WIDTH, 34), fill=BLUE)
    for idx, shade in enumerate(("#E9F4FF", "#D7EBFC", "#C4E1F8")):
        draw.rounded_rectangle((100 + idx * 110, 1320 - idx * 125, 1450 - idx * 80, 2030 - idx * 120), radius=44, outline=shade, width=20)
    draw.text((120, 520), tag, font=font(30, True), fill=BLUE)
    draw.multiline_text((120, 600), title, font=font(70, True), fill=NAVY, spacing=18)
    draw.multiline_text((120, 840), subtitle, font=font(32), fill=INK, spacing=14)
    draw.rounded_rectangle((120, 1080, 850, 1146), radius=32, fill=PALE_ORANGE, outline=ORANGE, width=2)
    draw.text((150, 1096), "7 服务｜监管内部就绪｜真实接入 C3-R Go", font=font(25, True), fill=ORANGE)
    draw.text((120, HEIGHT - 160), "管理汇报摘要；实施以现役架构、契约、状态机和执行台账为准", font=font(24), fill=MUTED)
    draw.text((120, HEIGHT - 112), "2026-08-14", font=font(24), fill=MUTED)
    return image


def tech_pages() -> list[Image.Image]:
    pages = [cover("充电运营平台\n技术选型说明", "单机起步形态、边界与验证门禁", "TECHNOLOGY BRIEF")]
    im, d = page("选型结论：七个受控部署单元", "01｜系统组成", "隔离资金、高频设备 I/O、渠道慢调用、监管任务与遥测查询。")
    names = [("api-gateway", "访问与限流"), ("platform-service", "业务与订单"), ("finance-service", "账本/退款/结算"), ("设备互联服务", "MQTT/控制"), ("integration-service", "支付/银行/门禁/开票/通知"), ("ecosystem-service", "监管报送/回执/对账"), ("telemetry-service", "状态/遥测/分析")]
    x, y = 96, 390
    for i, (name, desc) in enumerate(names):
        card(d, x, y, 430, 155, name, desc, GREEN if name == "ecosystem-service" else BLUE, PALE_GREEN if name == "ecosystem-service" else PALE_BLUE)
        x += 486
        if (i + 1) % 3 == 0:
            x, y = 96, y + 195
    card(d, 96, 1050, 1408, 190, "监管归属固定", "国内省市监管只由 ecosystem-service 处理；integration-service 只负责支付、银行、门禁、开票和通知，不得承接监管或设备控制。", ORANGE, PALE_ORANGE)
    card(d, 96, 1280, 1408, 190, "Kafka 的用途", "Kafka 用于已落库事实后的跨服务协作与重试，不是余额账本、前端接口或“恰好一次扣款”保证。Topic 与消费者组以现役事件目录为准。", BLUE, PALE_BLUE)
    pages.append(im)
    im, d = page("关键组件与责任边界", "02｜不混写事实", "组件不是能力承诺；每个组件都要有数据所有者、失败策略和测试证据。")
    items = [("MySQL", "业务与资金事实；各服务独占 schema", BLUE, PALE_BLUE), ("Kafka", "Outbox 后的事件协作、重试、DLQ、受控重放", ORANGE, PALE_ORANGE), ("EMQX + MQTT", "设备接入、控制下发与回执；先验签/落证据", GREEN, PALE_GREEN), ("Redis / ClickHouse / MinIO", "实时投影 / 分钟遥测 / 附件与证据对象", BLUE, PALE_BLUE)]
    for i, (h, b, c, bg) in enumerate(items):
        card(d, 96 + (i % 2) * 720, 390 + (i // 2) * 240, 680, 200, h, b, c, bg)
    card(d, 96, 950, 1408, 230, "资金链路必须这样走", "财务服务是余额、冻结、支付意图和退款交易的唯一写入者。渠道调用与回调证据由 integration-service 管理；Kafka 只传递请求和结果，不能让消费者直接加余额或退款。", RED, PALE_RED)
    card(d, 96, 1220, 1408, 190, "高风险操作已冻结", "设备控制、退款、双模式和监管任务都有版本化 Schema 与状态机。任何新增字段、Topic 或消费者组必须先变更审批、合同测试和回归。", GREEN, PALE_GREEN)
    pages.append(im)
    im, d = page("容量与恢复：现在不签“万台”", "03｜待验证门禁", "8 核 32 GB 是起步测试形态，不是已证明的生产规模或 SLA。")
    card(d, 96, 390, 680, 240, "验证输入", "真实设备、真实协议字段、目标连接数、最短上报周期、订单量、查询并发、保留年限与故障注入。", BLUE, PALE_BLUE)
    card(d, 824, 390, 680, 240, "验证输出", "设备/枪口数量、峰值与突发、资源水位、Kafka 积压、恢复结果与可签发的边界。", GREEN, PALE_GREEN)
    card(d, 96, 680, 1408, 210, "RPO / RTO 的正确含义", "RPO 是最多可接受的数据丢失窗口；RTO 是恢复到可用所需时间。两者必须由真实恢复演练测得并由责任人签发，不能从 Docker 或单机配置推导。", ORANGE, PALE_ORANGE)
    card(d, 96, 940, 1408, 220, "No-Go", "没有真实设备与目标负载压测，或没有从空白主机恢复并校验账本、订单、遥测和证据对象的演练：不得写“万台可用”，不得承诺 RPO/RTO。", RED, PALE_RED)
    pages.append(im)
    im, d = page("上线前不可跳过的生产门禁", "04｜真实能力与模拟能力分开", "模拟器、Mock 或内部闭环只证明开发完成，不等于真实外部生产能力。")
    gates = [("C1/C2", "微信登录、支付、回调与原路退款真实 E2E"), ("C3-R", "每个目标省市：网络白名单、生产凭据、真实报送/回执/双向对账"), ("V2G", "默认真实执行关闭；只允许模拟，或经单独批准的现场试点"), ("容量", "真实设备 + 目标负载 + 恢复演练后，才决定规模与 RPO/RTO")]
    for i, (h, b) in enumerate(gates):
        card(d, 96 + (i % 2) * 720, 400 + (i // 2) * 280, 680, 230, h, b, RED if h in ("C3-R", "V2G") else BLUE, PALE_RED if h in ("C3-R", "V2G") else PALE_BLUE)
    card(d, 96, 1080, 1408, 190, "管理层需要的结论", "先签发每个门禁的真实证据，再讨论范围、规模和上线日期。没有证据时，正确状态是“待完成”或“No-Go”，不是以术语替代验证。", ORANGE, PALE_ORANGE)
    pages.append(im)
    return pages


def architecture_pages() -> list[Image.Image]:
    pages = [cover("充电运营平台\n系统架构设计", "服务边界、数据主责与生产门禁", "ARCHITECTURE BRIEF")]
    im, d = page("逻辑架构：入口、七服务、设备与监管分离", "01｜服务边界", "这是一张管理摘要图；精确 Topic、Schema、状态和字段以现役契约为准。")
    card(d, 96, 360, 1408, 120, "七端 → api-gateway", "P1 / P2 / M1–M4 / D1 经 HTTPS/WSS 进入；网关只做路由、预校验、限流和追踪。", BLUE, PALE_BLUE)
    services = [("platform", "用户/资产/订单/运营/V2G"), ("finance", "账本/冻结/支付/退款/结算"), ("integration", "支付/银行/门禁/开票/通知"), ("ecosystem", "监管：任务/报送/回执/对账"), ("设备互联服务", "MQTT/控制/协同同步"), ("telemetry", "实时状态/分钟遥测/分析")]
    for i, (h, b) in enumerate(services):
        label = h if h.endswith("服务") else h + "-service"
        card(d, 96 + (i % 3) * 486, 550 + (i // 3) * 220, 450, 180, label, b, GREEN if h == "ecosystem" else BLUE, PALE_GREEN if h == "ecosystem" else PALE_BLUE)
    card(d, 96, 1050, 1408, 160, "设备与外部边界", "设备只经 EMQX/MQTT 进入设备互联服务；国内监管只经生态监管服务；支付等渠道只经集成服务。监管没有设备控制权。", ORANGE, PALE_ORANGE)
    card(d, 96, 1260, 1408, 160, "异步协作", "Kafka 只承载已登记的版本化领域事件。每个服务拥有本地 Outbox/Inbox；重复、乱序、重试与 DLQ 必须被显式处理。", BLUE, PALE_BLUE)
    pages.append(im)
    im, d = page("七服务责任表：谁负责，谁绝不能做", "02｜边界即风险控制")
    rows = [("platform-service", "业务事实、订单、授权", "账本、MQTT、渠道 I/O"), ("finance-service", "账本、余额、冻结、退款、结算", "设备控制、直接渠道调用"), ("integration-service", "支付/银行/门禁/开票/通知协议", "监管、账本、订单状态机"), ("ecosystem-service", "监管任务、报送、回执、对账", "业务事实写入、设备控制"), ("device-connectivity-service", "MQTT、控制、回执、协同同步", "资产/订单/资金写入"), ("telemetry-service", "实时投影、分钟遥测、分析", "订单、资金、权限写入"), ("api-gateway", "路由、预校验、限流、TraceId", "对象级授权、资金决策"),]
    y = 370
    for i, (h, own, no) in enumerate(rows):
        bg = PALE_GREEN if h == "ecosystem-service" else ("#F7FAFD" if i % 2 == 0 else "#FFFFFF")
        d.rectangle((96, y, 1504, y + 125), fill=bg, outline=LINE, width=2)
        d.text((120, y + 20), h, font=font(23, True), fill=GREEN if h == "ecosystem-service" else NAVY)
        d.multiline_text((520, y + 18), "负责：" + own, font=font(22), fill=INK, spacing=5)
        d.multiline_text((1010, y + 18), "禁止：" + no, font=font(22), fill=RED, spacing=5)
        y += 125
    pages.append(im)
    im, d = page("数据主责：事实不可跨库偷写", "03｜11 个 schema，7 个服务")
    boxes = [("platform", "evco_iam / master / customer / trade / marketing / operations / v2g"), ("finance", "evco_finance：账本、资金批次、冻结、支付与退款交易"), ("integration", "evco_integration：渠道请求、回调原文、验签、重试、死信"), ("ecosystem", "evco_ecosystem：监管目标、任务、回执、补推、对账"), ("device", "evco_device_connectivity：协议证据、收件箱、检查点"), ("telemetry", "ClickHouse evco_telemetry + Redis 投影")]
    for i, (h, b) in enumerate(boxes):
        card(d, 96 + (i % 2) * 720, 390 + (i // 2) * 245, 680, 205, h + " 数据主责", b, GREEN if h == "ecosystem" else BLUE, PALE_GREEN if h == "ecosystem" else PALE_BLUE)
    card(d, 96, 1180, 1408, 190, "安全与治理", "数据分类、留存、境内存储、导出、删除和密钥管理以矩阵执行。导出、密钥引用变更、监管补推、退款和控制命令均需审计。", ORANGE, PALE_ORANGE)
    pages.append(im)
    im, d = page("上线判断：内部就绪不等于真实生产接入", "04｜必须区分状态")
    card(d, 96, 390, 680, 260, "W10：监管内部就绪", "完成档案、任务、映射、模拟报送、补推和对账的内部闭环。它只证明本方系统可演练，不能写成已接入监管生产。", ORANGE, PALE_ORANGE)
    card(d, 824, 390, 680, 260, "C3-R：真实监管生产 Go", "逐目标完成网络/白名单、生产凭据、真实档案/状态/订单报送、有效回执与双向对账 E2E；缺任一项即 No-Go。", RED, PALE_RED)
    card(d, 96, 710, 680, 240, "V2G", "真实控制默认关闭。W11 仅模拟；现场真实试点必须另批并冻结设备、车辆、计量、停止/回退、负责人和 E2E 证据。", RED, PALE_RED)
    card(d, 824, 710, 680, 240, "规模与恢复", "完成真实设备、目标负载、故障注入和恢复演练后，才确定可签发的容量与恢复边界；当前不得标称万台可用。", RED, PALE_RED)
    card(d, 96, 1030, 1408, 170, "实施入口", "架构基线 → Kafka 事件目录 → 高风险契约与状态机 → 数据治理矩阵 → 容量与恢复验收方案。管理摘要不替代这些实施依据。", BLUE, PALE_BLUE)
    pages.append(im)
    return pages


def plan_pages() -> list[Image.Image]:
    pages = [cover("充电运营平台\n全功能交付实施周期", "12 周串行交付、真实门禁与生产 Go", "DELIVERY BRIEF")]
    im, d = page("12 周交付：范围不靠压缩门禁换取", "01｜串行节奏", "模块遵循“需求确认 → 后端/迁移/契约 → API 验证 → 前端 → E2E/权限/异常 → 完成记录”。")
    weeks = [("W1–W2", "工程跑道、7 服务契约、IAM/双模式/渠道身份"), ("W3–W4", "资产、MQTT、遥测、控制与实时状态"), ("W5–W6", "订单、个人资金、充值、支付/退款与对账"), ("W7–W8", "企业、企业资金、租户结算与权限"), ("W9", "运营、工单、营销、条件连接器"), ("W10", "分析、开票、监管内部就绪"), ("W11", "V2G 模拟与关闭态；条件渠道联调"), ("W12", "全量 E2E、性能、安全、恢复、UAT、上线演练")]
    for i, (h, b) in enumerate(weeks):
        card(d, 96 + (i % 2) * 720, 380 + (i // 2) * 200, 680, 160, h, b, ORANGE if h == "W10" else BLUE, PALE_ORANGE if h == "W10" else PALE_BLUE)
    pages.append(im)
    im, d = page("W10 的准确叫法：监管内部就绪", "02｜不把模拟当生产", "监管是第七服务 ecosystem-service 的现行范围；真实接入另列生产 Go。")
    card(d, 96, 390, 1408, 200, "W10 交付", "监管档案、版本化任务、映射、模拟报送、回执处理、漏推/补推、对账、P1/D1 运营可见性；所有结果可追溯。", ORANGE, PALE_ORANGE)
    card(d, 96, 650, 1408, 220, "C3-R 生产 Go（按每个实际目标）", "网络与白名单、生产凭据、真实档案/状态/订单报送、有效回执与双向对账 E2E。没有这些证据，不得叫“真实监管接入”，不得随 W10 自动上线。", RED, PALE_RED)
    card(d, 96, 930, 1408, 190, "职责边界", "ecosystem-service 负责监管；integration-service 只负责支付、银行、门禁、开票、通知。监管没有设备控制权，监管失败不得阻断充电、支付或订单结算。", GREEN, PALE_GREEN)
    pages.append(im)
    im, d = page("真实能力与条件能力的上线门禁", "03｜门禁表", "每项外部能力都必须有真实证据，Mock 和模拟只能完成内部开发验证。")
    rows = [("微信登录/支付/原路退款", "C1-W / C2-W 真实 E2E", "未完成：生产 No-Go"), ("监管", "C3-R 逐目标真实 E2E", "未完成：监管生产 No-Go"), ("V2G", "默认关闭真实执行；另批现场试点", "仅模拟可验收"), ("容量与恢复", "真实设备、目标负载、故障注入、恢复演练", "未完成：不签万台/RPO/RTO")]
    y = 405
    for h, proof, result in rows:
        card(d, 96, y, 1408, 175, h, proof + "\n" + result, RED if "No-Go" in result or "仅模拟" in result else BLUE, PALE_RED if "No-Go" in result or "仅模拟" in result else PALE_BLUE)
        y += 215
    pages.append(im)
    im, d = page("上线包必须交付的证据", "04｜管理层验收清单", "如果证据不足，应呈现阻塞项与影响，而不是把待验证能力写成已完成。")
    evidence = [("实施合同", "OpenAPI、MQTT、Kafka Schema 与状态机已冻结并通过契约测试"), ("资金与控制", "幂等、顺序、原路退款、权限、审计、失败补偿 E2E"), ("数据治理", "分类、留存、境内存储、导出、删除、密钥管理与访问证据"), ("性能与恢复", "真实设备/目标负载压测，备份恢复与故障注入结果"), ("外部生产", "C1/C2/C3-R 以及任何批准试点的真实 E2E 证据")]
    for i, (h, b) in enumerate(evidence):
        card(d, 96 + (i % 2) * 720, 400 + (i // 2) * 220, 680, 180, h, b, GREEN if h == "数据治理" else BLUE, PALE_GREEN if h == "数据治理" else PALE_BLUE)
    card(d, 96, 1100, 1408, 160, "最终原则", "生产 Go 是证据判断，不是日历判断。真实监管、真实 V2G 与容量/RPO/RTO 任何一项未过门禁，都要明确为关闭态或 No-Go。", ORANGE, PALE_ORANGE)
    pages.append(im)
    return pages


def landscape_architecture() -> Image.Image:
    image = Image.new("RGB", (1800, 1290), "#FFFFFF")
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, 1800, 22), fill=BLUE)
    draw.text((62, 48), "系统逻辑架构（按调用、事件与数据主责分层）", font=font(42, True), fill=NAVY)
    draw.text((62, 112), "管理摘要：7 服务；监管由 ecosystem-service 负责；精确 Topic/Schema 以现役合同为准", font=font(23), fill=MUTED)
    card(draw, 62, 175, 470, 105, "七端客户端", "P1 / P2 / M1–M4 / D1", BLUE, PALE_BLUE)
    card(draw, 665, 175, 700, 105, "接入与安全边界", "Nginx + api-gateway｜TLS、预校验、限流、路由、追踪", BLUE, PALE_BLUE)
    draw.line((532, 230, 665, 230), fill=BLUE, width=5)
    draw.polygon([(665, 230), (645, 218), (645, 242)], fill=BLUE)
    draw.rounded_rectangle((62, 330, 1738, 810), radius=28, outline=LINE, width=3, fill="#F8FBFE")
    draw.text((90, 360), "业务服务层｜各服务拥有自己的业务主责；跨服务副作用不走共享数据库", font=font(25), fill=MUTED)
    services = [("platform-service", "用户、资产、订单、运营、V2G", PALE_GREEN, GREEN), ("finance-service", "账本、支付、退款、结算、对账", PALE_ORANGE, ORANGE), ("integration-service", "支付、银行、门禁、开票、通知", PALE_ORANGE, ORANGE), ("ecosystem-service", "监管：任务、报送、回执、对账", PALE_GREEN, GREEN), ("设备互联服务", "MQTT、控制、IOT 管理同步", PALE_GREEN, GREEN), ("telemetry-service", "实时投影、分钟遥测、分析", PALE_BLUE, BLUE)]
    for i, (h, b, bg, c) in enumerate(services):
        x = 92 + (i % 3) * 565
        y = 430 + (i // 3) * 200
        card(draw, x, y, 500, 150, h, b, c, bg)
    card(draw, 220, 850, 1360, 120, "Kafka 事件总线", "已登记的版本化领域事件｜Outbox｜幂等消费｜重试 / DLQ｜受控重放（不是余额账本或浏览器接口）", ORANGE, PALE_ORANGE)
    draw.rounded_rectangle((62, 1000, 1738, 1215), radius=28, outline=LINE, width=3, fill="#F8FBFE")
    card(draw, 92, 1028, 720, 158, "数据与对象层", "MySQL 分 schema；Redis 实时投影；ClickHouse 分钟遥测；MinIO 证据/附件", BLUE, PALE_BLUE)
    card(draw, 900, 1028, 780, 158, "外部边界", "支付等渠道 → 集成服务｜国内省市监管 → 生态监管服务｜设备 → MQTT/设备互联服务", GREEN, PALE_GREEN)
    draw.text((62, 1240), "真实监管须逐目标 C3-R 生产 Go；真实 V2G 默认关闭；真实设备/目标负载压测前不签发万台、RPO/RTO。", font=font(21, True), fill=RED)
    return image


def replace_media(docx: Path, pages: list[Image.Image]) -> None:
    with tempfile.TemporaryDirectory() as tmp:
        tmp_path = Path(tmp)
        replacements: dict[str, bytes] = {}
        for index, image in enumerate(pages, 1):
            buf = io.BytesIO()
            image.save(buf, format="PNG", optimize=True)
            replacements[f"word/media/image{index}.png"] = buf.getvalue()
        out = tmp_path / docx.name
        with zipfile.ZipFile(docx, "r") as source, zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as target:
            for member in source.infolist():
                target.writestr(member, replacements.get(member.filename, source.read(member.filename)))
        shutil.copy2(out, docx)


def main() -> None:
    replace_media(REPORTS / "充电运营平台技术选型说明-管理汇报版.docx", tech_pages())
    replace_media(REPORTS / "充电运营平台系统架构设计-管理汇报版.docx", architecture_pages())
    replace_media(REPORTS / "充电运营平台全功能交付实施周期-管理汇报版.docx", plan_pages())
    landscape_architecture().save(REPORTS / "系统逻辑架构-优化版.png", format="PNG", optimize=True)


if __name__ == "__main__":
    main()

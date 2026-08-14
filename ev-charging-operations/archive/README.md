# 归档中心

归档文件不作为当前设计、开发或验收依据；当前有效资料必须从 `docs/` 对应分类目录进入。

```text
archive/
├── requirements/
│   ├── archive-baselines/     # 被新基线替代的历史需求与菜单
│   └── rejected-drafts/       # 经评审未采纳的草案，保留追溯
│   └── reviews/               # 针对旧计划的审查/批判过程
├── design/                    # 被替代的设计与评估资料
│   └── review-history/        # 逻辑映射、蓝图与处置历史
├── integrations/              # 后续替代的接入资料
├── delivery/                  # 后续替代的交付物
└── code/                      # 后续退役代码或迁移快照；不得存放生产密钥
```

归档动作要求：保留原文件名、在现役文档中移除入口或改为历史链接、在文件首行（或归档 README 对二进制文件）标注归档原因和替代文件。不得将真实账号、密钥、生产数据或未脱敏日志归档。

`code/legacy-placeholder-backend/` 与 `code/legacy-placeholder-frontend/` 保存 2026-08-12 目录收敛前的 README 占位快照；其中没有业务源码。

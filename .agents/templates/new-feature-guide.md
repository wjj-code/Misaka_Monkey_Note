# 新增功能开发指南模板

## 功能名称


## 需求概述


## 涉及层级

| 层级 | 是否涉及 | 说明 |
|------|---------|------|
| Entity | ☐ | |
| DAO | ☐ | |
| Database | ☐ | |
| Repository | ☐ | |
| ViewModel | ☐ | |
| UI (Fragment/Adapter) | ☐ | |
| 资源 (layout/drawable/strings) | ☐ | |

## 开发步骤

### 1. 数据层改动 (data/)

<!-- 例如：新增 Entity、修改 DAO 查询、写 Migration -->

### 2. Repository 层改动

<!-- 新增方法，保持 LiveData 返回类型 -->

### 3. ViewModel 层改动

<!-- 透传 Repository 方法 -->

### 4. UI 层改动

<!-- Fragment 布局 + Java 逻辑 -->

### 5. 资源文件

<!-- 新增 layout、string、drawable 等 -->

## 测试要点

- [ ] 数据写入/读取正确
- [ ] LiveData 自动刷新
- [ ] 边界条件（空数据、非法输入）
- [ ] 不影响现有功能

## 关联参考

<!-- 参考 facts/ 和 examples/ 目录中的相关文档 -->
- `facts/class-method-reference.md`
- `facts/architecture-overview.md`

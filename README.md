# 团课预约与核销系统

面向中小型培训机构的团课报名、支付、核销与结算平台。毕业设计项目。

## 业务模型

- **班级**是交易单位：教练 + 场地 + 固定时段 + 总课次 + 名额 + 价格，一次性购买整班
- **课次**是履约单位：班级按周几展开成 N 条课次，到场一次核销一次
- 报名只占名额、不扣课次；缺勤不核销、不退款
- 退款金额 = 总价 − 单价 × 已核销课次

## 技术栈

| 层 | 选型 |
|---|---|
| 后端 | Spring Boot 3.2 / JDK 17 / MyBatis-Plus 3.5 |
| 数据库 | MySQL 8.0+ |
| 缓存 | Redis（抢课 Lua、名额计数） |
| 鉴权 | Spring Security + JWT |
| 前端 | Vue 3 + Vite（会员 H5 用 Vant 4，教练与机构端用 Element Plus） |

前后端分离：前端独立构建成静态资源，通过 HTTP 调用 `/api/**`。

## 目录

```
group-class\
├─ backend\            Spring Boot 工程，按业务域分包
├─ frontend\           Vue 3 工程（待建）
└─ deploy\
   └─ mysql\init\      建表与初始化数据
```

## 环境

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 17 | `JAVA_HOME` 必须指向 17，Maven 读它 |
| Maven | 3.9 | |
| MySQL | 8.0+ / 9.x | 本机 9.4 可用 |
| Redis | 5+ | 抢课 Lua 脚本需要 |

## 建库

```powershell
mysql -u root -p --default-character-set=utf8mb4 < deploy\mysql\init\01-schema.sql
mysql -u root -p --default-character-set=utf8mb4 < deploy\mysql\init\02-data.sql
mysql -u root -p --default-character-set=utf8mb4 < deploy\mysql\init\03-dev-accounts.sql
```

`03-dev-accounts.sql` 是开发环境测试账号，上线前删掉。

## 启动

```powershell
cd backend
$env:DB_PASSWORD = "你的密码"
mvn spring-boot:run
```

默认 8080。数据库账号密码走环境变量，不写进配置文件：

| 变量 | 默认值 | 说明 |
|---|---|---|
| `DB_USERNAME` | `root` | 数据库账号 |
| `DB_PASSWORD` | 无 | 数据库密码，必须设 |
| `JWT_SECRET` | 内置开发密钥 | 部署前必须换 |

## 测试账号

| 账号 | 密码 | 角色 |
|---|---|---|
| `admin` | `123456` | 机构管理员 |
| `coach1` | `123456` | 教练 |
| `member1` | `123456` | 会员 |

## 代码组织

按业务域分包，每个域内部再分层 `controller / service / mapper / entity / dto`：

```
com.ohc.groupclass
├─ common      统一返回、异常、错误码
├─ config      Security / MyBatis-Plus / Redis
├─ security    JWT 与角色校验
├─ system      健康检查
├─ user        账号与登录
├─ coach       教练档案
├─ course      课程模板、场地、班级、课次
├─ enrollment  抢课与名额
├─ order       订单、退款
├─ payment     支付渠道与回调
├─ checkin     核销
└─ settlement  结算、报表、对账
```

域之间只走 service，不跨域注入 mapper。

## 接口约定

统一响应：

```json
{ "code": 0, "message": "ok", "data": {} }
```

| code | 含义 |
|---|---|
| 0 | 成功 |
| 400 | 参数校验失败 |
| 401 | 未登录或 token 过期 |
| 403 | 无权访问 |
| 404 | 资源不存在 |
| 409 | 数据冲突 |
| 429 | 名额已满 |
| 500 | 服务器内部错误 |

鉴权方式：请求头 `Authorization: Bearer <token>`。

## 进度

- [x] 工程骨架、统一返回与异常、MySQL / Redis 配置
- [x] 建表脚本与初始化数据
- [x] 登录、JWT、三角色鉴权
- [x] 教练档案
- [ ] 课程与排期
- [ ] 抢课（Redis + Lua）
- [ ] 订单与支付回调
- [ ] 核销
- [ ] 结算与报表
- [ ] 前端三端

## 开发约定

- `deploy/mysql/init/01-schema.sql` 只增不改，任何改动新建 `03-alter-*.sql` 起编号
- 时间统一 `LocalDateTime` + `Asia/Shanghai`，不用 `java.util.Date`
- 并发场景一律条件更新或唯一键兜底，不靠先查后改
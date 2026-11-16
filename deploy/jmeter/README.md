# P3 抢课并发压测

目标：200 个会员并发抢 30 个名额，验收「成功 30、超卖 0、Redis 余量 + 数据库占位 = 名额」。

## 前置

1. MySQL、Redis、后端都在跑（`http://127.0.0.1:8080`）。
2. 已执行 `deploy/mysql/init/05-p3-test-accounts.sql`，库里存在 `member001` ~ `member200`，密码都是 `123456`。
3. 装了 JDK 17，JMeter 5.6.x（解压到 D 盘，例如 `D:\apache-jmeter-5.6.3`）。

## 跑

```powershell
cd E:\毕业设计\group-class\deploy\jmeter

# 1. 建一个干净的压测班，并登录 200 个会员生成 tokens.csv
powershell -ExecutionPolicy Bypass -File .\prepare.ps1

# 2. 记下脚本输出的 class id，然后压测
D:\apache-jmeter-5.6.3\bin\jmeter.bat -n -t .\p3-enroll.jmx `
  -JCLASS_ID=<上一步的 class id> -JTHREADS=200 -l .\result.jtl

# 3. 统计成功 / 满员 / 重复 / P95 / P99
powershell -ExecutionPolicy Bypass -File .\summarize.ps1 -Jtl .\result.jtl
```

## 结果核对

```sql
SELECT COUNT(*) FROM group_class.enrollment
WHERE class_id = <class id> AND status IN ('PENDING_PAYMENT', 'CONFIRMED');
-- 期望 30
```

```powershell
D:\Redis-x64-3.2.100\redis-cli.exe get class:stock:<class id>
# 期望 0
D:\Redis-x64-3.2.100\redis-cli.exe scard class:enrolled:<class id>
# 期望 30
```

## 说明

- JMeter 的 `-l` 用 CSV 才有响应体可统计；JMX 里已把 `responseData` 打开。
- `prepare.ps1` 每次会新建一个班，用随机未来日期避开排期冲突；重复跑不会覆盖旧班。
- 想复现「超卖」对照实验时，把 `05-p3-test-accounts.sql` 之外的账号也加进来即可，验收口径不变。
- 停止后端后 Redis 的值会保留，重启后端会按数据库重建，属于预期行为。

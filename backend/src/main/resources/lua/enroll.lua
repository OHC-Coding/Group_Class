-- 抢课原子脚本
-- KEYS[1] class:stock:{classId}    剩余名额
-- KEYS[2] class:enrolled:{classId} 已占位会员集合
-- ARGV[1] memberId
-- 返回：>=0 抢位成功，值为抢后剩余；-1 已报名；-2 库存未初始化；-3 已满

local added = redis.call('SADD', KEYS[2], ARGV[1])
if added == 0 then
    return -1
end

local stock = redis.call('GET', KEYS[1])
if not stock then
    redis.call('SREM', KEYS[2], ARGV[1])
    return -2
end

if tonumber(stock) <= 0 then
    redis.call('SREM', KEYS[2], ARGV[1])
    return -3
end

return redis.call('DECR', KEYS[1])

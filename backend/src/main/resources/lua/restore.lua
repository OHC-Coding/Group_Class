-- 库存重建脚本：以数据库为基准原子覆盖 Redis
-- KEYS[1] class:stock:{classId}
-- KEYS[2] class:enrolled:{classId}
-- ARGV[1] remaining
-- ARGV[2..] occupied memberId
-- 返回剩余名额

redis.call('SET', KEYS[1], ARGV[1])
redis.call('DEL', KEYS[2])

for i = 2, #ARGV do
    redis.call('SADD', KEYS[2], ARGV[i])
end

return tonumber(ARGV[1])

-- Redis Lua 原子锁座脚本
-- KEYS: 座位锁的 key 列表 (格式: lock:{showtimeId}:{seatId})
-- ARGV[1]: userId
-- ARGV[2]: TTL (秒)
-- 返回: 1=全部锁定成功, 0=有冲突 (冲突的 seatId 在 ARGV 中后续返回)

local userId = ARGV[1]
local ttl = tonumber(ARGV[2])

-- 第一步：检查所有座位是否可用
for i, key in ipairs(KEYS) do
    local val = redis.call('GET', key)
    if val and val ~= '' then
        -- 座位已被锁定，返回冲突的 key
        redis.call('EVAL', 'return 0', 0)  -- 这里不能直接返回冲突ID，改用下面的方式
        return {0, key}
    end
end

-- 第二步：全部锁定
for i, key in ipairs(KEYS) do
    redis.call('SETEX', key, ttl, userId)
end

return {1, ''}

local orderKey   = "orders:" .. KEYS[1]
local indexKey   = "orderIndex:" .. KEYS[1]
local bookKey    = "orderbook:{" .. ARGV[1] .. "}:" .. ARGV[2]

redis.call("ZREM", bookKey, KEYS[1])
redis.call("DEL", orderKey)
redis.call("DEL", indexKey)

return 1
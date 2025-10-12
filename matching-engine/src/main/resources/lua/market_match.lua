-- KEYS:
--   KEYS[1] = orderbook:{ticker}:bids        -- 매수 호가 ZSET
--   KEYS[2] = orderbook:{ticker}:asks        -- 매도 호가 ZSET
--
-- ARGV:
--   1: ticker                                -- 종목 코드
--   2: side ("BUY" or "SELL")                -- 들어온 시장가 주문의 사이드
--   3: quantity (string number)              -- 주문 수량(문자열 숫자)
--   4: maxMatches (integer per invocation)   -- 한 번 실행에서 최대 매칭 건수(예: 100)
--
-- 주문 상세 해시(HASH) 키 스키마(반드시 동일 {ticker} 해시태그 슬롯 사용):
--   orders:{ticker}:{orderId}                -- 필드: quantity, priceInt, side, ticker

local ticker      = ARGV[1]
local side        = ARGV[2]
local qty_left    = tonumber(ARGV[3])
local max_matches = tonumber(ARGV[4])

if qty_left == nil or qty_left <= 0 then
  return { "0", "0" } -- remaining, matchCount
end

-- 단일 슬롯 실행을 보장하기 위해 KEYS로 반대편 호가를 선택한다.
-- 시장가 BUY면 매도호가(ASKS, KEYS[2])를, 시장가 SELL이면 매수호가(BIDS, KEYS[1])를 조회한다.
local upper_side = string.upper(side)
local bookKey
local fetch_cmd

if upper_side == "BUY" then
  bookKey   = KEYS[2]       -- 매도호가 사용
  fetch_cmd = "ZRANGE"      -- 최저가부터
else
  bookKey   = KEYS[1]       -- 매수호가 사용
  fetch_cmd = "ZREVRANGE"   -- 최고가부터
end

local matches = {}
local matchCount = 0

-- 도우미: 해시에서 숫자 필드를 읽되 없으면 0 반환
local function hget_number(key, field)
  local v = redis.call("HGET", key, field)
  if not v then return 0.0 end
  return tonumber(v)
end

while qty_left > 0 and matchCount < max_matches do
  local topIds = redis.call(fetch_cmd, bookKey, 0, 0)
  if (not topIds) or (#topIds == 0) then
    break
  end

  local topId = topIds[1]
  -- 주문 상세 키는 반드시 동일한 {ticker} 슬롯을 사용해야 한다.
  local oKey  = "orders:{" .. ticker .. "}:" .. topId

  -- If detail is gone, clean zset member
  if redis.call("EXISTS", oKey) == 0 then
    redis.call("ZREM", bookKey, topId)

  else
    -- 안전 점검: 상세에 저장된 ticker가 일치하는지 확인(항상 참이어야 함)
    local topTicker = redis.call("HGET", oKey, "ticker")
    if (not topTicker) or (topTicker ~= ticker) then
      redis.log(redis.LOG_WARNING, "Unexpected ticker on " .. oKey .. " expected=" .. ticker .. " got=" .. tostring(topTicker))
      redis.call("ZREM", bookKey, topId)

    else
      local topQty   = hget_number(oKey, "quantity")
      local priceInt = hget_number(oKey, "priceInt")

      if topQty <= 0 then
        -- 오래된/무효 주문 정리
        redis.call("ZREM", bookKey, topId)
        redis.call("DEL",  oKey)

      else
        local fill = qty_left
        if topQty < fill then fill = topQty end

        local remain = topQty - fill
        if remain <= 0 then
          redis.call("ZREM", bookKey, topId)
          redis.call("DEL",  oKey)
          -- 주의: orderIndex:{orderId}는 다른 슬롯에 있을 수 있으므로 여기서 건드리지 않음
        else
          redis.call("HSET", oKey, "quantity", tostring(remain))
        end

        -- 매칭 결과 누적: orderId, 체결수량(fillQty), 가격정수(priceInt)
        table.insert(matches, topId)
        table.insert(matches, tostring(fill))
        table.insert(matches, tostring(priceInt))

        qty_left   = qty_left - fill
        matchCount = matchCount + 1
      end
    end
  end
end

-- 반환 형식:
-- [ remaining, matchCount, orderId1, fill1, priceInt1, orderId2, fill2, priceInt2, ... ]
local out = { tostring(qty_left), tostring(matchCount) }
for i = 1, #matches do
  table.insert(out, matches[i])
end
return out

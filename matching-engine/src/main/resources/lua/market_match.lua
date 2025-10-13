-- KEYS:
--   KEYS[1] = orderbook:{ticker}:bids        -- 매수 호가 ZSET (score = priceInt, 높은 값 우선)
--   KEYS[2] = orderbook:{ticker}:asks        -- 매도 호가 ZSET (score = priceInt, 낮은 값 우선)
--
-- ARGV:
--   1: ticker
--   2: side ("BUY" or "SELL")                -- 들어온 주문의 사이드
--   3: quantity (string number)              -- 주문 수량(문자열 숫자)
--   4: maxMatches (int)                      -- 한 번 실행에서 최대 매칭 수
--   5: guardPxInt (int or nil-string)        -- 가격 가드(시장가면 nil/빈문자)
--   6: orderIdToAdd (string)                 -- LIMIT 잔량 적재용(시장가면 "")
--   7: priceIntForAdd (int)                  -- LIMIT 잔량 적재 가격(시장가면 0)
--
-- 주문 상세 해시(HASH) 스키마(동일 {ticker} 해시태그 슬롯 사용 필수):
--   orders:{ticker}:{orderId}  -- fields: quantity(string), priceInt(string), side, ticker

local ticker         = ARGV[1]
local side           = string.upper(ARGV[2] or "")
local qty_left       = tonumber(ARGV[3])
local max_matches    = tonumber(ARGV[4])
local guardPxInt_raw = ARGV[5]
local orderIdToAdd   = ARGV[6] or ""
local priceIntForAdd = tonumber(ARGV[7] or "0")

if not qty_left or qty_left <= 0 then
  return { "0", "0" }
end
if not max_matches or max_matches <= 0 then
  max_matches = 100
end

-- guardPxInt: nil/빈문자면 가드 미적용(시장가)
local guardPxInt = nil
if guardPxInt_raw and guardPxInt_raw ~= "" then
  guardPxInt = tonumber(guardPxInt_raw)
end

-- 시장가 BUY면 매도호가(ASKS), 시장가 SELL이면 매수호가(BIDS)를 소비
local bookKey, fetch_cmd, zscore_cmd, oppositeSide
if side == "BUY" then
  bookKey      = KEYS[2]      -- asks
  fetch_cmd    = "ZRANGE"     -- 최저가부터
  zscore_cmd   = "ZSCORE"
  oppositeSide = "SELL"
elseif side == "SELL" then
  bookKey      = KEYS[1]      -- bids
  fetch_cmd    = "ZREVRANGE"  -- 최고가부터
  zscore_cmd   = "ZSCORE"
  oppositeSide = "BUY"
else
  return { tostring(qty_left), "0" }
end

local function hget_number(key, field)
  local v = redis.call("HGET", key, field)
  if not v then return 0.0 end
  return tonumber(v)
end

local matches = {}
local matchCount = 0

while qty_left > 0 and matchCount < max_matches do
  local topIds = redis.call(fetch_cmd, bookKey, 0, 0)
  if (not topIds) or (#topIds == 0) then
    break
  end
  local topId = topIds[1]
  local oKey  = "orders:{" .. ticker .. "}:" .. topId

  if redis.call("EXISTS", oKey) == 0 then
    redis.call("ZREM", bookKey, topId)
  else
    local topTicker = redis.call("HGET", oKey, "ticker")
    if (not topTicker) or (topTicker ~= ticker) then
      redis.log(redis.LOG_WARNING, "Unexpected ticker on " .. oKey .. " expected=" .. ticker .. " got=" .. tostring(topTicker))
      redis.call("ZREM", bookKey, topId)
    else
      local topQty   = hget_number(oKey, "quantity")
      local priceInt = hget_number(oKey, "priceInt")

      if topQty <= 0 then
        redis.call("ZREM", bookKey, topId)
        redis.call("DEL",  oKey)
      else
        -- 가격 가드 적용
        if guardPxInt ~= nil then
          if side == "BUY" then
            -- BUY는 guardPxInt(최대 허용가) 보다 비싼 가격이면 체결 금지
            if priceInt > guardPxInt then
              break
            end
          else
            -- SELL은 guardPxInt(최저 허용가) 보다 싼 가격이면 체결 금지
            if priceInt < guardPxInt then
              break
            end
          end
        end

        local fill = qty_left
        if topQty < fill then fill = topQty end

        local remain = topQty - fill
        if remain <= 0 then
          redis.call("ZREM", bookKey, topId)
          redis.call("DEL",  oKey)
        else
          redis.call("HSET", oKey, "quantity", tostring(remain))
        end

        table.insert(matches, topId)
        table.insert(matches, tostring(fill))
        table.insert(matches, tostring(priceInt))

        qty_left   = qty_left - fill
        matchCount = matchCount + 1
      end
    end
  end
end

-- LIMIT 잔량 적재: qty_left > 0 이고 orderIdToAdd 지정된 경우에만
if qty_left > 0 and orderIdToAdd ~= "" then
  local addSide = side
  local zsetKey = (addSide == "BUY") and KEYS[1] or KEYS[2]

  -- 시간 tie-breaker 시퀀스 (자바와 동일 키)
  local seqKey  = (addSide == "BUY") and KEYS[3] or KEYS[4]
  local seq     = redis.call("INCR", seqKey)

  -- 자바와 동일 FACTOR (ARGV[8]로 전달됨)
  local FACTOR  = tonumber(ARGV[8]) or 1000000

  local function bidScore(priceInt, seq)
    local s = seq % FACTOR
    return priceInt * FACTOR + (FACTOR - s)   -- 높은 가격, 빠른 시간 우선
  end
  local function askScore(priceInt, seq)
    local s = seq % FACTOR
    return priceInt * FACTOR + s              -- 낮은 가격, 빠른 시간 우선
  end

  local score = (addSide == "BUY")
          and bidScore(priceIntForAdd, seq)
          or  askScore(priceIntForAdd, seq)

  local addKey = "orders:{" .. ticker .. "}:" .. orderIdToAdd
  redis.call("HSET", addKey,
          "ticker",  ticker,
          "side",    addSide,
          "quantity", tostring(qty_left),
          "priceInt", tostring(priceIntForAdd)
  )
  redis.call("ZADD", zsetKey, score, orderIdToAdd)
end

-- out: [ remaining, matchCount, orderId1, fill1, priceInt1, ... ]
local out = { tostring(qty_left), tostring(matchCount) }
for i = 1, #matches do
  table.insert(out, matches[i])
end
return out

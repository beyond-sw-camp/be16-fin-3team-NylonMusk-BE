-- KEYS:
--   KEYS[1] = orderbook:{ticker}:bids
--   KEYS[2] = orderbook:{ticker}:asks
-- ARGV:
--   ARGV[1] = ticker
--   ARGV[2] = max_depth (상위 N개 호가 조회, 기본 20)

-- 반환 형식: 평탄화된 리스트
-- [bid_price1, bid_qty1, bid_price2, bid_qty2, ..., "ASKS", ask_price1, ask_qty1, ...]

local ticker = ARGV[1]
local max_depth = tonumber(ARGV[2] or "20")
if not max_depth or max_depth <= 0 then
  max_depth = 20
end

local result = {}
local bidPriceMap = {}
local askPriceMap = {}

-- ✅ 매수 호가 조회: unique 가격대가 max_depth개 찾을 때까지 순회
-- ZREVRANGE는 score가 큰 것부터 (높은 가격 우선)
local bidPriceCount = 0
local bidIndex = 0

-- Redis ZSET의 총 크기를 먼저 확인 (실제 존재하는 주문 개수)
local totalBidOrders = redis.call('ZCARD', KEYS[1])
-- 안전상 상한선: 실제 주문 개수의 2배 또는 10000 중 작은 값
-- (모든 주문이 같은 가격일 때를 대비한 여유)
local max_iterations = math.min(totalBidOrders * 2, 10000)

-- 루프 조건: 
-- 1. 아직 max_depth개 unique 가격대를 못 찾았고
-- 2. 조회 인덱스가 상한선 이하이고
-- 3. 실제 존재하는 주문 개수보다 작을 때
while bidPriceCount < max_depth and bidIndex < max_iterations and bidIndex < totalBidOrders do
  -- 한 번에 100개씩 배치로 조회 (성능 최적화)
  local batchStart = bidIndex
  local batchEnd = math.min(bidIndex + 99, totalBidOrders - 1) -- 범위 초과 방지
  local bidOrderIds = redis.call('ZREVRANGE', KEYS[1], batchStart, batchEnd)
  
  if not bidOrderIds or #bidOrderIds == 0 then
    break -- 더 이상 주문이 없음 (이론상 발생하지 않아야 하지만 방어 코드)
  end
  
  for i, orderId in ipairs(bidOrderIds) do
    bidIndex = bidIndex + 1
    
    if bidPriceCount >= max_depth then
      break -- 충분한 unique 가격대를 찾음
    end
    
    local detailKey = "orders:{" .. ticker .. "}:" .. orderId
    
    -- 주문 상세 정보 조회
    local quantity = redis.call('HGET', detailKey, 'quantity')
    local priceInt = redis.call('HGET', detailKey, 'priceInt')
    
    -- ✅ 수정: nil/false 체크를 명확하게 (Lua에서 false와 nil은 다름)
    if quantity == nil or quantity == false or priceInt == nil or priceInt == false then
      -- hash key가 없거나 필드가 없으면 ZSET에서 제거 (불일치 데이터 정리)
      redis.call('ZREM', KEYS[1], orderId)
    else
      -- ✅ 문자열을 숫자로 변환 (Lua의 tonumber는 nil을 반환할 수 있음)
      local qty = tonumber(tostring(quantity))
      local price = tonumber(tostring(priceInt))
      
      -- ✅ 수정: tonumber가 nil을 반환할 수 있으므로 명확하게 체크
      if qty ~= nil and price ~= nil and qty > 0 and price > 0 then
        -- 같은 가격의 호가를 그룹핑하여 수량 합산
        if bidPriceMap[price] then
          bidPriceMap[price] = bidPriceMap[price] + qty
        else
          bidPriceMap[price] = qty
          bidPriceCount = bidPriceCount + 1 -- 새로운 가격대 발견
        end
      end
    end
  end
  
  -- 배치가 요청한 개수(100개)보다 적으면 더 이상 조회할 데이터가 없음
  if #bidOrderIds < 100 then
    break
  end
end

-- ✅ 매도 호가 조회: unique 가격대가 max_depth개 찾을 때까지 순회
-- ZRANGE는 score가 작은 것부터 (낮은 가격 우선)
local askPriceCount = 0
local askIndex = 0

-- Redis ZSET의 총 크기를 먼저 확인 (실제 존재하는 주문 개수)
local totalAskOrders = redis.call('ZCARD', KEYS[2])
-- 안전상 상한선: 실제 주문 개수의 2배 또는 10000 중 작은 값
-- (모든 주문이 같은 가격일 때를 대비한 여유)
local max_ask_iterations = math.min(totalAskOrders * 2, 10000)

-- 루프 조건: 
-- 1. 아직 max_depth개 unique 가격대를 못 찾았고
-- 2. 조회 인덱스가 상한선 이하이고
-- 3. 실제 존재하는 주문 개수보다 작을 때
while askPriceCount < max_depth and askIndex < max_ask_iterations and askIndex < totalAskOrders do
  -- 한 번에 100개씩 배치로 조회 (성능 최적화)
  local batchStart = askIndex
  local batchEnd = math.min(askIndex + 99, totalAskOrders - 1) -- 범위 초과 방지
  local askOrderIds = redis.call('ZRANGE', KEYS[2], batchStart, batchEnd)
  
  if not askOrderIds or #askOrderIds == 0 then
    break -- 더 이상 주문이 없음 (이론상 발생하지 않아야 하지만 방어 코드)
  end
  
  for i, orderId in ipairs(askOrderIds) do
    askIndex = askIndex + 1
    
    if askPriceCount >= max_depth then
      break -- 충분한 unique 가격대를 찾음
    end
    
    local detailKey = "orders:{" .. ticker .. "}:" .. orderId
    
    -- 주문 상세 정보 조회
    local quantity = redis.call('HGET', detailKey, 'quantity')
    local priceInt = redis.call('HGET', detailKey, 'priceInt')
    
    -- ✅ 수정: nil/false 체크를 명확하게 (Lua에서 false와 nil은 다름)
    if quantity == nil or quantity == false or priceInt == nil or priceInt == false then
      -- hash key가 없거나 필드가 없으면 ZSET에서 제거 (불일치 데이터 정리)
      redis.call('ZREM', KEYS[2], orderId)
    else
      -- ✅ 문자열을 숫자로 변환 (Lua의 tonumber는 nil을 반환할 수 있음)
      local qty = tonumber(tostring(quantity))
      local price = tonumber(tostring(priceInt))
      
      -- ✅ 수정: tonumber가 nil을 반환할 수 있으므로 명확하게 체크
      if qty ~= nil and price ~= nil and qty > 0 and price > 0 then
        -- 같은 가격의 호가를 그룹핑하여 수량 합산
        if askPriceMap[price] then
          askPriceMap[price] = askPriceMap[price] + qty
        else
          askPriceMap[price] = qty
          askPriceCount = askPriceCount + 1 -- 새로운 가격대 발견
        end
      end
    end
  end
  
  -- 배치가 요청한 개수(100개)보다 적으면 더 이상 조회할 데이터가 없음
  if #askOrderIds < 100 then
    break
  end
end

-- BID 정렬 (가격 높은 순)
local bidPrices = {}
for price, qty in pairs(bidPriceMap) do
  table.insert(bidPrices, {price = price, quantity = qty})
end
table.sort(bidPrices, function(a, b) return a.price > b.price end)

-- ASK 정렬 (가격 낮은 순)
local askPrices = {}
for price, qty in pairs(askPriceMap) do
  table.insert(askPrices, {price = price, quantity = qty})
end
table.sort(askPrices, function(a, b) return a.price < b.price end)

-- 결과 평탄화: [bid_price1, bid_qty1, bid_price2, bid_qty2, ..., "ASKS", ask_price1, ask_qty1, ...]
for i, entry in ipairs(bidPrices) do
  if i <= max_depth then
    table.insert(result, tostring(entry.price))
    table.insert(result, tostring(entry.quantity))
  end
end

-- 구분자 추가
table.insert(result, "ASKS")

for i, entry in ipairs(askPrices) do
  if i <= max_depth then
    table.insert(result, tostring(entry.price))
    table.insert(result, tostring(entry.quantity))
  end
end

return result

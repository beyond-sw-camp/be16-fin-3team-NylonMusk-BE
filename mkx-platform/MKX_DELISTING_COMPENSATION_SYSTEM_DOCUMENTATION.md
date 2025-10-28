# 🚀 MKX 상장폐지 보상금 지급 시스템 - 완전 구현 문서

## 📋 개요

MKX 플랫폼의 상장폐지 시 주주 보상금을 실제 금융 환경과 유사한 3단계 방식으로 지급하는 시스템입니다.

## 🎯 시스템 아키텍처

### 핵심 컴포넌트

- **mkx-platform**: 상장폐지 관리 및 보상금 처리
- **ordering**: 주식 보유자 정보 및 회원 계좌 관리
- **marketdata**: 현재 주가 정보 제공
- **FeignClient**: MSA 간 통신

## 📋 초기 제공 상장폐지 기준

애플리케이션 시작 시 `InitialDelistingCriteriaLoader`가 자동으로 다음 기준들을 생성합니다.

### 💰 재무 기준 (10개)

| 기준 코드                   | 기준명                        | 기준값        | 단위  | 비교연산자   | 설명                                        |
| --------------------------- | ----------------------------- | ------------- | ----- | ------------ | ------------------------------------------- |
| `LOW_REVENUE_2Y`            | 2년간 평균 매출액 50억원 미만 | 5,000,000,000 | YEARS | LESS_THAN    | 최근 2년간 평균 매출액이 50억원 미만인 경우 |
| `NEGATIVE_NET_INCOME`       | 순이익 마이너스               | 0             | -     | LESS_THAN    | 최근 사업연도 순이익이 적자인 경우          |
| `LOW_EQUITY_RATIO`          | 자기자본비율 20% 미만         | 20.0          | -     | LESS_THAN    | 자기자본비율이 20% 미만인 경우              |
| `HIGH_DEBT_RATIO`           | 부채비율 200% 초과            | 200.0         | -     | GREATER_THAN | 부채비율이 200% 초과인 경우                 |
| `LOW_CURRENT_RATIO`         | 유동비율 1.0 미만             | 1.0           | -     | LESS_THAN    | 유동비율이 1.0 미만인 경우                  |
| `INSUFFICIENT_CAPITAL`      | 자본금 10억원 미만            | 1,000,000,000 | -     | LESS_THAN    | 자본금이 10억원 미만인 경우                 |
| `LOW_ROE`                   | ROE 5% 미만                   | 5.0           | -     | LESS_THAN    | 자기자본이익률(ROE)이 5% 미만인 경우        |
| `LOW_ROA`                   | ROA 3% 미만                   | 3.0           | -     | LESS_THAN    | 총자산이익률(ROA)이 3% 미만인 경우          |
| `NEGATIVE_OPERATING_INCOME` | 영업이익 마이너스             | 0             | -     | LESS_THAN    | 최근 사업연도 영업이익이 적자인 경우        |
| `LOW_INTEREST_COVERAGE`     | 이자보상배수 1.0 미만         | 1.0           | -     | LESS_THAN    | 이자보상배수가 1.0 미만인 경우              |

### 📈 거래 기준 (4개)

| 기준 코드                   | 기준명                       | 기준값 | 단위     | 비교연산자 | 설명                                                   |
| --------------------------- | ---------------------------- | ------ | -------- | ---------- | ------------------------------------------------------ |
| `LOW_TRADING_VOLUME_2Q`     | 거래량 부족 2분기 연속       | -      | QUARTERS | LESS_THAN  | 분기 월 평균 거래량이 유동주식 수의 1% 미만 2분기 연속 |
| `LOW_STOCK_PRICE_30D`       | 30일 평균 주가 1000원 미만   | 1,000  | DAYS     | LESS_THAN  | 30일 평균 주가가 1000원 미만인 경우                    |
| `LOW_MARKET_CAP_CONTINUOUS` | 저시가총액 지속              | -      | -        | LESS_THAN  | 관리종목 지정 후 시가총액 부족 상태 지속               |
| `LOW_MINORITY_SHAREHOLDERS` | 소액주주 200인 미만 2년 연속 | 200    | YEARS    | LESS_THAN  | 소액주주 수 200인 미만(또는 지분 20% 미만) 2년 연속    |

### ⚖️ 법규 기준 (7개)

| 기준 코드                  | 기준명                     | 기준값 | 단위 | 비교연산자   | 설명                                         |
| -------------------------- | -------------------------- | ------ | ---- | ------------ | -------------------------------------------- |
| `AUDIT_OPINION_ADVERSE`    | 감사 의견 부적정           | -      | -    | EQUAL        | 감사 의견이 부적정인 경우                    |
| `AUDIT_OPINION_QUALIFIED`  | 감사 의견 한정             | -      | -    | EQUAL        | 감사 의견이 한정인 경우                      |
| `AUDIT_OPINION_DISCLAIMER` | 감사 의견 거절             | -      | -    | EQUAL        | 감사 의견이 거절인 경우                      |
| `REPORT_DELAY`             | 보고서 제출 지연 10일 초과 | 10     | DAYS | GREATER_THAN | 분기별 사업보고서 제출 기한 후 10일내 미제출 |
| `FINAL_BANKRUPTCY`         | 최종 부도 발생             | -      | -    | EQUAL        | 최종 부도 발생 시                            |
| `EMBEZZLEMENT`             | 횡령 발생                  | -      | -    | EQUAL        | 횡령 발생 시                                 |
| `BREACH_OF_TRUST`          | 배임 발생                  | -      | -    | EQUAL        | 배임 발생 시                                 |

### 🔧 기준 설정 정보

#### 기준 유형 (CriteriaType)

- **FINANCIAL**: 재무 기준 (매출액, 수익성, 안정성 등)
- **TRADING**: 거래 기준 (거래량, 주가, 시가총액 등)
- **REGULATORY**: 법규 기준 (감사 의견, 보고서 제출, 법적 사건 등)

#### 기준 단위 (ThresholdUnit)

- **DAYS**: 일
- **MONTHS**: 월
- **QUARTERS**: 분기
- **YEARS**: 년

#### 비교 연산자 (ComparisonOperator)

- **LESS_THAN**: 미만 (<)
- **LESS_THAN_OR_EQUAL**: 이하 (≤)
- **GREATER_THAN**: 초과 (>)
- **GREATER_THAN_OR_EQUAL**: 이상 (≥)
- **EQUAL**: 동일 (=)
- **NOT_EQUAL**: 상이 (≠)

### 📊 자동 감지 로직

#### 재무 데이터 기반 자동 감지

- **CompanyFinancials**: 매출액, 영업이익, 순이익, 총자산, 자기자본
- **FinancialRatios**: 부채비율, 유동비율, ROE, ROA, 이자보상배수
- **Corporation**: 자본금, 최근 연매출

#### 감지 방식

1. **분기별 자동 스케줄러**: 매 분기 말 자동 실행
2. **실시간 감지**: 재무제표 업데이트 시 즉시 감지
3. **수동 감지**: 관리자가 필요시 수동 실행

## 💰 3단계 보상금 지급 시스템

### 1단계: 기업 계좌 현금 지급

```
기업 계좌 잔액 ≥ 총 보상금 → 정상 지급
```

**처리 과정:**

1. 기업 계좌 잔액 확인
2. 기업 계좌에서 출금 (`CorporationAccount.withdraw()`)
3. 각 주주 계좌로 입금 (`MemberAccountClient.deposit()`)
4. 보상금 상태 → `COMPLETED`

### 2단계: 유동자산 활용 지급

```
기업 계좌 잔액 < 총 보상금 → 유동자산 활용
```

**처리 과정:**

1. 최신 재무제표에서 유동자산 조회
2. 유동자산의 70% 활용 (30%는 운영자금으로 보존)
3. 현금 + 유동자산으로 충분하면 → 완전 지급
4. 부족하면 → 비례 부분 지급 (`PARTIAL_PAID`)

**재무제표 업데이트:**

- `CompanyFinancials.currentAssets` 차감
- 기업 계좌 현금 출금

### 3단계: 파산 처리 후 거래소 지급

```
유동자산으로도 부족 → 파산 처리 후 거래소 지급
```

**처리 과정:**

1. 기업 파산 처리 (`Corporation.status = DELISTED`)
2. 거래소 운영 계좌에서 미지급분 지급
3. **거래소 지원금 기록 생성** (`ExchangeSupportFund`)
   - 지원금 유형: `COMPENSATION_LOAN` (보상금 대출)
   - 상환 예정일: 3년 후
   - 이자율: 연 5%
   - 상태: `ACTIVE` (활성 대출)
4. 거래소 계좌도 부족하면 → 최종 실패

## 📊 보상금 상태 관리

### 상태 정의

```java
public enum CompensationStatus {
    PENDING("대기"),           // 보상금 생성됨
    PROCESSING("처리중"),       // 보상금 처리 중
    COMPLETED("완료"),         // 보상금 지급 완료
    PARTIAL_PAID("부분지급"),   // 보상금 부분 지급 (새로 추가)
    FAILED("실패"),           // 보상금 지급 실패
    CANCELLED("취소")         // 보상금 취소됨
}
```

### 상태 전이

```
PENDING → PROCESSING → COMPLETED
                    → PARTIAL_PAID → COMPLETED (3단계에서)
                    → FAILED
```

## 🏦 거래소 지원금 관리 시스템

### 지원금 기록 (`ExchangeSupportFund`)

거래소에서 지급한 지원금은 **대출**로 기록되어 관리됩니다.

#### 지원금 유형

```java
public enum SupportType {
    COMPENSATION_LOAN("보상금 대출"),      // 상장폐지 보상금 지원
    OPERATING_LOAN("운영자금 대출"),      // 기업 운영자금 지원
    EMERGENCY_LOAN("긴급자금 대출"),      // 긴급 상황 지원
    GUARANTEE("보증"),                   // 보증 제공
    DIRECT_SUPPORT("직접 지원");         // 직접 지원금
}
```

#### 지원금 상태

```java
public enum SupportStatus {
    ACTIVE("활성"),           // 대출 중
    REPAID("상환완료"),       // 완전 상환
    PARTIAL_REPAID("부분상환"), // 부분 상환
    OVERDUE("연체"),         // 연체
    WRITTEN_OFF("대손처리"),  // 대손 처리
    CANCELLED("취소");       // 취소
}
```

### 지원금 관리 기능

#### 1. 상환 처리

- 부분 상환 및 완전 상환 지원
- 잔여 금액 자동 계산
- 상태 자동 업데이트

#### 2. 연체 관리

- 상환 예정일 기반 연체 자동 감지
- 연체 상태 자동 변경

#### 3. 대손 처리

- 회수 불가능한 대출 대손 처리
- 손실 인정 및 정리

### API 엔드포인트

```
GET    /api/delisting/support-fund                    # 모든 지원금 조회
GET    /api/delisting/support-fund/stock/{stockId}    # 주식별 지원금 조회
GET    /api/delisting/support-fund/corporation/{id}   # 기업별 지원금 조회
GET    /api/delisting/support-fund/overdue            # 연체된 지원금 조회
GET    /api/delisting/support-fund/total              # 거래소 총 지원금 합계
GET    /api/delisting/support-fund/total-unpaid       # 거래소 총 미상환 금액

POST   /api/delisting/support-fund/{id}/repay         # 상환 처리
POST   /api/delisting/support-fund/{id}/mark-overdue  # 연체 처리
POST   /api/delisting/support-fund/{id}/write-off     # 대손 처리
```

## 🔄 자동 재처리 시스템

### 수동 재처리

```java
@Transactional
public void retryFailedCompensations(UUID stockId)
```

- 관리자가 수동으로 호출
- 실패한 보상금 기록 조회 후 재처리

### 자동 재처리 스케줄러

```java
@Scheduled(fixedRate = 600000) // 10분마다 실행
public void scheduledRetryFailedCompensations()
```

- **10분마다 자동 실행**
- 모든 실패한 보상금 자동 재처리
- 주식별 그룹화로 중복 처리 방지

## 🏗️ 구현된 주요 클래스

### 1. 엔티티

- `DelistingCompensation`: 보상금 정보
- `CompensationStatus`: 보상금 상태 (PARTIAL_PAID 추가)
- `CorporationAccount`: 기업 계좌
- `ExchangeAccount`: 거래소 운영 계좌

### 2. 서비스

- `DelistingService`: 핵심 비즈니스 로직
  - `createCompensations()`: 보상금 생성 및 지급
  - `processCompensationPayment()`: 3단계 지급 처리
  - `processPaymentFromCash()`: 1단계 현금 지급
  - `processPaymentFromCurrentAssets()`: 2단계 유동자산 활용
  - `processPaymentFromExchange()`: 3단계 거래소 지급
  - `retryFailedCompensations()`: 수동 재처리
  - `scheduledRetryFailedCompensations()`: 자동 재처리

### 3. FeignClient

- `MemberAccountClient`: 회원 계좌 입금
- `StockHoldingClient`: 주식 보유자 조회
- `CurrentPriceClient`: 현재 주가 조회

### 4. Repository

- `ExchangeAccountRepository`: 거래소 운영 계좌 관리
- `CorporationAccountRepository`: 기업 계좌 관리
- `DelistingCompensationRepository`: 보상금 관리

## 🔧 API 엔드포인트

### 상장폐지 실행

```
POST /api/delisting/execute
{
  "stockId": "uuid",
  "reason": "상장폐지 사유"
}
```

### 보상금 재처리

```
POST /api/delisting/retry-compensation
{
  "stockId": "uuid"
}
```

## 📈 실제 처리 시나리오

### 시나리오 1: 정상 지급

```
총 보상금: 10억원
기업 계좌 잔액: 15억원
→ 1단계: 현금으로 완전 지급 ✅
```

### 시나리오 2: 유동자산 활용

```
총 보상금: 10억원
기업 계좌 잔액: 3억원
유동자산: 12억원 (사용가능: 8.4억원)
→ 2단계: 현금 3억 + 유동자산 7억 = 완전 지급 ✅
```

### 시나리오 3: 부분 지급 + 거래소 지원

```
총 보상금: 10억원
기업 계좌 잔액: 2억원
유동자산: 8억원 (사용가능: 5.6억원)
→ 2단계: 현금 2억 + 유동자산 5.6억 = 7.6억원 부분 지급
→ 3단계: 거래소에서 2.4억원 추가 지급
→ 거래소 지원금 기록: 2.4억원 (3년 후 상환, 연 5% 이자) ✅
```

### 시나리오 4: 최종 실패

```
총 보상금: 10억원
기업 계좌 잔액: 1억원
유동자산: 2억원 (사용가능: 1.4억원)
거래소 운영 계좌: 1억원
→ 1+2+3단계 모두 부족 → 최종 실패 ❌
```

## 🛡️ 안전장치 및 예외처리

### 1. 타입 안전성

- `BigDecimal` 사용으로 정밀한 금액 계산
- `BigInteger`와 `BigDecimal` 간 적절한 변환

### 2. 트랜잭션 관리

- `@Transactional`로 데이터 일관성 보장
- 실패 시 롤백 처리

### 3. 예외 처리

- FeignClient 호출 실패 시 대체 로직
- 계좌 잔액 부족 시 적절한 예외 발생
- 상세한 로그 기록으로 추적 가능

### 4. 멱등성 보장

- 재처리 시 중복 지급 방지
- 상태 기반 처리로 안전성 확보

## 📊 모니터링 및 로깅

### 로그 레벨

- `INFO`: 정상 처리 과정
- `WARN`: 부분 지급, 자산 부족
- `ERROR`: 처리 실패, 예외 상황

### 이력 관리

- `DelistingHistory`로 모든 처리 과정 기록
- `ActionType.COMPENSATION_FAILED`로 실패 추적
- 재처리 시도 기록으로 관리 가능

## 🚀 실제 금융 환경 적용 가능성

### 장점

1. **현실적인 처리 방식**: 실제 증권거래소와 유사한 3단계 구조
2. **운영자금 보존**: 유동자산의 30% 보존으로 기업 운영 보장
3. **자동화**: 실패 시 자동 재처리로 관리 부담 최소화
4. **투명성**: 모든 처리 과정의 상세한 이력 관리
5. **확장성**: MSA 구조로 각 서비스 독립적 확장 가능

### 개선 가능 영역

1. **자산 매각 시스템**: 부동산, 장비 등 비유동자산 매각 로직
2. **보험 처리**: 상장폐지 보험 등 추가 보상 수단
3. **법적 절차**: 파산 신청, 법원 승인 등 법적 프로세스
4. **국제 표준**: IFRS, K-IFRS 등 국제 회계 기준 적용

## 📝 테스트 가이드

### Postman 컬렉션

- `MKX_Delisting_Auto_System_Collection.postman_collection.json`
- `MKX_Delisting_Auto_Environment.postman_environment.json`

### 테스트 시나리오

1. **기준 관리**: 상장폐지 기준 생성/조회/수정
2. **위반 감지**: 자동/수동 위반 감지
3. **상장폐지 실행**: 전체 프로세스 테스트
4. **보상금 지급**: 3단계 지급 시스템 테스트
5. **재처리**: 실패한 보상금 재처리 테스트

## 🎯 결론

이 시스템은 실제 금융 환경에서 사용할 수 있는 수준의 상장폐지 보상금 지급 시스템을 구현했습니다. 3단계 지급 방식, 자동 재처리, 상세한 이력 관리 등을 통해 투자자 보호와 시스템 안정성을 동시에 확보했습니다.

---

![증권사 관리자-고객 관리 페이지](https://github.com/user-attachments/assets/6d059ae8-7f02-4941-ac6d-93d9727db6b2)<body>
<main class="container">
  <h1>MKX — 기업을 위한 증권 공급망 거래소</h1>
 <img width="1200" height="710" alt="MKX 리드미 배너" src="https://github.com/user-attachments/assets/5a5f48d4-dae1-4e99-aaf4-15a9c1d54028" />
  <div class="muted">프로젝트 기간: <strong>2025.09.12</strong> ~ <strong>2025.11.12</strong> · 작성자: 김진호 · 김형진 · 박혜성 · 이우영 · 윤세진</div>
  <div style="margin-top:10px">
    <span class="badge">B2B 전용</span>
    <span class="badge">비상장/상장/유상증자</span>
    <span class="badge">다크풀(기관)</span>
    <span class="badge">MFA · 감사로그</span>
  </div>

  <hr/>

  <div class="card">
    <h2>소개</h2>
    <p><strong>MKX</strong>는 <em>기업을 위한 증권 공급망 관리(SSOM: Securities Supply–Order Management)</em>를 핵심 컨셉으로 하는 B2B 거래소입니다. 
      기업은 상장(공급 등록) → 발행/유상증자(공급 확장) → 거래/정산 → 공시/주주총회(거버넌스) → 상장폐지/관리까지 <strong>end-to-end</strong>로 처리할 수 있습니다.
      우리는 일반 리테일 브로커가 아닌, <strong>거래소 그 자체</strong>입니다. <strong>법인 전용</strong> 거래/관리 흐름에 집중합니다.</p>
    <ul class="list-tight">
      <li><strong>B2B 전용:</strong> 법인 계정, KYB, 전 과정 MFA/감사.</li>
      <li><strong>공급망형 주문관리:</strong> 상장·발행·유상증자·락업·거래 규칙을 공급/수요 데이터로 일관 관리.</li>
      <li><strong>비상장/다크풀:</strong> 기관 간 비상장 블록딜, 다크풀 매칭(공매도 정책 범위 내 시뮬레이션 포함).</li>
      <li><strong>거버넌스 내장:</strong> 온라인 주주총회(전자위임/의결/의사록), 기업 공지/알림.</li>
    </ul>
  </div>

  <h2 id="toc">목차</h2>
  <div class="toc">
  <a href="#value">1. 문제정의 & 가치제안</a><br />
  <a href="#scope">2. 범위(Out of Scope 포함)</a><br />
  <a href="#features">3. 핵심 기능 요약</a><br />
  <a href="#order-supply">4. 공급망형 주문관리(SSOM) 흐름</a><br />
  <a href="#governance">5. 거버넌스(주총/공지)</a><br />
  <a href="#security">6. 보안·컴플라이언스</a><br />
  <a href="#demo">7. 발표 시나리오(데모 동선)</a><br />
  <a href="#demo-video">8. 기능 시연 영상</a><br />
  <a href="#evidence">9. 단위 테스트 결과서</a><br />
  <a href="#reqspec">10. 요구사항 명세(이미지·URL)</a><br />
  <a href="#wbs">11. WBS(이미지·URL)</a><br />
  <a href="#figma">12. 화면설계도(이미지·URL)</a><br />
  <a href="#erd">13. ERD(이미지·URL)</a><br />
  <a href="#team">14. 팀 소개</a><br />
  <a href="#timeline">15. 일정</a><br />
  <a href="#techstack">16. 기술 스택</a><br />
  <a href="#architecture">17. 시스템 아키텍처</a><br />
  <a href="#api">18. API 명세서</a><br />
  </div>

  <hr/>



  <section id="value">
        <h2>1) 문제정의 & 가치제안</h2>
  <p>
    기존 시장에서는 발행, 유상증자, 주주총회, 상장폐지와 같은 기업 중심의 프로세스가 실제 거래 시스템과 단절되어 단편적으로 운영됩니다. 이러한 구조는 정보의 비대칭성과 절차적 비효율을 초래하며, 투자자와 기업 모두에게 불투명한 경험을 제공합니다.
  </p>

  <p>
    MKX는 이와 달리, <strong>발행·공급에서 거래·정산, 나아가 거버넌스</strong>에 이르기까지 전 과정을 단일한 <strong>주문·공급망 데이터 모델</strong> 위에서 통합합니다. 이를 통해 각각 분리되어 있던 프로세스를 하나의 흐름으로 연결하고, 기업과 투자자 간의 정보 단절을 해소합니다.
  </p>

  <p>
    이러한 통합 모델은 거래 속도를 향상시키고, 절차 전반의 투명성을 강화하며, 데이터 기반의 감사 가능성을 본질적으로 내재화합니다. 기업은 복잡한 절차를 간소화할 수 있으며, 투자자는 명확한 기준과 기록을 기반으로 신뢰할 수 있는 참여가 가능합니다.
  </p>

  <p>
    결과적으로 MKX는 단순한 거래 플랫폼을 넘어, 기업의 자본 활동과 투자자의 의사결정이 실시간으로 유기적으로 맞물리는 새로운 생태계를 제공합니다. 이는 단순한 기술적 개선을 넘어, <strong>시장 운영 방식 자체를 혁신적으로 재편하는 핵심 동력</strong>입니다.
  </p>
</section>

  <section id="scope">
    <h2>2) 범위(Out of Scope 포함)</h2>
    <div class="grid">
      <div class="col6">
        <h3>In Scope</h3>
        <ul>
          <li>법인 회원가입·로그인(KYB, MFA, CI 중복 방지)</li>
          <li>상장 요청·심사(재무제표 PDF·필수값 입력/검증)</li>
          <li>발행/유상증자(신주인수권, 공모/3자배정)</li>
          <li>비상장/기관간 블록딜, 다크풀 매칭</li>
          <li>주주총회(전자위임/의결/정족수·가중치 판정·의사록)</li>
          <li>상장폐지/관리종목·거래정지 로직</li>
          <li>감사 로그, 관리자 RBAC, 공지/알림</li>
        </ul>
      </div>
      <div class="col6">
        <h3>Out of Scope</h3>
        <ul>
          <li>개인(B2C) 위주의 리테일 브로커 기능 전반</li>
          <li>개인 대상 투자교육/커뮤니티 추천 알고리즘</li>
          <li>암호자산 거래 기능</li>
        </ul>
      </div>
    </div>
  </section>

  <section id="features">
    <h2>3) 핵심 기능 요약</h2>
    <table>
      <thead><tr><th>영역</th><th>주요 기능</th></tr></thead>
      <tbody>
        <tr>
          <td><strong>관리자</strong></td>
          <td>RBAC(SUPER_ADMIN/ADMIN), MFA 상시, 계정 생성/비활성/권한변경, 전행위 감사로그</td>
        </tr>
        <tr>
          <td><strong>상장/발행</strong></td>
          <td>상장요청(필수값/재무제표 검증), 발행량/락업/거래규칙 관리, 상폐 심사/고지</td>
        </tr>
        <tr>
          <td><strong>거래</strong></td>
          <td>B2B 지정가/시장가/조건주문, 대기/부분체결, 기관간 블록딜/다크풀, 결제상태 반영</td>
        </tr>
        <tr>
          <td><strong>거버넌스</strong></td>
          <td>온라인 주주총회(전자위임·가중치·정족수·의사록), 기업 공지/실시간 알림</td>
        </tr>
        <tr>
          <td><strong>데이터</strong></td>
          <td>차트/보조지표, 기업 개요·재무, MCP 기반 기술적 분석</td>
        </tr>
        <tr>
          <td><strong>시뮬레이션</strong></td>
          <td>가상거래 봇(횡보/상승/하락/급등락), 시나리오 스케줄·파라미터 제어</td>
        </tr>
      </tbody>
    </table>
    <div class="callout"><strong>전체 요구사항 목록</strong>은 아래 “요구사항 명세” 섹션의 문서/이미지로 연결합니다.</div>
  </section>

  <section id="order-supply">
    <h2>4) 공급망형 주문관리(SSOM) 흐름</h2>
    <ol>
      <li><strong>공급 등록:</strong> 상장요청 → 심사(재무제표/지표/규정) → 상장 승인 → 종목 생성</li>
      <li><strong>공급 확장:</strong> 유상증자/발행량 변경 → 공시/신주인수권 → 주문규칙(시초가/단위/밴드)</li>
      <li><strong>수요 매칭:</strong> 기관 매수/매도, 블록딜/다크풀 라우팅, 조건주문</li>
      <li><strong>정산/잔고:</strong> 체결·수수료·가용현금/재고 반영, 결제상태 추적</li>
      <li><strong>거버넌스:</strong> 공지/주총/의사록/감사로그 및 상장폐지 프로세스</li>
    </ol>
  </section>

  <section id="governance">
    <h2>5) 거버넌스(주총/공지)</h2>
    <ul>
      <li>사전승인(안건/일시/스냅샷/마감), 참석 자격 매핑, 본인확인 및 1회용 초대토큰</li>
      <li>안건별 찬반/기권, 보유주식 가중치 집계, 정족수/가결 요건 자동판정</li>
      <li>의사록 전자서명, 결과 공시, 감사로그/CSV·PDF 내보내기</li>
      <li>기업 공지 등록 시 보유자 대상 실시간 알림</li>
    </ul>
  </section>

  <section id="security">
    <h2>6) 보안·컴플라이언스</h2>
    <ul>
      <li><strong>MFA 상시</strong>(패스키 우선, 미지원 시 OTP), 민감행위 전 추가 재인증</li>
      <li>세션 정책(비활성 타임아웃/절대만료/동시세션 제한/RT 로테이션)</li>
      <li><strong>감사 로그</strong>(행위자/대상/전후값/사유/시각), 관리자 고권한 작업 이중확인</li>
      <li>계좌 실명·중복가입 방지(CI/DI), 주민등록번호 원문 미저장</li>
    </ul>
  </section>

  <section id="demo">
    <h2>7) 발표 시나리오(데모 동선)</h2>
    <ol>
      <li><strong>기업 A 상장요청</strong> (필수값·재무제표 업로드) → <em>관리자 심사 승인</em></li>
      <li><strong>기관 블록딜</strong> (비상장→상장 직전 프리IPO 시나리오 or 상장 후 다크풀 라우팅)</li>
      <li><strong>유상증자 실행</strong> (조건 공시→신주인수권 처리→공급량 반영→거래 규칙 업데이트)</li>
      <li><strong>온라인 주총</strong> (전자위임/가중치·정족수 충족→의사록/공시 출력)</li>
      <li><strong>관리종목/상폐</strong> (조건 충족→거래정지→상폐 고지 7일→상폐 확정)</li>
    </ol>
  </section>


<section id="demo-video">
  <h2>8) 기능 시연 영상</h2>
  <p>MKX 프로젝트의 주요 기능 시연 영상을 정리합니다.</p>

  <!-- 회원가입 / 로그인 -->
<details>
<summary><b>① 회원가입 / 로그인</b></summary>

<details>
<summary><b>관리자 회원가입 / 로그인</b></summary>

<details>
<summary><b>기업 관리자 회원가입</b></summary>
<p align="center">
<img src="https://github.com/user-attachments/assets/5f42ae7b-a82f-4bb2-a1a4-49e77f9342dc" alt="기업 관리자 회원가입" width="#">
</p>
</details>

<details>
<summary><b>증권사 관리자 회원가입</b></summary>
<p align="center">
<img src="https://github.com/user-attachments/assets/1c8097b4-10d8-4c36-8252-fb579fdf5648" alt="증권사 관리자 회원가입" width="#">
</p>
</details>

<details>
<summary><b>관리자 로그인</b></summary>
<p align="center">
<img src="https://github.com/user-attachments/assets/1d6f40c3-0f40-4597-8d53-32a4600bd456" alt="관리자 로그인" width="#">
</p>
</details>
</details>

<details>
<summary><b>일반 유저 회원가입 / 로그인</b></summary>

<details>
<summary><b>일반 유저 회원가입</b></summary>
<p align="center">
<img src="https://github.com/user-attachments/assets/3464dc80-c240-4479-b6a6-92f895ff0044" alt="일반 유저 회원가입" width="#">
</p>
</details>

<details>
<summary><b>일반 유저 로그인</b></summary>
<p align="center">
<img src="https://github.com/user-attachments/assets/c429e45d-fd2b-4e82-8eef-b2e301ab8e49" alt="일반 유저 로그인" width="#">
</p>
</details>

<details>
<summary><b>일반 유저 캡챠 시연</b></summary>
<p align="center">
<img src="https://github.com/user-attachments/assets/e0a279f7-0ba8-4437-b4c8-69b0d3082114" alt="일반 유저 캡챠 시연" width="#">
</p>
</details>
</details>
</details>

<!-- 기업 관리자 페이지 -->
<details>
<summary><b>② 기업 관리자 페이지</b></summary>

<details>
<summary><b>공시 관리</b></summary>

<details>
<summary><b>공시 목록 페이지 및 필터</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/e1b66a3f-e77e-49f7-8f5c-8caa12746c7e" alt="공시 목록 페이지 및 필터" width="#"></p>
</details>

<details>
<summary><b>공시 등록 페이지</b></summary>

<details>
<summary><b>본공시</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/ddf11478-db2a-41e1-8d94-23ccd5f9f0a2" alt="본공시" width="#"></p>
</details>

<details>
<summary><b>정정공시</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/16859a87-65f2-45fc-af1e-a235bd658f52" alt="정정공시" width="#"></p>
</details>

<details>
<summary><b>추가등록</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/b26f95e7-226f-444a-98f7-9b1d216f4f14" alt="추가공시" width="#"></p>
</details>
</details>
</details>

<details>
<summary><b>상장 관리</b></summary>

<details>
<summary><b>상장 신청</b></summary>

<details>
<summary><b>공모</b></summary>
<p align="center"><img src="#" alt="공모 상장 신청" width="#"></p>
</details>

<details>
<summary><b>직상장</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/c5ce43b2-2577-4878-85fd-3b2a6bb34c3a" alt="직상장 신청" width="#"></p>
</details>
</details>

<details>
<summary><b>공모 등록</b></summary>
<p align="center"><img src="#" alt="공모 등록" width="#"></p>
</details>

<details>
<summary><b>공모 목록</b></summary>

<details>
<summary><b>공모 목록 필터</b></summary>
<p align="center"><img src="#" alt="공모 목록 필터" width="#"></p>
</details>

<details>
<summary><b>공모 목록 검색</b></summary>
<p align="center"><img src="#" alt="공모 목록 검색" width="#"></p>
</details>

<details>
<summary><b>공모 참여</b></summary>
<p align="center"><img src="#" alt="공모 참여" width="#"></p>
</details>
</details>

<details>
<summary><b>수요예측 참여하기</b></summary>

<details>
<summary><b>수요예측 목록</b></summary>
<p align="center"><img src="#" alt="수요예측 목록" width="#"></p>
</details>

<details>
<summary><b>수요예측 참여</b></summary>
<p align="center"><img src="#" alt="수요예측 참여" width="#"></p>
</details>
</details>

<details>
<summary><b>내 청약 내역</b></summary>

<details>
<summary><b>청약 내역 조회</b></summary>
<p align="center"><img src="#" alt="청약 내역 조회" width="#"></p>
</details>

<details>
<summary><b>추가 납입</b></summary>
<p align="center"><img src="#" alt="추가 납입" width="#"></p>
</details>

<details>
<summary><b>정산</b></summary>
<p align="center"><img src="#" alt="정산" width="#"></p>
</details>
</details>

<details>
<summary><b>상장 현황</b></summary>
<p align="center"><img src="#" alt="상장 현황 페이지" width="#"></p>
</details>

<details>
<summary><b>주식 관리</b></summary>

<details>
<summary><b>주식 관리 페이지</b></summary>
<p align="center"><img src="#" alt="주식 관리 페이지" width="#"></p>
</details>

<details>
<summary><b>공시 문제 발생 시 재제출</b></summary>
<p align="center"><img src="#" alt="공시 재제출" width="#"></p>
</details>
</details>
</details>

<details>
<summary><b>자산</b></summary>

<details>
<summary><b>계좌 등록 신청</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/f4985708-472e-4d42-94ce-2c6067077c5a" alt="계좌 등록 신청" width="#"></p>
</details>

<details>
<summary><b>입금</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/c7d60ce3-7e84-4d36-9937-f2b70967d023" alt="입금" width="#"></p>
</details>

<details>
<summary><b>출금</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/c8d7debc-fb07-4134-a8b2-43291c5e316e" alt="출금" width="#"></p>
</details>

<details>
<summary><b>거래내역 조회</b></summary>
  <p align="center"><img src="https://github.com/user-attachments/assets/a264bd19-e4a0-4ec6-a6d7-e988ee8cdba2" alt="거래내역 조회" width="#"></p>
</details>
</details>
</details>

<!-- 증권사 관리자 페이지 -->
<details>
<summary><b>③ 증권사 관리자 페이지</b></summary>

<details>
<summary><b>대시보드</b></summary>

<details>
<summary><b>대시보드 페이지</b></summary>
<p align="center"><img src="#" alt="대시보드 페이지" width="#"></p>
</details>

<details>
<summary><b>실시간 고객 활동 자세히 보기 모달</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/2828caf8-4c30-4cb3-a5b9-b16ffd8f5ed1" alt="실시간 고객 활동 모달" width="#"></p>
</details>
</details>

<details>
<summary><b>고객 관리 페이지</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/8734df66-a3b8-4e07-93fa-8ae0503b55dd" alt="실시간 고객 활동 모달" width="#"></p>
</details>

<details>
<summary><b>거래 내역 페이지</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/43be5fc1-f3f3-4304-9299-bcd07a4075d5" alt="거래 내역 조회" width="#"></p>
</details>

</details>
</details>

<details>
<summary><b>수익 관리</b></summary>

<details>
<summary><b>수수료 관리 페이지</b></summary>
<p align="center"><img src="#" alt="수수료 관리 페이지" width="#"></p>
</details>

<details>
<summary><b>수수료 설정</b></summary>
<p align="center"><img src="#" alt="수수료 설정" width="#"></p>
</details>
</details>

<details>
<summary><b>자금 관리</b></summary>

<details>
<summary><b>입출금</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/3de56f76-1344-4024-92ca-5ab1de2d2d2b" alt="입금" width="#"></p>
</details>

<details>
<summary><b>거래내역</b></summary>

<details>
<summary><b>거래내역 조회</b></summary>
<p align="center"><img src="#" alt="거래내역 조회" width="#"></p>
</details>

<details>
<summary><b>거래 내역 필터</b></summary>
<p align="center"><img src="#" alt="거래 내역 필터" width="#"></p>
</details>

<details>
<summary><b>거래 내역 날짜 검색</b></summary>
<p align="center"><img src="#" alt="거래 내역 날짜 검색" width="#"></p>
</details>
</details>
</details>
</details>

<!-- 거래소 관리자 페이지 -->
<details>
<summary><b>④ 거래소 관리자 페이지</b></summary>

<details>
<summary><b>대시보드</b></summary>

<details>
<summary><b>대시보드 페이지</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/842737a9-11fc-402a-a1bf-d80a92a21b0e" alt="대시보드 페이지" width="#"></p>
</details>
</details>

<details>
<summary><b>기업 관리</b></summary>

<details>
<summary><b>기업 관리 페이지</b></summary>
<p align="center"><img src="#" alt="기업 관리 페이지" width="#"></p>
</details>

<details>
<summary><b>기업 등록 승인</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/c2658d77-aed2-47b0-8b5d-d1ddec288992" alt="기업 등록 승인" width="#"></p>
</details>

<details>
<summary><b>기업 등록 거절</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/2bf63c5c-3df9-45e6-9884-804e93593eb6" alt="기업 등록 거절" width="#"></p>
</details>

<details>
<summary><b>기업 가입 필터전체</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/5c8548fb-9cba-404c-879f-41666ca915ac" alt="기업 가입 필터전체" width="#"></p>
</details>

<details>
<summary><b>기업 상태별 필터</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/10b995e5-a32f-44fc-bf0f-0da74189d0bd" alt="기업 상태별 필터" width="#"></p>
</details>

<details>
<summary><b>기업 가입 상장폐지 화면</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/dd8cbd07-3064-4715-b87d-2ac69fea0e4f" alt="기업 가입 상장폐지 화면" width="#"></p>
</details>

<details>
<summary><b>기업 가입 거절필터 화면</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/4f253115-5586-4406-84d9-5e9268a70ffd" alt="기업 가입 거절필터 화면" width="#"></p>
</details>
</details>

<details>
<summary><b>증권사 관리</b></summary>

<details>
<summary><b>증권사 관리 페이지</b></summary>
<p align="center"><img src="#" alt="증권사 관리 페이지" width="#"></p>
</details>

<details>
<summary><b>증권사 등록 승인</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/96165c3b-7618-4c7a-8880-2c7a721a570e" alt="증권사 등록 승인" width="#"></p>
</details>

<details>
<summary><b>증권사 등록 거절</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/67b3c1d2-a411-4b37-8f79-0f353a0339aa" alt="증권사 등록 거절" width="#"></p>
</details>

<details>
<summary><b>증권사 검색</b></summary>
<p align="center"><img src="#" alt="증권사 검색" width="#"></p>
</details>

<details>
<summary><b>증권사 상태별 필터</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/1463cc14-2c2b-4ae8-82f1-5320a6e2835d" alt="증권사 상태별 필터" width="#"></p>
</details>

<details>
<summary><b>증권사 상세 조회 사이드</b></summary>
<p align="center"><img src="#" alt="증권사 상세 조회 사이드" width="#"></p>
</details>
</details>

<details>
<summary><b>사용자 관리</b></summary>

<details>
<summary><b>사용자 관리 페이지</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/eb805ef9-cff5-43b0-ad97-eca9a5430861" alt="사용자 관리 페이지" width="#"></p>
</details>


<details>
<summary><b>사용자 상세 조회 모달</b></summary>
<p align="center"><img src="#" alt="사용자 상세 조회 모달" width="#"></p>
</details>

<details>
<summary><b>사용자 계정 정지</b></summary>
<p align="center"><img src="#" alt="사용자 계정 정지" width="#"></p>
</details>
</details>

<details>
<summary><b>계좌 관리</b></summary>

<details>
<summary><b>계좌 관리 페이지</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/0b5f17cc-0c3e-4e37-957a-6d8463021ebc" alt="계좌 관리 페이지" width="#"></p>
</details>

<details>
<summary><b>기업 계좌 관리 페이지</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/ac1e0537-961f-4418-a99a-d5e700a4a26b" alt="계좌 관리 페이지" width="#"></p>
</details>

<details>
<summary><b>증권사 계좌 관리 페이지</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/494b208f-6895-4a66-ac59-0473a1b189df" alt="계좌 관리 페이지" width="#"></p>
</details>


<details>
<summary><b>계좌 등록 승인</b></summary>
<p align="center"><img src="#" alt="계좌 등록 승인" width="#"></p>
</details>

<details>
<summary><b>계좌 등록 거절</b></summary>
<p align="center"><img src="#" alt="계좌 등록 거절" width="#"></p>
</details>

<details>
<summary><b>계좌 상태별 필터</b></summary>
<p align="center"><img src="#" alt="계좌 상태별 필터" width="#"></p>
</details>

<details>
<summary><b>계좌 검색</b></summary>
<p align="center"><img src="#" alt="계좌 검색" width="#"></p>
</details>
</details>

<details>
<summary><b>IPO 관리</b></summary>

<details>
<summary><b>IPO 관리 페이지</b></summary>

<details>
<summary><b>IPO 요청 승인</b></summary>
<p align="center"><img src="#" alt="IPO 요청 승인" width="#"></p>
</details>

<details>
<summary><b>IPO 요청 거절</b></summary>
<p align="center"><img src="#" alt="IPO 요청 거절" width="#"></p>
</details>

<details>
<summary><b>IPO 상태별 필터</b></summary>
<p align="center"><img src="#" alt="IPO 상태별 필터" width="#"></p>
</details>

<details>
<summary><b>IPO 검색</b></summary>
<p align="center"><img src="#" alt="IPO 검색" width="#"></p>
</details>
</details>

<details>
<summary><b>공모 요청 목록 페이지</b></summary>

<details>
<summary><b>공모 요청 승인</b></summary>
<p align="center"><img src="#" alt="공모 요청 승인" width="#"></p>
</details>

<details>
<summary><b>공모 요청 거절</b></summary>
<p align="center"><img src="#" alt="공모 요청 거절" width="#"></p>
</details>

<details>
<summary><b>공모 상태별 필터</b></summary>
<p align="center"><img src="#" alt="공모 상태별 필터" width="#"></p>
</details>

<details>
<summary><b>공모 검색</b></summary>
<p align="center"><img src="#" alt="공모 검색" width="#"></p>
</details>

<details>
<summary><b>공모 배정 심사 승인</b></summary>
<p align="center"><img src="#" alt="공모 배정 심사 승인" width="#"></p>
</details>

<details>
<summary><b>공모 배정 확정</b></summary>
<p align="center"><img src="#" alt="공모 배정 확정" width="#"></p>
</details>
</details>
</details>

<details>
<summary><b>공시 승인</b></summary>

<details>
<summary><b>공시 승인 페이지</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/818a7b5b-ad5e-4d0e-9d91-e9faee838e1a" alt="공시 승인 페이지" width="#"></p>
</details>


<details>
<summary><b>공시 거절</b></summary>

<details>
<summary><b>사유 작성</b></summary>
<p align="center"><img src="#" alt="공시 거절 사유 작성" width="#"></p>
</details>
</details>

<details>
<summary><b>공시 상태별 필터</b></summary>
<p align="center"><img src="#" alt="공시 상태별 필터" width="#"></p>
</details>

<details>
<summary><b>공시 검색</b></summary>
<p align="center"><img src="#" alt="공시 검색" width="#"></p>
</details>
</details>

<details>
<summary><b>상장 주식 관리</b></summary>

<details>
<summary><b>통합관리 페이지</b></summary>

<details>
<summary><b>주식 상태별 필터</b></summary>
<p align="center"><img src="#" alt="주식 상태별 필터" width="#"></p>
</details>

<details>
<summary><b>주식 검색</b></summary>
<p align="center"><img src="#" alt="주식 검색" width="#"></p>
</details>
</details>

<details>
<summary><b>폐지 위험 관리 페이지</b></summary>

<details>
<summary><b>폐지 위험 상태별 필터</b></summary>
<p align="center"><img src="#" alt="폐지 위험 상태별 필터" width="#"></p>
</details>

<details>
<summary><b>폐지 위험 검색</b></summary>
<p align="center"><img src="#" alt="폐지 위험 검색" width="#"></p>
</details>

<details>
<summary><b>폐지 위험 상세 모달</b></summary>
<p align="center"><img src="#" alt="폐지 위험 상세 모달" width="#"></p>
</details>

<details>
<summary><b>폐지 예고 발행</b></summary>
<p align="center"><img src="#" alt="폐지 예고 발행" width="#"></p>
</details>
</details>

<details>
<summary><b>폐지 진행 상황 관리 페이지</b></summary>

<details>
<summary><b>폐지 진행 상황 상태별 필터</b></summary>
<p align="center"><img src="#" alt="폐지 진행 상황 상태별 필터" width="#"></p>
</details>

<details>
<summary><b>폐지 진행 검색</b></summary>
<p align="center"><img src="#" alt="폐지 진행 검색" width="#"></p>
</details>

<details>
<summary><b>상장폐지 진행</b></summary>
<p align="center"><img src="#" alt="상장폐지 진행" width="#"></p>
</details>

<details>
<summary><b>상장폐지 환불 진행</b></summary>
<p align="center"><img src="#" alt="상장폐지 환불 진행" width="#"></p>
</details>
</details>

<details>
<summary><b>폐지된 종목 페이지</b></summary>

<details>
<summary><b>폐지 사유별 필터</b></summary>
<p align="center"><img src="#" alt="폐지 사유별 필터" width="#"></p>
</details>

<details>
<summary><b>폐지 종목 검색</b></summary>
<p align="center"><img src="#" alt="폐지 종목 검색" width="#"></p>
</details>

<details>
<summary><b>폐지 종목 상세 모달</b></summary>
<p align="center"><img src="#" alt="폐지 종목 상세 모달" width="#"></p>
</details>
</details>

<details>
<summary><b>기준 관리 페이지</b></summary>

<details>
<summary><b>기준별 필터</b></summary>
<p align="center"><img src="#" alt="기준별 필터" width="#"></p>
</details>

<details>
<summary><b>기준 생성</b></summary>
<p align="center"><img src="#" alt="기준 생성" width="#"></p>
</details>

<details>
<summary><b>기준 수정</b></summary>
<p align="center"><img src="#" alt="기준 수정" width="#"></p>
</details>

<details>
<summary><b>기준 삭제</b></summary>
<p align="center"><img src="#" alt="기준 삭제" width="#"></p>
</details>

<details>
<summary><b>기준 비활성화</b></summary>
<p align="center"><img src="#" alt="기준 비활성화" width="#"></p>
</details>

<details>
<summary><b>기준 검색</b></summary>
<p align="center"><img src="#" alt="기준 검색" width="#"></p>
</details>
</details>
</details>

<details>
<summary><b>거래소 자금 관리</b></summary>

<details>
<summary><b>입금</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/1b2a6c88-6382-414b-897b-ac3e46142eb2" alt="거래소 입금" width="#"></p>
</details>

<details>
<summary><b>출금</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/8f72ef19-5792-43de-b775-f0f28805df9a" alt="거래소 출금" width="#"></p>
</details>

<details>
<summary><b>입출금 내역</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/59576687-e531-4426-a8b9-9aa96d686333" alt="최근 입금 내역" width="#"></p>
</details>
</details>
</details>

<!-- 일반 유저 페이지 -->
<details>
<summary><b>⑤ 일반 유저 페이지</b></summary>

<details>
<summary><b>로그인 / 회원가입 페이지</b></summary>

<details>
<summary><b>로그인</b></summary>

<details>
<summary><b>로그인</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/4dcfbab1-3ef4-47cb-b806-3d6c74ba219c" alt="로그인" width="#"></p>
</details>

<details>
<summary><b>로그인 캡챠 인증</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/b5623a93-5af9-4b82-a8fc-053e6c614aed" alt="로그인 캡챠 인증" width="#"></p>
</details>
</details>

<details>
<summary><b>회원가입</b></summary>

<details>
<summary><b>증권사 선택</b></summary>
<p align="center"><img src="#" alt="증권사 선택" width="#"></p>
</details>

<details>
<summary><b>OCR 검증</b></summary>
<p align="center"><img src="#" alt="OCR 검증" width="#"></p>
</details>
</details>

<details>
<summary><b>비밀번호 찾기</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/79887ed3-8292-4cee-9596-d665aa41c5f0" alt="비밀번호 찾기" width="#"></p>
</details>

<details>
<summary><b>로그인 배경 플로팅 이미지</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/53a094b8-fcab-480a-aaff-ee9bd2c38dc9" alt="로그인 배경 플로팅 이미지" width="#"></p>
</details>
</details>

<details>
<summary><b>홈 페이지</b></summary>

<details>
<summary><b>시가총액순</b></summary>
<p align="center"><img src="#" alt="시가총액순" width="#"></p>
</details>

<details>
<summary><b>거래량순</b></summary>
<p align="center"><img src="#" alt="거래량순" width="#"></p>
</details>

<details>
<summary><b>24H 변동성순</b></summary>
<p align="center"><img src="#" alt="24H 변동성순" width="#"></p>
</details>

<details>
<summary><b>주식 즐겨찾기</b></summary>
<p align="center"><img src="#" alt="주식 즐겨찾기" width="#"></p>
</details>
</details>

<details>
<summary><b>뉴스 페이지</b></summary>

<details>
<summary><b>전체 뉴스</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/7491c743-9b60-4f1b-8a93-8dd46c533a3a" alt="전체 뉴스" width="#"></p>
</details>

<details>
<summary><b>보유주식 뉴스</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/78a33a5e-0360-4cdb-88d9-1e064ab8128f" alt="보유주식 뉴스" width="#"></p>
</details>

<details>
<summary><b>즐겨찾기 뉴스</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/a073961c-26cf-496c-99aa-dd4608191123" alt="즐겨찾기 뉴스" width="#"></p>
</details>

<details>
<summary><b>공시 페이지</b></summary>

<details>
<summary><b>전체 공시</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/45a66ae7-9ebd-469b-8fe6-401e6225010a" alt="전체 공시" width="#"></p>
</details>

<details>
<summary><b>보유주식 공시</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/985a02a5-b1f9-4768-9a32-06617a764a8b" alt="보유주식 공시" width="#"></p>
</details>

<details>
<summary><b>즐겨찾기 공시</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/925834ea-812a-4bd8-b855-a1b534d4969c" alt="즐겨찾기 공시" width="#"></p>
</details>
</details>

<details>
<summary><b>뉴스 공유 링크 자동 생성</b></summary>
<p align="center"><img src="#" alt="뉴스 공유 링크 자동 생성" width="#"></p>
</details>

<details>
<summary><b>관련 주식</b></summary>
<p align="center"><img src="#" alt="관련 주식" width="#"></p>
</details>

<details>
<summary><b>인기순</b></summary>
<p align="center"><img src="#" alt="인기순" width="#"></p>
</details>

<details>
<summary><b>뉴스 상세 조회</b></summary>
<p align="center"><img src="#" alt="뉴스 상세 조회" width="#"></p>
</details>
</details>

<details>
<summary><b>주식 목록 페이지</b></summary>

<details>
<summary><b>조건별 순위</b></summary>
<p align="center"><img src="#" alt="조건별 순위" width="#"></p>
</details>

<details>
<summary><b>실시간 반영</b></summary>
<p align="center"><img src="#" alt="실시간 반영" width="#"></p>
</details>
</details>

<details>
<summary><b>공모 목록 페이지</b></summary>

<details>
<summary><b>공모 청약</b></summary>
<p align="center"><img src="#" alt="공모 청약" width="#"></p>
</details>

<details>
<summary><b>공모 상태 필터</b></summary>
<p align="center"><img src="#" alt="공모 상태 필터" width="#"></p>
</details>

<details>
<summary><b>공모 검색</b></summary>
<p align="center"><img src="#" alt="공모 검색" width="#"></p>
</details>
</details>

<details>
<summary><b>내 계좌 페이지</b></summary>

<details>
<summary><b>보유 주식 가치에 따른 실시간 변화</b></summary>
<p align="center"><img src="#" alt="보유 주식 실시간 변화" width="#"></p>
</details>

<details>
<summary><b>보유 주식 라우팅</b></summary>
<p align="center"><img src="#" alt="보유 주식 라우팅" width="#"></p>
</details>

<details>
<summary><b>입금</b></summary>
<p align="center"><img src="#" alt="입금" width="#"></p>
</details>

<details>
<summary><b>출금</b></summary>
<p align="center"><img src="#" alt="출금" width="#"></p>
</details>

<details>
<summary><b>계좌이체</b></summary>
<p align="center"><img src="#" alt="계좌이체" width="#"></p>
</details>

<details>
<summary><b>거래내역 조회</b></summary>

<details>
<summary><b>거래내역 카테고리별 필터</b></summary>
<p align="center"><img src="#" alt="거래내역 카테고리별 필터" width="#"></p>
</details>

<details>
<summary><b>거래내역 날짜별 조회</b></summary>
<p align="center"><img src="#" alt="거래내역 날짜별 조회" width="#"></p>
</details>

<details>
<summary><b>거래내역 상세 조회 모달</b></summary>
<p align="center"><img src="#" alt="거래내역 상세 조회 모달" width="#"></p>
</details>
</details>
</details>

<details>
<summary><b>주식 즐겨찾기 페이지</b></summary>

<details>
<summary><b>주식 즐겨찾기 해제</b></summary>
<p align="center"><img src="#" alt="주식 즐겨찾기 해제" width="#"></p>
</details>
</details>

<details>
<summary><b>내 정보 페이지</b></summary>
<p align="center"><img src="#" alt="내 정보 페이지" width="#"></p>
</details>

<details>
<summary><b>내 주식 포트폴리오 페이지</b></summary>

<details>
<summary><b>실시간 반영 시연</b></summary>
<p align="center"><img src="#" alt="포트폴리오 실시간 반영" width="#"></p>
</details>
</details>

<details>
<summary><b>내 청약 내역 페이지</b></summary>

<details>
<summary><b>청약 추가납입</b></summary>
<p align="center"><img src="#" alt="청약 추가납입" width="#"></p>
</details>

<details>
<summary><b>청약 환불</b></summary>
<p align="center"><img src="#" alt="청약 환불" width="#"></p>
</details>

<details>
<summary><b>청약 목록 조회</b></summary>
<p align="center"><img src="#" alt="청약 목록 조회" width="#"></p>
</details>
</details>

<details>
<summary><b>검색 모달</b></summary>

<details>
<summary><b>전체</b></summary>
<p align="center"><img src="#" alt="전체 검색" width="#"></p>
</details>

<details>
<summary><b>종목</b></summary>
<p align="center"><img src="#" alt="종목 검색" width="#"></p>
</details>

<details>
<summary><b>뉴스</b></summary>
<p align="center"><img src="#" alt="뉴스 검색" width="#"></p>
</details>

<details>
<summary><b>공시</b></summary>
<p align="center"><img src="#" alt="공시 검색" width="#"></p>
</details>
</details>
</details>
</section>

  <section id="evidence">
    <h2>9) 단위 테스트 결과서</h2>
    <p>MKX 프로젝트의 주요 기능 영역별 단위 테스트 결과를 정리합니다.</p>
    <div class="card">
      <p><a href="https://documenter.getpostman.com/view/43742779/2sB3QRnmew" target="_blank" rel="noopener">단위 테스트 결과서 (Postman Collection)</a></p>
    </div>
  </section>

  <section id="reqspec">
    <h2>10) 요구사항 명세</h2>
    <div class="card">
      <p><a href="https://docs.google.com/spreadsheets/d/1KsOnMh4J6d19r1ddL_Do8jKxRJrOoILHzYmdGbvL0C0/edit?gid=0#gid=0" target="_blank" rel="noopener">요구사항 명세서 문서 URL</a></p>
      <p><a href="https://github.com/user-attachments/files/22421176/_.WBS.-.WBS.pdf" target="_blank" rel="noopener">요구사항 명세서 다운로드하기</a></p>
    </div>
  </section>

  <section id="wbs">
    <h2>11) WBS</h2>
    <div class="card">
      <p><a href="https://docs.google.com/spreadsheets/d/1KsOnMh4J6d19r1ddL_Do8jKxRJrOoILHzYmdGbvL0C0/edit?gid=930058085#gid=930058085" target="_blank" rel="noopener">WBS 문서 URL</a></p>
      <p><a href="https://github.com/user-attachments/files/22421263/_.WBS.-.WBS.1.pdf" target="_blank" rel="noopener">WBS 문서 다운로드하기</a></p>
    </div>
  </section>

  <section id="figma">
    <h2>12) 화면설계도</h2>
    <div class="card">
      <p><a href="https://www.figma.com/design/e0qAws8lXICFIVqCcyxCy6/%EC%A0%9C%EB%AA%A9-%EC%97%86%EC%9D%8C?node-id=2-439&t=isFparErgr1XIkiz-1" target="_blank" rel="noopener">피그마 URL</a></p>
    </div>
  </section>

  <section id="erd">
    <h2>13) ERD</h2>
    <div class="card">
      <p><a href="https://www.erdcloud.com/d/T6DQu8zAzzw2Pj6FS" target="_blank" rel="noopener">ERD URL</a></p>
      <img width="5660" height="4002" alt="주식 (2)" src="https://github.com/user-attachments/assets/08ddddc7-1481-423e-a55c-795ea492f676" />
    </div>
  </section>

  <section id="team">
    <h2>14) 팀 소개</h2>
    <table>
      <thead><tr><th>이름</th><th>역할</th><th>주요 담당</th></tr></thead>
      <tbody>
        <tr><td>김진호</td><td>Domain & Listing Lead</td><td>상장, 공모, 청약, 배정, 수요예측, 유상증자, 거래소 백오피스</td></tr>
        <tr><td>김형진</td><td>Trading Engine Lead</td><td>시장가/지정가 주문, 종목 랭킹, 체결 후처리, 주문 파이프라인 연결, EKS 배포</td></tr>
        <tr><td>박혜성</td><td>Trading Engine Lead</td><td>오더북 설계, 시장가/지정가 주문 체결, 상장폐지, 커뮤니티, 증권사 백오피스</td></tr>
        <tr><td>이우영</td><td>Identity & Admin</td><td>로그인, 회원가입, OCR 인증, 캡차 인증, 공시, 뉴스, 검색</td></tr>
        <tr><td>윤세진</td><td>Data & Governance</td><td>차트/보조지표, 호가창 시각화, 기업 정보 시각화</td></tr>
      </tbody>
    </table>
  </section>

  <section id="timeline">
    <h2>15) 일정</h2>
    <ul>
      <li><strong>프로젝트 시작:</strong> 2025-09-12</li>
      <li><strong>프로젝트 종료:</strong> 2025-11-12</li>
    </ul>
  </section>

  <hr/>

<section id="techstack">
  <h2>16) 기술 스택</h2>
  <div class="card">
    <div align="center"><h3>📚 STACKS</h3></div>
    <div align="center">
      <!-- Language & Framework -->
      <img src="https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=java&logoColor=white">
      <img src="https://img.shields.io/badge/Spring%20Boot%20v3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white">
      <img src="https://img.shields.io/badge/Spring%20JPA-59666C?style=for-the-badge&logo=spring&logoColor=white">
      <img src="https://img.shields.io/badge/Lua%20Script-2C2D72?style=for-the-badge&logo=lua&logoColor=white">
      <img src="https://img.shields.io/badge/Kafka-231F20?style=for-the-badge&logo=apachekafka&logoColor=white">
      <img src="https://img.shields.io/badge/Kafka%20Debezium-E6522C?style=for-the-badge&logo=apachekafka&logoColor=white">
      <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white">
      <img src="https://img.shields.io/badge/Redis%20Sharding-DC382D?style=for-the-badge&logo=redis&logoColor=white">
      <img src="https://img.shields.io/badge/Redis%20Clustering-DC382D?style=for-the-badge&logo=redis&logoColor=white">
      <br>
      <!-- AWS & Infra -->
      <img src="https://img.shields.io/badge/AWS%20S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white">
      <img src="https://img.shields.io/badge/CloudFront-232F3E?style=for-the-badge&logo=amazoncloudfront&logoColor=white">
      <img src="https://img.shields.io/badge/Elastic%20Kubernetes%20Service-FF9900?style=for-the-badge&logo=amazonEKS&logoColor=white">
      <img src="https://img.shields.io/badge/Elastic%20Container%20Registry-FF9900?style=for-the-badge&logo=amazonecr&logoColor=white">
      <img src="https://img.shields.io/badge/Elastic%20Cache-FF4F8B?style=for-the-badge&logo=amazonelasticache&logoColor=white">
      <img src="https://img.shields.io/badge/RDS-527FFF?style=for-the-badge&logo=amazonrds&logoColor=white">
      <img src="https://img.shields.io/badge/Route%2053-8C4FFF?style=for-the-badge&logo=amazonroute53&logoColor=white">
      <img src="https://img.shields.io/badge/EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white">
      <img src="https://img.shields.io/badge/Certificate%20Manager-569A31?style=for-the-badge&logo=amazonaws&logoColor=white">
      <img src="https://img.shields.io/badge/VPC-0073BB?style=for-the-badge&logo=amazonvpc&logoColor=white">
      <img src="https://img.shields.io/badge/IAM-232F3E?style=for-the-badge&logo=awsiam&logoColor=white">
      <br>
      <!-- DevOps -->
      <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white">
      <img src="https://img.shields.io/badge/Docker%20Compose-1D63ED?style=for-the-badge&logo=docker&logoColor=white">
      <img src="https://img.shields.io/badge/GitHub%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white">
      <img src="https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white">
    </div>
    <hr/>
    <h3>Backend</h3>
    <p>Spring Boot v3 · Spring JPA · Kafka · Kafka Debezium · Lua Script · Redis (Sharding/Clustering) · MariaDB</p>
    <h3>Frontend</h3>
    <p>Vue.js / React · Vuetify · Chart.js</p>
    <h3>Infra & DevOps</h3>
    <p>AWS EC2 · ECR · EKS · S3 · CloudFront · RDS · Elastic Cache · Route53 · VPC · IAM · Certificate Manager · Docker · Docker Compose · Nginx · GitHub Actions</p>
  </div>
</section>

  <section id="architecture">
    <h2>17) 시스템 아키텍처</h2>
    <div class="card">
      <p align="center"><img src="https://github.com/user-attachments/assets/b303dbc7-93a0-43cf-8143-24cf18d62ae9" alt="시스템 아키텍처 다이어그램" width="800"></p>
    </div>
  </section>

  <hr/>

  <section id="api">
    <h2>18) API 명세서</h2>
    <div class="card">
      <p><a href="https://documenter.getpostman.com/view/43742779/2sB3QRnmew" target="_blank" rel="noopener">API 명세서</a></p>
    </div>
  </section>

  <hr/>

  <footer class="muted" style="margin-top:28px">
    © MKX. All rights reserved.
  </footer>

</main>
</body>
</html>

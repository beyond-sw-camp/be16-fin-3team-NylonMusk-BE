<body>
<main class="container">

  <h1>MeX — 기업을 위한 증권 공급망 거래소</h1>
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
    <p><strong>MeX</strong>는 <em>기업을 위한 증권 공급망 관리(SSOM: Securities Supply–Order Management)</em>를 핵심 컨셉으로 하는 B2B 거래소입니다. 
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
    <a href="#api">16. API 명세서</a><br />
  </div>

  <hr/>



  <section id="value">
        <h2>1) 문제정의 & 가치제안</h2>
  <p>
    기존 시장에서는 발행, 유상증자, 주주총회, 상장폐지와 같은 기업 중심의 프로세스가 실제 거래 시스템과 단절되어 단편적으로 운영됩니다. 이러한 구조는 정보의 비대칭성과 절차적 비효율을 초래하며, 투자자와 기업 모두에게 불투명한 경험을 제공합니다.
  </p>

  <p>
    MeX는 이와 달리, <strong>발행·공급에서 거래·정산, 나아가 거버넌스</strong>에 이르기까지 전 과정을 단일한 <strong>주문·공급망 데이터 모델</strong> 위에서 통합합니다. 이를 통해 각각 분리되어 있던 프로세스를 하나의 흐름으로 연결하고, 기업과 투자자 간의 정보 단절을 해소합니다.
  </p>

  <p>
    이러한 통합 모델은 거래 속도를 향상시키고, 절차 전반의 투명성을 강화하며, 데이터 기반의 감사 가능성을 본질적으로 내재화합니다. 기업은 복잡한 절차를 간소화할 수 있으며, 투자자는 명확한 기준과 기록을 기반으로 신뢰할 수 있는 참여가 가능합니다.
  </p>

  <p>
    결과적으로 MeX는 단순한 거래 플랫폼을 넘어, 기업의 자본 활동과 투자자의 의사결정이 실시간으로 유기적으로 맞물리는 새로운 생태계를 제공합니다. 이는 단순한 기술적 개선을 넘어, <strong>시장 운영 방식 자체를 혁신적으로 재편하는 핵심 동력</strong>입니다.
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
    <p>MeX 프로젝트의 주요 기능 시연 영상을 정리합니다.</p>
    <details>
      <summary>
        <b>증권사 관리자 회원가입/로그인</b>
      </summary>
      <b>증권사 관리자 회원가입(도로명 검색)</b>
      <p align="center">
        <img src="#" alt="#" width="#">
      </p>
      <b>증권사 관리자 로그인</b>
      <p align="center">
        <img src="#" alt="#" width="#">
      </p>
      <summary>
        <b>기업 관리자 회원가입/로그인</b>
      </summary>
      <b>기업 관리자 회원가입(도로명 검색)</b>
      <p align="center">
        <img src="#" alt="#" width="#">
      </p>
      <b>기업 관리자 로그인</b>
      <p align="center">
        <img src="#" alt="#" width="#">
      </p>
      <summary>
        <b>거래소 관리자 로그인</b>
      </summary>
      <b>거래소 관리자 로그인</b>
      <p align="center">
        <img src="#" alt="#" width="#">
      </p>
    </details>
  </section>

  <section id="evidence">
    <h2>9) 단위 테스트 결과서</h2>
    <p>MeX 프로젝트의 주요 기능 영역별 단위 테스트 결과를 정리합니다.</p>
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
        <tr><td>김진호</td><td>Domain & Listing Lead</td><td>상장/상폐·공시 도메인 설계, 기업 커뮤니케이션, 관리자 상장심사 UI/흐름</td></tr>
        <tr><td>김형진</td><td>Trading Engine Lead</td><td>주문/호가/체결, 조건주문, 블록딜/다크풀 라우팅, 가상거래 시나리오</td></tr>
        <tr><td>박혜성</td><td>Trading Engine Lead</td><td>주문/호가/체결, 조건주문, 블록딜/다크풀 라우팅, 가상거래 시나리오</td></tr>
        <tr><td>이우영</td><td>Identity & Admin</td><td>회원/계좌/KYB·KYC, MFA/세션/권한, 관리자 RBAC/감사로그, 온라인 주주총회</td></tr>
        <tr><td>윤세진</td><td>Data & Governance</td><td>차트/보조지표/MCP 분석, 기업 개요/재무, 기업 정보 시각화</td></tr>
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
      <h3>Backend</h3>
      <p><!-- 예: Spring Boot, JPA, Kafka, Redis, MariaDB, Feign, Spring Cloud Gateway 등 --></p>
      <h3>Frontend</h3>
      <p><!-- 예: Vue.js / React, Vuetify, Chart.js 등 --></p>
      <h3>Infra & DevOps</h3>
      <p><!-- 예: AWS EC2, Docker Compose, Nginx, Elasticache, S3, GitHub Actions 등 --></p>
    </div>
  </section>

  <section id="deployment">
    <h2>17) 배포 환경</h2>
    <div class="card">
      <p><!-- 예: AWS EC2 (Ubuntu 22.04), Docker 기반 멀티 컨테이너 구조, Railway/MySQL, Render/Frontend 등 --></p>
      <p><!-- 예: Backend와 DB는 Private Subnet 내 운영, Redis는 Elasticache SubnetGroup에 구성 등 --></p>
    </div>
  </section>

  <section id="architecture">
    <h2>18) 시스템 아키텍처</h2>
    <div class="card">
      <p><!-- 예: MSA 구조(Exchange, MarketData, MatchingEngine, Brokerage, Community 등) --></p>
      <p><!-- 예: Kafka Topic (orders, executions, order-status), Redis Cluster, MariaDB Replication 등 --></p>
      <p align="center"><img src="#" alt="시스템 아키텍처 다이어그램" width="800"></p>
    </div>
  </section>
  <!-- ✅ 추가된 섹션 끝 -->

  <hr/>

  <section id="api">
    <h2>19) API 명세서</h2>
    <div class="card">
      <p><a href="https://documenter.getpostman.com/view/46241392/2sB3dQtodh" target="_blank" rel="noopener">API 명세서</a></p>
    </div>
  </section>

  <hr/>

  <footer class="muted" style="margin-top:28px">
    © MeX. All rights reserved.
  </footer>

</main>
</body>
</html>
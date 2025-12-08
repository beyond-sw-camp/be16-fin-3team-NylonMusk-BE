<body>
<main class="container">
  <h1>MKX — 증권사, 기업, 투자자를 하나로 잇는 통합 증권 거래 플랫폼</h1>
  <h3>🏆 한화시스템 BEYOND SW CAMP 16기 최종프로젝트 1위 수상 작품 🏆</h3>
  <img width="1920" height="1080" alt="MKX 리드미 배너" src="https://github.com/user-attachments/assets/ef7b248f-7934-47f8-b402-eef19aaab2a8" />

  <hr/>

  <div class="card">
    <h2>소개</h2>
    <p align="center">
      MKX는 중소·중견 증권사의 높은 인프라 구축 비용과 운영 부담을 해결하기 위해 설계된,
    </p>
    <p align="center">
      <strong>입점형 B2B 디지털 증권 거래소 플랫폼</strong>입니다.
    </p>
    <p align="center">
      증권사는 MKX에 입점해 주문·체결 인프라를 즉시 사용할 수 있으며,  
    </p>
    <p align="center">
      사용자는 원하는 증권사를 선택해 거래에 참여할 수 있습니다.
    </p>
  </div>

<h2 id="toc">목차</h2>
  <div class="toc">
  <a href="#team">1. 팀원</a><br />
  <a href="#project-plan">2. 프로젝트 기획서</a><br />
  <a href="#analysis-design">3. 분석 및 설계</a><br />
  <a href="#techstack">4. 기술 스택</a><br />
  <a href="#architecture">5. 시스템 아키텍처</a><br />
  <a href="#tech-summary">6. 기술 요약</a><br />
  <a href="#features">7. 주요 기능</a><br />
  <a href="#ui-ux-test">8. 기능 시연 영상</a><br />
  </div>

<h2>1. 팀원 소개</h2>

|                                                                        **김진호**                                                                         |                                                    **김형진**                                                    |                                                        **박혜성**                                                         |                                                                       **이우영**                                                                        |                                                                      **윤세진**                                                                      |
|:------------------------------------------------------------------------------------------------------------------------------------------------------:|:-------------------------------------------------------------------------------------------------------------:|:----------------------------------------------------------------------------------------------------------------------:|:----------------------------------------------------------------------------------------------------------------------------------------------------:|:-------------------------------------------------------------------------------------------------------------------------------------------------:|
| [<img src="https://github.com/jinnn12.png" height=150 width=150> <br/> @jinnn12 <br/><sub>**Domain & Listing Lead**</sub>](https://github.com/jinnn12) | [<img src="https://github.com/JeaPple.png" height=150 width=150> <br/> @JeaPple <br/><sub>**Trading Engine Lead**</sub>](https://github.com/JeaPple) | [<img src="https://github.com/solidify-d.png" height=150 width=150> <br/> @solidify-d <br/><sub>**Trading Engine Lead**</sub>](https://github.com/solidify-d) |          [<img src="https://github.com/ggj0228.png" height=150 width=150> <br/> @ggj0228 <br/><sub>**Identity & Admin**</sub>](https://github.com/ggj0228)           |      [<img src="https://github.com/AstroJini.png" height=150 width=150> <br/> @AstroJini<br/><sub>**Data & Governance**</sub>](https://github.com/AstroJini)       |


  <section id="project-plan">
    <h2>2. 프로젝트 기획서</h2>
    
  <h3>1) 문제정의 & 가치제안</h3>
  <details>
    <summary><b>① 문제정의</b></summary>
    <p>
      한국 증권 산업은 겉으로 보기에는 경쟁이 활발한 것처럼 보이지만, 실제 구조를 들여다보면 
      <strong>기술 인프라를 갖춘 소수 대형 증권사 중심의 과점화</strong>가 심화된 시장이다.  
      현재 60개 증권사가 존재하지만, 상위 10개사가 전체 리테일 거래의 70~80%를 차지한다는 점은 이러한 왜곡된 구조를 잘 보여준다.  
      이는 단순한 브랜드 인지도 때문이 아니라, 각사가 보유한 <strong>주문·체결·정산 기술력의 격차</strong>에서 기인한 구조적 문제이다.
    </p>
    <img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/4f4eca84-552e-425e-aeb2-26bb4707b412" />
    <p>
      특히 많은 중소형 증권사는 1990~2000년대 구축된 레거시 시스템을 여전히 사용하고 있으며, 
      시스템 교체 시 최소 수백억 원의 비용이 발생해 <strong>전면 교체 자체가 사실상 불가능한 상태</strong>로 남아 있다.  
      금융 IT 교체 주기가 20년 이상 지연되는 대표적인 사례가 바로 이 시장이며, 이로 인해 
      중소형사는 '유지보수 중심의 운영 구조'에 갇혀 혁신을 시도하기 어려운 현실에 놓여 있다.
    </p>
    <img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/f3b37ee9-0b1f-4803-8deb-429e642d2b88" />
    <p>
      금융당국은 이러한 문제를 완화하기 위해 2023년 자본금 15억 원만으로도 설립 가능한 
      <strong>중개전문증권사 규제 완화</strong> 정책을 발표했다.  
      그러나 이는 겉으로만 완화된 조치이다.  
      실제로는 전산 구축비가 여전히 수십~수백억 원에 달해 <strong>신규 진입은 거의 불가능한 환경</strong>이라는 점에서 
      핀테크 기업들이 사업을 포기하는 사례도 다수 발생하고 있다.
    </p>
    <img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/632d0260-0fe5-42ed-a608-1898023823f8" />
    <p>
      또 하나의 핵심 문제는 <strong>비표준화된 시스템 구조</strong>이다.  
      각 증권사는 주문 시스템, 계좌 시스템, 공모·배정 시스템, 리스크 관리, 공시·심사 시스템 등을 제각각 구축해 운영하고 있으며,  
      이로 인해 데이터 정합성이 자주 깨지고, 필수적인 <strong>투명성·추적성·감사 가능성</strong>이 기술 구조 상 충분히 확보되지 않는다.  
      하나의 매매가 체결되기까지 여러 시스템을 거치는 과정에서 데이터가 분절되고 흐름이 끊기는 것이다.
    </p>
    <p>
  요약하면 한국 증권 시장에는 다음과 같은 구조적 한계가 존재한다:
</p>
<ul>
  <li>① 레거시 시스템 고착 → 중소형사의 기술 경쟁력 확보 불가</li>
  <li>② 전산 구축 비용 과다 → 신규 중개사 실질적 진입 불가</li>
  <li>③ 주문·정산·리스크·공시 시스템이 분절 → 데이터 단절 발생</li>
  <li>④ 기업·증권사·투자자가 사용하는 데이터 기준이 제각각</li>
</ul>
<p>
  이러한 문제는 단순한 UI 불편을 넘어,  
  <strong>시장 경쟁의 공정성을 저해하고 기술 격차로 인한 불균형 구조를 더욱 강화하는 핵심 요인</strong>으로 작동하고 있다.
</p>

</details>

<details>
  <summary><b>② 가치제안</b></summary>
  <p>
    MKX는 이러한 구조적 문제를 해결하기 위해 설계된  
    <strong>B2B 입점형 디지털 증권 거래 플랫폼</strong>이다.  
    기존 증권사들이 각기 따로 구축하던 주문·체결·정산·공모·배정·리스크 모듈을 하나의 기술 스택으로 통합하고,  
    이를 <strong>SaaS 형태로 제공</strong>함으로써 기술 격차 문제를 근본적으로 해소한다.
  </p>
  <img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/aab11c01-0042-4a59-83f8-71e0bdefbc80" />
  <p>
    증권사는 MKX에 입점하는 즉시,  
    자체 인프라 구축 없이도 <strong>거래소 수준의 주문 서버·매칭엔진·정산·리스크 관리 인프라</strong>에 연결된다.  
    이로써 기존 수백억 원 규모의 초기 구축 비용 없이도  
    <strong>실시간 주문·차트·리스크 모니터링·공모·배정</strong> 등 핵심 증권 기능을 즉시 사용할 수 있다.
  </p>
<p>
투자자는 가입 시 자신이 선택한 증권사 계정을 통해 MKX 인프라를 사용하게 되며,  
모든 증권사는 동일한 기술 표준을 기반으로 운영된다.  
이는 자연스럽게 다음 두 가지 효과를 만들어낸다:
</p>
<ul>
<li><strong>① 다수 증권사의 입점 → 투자자의 선택권 확대 및 유입 증가</strong></li>
<li><strong>② 전산 부담 완화 → 중소형사 및 신규 증권사의 시장 진입 가속화</strong></li>
</ul>

<p>
    MKX의 가장 큰 차별성은
    주문·체결·정산·공모·위험감시로 이어지는 <strong>증권 업무 전체 공급망(Full Supply Chain)</strong>을
    단일 데이터 모델로 설계했다는 점이다.
    데이터가 흐름 단위로 연결되어 있어 어느 단계에서도 단절이 발생하지 않으며,
    이는 기존 전통 증권사조차 해결하지 못한 구조적 문제를 해결하는 핵심 기술적 진전이다.
  </p>
  <p>
    결국 MKX는
    <strong>중소형 증권사의 진입 장벽을 실질적으로 해소하는 인프라</strong>이자,
    <strong>기업·증권사·투자자를 하나의 생태계로 연결하는 차세대 디지털 시장 플랫폼</strong>이다.
    단순한 거래 시스템을 넘어서,
    누구나 시장에 진입하고, 빠르게 운영하며, 안정적으로 성장할 수 있는
    <strong>증권업의 새로운 표준(AWS-like Infrastructure for Securities)</strong>을 제안한다.
  </p>
  <img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/3ea1ab6c-9c7e-44d2-bce7-1a3b44987287" />

</details>

<h3>2) 범위(Out of Scope 포함)</h3>
  <div id="scope">
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
  </div>

<h3>3) 핵심 기능 요약</h3>
  <div id="features">
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
  <div class="callout"><strong>전체 요구사항 목록</strong>은 아래 "요구사항 명세" 섹션의 문서/이미지로 연결합니다.</div>
  </div>

<h3>4) 공급망형 주문관리(SSOM) 흐름</h3>
  <div id="order-supply">
  <ol>
    <li><strong>공급 등록:</strong> 상장요청 → 심사(재무제표/지표/규정) → 상장 승인 → 종목 생성</li>
    <li><strong>공급 확장:</strong> 공모&유상증자 → 공시 → 주문규칙(시초가/단위/밴드)</li>
    <li><strong>수요 매칭:</strong> 기관 매수/매도, 블록딜/다크풀 라우팅, 조건주문</li>
    <li><strong>정산/잔고:</strong> 체결·수수료·가용현금/재고 반영, 결제상태 추적</li>
    <li><strong>거버넌스:</strong> 공지/주총/의사록/감사로그 및 상장폐지 프로세스</li>
  </ol>
  </div>

<h3>5) 거버넌스(주총/공지)</h3>
  <div id="governance">
  <ul>
    <li>사전승인(안건/일시/스냅샷/마감), 참석 자격 매핑, 본인확인 및 1회용 초대토큰</li>
    <li>안건별 찬반/기권, 보유주식 가중치 집계, 정족수/가결 요건 자동판정</li>
    <li>의사록 전자서명, 결과 공시, 감사로그/CSV·PDF 내보내기</li>
    <li>기업 공지 등록 시 보유자 대상 실시간 알림</li>
  </ul>
  </div>

  <h3>6) 보안·컴플라이언스</h3>
  <div id="security">
  <ul>
    <li><strong>MFA 상시</strong>(패스키 우선, 미지원 시 OTP), 민감행위 전 추가 재인증</li>
    <li>세션 정책(비활성 타임아웃/절대만료/동시세션 제한/RT 로테이션)</li>
    <li><strong>감사 로그</strong>(행위자/대상/전후값/사유/시각), 관리자 고권한 작업 이중확인</li>
    <li>계좌 실명·중복가입 방지(CI/DI), 주민등록번호 원문 미저장</li>
  </ul>
  </div>

  <h3>7) 발표 시나리오</h3>
  <div id="demo">
  <ol>
    <li><strong>기업 A 상장요청</strong> (필숫값·재무제표 업로드) → <em>관리자 심사 승인</em></li>
    <li><strong>거래소 관리자 기업 A 상장 심사</strong> (비상장 → 필숫값·재무제표 기반 상장 심사 및 공모 승인 심사)</li>
    <li><strong>공모 실행</strong> (조건 공시→기관 수요예측 처리→경쟁률 기반 확정가 반영→공모 진행)</li>
    <li><strong>공모 청약</strong> (기업 A 공모 시작→기관 투자자&개인 투자자 공모 청약→투자자들 공모 배정)</li>
    <li><strong>기업 A 상장 완료 및 종목 등록</strong> (공모가=시초가→거래소 종목 등록→시장 거래 시작)</li>
    <li><strong>관리종목/상폐</strong> (조건 충족→거래정지→상폐 고지 7일→상폐 확정)</li>
  </ol>
  </div>

</section>

<section id="analysis-design">
  <h2>3. 분석 및 설계</h2>
  
  ### 요구사항 명세서 [상세보기](https://docs.google.com/spreadsheets/d/1KsOnMh4J6d19r1ddL_Do8jKxRJrOoILHzYmdGbvL0C0/edit?gid=0#gid=0)
  <details>
    <summary><b>요구사항 명세서</b></summary>
    <img width="1159" height="621" alt="스크린샷 2025-12-01 오전 11 36 56" src="https://github.com/user-attachments/assets/ed16805b-5659-49ed-8bbf-a930cbf46b16" />
    <img width="1162" height="641" alt="스크린샷 2025-12-01 오전 11 37 14" src="https://github.com/user-attachments/assets/6cff86f9-7c15-4046-a217-023ef63766d9" />
  </details>

  ### 화면 설계서 [상세보기](https://www.figma.com/design/e0qAws8lXICFIVqCcyxCy6/MKX-%ED%99%94%EB%A9%B4%EC%84%A4%EA%B3%84%EC%84%9C?node-id=0-1&t=qDSgAPhAlWUHEttd-1)
  <details>
    <summary><b>화면 설계서</b></summary>
    <img width="569" height="548" alt="스크린샷 2025-12-01 오후 12 18 22" src="https://github.com/user-attachments/assets/332505f1-dfe1-4b38-a395-c26ba3885f1e" />
  </details>
  
  ### ERD [상세보기](https://www.erdcloud.com/d/5TY5j6PZFDwyjp5Kc)
  <details>
    <summary><b>ERD</b></summary>
    <img width="14150" height="7982" alt="ERD" src="https://github.com/user-attachments/assets/1b34329d-2f3a-49e2-9416-97c5c0a11953" />
  </details>

  ### WBS [상세보기](https://docs.google.com/spreadsheets/d/1KsOnMh4J6d19r1ddL_Do8jKxRJrOoILHzYmdGbvL0C0/edit?gid=930058085#gid=930058085)
  <details>
    <summary><b>WBS</b></summary>
    <img width="1387" height="564" alt="스크린샷 2025-12-01 오후 12 20 42" src="https://github.com/user-attachments/assets/297bae3d-432f-4b73-9bc2-03cf970a1fa7" />
    <img width="1395" height="465" alt="스크린샷 2025-12-01 오후 12 20 54" src="https://github.com/user-attachments/assets/630537d2-923c-4477-b7b1-5249a25df0e3" />
  </details>

  ### API 명세서 [상세보기](https://documenter.getpostman.com/view/46241392/2sB3dQtodh)
  <details>
    <summary><b>API 명세서</b></summary>
    <!-- <img width="1437" height="775" alt="스크린샷 2025-12-01 오후 12 23 44" src="https://github.com/user-attachments/assets/3de326a5-cac2-41ac-b771-31f600f0a7cc" /> -->
    <img width="2552" height="1394" alt="image" src="https://github.com/user-attachments/assets/2b63dc55-f198-4145-8705-b03862ef27bc" />
  </details>
</section>


<section id="techstack">
  <h2>4. 기술 스택</h2>
  <div class="card">
    <div align="center"><h3>Backend</h3></div>
    <div align="center">
      <!-- Backend -->
      <img src="https://img.shields.io/badge/Java%2017-007396?style=for-the-badge&logo=OpenJDK&logoColor=white">
      <img src="https://img.shields.io/badge/Spring%20Boot%20v3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white">
      <img src="https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=for-the-badge&logo=Spring&logoColor=white">
      <img src="https://img.shields.io/badge/Hibernate-59666C?style=for-the-badge&logo=Hibernate&logoColor=white">
      <img src="https://img.shields.io/badge/STOMP-000000?style=for-the-badge">
      <img src="https://img.shields.io/badge/MariaDB-003545?style=for-the-badge&logo=MariaDB&logoColor=white">
      <img src="https://img.shields.io/badge/SSE%20(Server--Sent%20Events)-000000?style=for-the-badge">
      <img src="https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=JSON%20web%20tokens&logoColor=white">
      <img src="https://img.shields.io/badge/Lua%20Script-2C2D72?style=for-the-badge&logo=lua&logoColor=white">
      <img src="https://img.shields.io/badge/Kafka-231F20?style=for-the-badge&logo=apachekafka&logoColor=white">
      <img src="https://img.shields.io/badge/Kafka%20Debezium-E6522C?style=for-the-badge&logo=apachekafka&logoColor=white">
      <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white">
      <img src="https://img.shields.io/badge/Redis%20Sharding-DC382D?style=for-the-badge&logo=redis&logoColor=white">
      <img src="https://img.shields.io/badge/Redis%20Clustering-DC382D?style=for-the-badge&logo=redis&logoColor=white">
      <img src="https://img.shields.io/badge/InfluxDB-22ADF6?style=for-the-badge&logo=influxdb&logoColor=white">
      <br>
    <div align="center"><h3>Frontend</h3></div>
      <!-- Frontend -->
      <img src="https://img.shields.io/badge/vuejs-%2335495e.svg?style=for-the-badge&logo=vuedotjs&logoColor=%234FC08D">
      <img src="https://img.shields.io/badge/Vuetify-1867C0?style=for-the-badge&logo=vuetify&logoColor=AEDDFF">
      <img src="https://img.shields.io/badge/chart.js-F5788D.svg?style=for-the-badge&logo=chart.js&logoColor=white">
      <img src="https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black">
      <img src="https://img.shields.io/badge/CSS3-1572B6?style=for-the-badge&logo=css3&logoColor=white">
      <br>
    <div align="center"><h3>Infra & Cloud</h3></div>
      <!-- AWS & Infra -->
      <img src="https://img.shields.io/badge/Amazon%20S3-FF9900?style=for-the-badge&logo=amazons3&logoColor=white">
      <img src="https://img.shields.io/badge/CloudFront-232F3E?style=for-the-badge&logo=amazoncloudfront&logoColor=white">
      <img src="https://img.shields.io/badge/kubernetes-%23326ce5.svg?style=for-the-badge&logo=kubernetes&logoColor=white">
      <img src="https://img.shields.io/badge/Elastic%20Kubernetes%20Service-FF9900?style=for-the-badge&logo=amazonEKS&logoColor=white">
      <img src="https://img.shields.io/badge/Elastic%20Container%20Registry-FF9900?style=for-the-badge&logo=amazonecr&logoColor=white">
      <img src="https://img.shields.io/badge/Elastic%20Cache-FF4F8B?style=for-the-badge&logo=amazonelasticache&logoColor=white">
      <img src="https://img.shields.io/badge/RDS-527FFF?style=for-the-badge&logo=amazonrds&logoColor=white">
      <img src="https://img.shields.io/badge/Route%2053-8C4FFF?style=for-the-badge&logo=amazonroute53&logoColor=white">
      <img src="https://img.shields.io/badge/EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white">
      <img src="https://img.shields.io/badge/Certificate%20Manager-569A31?style=for-the-badge&logo=amazonaws&logoColor=white">
      <img src="https://img.shields.io/badge/VPC-0073BB?style=for-the-badge&logo=amazonvpc&logoColor=white">
      <img src="https://img.shields.io/badge/IAM-232F3E?style=for-the-badge&logo=awsiam&logoColor=white">
      <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white">
      <img src="https://img.shields.io/badge/Docker%20Compose-1D63ED?style=for-the-badge&logo=docker&logoColor=white">
      <img src="https://img.shields.io/badge/GitHub%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white">
      <img src="https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white">
      <br>
    <div align="center"><h3>External API & Integration</h3></div>
      <img src="https://img.shields.io/badge/TradingView%20Lightweight%20Charts-000000?style=for-the-badge">
      <img src="https://img.shields.io/badge/OCR-000000?style=for-the-badge">
      <img src="https://img.shields.io/badge/OpenDART-0054A6?style=for-the-badge">
      <img src="https://img.shields.io/badge/gpt--4o--mini-412991?style=for-the-badge">
      <img src="https://img.shields.io/badge/CAPTCHA-000000?style=for-the-badge">
      <br>
    <div align="center"><h3>Test & Docs</h3></div>
      <img src="https://img.shields.io/badge/Apache%20JMeter-D22128?style=for-the-badge&logo=apachejmeter&logoColor=white">
      <img src="https://img.shields.io/badge/Postman-FF6C37?style=for-the-badge&logo=postman&logoColor=white">
      <br>
    <div align="center"><h3>Tools & Collaboration</h3></div>
      <img src="https://img.shields.io/badge/Git-F05032?style=for-the-badge&logo=git&logoColor=white">
      <img src="https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white">
      <img src="https://img.shields.io/badge/Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white">
      <img src="https://img.shields.io/badge/Notion-000000?style=for-the-badge&logo=notion&logoColor=white">
      <img src="https://img.shields.io/badge/Figma-F24E1E?style=for-the-badge&logo=figma&logoColor=white">
      <br>
    </div>
  </div>
</section>


<section id="architecture">
  <h2>5. 시스템 아키텍처</h2>
    <img width="1920" height="1080" alt="MKX_architecture" src="https://github.com/user-attachments/assets/fd1394a3-d4ee-4571-9d0b-3b2dc09901d5" />
</section>

<section id="tech-summary">
  <h2>6. 기술 요약</h2>
<table>
  <thead>
    <tr>
      <th>사용기술</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><b>Kafka 기반 주문·체결 이벤트 파이프라인</b></td>
      <td>주식의 주문·체결·정산 전 구간을 Kafka 중심의 비동기 이벤트 스트림 구조로 설계하여 서비스 간 강한 결합을 제거했습니다. 각 서비스는 Producer/Consumer 모델로 동작하며, 토픽·파티션·컨슈머 그룹을 활용해 고가용성과 순서 보장을 동시에 달성했습니다. 또한 AckMode와 재시도 정책을 직접 제어해 정확한 처리·오프셋 관리를 구현하고, DLT 기반으로 장애 이벤트를 자동 격리해 안정적인 운영을 확보했습니다.
      </td>
    </tr>
    <tr>
      <td><b>Debezium 기반 CDC 주문 파이프라인</b></td>
      <td>카프카는 비동기 메시징 특성상 애플리케이션 단에서 트랜잭션 일관성을 보장하기 어렵다는 한계가 있습니다. 기존 Outbox 스케줄러 폴링 방식은 지연, 중복 처리, 서버 부하 증가 문제를 야기해 고빈도 주문 환경에 적합하지 않았습니다. 이를 해결하기 위해 MKX는 MariaDB binlog를 직접 읽어 Kafka로 전달하는 CDC(Change Data Capture) 아키텍처를 도입했습니다. Debezium + Kafka Connect 기반 구조를 통해 데이터 기록 즉시 이벤트가 발행되며, 낮은 지연, 높은 처리량, 메시지 누락 방지를 보장합니다.</td>
    </tr>
      
  </tbody>
</table>

  
</section>

<section id="features">
  <h2>7. 주요 기능</h2>
</section>

<!-- <section id="ui-ux-test">
  <h2>8. UI/UX 단위 테스트 결과서</h2>
</section> -->

  <hr/>
  </div>

  <hr/>

<section id="demo-video">
  <h2>8) 기능 시연 영상</h2>
  <p>MKX 프로젝트의 주요 기능 시연 영상을 정리합니다.</p>

  <!-- 회원가입 / 로그인 -->
<details>
<summary><b>① 회원가입 / 로그인</b></summary>

<details>
<summary><b>관리자 회원가입 / 로그인</b></summary>

<p><b>기업 관리자 회원가입</b></p>
<p align="center">
<img src="https://github.com/user-attachments/assets/5f42ae7b-a82f-4bb2-a1a4-49e77f9342dc" alt="기업 관리자 회원가입" width="#">
</p>

<p><b>증권사 관리자 회원가입</b></p>
<p align="center">
<img src="https://github.com/user-attachments/assets/1c8097b4-10d8-4c36-8252-fb579fdf5648" alt="증권사 관리자 회원가입" width="#">
</p>

<p><b>거래소 관리자 로그인</b></p>
<p align="center">
<img src="https://github.com/user-attachments/assets/1d6f40c3-0f40-4597-8d53-32a4600bd456" alt="관리자 로그인" width="#">
</p>
</details>
</details>

<details>
<summary><b>일반 유저 회원가입 / 로그인</b></summary>
  
<p><b>일반 유저 회원가입</b></p>
<p align="center">
<img src="https://github.com/user-attachments/assets/3464dc80-c240-4479-b6a6-92f895ff0044" alt="일반 유저 회원가입" width="#">
</p>

<p><b>일반 유저 로그인</b></p>
<p align="center">
<img src="https://github.com/user-attachments/assets/c429e45d-fd2b-4e82-8eef-b2e301ab8e49" alt="일반 유저 로그인" width="#">
</p>

<p><b>일반 유저 캡챠 시연</b></p>
<p align="center">
<img src="https://github.com/user-attachments/assets/e0a279f7-0ba8-4437-b4c8-69b0d3082114" alt="일반 유저 캡챠 시연" width="#">
</p>

</details>
</details>

<!-- 기업 관리자 페이지 -->
<details>
<summary><b>② 기업 관리자 페이지</b></summary>

<details>
<summary><b>공시 관리</b></summary>

<p><b>공시 목록 페이지 및 필터</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/e1b66a3f-e77e-49f7-8f5c-8caa12746c7e" alt="공시 목록 페이지 및 필터" width="#"></p>

<p><b>본공시</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/ddf11478-db2a-41e1-8d94-23ccd5f9f0a2" alt="본공시" width="#"></p>

<p><b>정정공시</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/16859a87-65f2-45fc-af1e-a235bd658f52" alt="정정공시" width="#"></p>

<p><b>추가등록</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/b26f95e7-226f-444a-98f7-9b1d216f4f14" alt="추가공시" width="#"></p>
</details>
</details>

<details>
<summary><b>상장 관리</b></summary>

<p><b>상장 신청</b></p>

<p><b>공모</b></p>
<p align="center"><img src="#" alt="공모 상장 신청" width="#"></p>

<p><b>직상장</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/c5ce43b2-2577-4878-85fd-3b2a6bb34c3a" alt="직상장 신청" width="#"></p>
</details>

<details>
<summary><b>공모 관리</b></summary>

<p><b>공모 등록</b></p>
<p align="center"><img src="#" alt="공모 등록" width="#"></p>

<p><b>공모 목록</b><p>

<p><b>공모 목록 필터</b><p>
<p align="center"><img src="#" alt="공모 목록 필터" width="#"></p>

<p><b>공모 목록 검색</b></p>
<p align="center"><img src="#" alt="공모 목록 검색" width="#"></p>

<p><b>공모 참여</b></p>
<p align="center"><img src="#" alt="공모 참여" width="#"></p>
</details>

<details>
<summary><b>수요예측 참여하기</b></summary>

<p><b>수요예측 목록</b></p>
<p align="center"><img src="#" alt="수요예측 목록" width="#"></p>

<p><b>수요예측 참여</b></p>
<p align="center"><img src="#" alt="수요예측 참여" width="#"></p>
</details>

<details>
<summary><b>내 청약 내역</b></summary>

<p><b>청약 내역 조회</b></p>
<p align="center"><img src="#" alt="청약 내역 조회" width="#"></p>

<p><b>추가 납입</b></p>
<p align="center"><img src="#" alt="추가 납입" width="#"></p>

<p><b>정산</b></p>
<p align="center"><img src="#" alt="정산" width="#"></p>
</details>

<details>
<summary><b>상장 현황</b></summary>
<p align="center"><img src="#" alt="상장 현황 페이지" width="#"></p>
</details>

<details>
<summary><b>주식 관리</b></summary>

<p><b>주식 관리 페이지</b></p>
<p align="center"><img src="#" alt="주식 관리 페이지" width="#"></p>

<p><b>공시 문제 발생 시 재제출</b></p>
<p align="center"><img src="#" alt="공시 재제출" width="#"></p>
</details>
</details>

<details>
<summary><b>자산</b></summary>

<p><b>계좌 등록 신청</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/f4985708-472e-4d42-94ce-2c6067077c5a" alt="계좌 등록 신청" width="#"></p>

<p><b>입금</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/c7d60ce3-7e84-4d36-9937-f2b70967d023" alt="입금" width="#"></p>

<p><b>출금</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/c8d7debc-fb07-4134-a8b2-43291c5e316e" alt="출금" width="#"></p>

<p><b>거래내역 조회</b></p>
  <p align="center"><img src="https://github.com/user-attachments/assets/a264bd19-e4a0-4ec6-a6d7-e988ee8cdba2" alt="거래내역 조회" width="#"></p>

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
<p align="center"><img src="https://github.com/user-attachments/assets/93b1ef23-f75e-495b-8714-6857de4cf8a7" alt="로그인 캡챠 인증" width="#"></p>
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
<p align="center"><img src="https://github.com/user-attachments/assets/aab79ec3-3ab6-42eb-93cf-53e0286d8595" alt="뉴스 공유 링크 자동 생성" width="#"></p>
</details>

<details>
<summary><b>관련 주식</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/c2846054-55de-4c2b-8f82-007192ec9192" alt="관련 주식" width="#"></p>
</details>

<details>
<summary><b>인기순</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/a2d55ef3-89b4-419c-9268-8031399c72ea" alt="인기순" width="#"></p>
</details>

<details>
<summary><b>뉴스 상세 조회</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/baddde83-9376-49ce-af6b-dc3b6b117676" alt="뉴스 상세 조회" width="#"></p>
</details>
</details>

<details>
<summary><b>주식 목록 페이지</b></summary>

<details>
<summary><b>조건별 순위</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/0de1fbab-c2fa-4df9-a6dd-fa0667dc6823" alt="조건별 순위" width="#"></p>
</details>

<details>
<summary><b>실시간 반영</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/9846c3eb-8204-4d27-8692-24897d5f6bd4" alt="실시간 반영" width="#"></p>
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
<summary><b>보유 주식 가치에 따른 실시간 변화 및 라우팅</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/01dd7dd4-eb33-4708-a34d-c51c5da595cc" alt="보유 주식 실시간 변화" width="#"></p>
</details>

<details>
<summary><b>입금</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/493e59d6-1ac6-45f2-9d51-b8033e8f079a" alt="입금" width="#"></p>
</details>

<details>
<summary><b>출금</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/72dec4f5-e74a-4b26-91f1-f62818773ad4" alt="출금" width="#"></p>
</details>

<details>
<summary><b>계좌이체</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/43a33e62-7c25-4b71-8558-0e25463c3988" alt="계좌이체" width="#"></p>
</details>

<details>
<summary><b>거래내역 조회</b></summary>

<details>
<summary><b>거래내역 카테고리별 필터</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/260b011d-976c-4199-8934-eb4dd24fb217" alt="거래내역 카테고리별 필터" width="#"></p>
</details>

<details>
<summary><b>거래내역 날짜별 조회</b></summary>
<p align="center"><img src="#" alt="거래내역 날짜별 조회" width="#"></p>
</details>

<details>
<summary><b>거래내역 상세 조회 모달</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/dcd11207-4e08-40c1-a8fa-b857842bea54" alt="거래내역 상세 조회 모달" width="#"></p>
</details>

</details>
</details>

<details>
<summary><b>주식 즐겨찾기 페이지</b></summary>

<details>
<summary><b>주식 즐겨찾기 해제</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/7f1f5f04-e0fc-4d23-9977-4a1fb8de9ea1" alt="주식 즐겨찾기 해제" width="#"></p>
</details>
</details>

<details>
<summary><b>내 정보 페이지</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/73375e95-b15a-44fb-a268-33c341c8cd9b" alt="내 정보 페이지" width="#"></p>
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
<p align="center"><img src="https://github.com/user-attachments/assets/b3e43b24-438b-4bee-84a8-00c9b1da4132" alt="전체 검색" width="#"></p>
</details>

<details>
<summary><b>종목</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/fbc7ebac-38a5-477c-97b9-a02bc479e540" alt="종목 검색" width="#"></p>
</details>

<details>
<summary><b>뉴스</b></summary>
<p align="center"><img src="https://github.com/user-attachments/assets/7803cb58-8bec-4ad7-b928-f54c9e9e1625" alt="뉴스 검색" width="#"></p>
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
      <img width="14150" height="7982" alt="ERD" src="https://github.com/user-attachments/assets/1b34329d-2f3a-49e2-9416-97c5c0a11953" />
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



  <section id="architecture">
    <h2>17) 시스템 아키텍처</h2>
<img width="1920" height="1080" alt="MKX_architecture" src="https://github.com/user-attachments/assets/fd1394a3-d4ee-4571-9d0b-3b2dc09901d5" />
  </section>

  <hr/>

  <section id="api">
    <h2>18) API 명세서</h2>
    <div class="card">
      <p><a href="https://documenter.getpostman.com/view/46241392/2sB3dQtodh" target="_blank" rel="noopener">API 명세서</a></p>
    </div>
  </section>

  <h2 id="toc-legacy">전 버전</h2>
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

  <hr/>

  <footer class="muted" style="margin-top:28px">
    © MKX. All rights reserved.
  </footer>

</main>
</body>
</html>

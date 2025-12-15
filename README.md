<body>
<main class="container">
  <h1>MKX — 증권사, 기업, 투자자를 하나로 잇는 통합 증권 거래 플랫폼</h1>
  <h2 align="center"> 🏆 한화시스템 BEYOND SW CAMP 16기 최종프로젝트 1위 수상 작품 🏆</h2>
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

<section id="team">
<h2>1. 팀원 소개</h2>

|                                                                        **김진호**                                                                         |                                                    **김형진**                                                    |                                                        **박혜성**                                                         |                                                                       **이우영**                                                                        |                                                                      **윤세진**                                                                      |
|:------------------------------------------------------------------------------------------------------------------------------------------------------:|:-------------------------------------------------------------------------------------------------------------:|:----------------------------------------------------------------------------------------------------------------------:|:----------------------------------------------------------------------------------------------------------------------------------------------------:|:-------------------------------------------------------------------------------------------------------------------------------------------------:|
| [<img src="https://github.com/jinnn12.png" height=150 width=150> <br/> @jinnn12 <br/><sub>**Domain & Listing Lead**</sub>](https://github.com/jinnn12) | [<img src="https://github.com/JeaPple.png" height=150 width=150> <br/> @JeaPple <br/><sub>**Trading Engine Lead**</sub>](https://github.com/JeaPple) | [<img src="https://github.com/solidify-d.png" height=150 width=150> <br/> @solidify-d <br/><sub>**Trading Engine Lead**</sub>](https://github.com/solidify-d) |          [<img src="https://github.com/ggj0228.png" height=150 width=150> <br/> @ggj0228 <br/><sub>**Identity & Admin**</sub>](https://github.com/ggj0228)           |      [<img src="https://github.com/AstroJini.png" height=150 width=150> <br/> @AstroJini<br/><sub>**Data & Governance**</sub>](https://github.com/AstroJini)       |
</section>


  <section id="project-plan">
    <h2>2. 프로젝트 기획서</h2>
    
  <h3>1) 문제정의 & 가치제안</h3>
  <details>
    <summary><b>①.1 문제정의</b></summary>
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
  <summary><b>①.2 가치제안</b></summary>
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

<h3>2) 핵심 기능 요약</h3>
  <div id="features">
    <details>
      <summary><b>②.1 요약</b></summary>
  <table>
    <thead><tr><th>영역</th><th>주요 기능</th></tr></thead>
    <tbody>
      <tr>
        <td><strong>관리자</strong></td>
        <td>ADMIN 권한 관리, 증권사/기업 계정 생성·비활성·권한변경, 가입 승인/거절, 전행위 감사로그, 증권사 대시보드(통계·주문내역·수수료·인기종목)</td>
      </tr>
      <tr>
        <td><strong>상장/공모/유상증자/종목발행</strong></td>
        <td>상장요청(필수값/재무제표 검증), 공모 생성/승인/확정가/오픈/마감(유상증자 포함), 유상증자(N차 공모), 공모청약/취소, 배정 실행/정산/송금, 수요예측(북빌딩), 상장폐지 심사·고지·보상금·위반감지, 종목 정보·재무비율 조회</td>
      </tr>
      <tr>
        <td><strong>거래</strong></td>
        <td>B2B·B2C 지정가/시장가 주문, 주문 취소, 대기/부분체결, 회원 계좌 자동생성/입출금/이체, 증권사/거래소 계좌 입출금, 보유종목 조회, 거래내역 조회, 결제상태 반영</td>
      </tr>
      <tr>
        <td><strong>공시·커뮤니티</strong></td>
        <td>기업 공시 등록/수정/정정/추가, 공시 템플릿, 공시 승인/반려, 포럼 카테고리/게시글/댓글/좋아요, 포럼 투표, 뉴스 조회/연동, 종목 즐겨찾기</td>
      </tr>
      <tr>
        <td><strong>데이터</strong></td>
        <td>차트(캔들/미니차트), 보조지표(MA/EMA/RSI/MACD/볼린저밴드/거래량), 호가/현재가/체결 실시간 조회, 통합 마켓 데이터, 기업 재무제표, 랭킹(상승률/하락률/거래량/거래대금), 관심종목 마켓 데이터</td>
      </tr>
      <tr>
        <td><strong>시뮬레이션</strong></td>
        <td>가상거래 봇 생성/상태변경/비활성화, 다이나믹 봇 자동 생성, 거래량 데이터 생성, 횡보/상승/하락/급등락 시나리오</td>
      </tr>
    </tbody>
  </table>
    </details>
  <!-- <div class="callout"><strong>전체 요구사항 목록</strong>은 아래 "요구사항 명세" 섹션의 문서/이미지로 연결합니다.</div> -->
  </div>

<h3>3) 공급망형 주문관리(SSOM) 흐름</h3>
  <div id="order-supply">
    <details>
      <summary><b>③.1 흐름</b></summary>
  <ol>
    <li><strong>공급 등록:</strong> 상장요청(재무제표/주주명부 파일 업로드) → 심사(재무제표 검증/승인·반려) → 상장 확정 → 종목 생성(티커 자동 생성) → 재무제표 저장 → 뉴스 재매핑</li>
    <li><strong>공급 확장:</strong> 공모/유상증자 생성(N차 공모) → 수요예측(북빌딩) → 확정공모가 산정 → 청약 오픈/마감 → 배정 실행 → 정산(추가납입/환불) → 발행사 송금 → (유상 증자 시)거래 정지(recordDate 기준) → 거래 재개(recordDate+5분)</li>
    <li><strong>수요 매칭:</strong> 지정가/시장가 주문 접수 → 자산 동결(매수: 현금+수수료, 매도: 보유주식) → 호가 등록(Redis) → 매칭 엔진 → 부분/전체 체결 → 주문 취소(대기 주문)</li>
    <li><strong>정산/잔고:</strong> 체결 완료 → 수수료/거래세 계산 → 가용현금/보유주식 반영 → 거래내역(Ledger) 기록 → 결제상태 추적</li>
    <li><strong>거버넌스(공시·상장폐지·감사):</strong> 공시 등록/수정/정정/추가/승인·반려, 상장폐지(기준 위반 감지 → 연속 위반 체크 → 예고 → 실행 → 보상금 처리), 감사로그</li>
  </ol>
  </div>
    </details>

  <h3>4) 발표 시나리오</h3>
  <div id="demo">
    <details>
      <summary><b>④.1 시나리오</b></summary>
  <ol>
    <li><strong>기업 A 상장요청</strong> (필숫값·재무제표 업로드) → <em>관리자 심사 승인</em></li>
    <li><strong>거래소 관리자 기업 A 상장 심사</strong> (비상장 → 필숫값·재무제표 기반 상장 심사 및 공모 승인 심사)</li>
    <li><strong>공모 실행</strong> (조건 공시→기관 수요예측 처리→경쟁률 기반 확정가 반영→공모 진행)</li>
    <li><strong>공모 청약</strong> (기업 A 공모 시작→기관 투자자&개인 투자자 공모 청약→투자자들 공모 배정)</li>
    <li><strong>기업 A 상장 완료 및 종목 등록</strong> (공모가=시초가→거래소 종목 등록→시장 거래 시작)</li>
    <li><strong>관리종목/상폐</strong> (조건 충족→거래정지→상폐 고지 7일→상폐 확정)</li>
    <li><strong>매칭엔진</strong> (Kafka, Redis)</li>
  </ol>
  </div>
    </details>

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
      <td>주식의 주문·체결·정산 전 구간을 Kafka 중심의 비동기 이벤트 스트림 구조로 설계하여 서비스 간 강한 결합을 제거했습니다. 각 서비스는 Producer/Consumer 모델로 동작하며, 토픽·파티션·컨슈머 그룹을 활용해 고가용성과 순서 보장을 동시에 달성했습니다. 또한 AckMode와 재시도 정책을 직접 제어해 정확한 처리·오프셋 관리를 구현하고, DLT 기반으로 장애 이벤트를 자동 격리해 안정적인 운영을 확보했습니다.</td>
    </tr>
    <tr>
      <td><b>Debezium 기반 CDC 주문 파이프라인</b></td>
      <td>카프카는 비동기 메시징 특성상 애플리케이션 단에서 트랜잭션 일관성을 보장하기 어렵다는 한계가 있습니다. 기존 Outbox 스케줄러 폴링 방식은 지연, 중복 처리, 서버 부하 증가 문제를 야기해 고빈도 주문 환경에 적합하지 않았습니다. 이를 해결하기 위해 MKX는 MariaDB binlog를 직접 읽어 Kafka로 전달하는 CDC(Change Data Capture) 아키텍처를 도입했습니다. Debezium + Kafka Connect 기반 구조를 통해 데이터 기록 즉시 이벤트가 발행되며, 낮은 지연, 높은 처리량, 메시지 누락 방지를 보장합니다.</td>
    </tr>
    <tr>
      <td><b>InfluxDB 기반 시계열 데이터 관리</b></td>
      <td>체결 데이터와 캔들 데이터는 초당 수천 건의 고빈도 데이터로, 관계형 DB로는 처리 한계가 있습니다. MKX는 InfluxDB를 시계열 데이터 전용 저장소로 활용하여 체결 이벤트를 실시간 저장하고, 캔들 확정 시 InfluxDB 체결 데이터를 기반으로 재계산하여 정확성을 보장합니다. 또한 52주 최고/최저가 계산, OHLCV 집계, 페이징된 체결 내역 조회 등 대용량 시계열 데이터 조회를 효율적으로 처리합니다. Redis 캐시와 함께 사용하여 실시간 조회 성능과 장기 데이터 보관의 균형을 맞췄습니다.</td>
    </tr>
    <tr>
      <td><b>Redis + Lua 스크립트 기반 실시간 호가 관리</b></td>
      <td>주문 매칭과 호가 조회는 초저지연과 원자성이 필수입니다. MKX는 Redis ZSET 기반 호가 관리와 Lua 스크립트를 결합하여 단일 원자적 연산으로 주문 매칭을 수행합니다. Lua 스크립트는 Redis 서버에서 실행되어 네트워크 왕복을 최소화하고, 가격 우선·시간 우선 정렬, 잔량 집계, 총 호가량 관리 등을 원자적으로 처리합니다. 또한 호가 조회 시에도 Lua 스크립트를 통해 배치 조회와 가격 그룹핑을 한 번에 수행하여 조회 성능을 극대화했습니다.</td>
    </tr>
    <tr>
      <td><b>WebSocket/STOMP 기반 실시간 데이터 전송</b></td>
      <td>호가, 현재가, 체결, 차트, 보조지표 등은 밀리초 단위 업데이트가 필요한 데이터입니다. MKX는 STOMP 프로토콜 기반 WebSocket을 통해 실시간 데이터를 전송하며, Redis Pub/Sub을 활용하여 다중 인스턴스 환경에서도 일관된 브로드캐스트를 보장합니다. 클라이언트는 종목별 토픽(/topic/orderbook/{ticker})을 구독하여 필요한 데이터만 선택적으로 수신할 수 있으며, 초기 구독 시 즉시 스냅샷 데이터를 제공하여 지연 없는 화면 구성을 지원합니다.</td>
    </tr>
    <tr>
      <td><b>Apache POI 기반 재무제표 자동 파싱</b></td>
      <td>상장 심사와 공시 관리를 위해 기업이 제출한 Excel 재무제표를 자동으로 파싱합니다. MKX는 Apache POI를 활용하여 2시트 템플릿(CompanyFinancials/CashFlowStatement)과 단일 시트 템플릿(Earnings_Validation)을 지원하며, 각 템플릿의 헤더 구조를 검증하여 필수 컬럼 존재 여부를 확인합니다. 또한 DART 원본 한글 헤더 파일에 대한 폴백 파싱을 제공하여 다양한 형식의 재무제표를 처리할 수 있습니다. 파싱된 데이터는 손익계산서, 재무상태표, 현금흐름표로 분리 저장되며, IPO 상장 시 연간 5개년 + 최신 분기 데이터를 자동 추출하여 저장합니다.</td>
    </tr>
  </tbody>
</table>
</section>

<section id="core-feature-structure"> <h2>7. 핵심 기능별 설계·구현 요약</h2> <details> <summary>7.1 오더북</summary>
<ul>
  <li>gif 추가 예정</li>

  <li>
    한 줄 설명 (기능 설명): 매수·매도 주문을 가격-시간 우선순위(Price-Time Priority)로 정렬해,
    실시간으로 들어오는 주문과 즉시 매칭할 수 있도록 관리한다.
  </li>

  <li>
    한 줄 설명 (기술 설명): Redis ZSET + LuaScript를 사용해 정렬·매칭·잔량 처리까지
    모두 원자적(Atomic)으로 수행하는 초고속 실시간 오더북 엔진이다.
  </li>

  <li>
    <details>
      <summary>기능 구현</summary>

<ul>
<li>
사용된 기술
<ul>
<li>Redis Cluster (ZSET 기반 오더북 분리 관리)</li>
<li>LuaScript Atomic Execution</li>
<li>Price-Time Priority Score 생성</li>
<li>Kafka (주문/체결 이벤트 스트림)</li>
</ul>
</li>
</ul>
<p></p>
<blockquote>
<p>
오더북은 매칭엔진의 중심 구성요소로, 매수·매도 주문을 각각 별도의 Redis ZSET으로
유지하여 가격·시간 우선순위에 따라 즉시 정렬된다.
Redis ZSET은 SkipList 기반의 O(logN) 삽입/삭제/탐색 성능을 제공하며,
초당 수천 건 이상의 주문이 유입되는 환경에서도 일관된 정렬 상태를 유지할 수 있다.
추가적인 정렬 비용 없이 항상 “최우선 가격·최우선 시간”을 즉시 조회할 수 있다는 점에서
오더북에 가장 적합한 자료구조이다.
</p>

<p>
각 주문은 <code>price × 1e9 + sequence</code> 방식의 단일 score를 부여받는다.
동일한 가격대에서도 순서가 명확하게 보장되도록 하기 위한 설계이며,
가격·시간 우선순위(Price-Time Priority)를 하나의 정규화된 score로 통합해
표현함으로써 정렬 기준이 단순하고 안정적으로 유지된다.
별도 comparator 로직 없이 score 기반 정렬만으로 의도한 우선순위가
구현된다는 점에서 성능적·구조적 효율성이 높다.
</p>

<p>
주문 접수 시 LuaScript가 실행되어,<br/>
① 상대 호가 존재 여부 확인 →<br/>
② 가격 교차 여부 판단 →<br/>
③ 체결 처리(부분체결 포함) →<br/>
④ 잔량 재적재 →<br/>
⑤ Kafka 체결 이벤트 발행
</p>

<p>
          까지의 흐름을 하나의 원자적 블록으로 처리한다.
          Redis는 단일 스레드 기반이기 때문에 LuaScript 내부 로직 전체가
          트랜잭션처럼 Atomic하게 수행된다.
          반면 애플리케이션 레벨에서 여러 Redis 명령을 순차 호출하면
          경쟁 조건, 중복체결, 잔량 누락 등 심각한 동시성 문제가 발생할 수 있다.
          LuaScript는 이러한 위험을 구조적으로 제거해
          매칭의 신뢰성과 일관성을 보장한다.
</p>

<p>
          체결 결과는 Kafka로 스트리밍된다.
          매칭엔진은 체결 판단에 집중하고,
          잔고·캔들·체결 기록 등 후처리는 개별 서비스가 병렬적으로 수행한다.
          Kafka는 높은 Throughput, 파티션 기반 확장성,
          소비자 그룹 기반 독립 확장, 디스크 기반 내구성을 갖추고 있어
          여러 서비스가 동일 체결 스트림을 동시에 소비해도 병목이 발생하지 않는다.
          특히 초저지연 반영을 위해 <code>linger-ms</code>를 0으로 설정해 즉시 전송하도록 하고,
          <code>acks=all</code>로 내구성까지 확보함으로써
          “빠르고 유실 없는 체결 스트림”이라는 요구사항을 충족시켰다.
</p>

<p>
          또한 Redis Cluster 기반 샤딩을 적용해 종목(symbol) 단위로
          오더북이 자동 분산되도록 설계했다.
          종목 단위로 독립적인 주문이 쏟아지는 증권 시스템 특성상,
          샤딩 효과가 극대화되며 특정 종목의 폭증 트래픽이
          전체 시스템 성능에 영향을 미치지 않는다.
          Redis Cluster는 Master–Replica 구조를 갖추고 있어
          장애 발생 시 Replica가 자동 승격되며,
          노드 간 통신을 통해 슬롯 재배치와 장애 감지를 수행한다.
          이는 단일 장애점(SPOF)을 제거하고,
          24시간 거래 환경에서 필수적인 고가용성(HA)과
          무중단 운영을 가능하게 한다.
</p>

<p>
          JMeter 기반의 고부하 테스트
          (2,500 스레드 × 100 iterations, 총 25만 건)에서도
          평균 응답속도 40~70ms,
          Throughput 18,000~23,000 TPM,
          에러율 0%, 체결 누락 0건을 확인했다.
          Redis Cluster와 LuaScript 기반 단일 원자적 매칭 구조는
          대량 주문 환경에서도 Price-Time Priority 유지,
          레이스 컨디션 차단,
          초저지연 체결 반영이 안정적으로 이뤄짐을 검증하였다.
          Kafka 후단 역시 병목 없이 이벤트를 소비하며
          실시간 데이터 정합성이 안정적으로 유지되었다.
</p>

<p>
          결과적으로 본 오더북 구조는
          초저지연·고신뢰·고가용성을 동시에 달성하기 위한
          목적 중심 설계이다.
          ZSET은 정렬·탐색 효율을,
          LuaScript는 원자성·동시성 안정성을,
          Kafka는 확장성과 후처리 분리를,
          샤딩은 부하 분산을,
          클러스터링은 무중단 운영을 보장한다.
          각 요소는 독립적으로도 유효하지만,
          전체적으로 결합될 때
          매칭엔진의 성능과 안정성을
          극대화하는 구조적 완성도를 갖추게 된다.
</p>
</blockquote>

</details>
  </li>
</ul>

</details> </section>

<section id="features">
  <h2>8. 주요 기능 설명</h2>

<details>
  <summary><b>8.1 관리자</b></summary>
  
  <h4>8.1.1 거래소 관리자 (EXCHANGE)</h4>
  <ul>
    <li><strong>가입 승인:</strong> 기업/증권사 가입 신청 심사 및 승인/거절, 가입 신청 상세 조회</li>
    <li><strong>상장 심사:</strong> 상장 요청 심사(승인/반려), 상장 확정</li>
    <li><strong>공모 승인:</strong> 공모 요청 승인, 배정 승인/확정 (비관적 락 사용: <code>findByIdForUpdate()</code>로 동시 승인 방지)</li>
    <li><strong>공시 관리:</strong> 기업 공시 승인/반려, 공시 목록 조회</li>
    <li><strong>재무제표 관리:</strong> 재무제표 번들 저장</li>
    <li><strong>계좌 관리:</strong> 기업 계좌 승인/반려/정지/활성화, 전체 계좌 목록 조회</li>
    <li><strong>거래소 계좌:</strong> 거래소 계좌 조회 및 입출금</li>
    <li><strong>기업/증권사 조회:</strong> 기업 목록 조회, 증권사 목록 조회</li>
  </ul>

  <h4>8.1.2 기업 관리자 (CORPORATION)</h4>
  <ul>
    <li><strong>상장 요청:</strong> 상장 요청서 작성 및 제출(재무제표/주주명부 파일 업로드), 내 기업 IPO 조회</li>
    <li><strong>공모 관리:</strong> 공모 요청, 수요예측(북빌딩) 참여, 수요예측 결과 조회</li>
    <li><strong>공시 관리:</strong> 공시 등록/수정/정정/추가(Multipart 파일), 내 기업 공시 조회</li>
    <li><strong>기업 계좌:</strong> 기업 계좌 등록 요청, 입출금, 이체, 계좌 정보 조회</li>
    <li><strong>보유종목 조회:</strong> 기업 보유종목 조회</li>
  </ul>

  <h4>8.1.3 증권사 관리자 (BROKERAGE)</h4>
  <ul>
    <li><strong>증권사 대시보드:</strong> 일일 거래량, 월간 수익, 최근 활동, 인기 종목, 주문 내역, 수수료 통계(일일/기간별/시간대별 추이)</li>
    <li><strong>회원 계좌 관리:</strong> 소속 증권사 회원 계좌 목록 조회, 회원 계좌 승인/반려/정지/활성화/삭제</li>
    <li><strong>증권사 계좌:</strong> 증권사 예치금 계좌 조회 및 입출금 (비관적 락 사용: <code>BrokerageDepositAccount.findByIdForUpdate()</code>로 잔액 충돌 방지)</li>
    <li><strong>회원 관리:</strong> 소속 증권사 회원 조회</li>
  </ul>

  <h4>8.1.4 공통 관리자 기능</h4>
  <ul>
    <li><strong>권한 관리:</strong> RBAC 기반 권한 체계, 계정 생성·비활성·권한변경</li>
    <li><strong>감사 로그:</strong> 전행위 추적 및 감사 가능성 확보</li>
    <li><strong>MFA 상시 인증:</strong> 패스키 우선, 미지원 시 OTP</li>
  </ul>
</details>

<details>
  <summary><b>8.2 상장</b></summary>
  
  <h4>8.2.1 상장 요청</h4>
  <ul>
    <li><strong>상장 요청서 작성:</strong> 재무제표/주주명부 파일 업로드(S3), 필수값 입력 및 검증</li>
    <li><strong>재무제표 자동 파싱:</strong>
      <ul>
        <li>Apache POI 기반 Excel 파싱</li>
        <li>다중 템플릿 지원: 2시트 템플릿(CompanyFinancials + CashFlowStatement), Earnings_Validation 시트</li>
        <li>DART 한글 헤더 폴백 파싱: 원본 한글 헤더 파일 자동 인식 및 파싱</li>
        <li>구조 검증: 필수 컬럼 존재 여부 확인, 데이터 템플릿 불일치 시 오류 처리</li>
        <li>IPO 상장 시: 연간 5개년 + 최신 분기 1개 자동 추출 및 저장</li>
      </ul>
    </li>
    <li><strong>중복 상장 방지:</strong> 비관적 락 사용 (<code>Ipo.findByIdForUpdate()</code>)로 동시 상장 요청 방지</li>
    <li><strong>상태 관리:</strong> PENDING → APPROVED/REJECTED 상태 전환</li>
  </ul>

  <h4>8.2.2 상장 심사</h4>
  <ul>
    <li><strong>거래소 관리자 심사:</strong> 재무제표/지표/규정 검증, 승인/반려 결정</li>
    <li><strong>심사 기준:</strong> 재무제표 검증, 필수값 검증, 규정 준수 여부 확인</li>
  </ul>

  <h4>8.2.3 상장 확정 및 종목 생성</h4>
  <ul>
    <li><strong>상장 확정:</strong> 심사 승인 후 상장 확정 처리</li>
    <li><strong>티커 자동 생성:</strong> 티커 자동 생성 및 충돌 방지</li>
    <li><strong>재무제표 저장:</strong> 파싱된 재무제표 데이터 저장 (손익계산서, 재무상태표, 현금흐름표)</li>
    <li><strong>뉴스 재매핑:</strong> 상장 종목 뉴스 자동 매핑</li>
    <li><strong>총 발행 주식 수 계산:</strong> 상장 시점 총 발행 주식 수 자동 계산</li>
    <li><strong>종목 상태:</strong> LISTED 상태로 전환, 거래 가능 상태로 설정</li>
  </ul>
</details>

<details>
  <summary><b>8.3 공모</b></summary>
  
  <h4>8.3.1 공모 생성</h4>
  <ul>
    <li><strong>공모 타입:</strong>
      <ul>
        <li>INITIAL: 신규 공모 (최초 상장)</li>
        <li>FOLLOW_ON: 유상증자 (N차 공모, 공모 로직 통합)</li>
      </ul>
    </li>
    <li><strong>공모 타입 자동 판별:</strong> IPO 상태 및 기존 공모 이력 기반 자동 판별</li>
    <li><strong>동시 생성 방지:</strong> 비관적 락 사용 (<code>Ipo.findByIdForUpdate()</code>)로 동시 공모 생성 방지</li>
    <li><strong>공모 정보 설정:</strong> 공모 수량, 공모가 범위(최소/최대), Lot Size, 공모 일정 설정</li>
  </ul>

  <h4>8.3.2 수요예측</h4>
  <ul>
    <li><strong>수요예측 오픈:</strong> 기관투자자 대상 수요예측 시작</li>
    <li><strong>수요 수집:</strong> 기관투자자 수요 예측 데이터 수집</li>
    <li><strong>공모가 범위 조정:</strong> 수요예측 결과 기반 공모가 범위 조정</li>
    <li><strong>수요예측 결과 조회:</strong> 수요예측 결과 및 통계 조회</li>
  </ul>

  <h4>8.3.3 확정공모가 산정</h4>
  <ul>
    <li><strong>공모가 확정:</strong> 수요예측 결과 기반 확정공모가 산정</li>
    <li><strong>액면가 검증:</strong> 확정공모가가 액면가 이상인지 검증</li>
    <li><strong>공모 상태 전환:</strong> PRICE_FIXED 상태로 전환</li>
  </ul>

  <h4>8.3.4 청약 관리</h4>
  <ul>
    <li><strong>청약 오픈:</strong> 청약 시작일 설정 및 오픈</li>
    <li><strong>청약 마감:</strong> 청약 종료일 설정 및 자동 마감</li>
    <li><strong>청약 취소:</strong> 청약 기간 내 청약 취소 가능</li>
    <li><strong>청약 상태 관리:</strong> PENDING → PAID → ALLOCATED 상태 전환</li>
  </ul>
</details>

<details>
  <summary><b>8.4 유상증자</b></summary>
  
  <h4>8.4.1 유상증자 특징</h4>
  <ul>
    <li><strong>공모 로직 통합:</strong> 공모 시스템 코드 재활용, 별도 시스템 불필요</li>
  </ul>

  <h4>8.4.2 recordDate 기반 거래 정지/재개</h4>
  <ul>
    <li><strong>거래 정지:</strong> recordDate + 5분 후 자동 거래 정지, 스케줄러 기반 자동 처리</li>
    <li><strong>거래 재개:</strong> 배정 완료 후 자동 거래 재개</li>
    <li><strong>상태 관리:</strong> 거래 정지 상태 자동 전환 및 복구</li>
  </ul>

  <h4>8.4.3 유상증자 프로세스</h4>
  <ul>
    <li><strong>유상증자 신청:</strong> 기업이 신규 자본 조달을 위해 유상증자를 거래소에 신청</li>
    <li><strong>거래소 관리자 승인:</strong> 유상증자는 신규 주식 발행으로 기존 주주 지분 희석·시가총액 변동이 발생하므로 
      거래소 관리자가 가격 적정성, 발행량, 일정(recordDate) 등을 검증하고 승인이 필수 불가결. 
      또한 기준일(recordDate) 전후 거래 정지/재개를 조율해야 하므로 기업 단독으로 진행할 수 없으므로 거래소 관리자 승인 단계를 거침. </li>
    <li><strong>청약 및 배정:</strong> 공모 시스템과 동일한 청약 및 배정 프로세스</li>
    <li><strong>발행량 반영:</strong> 배정 완료 후 총 발행 주식 수 자동 반영(기존 상장 주식 수량 + 신주 발행 수량) </li>
  </ul>
</details>

<details>
  <summary><b>8.5 청약 및 배정</b></summary>
  
  <h4>8.5.1 청약</h4>
  <ul>
    <li><strong>청약 신청:</strong> 개인/기업 투자자 청약 신청, 청약 수량 입력</li>
    <li><strong>증거금 납입:</strong> 청약 시 증거금 자동 계산 및 납입 요구</li>
    <li><strong>청약 상태:</strong> PENDING → PAID 상태 전환 (증거금 납입 완료 시)</li>
    <li><strong>청약 취소:</strong> 청약 기간 내 청약 취소 가능, 증거금 환불</li>
    <li><strong>청약 내역 조회:</strong> 개인/기업별 청약 내역 조회</li>
  </ul>

  <h4>8.5.2 비례배정 알고리즘</h4>
  <ul>
    <li><strong>비례 계산:</strong> <code>(청약 수량 × 공모 수량) / 총 청약 수량</code> 공식으로 각 청약자별 배정량 계산</li>
    <li><strong>Lot Size 고려:</strong> 배정량을 Lot Size 배수로 조정 (<code>raw = (raw / lotSize) * lotSize</code>)</li>
    <li><strong>최대 청약량 제한:</strong> 청약 수량을 초과하지 않도록 제한 (<code>Math.min(raw, applied)</code>)</li>
  </ul>

  <h4>8.5.3 라운드로빈 잔여량 분배</h4>
  <ul>
    <li><strong>잔여량 계산:</strong> 비례배정 후 남은 수량 계산 (<code>remain = offerQuantity - assigned</code>)</li>
    <li><strong>순차 분배:</strong> 잔여량을 Lot Size 단위로 청약자에게 순차 분배</li>
    <li><strong>분배 조건:</strong> 청약 수량을 초과하지 않는 범위 내에서만 분배</li>
    <li><strong>루프 종료 조건:</strong> 잔여량이 부족하거나 더 이상 분배할 수 없을 때 종료</li>
  </ul>

  <h4>8.5.4 배정 실행</h4>
  <ul>
    <li><strong>동시 배정 방지:</strong> 비관적 락 사용 (<code>IpoOffering.findByIdForUpdate()</code>)로 동시 배정 방지</li>
    <li><strong>트랜잭션 일관성:</strong> <code>@Transactional</code>로 배정 생성, 상태 전환, Outbox 기록을 원자적으로 처리</li>
    <li><strong>멱등성 보장:</strong> 동일 roundNo에 대한 중복 배정 방지 (<code>existsByIpoSubscription_IdAndRoundNo</code> 체크)</li>
    <li><strong>배정 상태 관리:</strong> 배정 생성 → COMPLETED 상태 전환</li>
    <li><strong>청약 상태 업데이트:</strong> 배정 완료 시 청약 상태를 PAID → ALLOCATED로 전환</li>
  </ul>

  <h4>8.5.5 Outbox 패턴</h4>
  <ul>
    <li><strong>이벤트 기록:</strong> 배정 완료 시 <code>IpoAllocationOutbox</code>에 이벤트 기록</li>
    <li><strong>비동기 이벤트 발행:</strong> 트랜잭션 커밋 후 비동기로 이벤트 발행</li>
    <li><strong>트랜잭션 일관성:</strong> DB 커밋과 이벤트 발행의 원자성 보장</li>
    <li><strong>이벤트 상태 관리:</strong> PENDING → PROCESSED 상태 전환</li>
  </ul>

  <h4>8.5.6 배정 승인 및 확정</h4>
  <ul>
    <li><strong>거래소 승인:</strong> N차 공모/유상증자 배정 시 거래소 관리자 승인 필요 (ALLOCATION_PENDING → VERIFIED)</li>
    <li><strong>발행사 확정:</strong> 발행사가 배정 확정 (VERIFIED → ALLOCATED)</li>
    <li><strong>배정 결과 조회:</strong> 배정 목록 및 통계 조회</li>
  </ul>

  <h4>8.5.7 정산</h4>
  <ul>
    <li><strong>추가납입/환불 계산:</strong> 배정량에 따른 추가납입 또는 환불 금액 계산</li>
    <li><strong>정산 실행:</strong> 추가납입 요구 또는 환불 처리</li>
    <li><strong>발행사 송금:</strong> 배정 완료 후 발행사에게 공모금 송금</li>
    <li><strong>자동 정산 스케줄러:</strong> 환불일 도래 시 자동 정산 실행</li>
  </ul>
</details>

<details>
  <summary><b>8.6 상장폐지</b></summary>
  
  <h4>8.6.1 기준 위반 감지</h4>
  <ul>
    <li><strong>재무 기준:</strong> 매출액, 자본금, 순이익 등 재무 지표 기준 위반 감지</li>
    <li><strong>거래 기준:</strong> 거래량, 거래대금, 거래일수 등 거래 지표 기준 위반 감지</li>
    <li><strong>연속 위반 체크:</strong> 위반 횟수 누적 및 연속 위반 여부 확인</li>
    <li><strong>자동 감지 스케줄러:</strong> 매일 오전 9시 기준 위반 자동 체크</li>
  </ul>

  <h4>8.6.2 GPT 기반 위험도 분석</h4>
  <ul>
    <li><strong>재무제표 분석:</strong> 재무제표 데이터를 OpenAI API로 전송</li>
    <li><strong>위험도 점수 산출:</strong> GPT가 위험도 점수(0-100) 및 분석 결과 반환</li>
    <li><strong>분석 결과 저장:</strong> 분석 결과를 DB에 저장하여 이력 관리</li>
    <li><strong>재시도 로직:</strong> API 실패 시 재시도 처리</li>
  </ul>

  <h4>8.6.3 상장폐지 진행 단계</h4>
  <ul>
    <li><strong>DELISTING_RISK:</strong> 위반 감지 시 위험 단계로 전환</li>
    <li><strong>DELISTING_NOTICE:</strong> 10분 후 자동으로 예고 단계로 전환 (스케줄러 기반)</li>
    <li><strong>거래 정지:</strong> 예고 발행 후 거래 정지</li>
    <li><strong>상장폐지 확정:</strong> 관리자가 상장폐지 확정 처리</li>
  </ul>

  <h4>8.6.4 보상금 처리</h4>
  <ul>
    <li><strong>보상금 계산:</strong> 보유 주식 수 기반 보상금 계산</li>
    <li><strong>보상금 지급:</strong> 보유자 계좌로 보상금 자동 지급</li>
    <li><strong>거래소 지원금 관리:</strong> 거래소 지원금 계산 및 관리</li>
    <li><strong>낙관적 락:</strong> <code>@Version</code> 필드로 동시성 제어 (DelistingCompensation, ExchangeSupportFund)</li>
  </ul>

  <h4>8.6.5 위반 해결 처리</h4>
  <ul>
    <li><strong>위반 해결 확인:</strong> 위반 사항 해결 여부 확인</li>
    <li><strong>상태 복구:</strong> 위반 해결 시 상장폐지 진행 중단 및 상태 복구</li>
    <li><strong>낙관적 락:</strong> <code>@Version</code> 필드로 동시성 제어 (DelistingViolation, DelistingHistory)</li>
  </ul>
</details>

<details>
  <summary><b>8.7 종목 정보 및 관리</b></summary>
  
  <h4>8.7.1 종목 목록</h4>
  <ul>
    <li><strong>종목 목록 조회:</strong> 검색/상태 필터 지원, 페이징 처리</li>
    <li><strong>종목 상태:</strong> PENDING, APPROVED, LISTED, DELISTED 등 상태 관리</li>
  </ul>

  <h4>8.7.2 종목 상세 정보</h4>
  <ul>
    <li><strong>종목 기본 정보:</strong> 티커, 종목명, 업종, 상장일 등</li>
    <li><strong>재무제표:</strong> 분기별/연간 재무제표 조회 (손익계산서, 재무상태표, 현금흐름표)</li>
    <li><strong>재무비율:</strong> PER/PBR/PSR/시가총액/Enterprise Value 조회</li>
    <li><strong>재무비율 자동 계산:</strong> 주기적 스케줄러로 재무비율 자동 업데이트</li>
  </ul>

  <h4>8.7.3 종목 관리</h4>
  <ul>
    <li><strong>발행량 관리:</strong> 공모/유상증자 시 총 발행 주식 수 자동 업데이트</li>
    <li><strong>거래 규칙 관리:</strong> 시초가, 거래 단위, 가격 변동폭 제한 등 거래 규칙 설정</li>
  </ul>
</details>

<details>
  <summary><b>8.8 거래</b></summary>
  
  <h4>8.8.1 주문 접수</h4>
  <ul>
    <li><strong>주문 유형:</strong> 지정가(LIMIT)/시장가(MARKET) 주문</li>
    <li><strong>자산 동결:</strong> 매수 시 현금+수수료 동결, 매도 시 보유주식 동결</li>
    <li><strong>예상 비용 계산:</strong> 수수료/거래세 미리 계산하여 표시</li>
    <li><strong>시장가 보호한도:</strong> 전일 종가 대비 ±5% 범위 내로 제한 (급격한 가격 변동 방지)</li>
    <li><strong>멱등성 보장:</strong> Redis 기반 PENDING/RESULT 패턴으로 중복 주문 방지</li>
    <li><strong>Outbox 패턴:</strong> 주문 생성 시 <code>OrderOutbox</code>에 기록, Kafka 발행 후 상태 업데이트</li>
  </ul>

  <h4>8.8.2 주문 매칭 엔진</h4>
  <ul>
    <li><strong>Redis ZSET 기반 호가 관리:</strong> 가격 우선 정렬, Redis 해시태그(<code>{ticker}</code>) 사용으로 클러스터 슬롯 일관성 보장</li>
    <li><strong>가격 우선/시간 우선 정렬:</strong>
      <ul>
        <li>Score 계산: <code>가격 × FACTOR(1,000,000) + 시간 tie-breaker</code></li>
        <li>매수: 높은 가격 우선, 동가격 시 이른 시간 우선 (<code>bidScore = priceInt × FACTOR + (FACTOR - seq)</code>)</li>
        <li>매도: 낮은 가격 우선, 동가격 시 이른 시간 우선 (<code>askScore = priceInt × FACTOR + seq</code>)</li>
        <li>FACTOR로 가격과 시간 분리, seq 오버플로우 방지 (<code>seq % FACTOR</code>)</li>
      </ul>
    </li>
    <li><strong>Lua 스크립트 기반 원자적 매칭:</strong> Redis 서버에서 단일 원자적 연산으로 주문 매칭 수행, 네트워크 왕복 최소화</li>
    <li><strong>부분/전체 체결:</strong> 주문 수량에 따라 부분 체결 또는 전체 체결 처리</li>
    <li><strong>Kafka 기반 비동기 처리:</strong> 체결 이벤트를 Kafka로 발행, 비동기 처리로 지연 최소화</li>
  </ul>

  <h4>8.8.3 주문 취소 및 조회</h4>
  <ul>
    <li><strong>주문 취소:</strong> 대기 주문 취소 및 자산 해제, 주문 상태 추적</li>
    <li><strong>대기 주문 조회:</strong> 사용자별 대기 주문 목록 조회(페이징)</li>
    <li><strong>주문 내역 조회:</strong> 종목별 주문 내역 조회(페이징)</li>
  </ul>

  <h4>8.8.4 계좌 및 자산 관리</h4>
  <ul>
    <li><strong>계좌 생성:</strong> SHA-256 해시 기반 고유 계좌번호 생성, 멱등성 보장 (Redis 기반 중복 방지)</li>
    <li><strong>입출금:</strong> 입금, 출금, 이체</li>
    <li><strong>자산 조회:</strong> 보유종목 조회, 거래내역(Ledger) 조회(거래 유형 필터), 가용현금/재고 조회</li>
  </ul>

  <h4>8.8.5 정산</h4>
  <ul>
    <li><strong>수수료/거래세 계산:</strong> 체결 완료 후 자동 계산, 증권사별/거래 유형별 차등 적용</li>
    <li><strong>자산 반영:</strong> 가용현금/보유주식 반영, 거래내역 기록</li>
    <li><strong>트랜잭션 일관성:</strong> <code>@Transactional</code>로 원자성 보장</li>
  </ul>
</details>

<details>
  <summary><b>8.9 공시 관리</b></summary>
  
  <ul>
    <li><strong>공시 관리:</strong> 기업 공시 등록/수정/정정/추가(Multipart 파일), 공시 템플릿, 거래소 승인/반려, 공시 목록 조회</li>
    <li><strong>재무제표 자동 파싱:</strong> 공시 승인 시 Excel 파일 자동 파싱, 다중 템플릿 지원 및 폴백 처리</li>
    <li><strong>공시 시퀀스:</strong> 비관적 락 사용 (<code>DisclosureSequence.findByIdForUpdate()</code>)로 시퀀스 중복 방지</li>
    <li><strong>감사 로그:</strong> 전행위 추적 및 감사 가능성</li>
  </ul>
</details>

<details>
  <summary><b>8.10 데이터</b></summary>
  
  <h4>8.10.1 차트</h4>
  <ul>
    <li><strong>캔들 데이터 조회:</strong> 1m/5m/15m/30m/1h/4h/1d, 시작/종료 시간 지정</li>
    <li><strong>캔들 확정 스케줄러:</strong>
      <ul>
        <li>주기별 확정: 1분/5분/15분/30분/1시간/4시간/일봉</li>
        <li>Source of Truth: InfluxDB 체결 데이터로 재계산 (Redis 유실 시에도 복구 가능)</li>
        <li>OHLCV 재계산: 해당 기간 체결 데이터 집계</li>
        <li>ShedLock 분산 락: 다중 인스턴스 환경에서 중복 실행 방지 (<code>@SchedulerLock</code>)</li>
        <li>WebSocket 브로드캐스트: 확정된 캔들 실시간 전송</li>
      </ul>
    </li>
    <li><strong>최신 캔들 조회:</strong> 최신 캔들 조회, 미니차트(24시간 종가), 기업용 차트</li>
  </ul>

  <h4>8.10.2 보조지표</h4>
  <ul>
    <li><strong>지표 종류:</strong> MA/EMA/RSI/MACD/볼린저밴드/거래량 등 30+ 지표</li>
    <li><strong>사전 계산:</strong> 비동기 스케줄러 기반 배치 계산, 성능 최적화</li>
    <li><strong>캐시 관리:</strong> Redis 기반 캐시, 무효화 전략, 사용자별 설정(ON/OFF)</li>
    <li><strong>ShedLock 분산 락:</strong> 다중 인스턴스 환경에서 중복 계산 방지</li>
  </ul>

  <h4>8.10.3 실시간 데이터</h4>
  <ul>
    <li><strong>호가/현재가/체결:</strong> 실시간 조회, 통합 마켓 데이터(호가창 최적화)</li>
    <li><strong>WebSocket 기반 실시간 업데이트:</strong> STOMP 프로토콜, Redis Pub/Sub 활용하여 다중 인스턴스 환경에서 일관된 브로드캐스트</li>
    <li><strong>호가 조회 Lua 스크립트 최적화:</strong>
      <ul>
        <li>배치 조회: 100개씩 배치로 조회하여 성능 최적화</li>
        <li>가격 그룹핑: 동일 가격 호가 수량 합산</li>
        <li>Unique 가격대 추출: max_depth 개수만큼만 조회</li>
        <li>안전장치: 상한선 설정으로 무한 루프 방지</li>
      </ul>
    </li>
    <li><strong>전일 종가 설정:</strong> 장 시작 시 초기화</li>
  </ul>

  <h4>8.10.4 기업 정보</h4>
  <ul>
    <li><strong>종목 정보:</strong> 종목 상세 정보, 재무제표(분기별/연간), 재무비율(PER/PBR/PSR/시가총액/Enterprise Value)</li>
    <li><strong>52주 최고/최저가 계산:</strong> InfluxDB 기반 365일 체결 데이터 조회, 주기적 갱신(스케줄러), 성능 최적화를 위해 주기적으로만 실행</li>
  </ul>

  <h4>8.10.5 랭킹 및 체결 데이터</h4>
  <ul>
    <li><strong>랭킹:</strong> 상승률/하락률/거래량/거래대금 TOP 30, 카드섹션 데이터, 관심종목 마켓 데이터</li>
    <li><strong>체결 데이터:</strong> 페이징된 체결 데이터 조회, 최근 체결 데이터, OHLCV 데이터, 체결 데이터 개수 조회</li>
  </ul>
</details>

<details>
  <summary><b>8.11 시뮬레이션</b></summary>
  
  <ul>
    <li><strong>트레이딩 봇:</strong> 가상거래 봇 생성/상태변경/비활성화, 다이나믹 봇 자동 생성, 간단한 봇 생성, 거래량 데이터 생성용 봇</li>
    <li><strong>시나리오:</strong> 횡보/상승/하락/급등락 시나리오, 봇 설정 조회</li>
  </ul>
</details>

<details>
  <summary><b>8.12 커뮤니티</b></summary>
  
  <h4>8.12.1 포럼</h4>
  <ul>
    <li><strong>카테고리:</strong> 카테고리 생성/수정/조회</li>
    <li><strong>게시글:</strong> 게시글 작성/수정/삭제/조회(상태/카테고리 필터), 댓글 작성/수정/삭제/조회</li>
    <li><strong>좋아요:</strong> 게시글/댓글 좋아요</li>
    <li><strong>낙관적 락:</strong> <code>@Version</code> 필드로 동시성 제어 (ForumPost, ForumCategory)</li>
  </ul>

  <h4>8.12.2 포럼 투표</h4>
  <ul>
    <li><strong>투표 생성:</strong> 투표 생성, 선택지 순서 변경</li>
    <li><strong>투표 제출:</strong> 투표 제출/변경/철회, 비관적 락 사용 (<code>ForumVote.findByIdForUpdate()</code>)로 중복 투표 방지</li>
    <li><strong>투표 조회:</strong> 투표 조회</li>
  </ul>

  <h4>8.12.3 종목별 뉴스</h4>
  <ul>
    <li><strong>실시간 수집:</strong> RSS 피드 크롤링(매일경제/한국경제/조선일보), 스케줄러 기반 주기적 수집, 중복 방지(URL 기반)</li>
    <li><strong>종목 자동 매칭:</strong> 제목/본문에서 종목명 추출 및 자동 매핑, 다중 종목 매핑 지원</li>
    <li><strong>AI 요약:</strong> OpenAI 기반 기사 요약 생성(10문장), 재시도 로직 포함</li>
    <li><strong>메타데이터 추출:</strong> 썸네일 추출(RSS/본문), 기자명 추출 및 정규화, 발행일시 파싱</li>
    <li><strong>뉴스 조회:</strong> 뉴스 조회(페이징), 종목별 뉴스 조회, 뉴스 배치 조회, 인기 뉴스, 뉴스 조회수/공유수 증가</li>
    <li><strong>뉴스 재매핑:</strong> 상장 종목 뉴스 재매핑(내부 API)</li>
  </ul>

  <h4>8.12.4 즐겨찾기</h4>
  <ul>
    <li><strong>종목 즐겨찾기:</strong> 종목 즐겨찾기 추가/삭제/조회</li>
  </ul>
</details>

<details>
  <summary><b>8.13 인증/회원</b></summary>
  
  <ul>
    <li><strong>회원가입:</strong> 기업/증권사/개인 회원가입, 이메일 중복 확인, OCR 기반 신분증 인식</li>
    <li><strong>로그인:</strong> 관리자/회원 로그인, JWT 기반 인증(AT/RT), 리프레시 토큰</li>
    <li><strong>이메일 인증:</strong> 이메일 인증 코드 발송/검증, 재발송</li>
    <li><strong>비밀번호 재설정:</strong> 비밀번호 찾기, 비밀번호 재설정</li>
    <li><strong>캡차:</strong> 캡차 키 발급, 이미지/오디오 캡차 생성</li>
    <li><strong>회원 정보:</strong> 회원 본인 정보 조회</li>
  </ul>
</details>
</section>

<!-- <section id="ui-ux-test">
  <h2>9. UI/UX 단위 테스트 결과서</h2>
</section> -->

<section id="ui-ux-test">
<h2>9) UI/UX 단위 테스트 결과서</h2>
  <p>MKX 프로젝트의 기능 시연 영상을 정리합니다.</p>

  <!-- 인증/회원가입 -->
<details>
    <summary><b>① 인증 및 회원가입</b></summary>
    
<p><b>관리자 회원가입</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/5f42ae7b-a82f-4bb2-a1a4-49e77f9342dc" alt="기업 관리자 회원가입" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/1c8097b4-10d8-4c36-8252-fb579fdf5648" alt="증권사 관리자 회원가입" width="#"></p>
    
<p><b>관리자 로그인</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/1d6f40c3-0f40-4597-8d53-32a4600bd456" alt="거래소 관리자 로그인" width="#"></p>
    
<p><b>일반 유저 회원가입/로그인</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/3464dc80-c240-4479-b6a6-92f895ff0044" alt="일반 유저 회원가입" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/c429e45d-fd2b-4e82-8eef-b2e301ab8e49" alt="일반 유저 로그인" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/e0a279f7-0ba8-4437-b4c8-69b0d3082114" alt="일반 유저 캡챠 시연" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/4dcfbab1-3ef4-47cb-b806-3d6c74ba219c" alt="로그인" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/b5623a93-5af9-4b82-a8fc-053e6c614aed" alt="로그인 캡챠 인증" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/93b1ef23-f75e-495b-8714-6857de4cf8a7" alt="회원가입" width="#"></p>
<p>OCR 검증: 추가 예정</p>
<p align="center"><img src="https://github.com/user-attachments/assets/79887ed3-8292-4cee-9596-d665aa41c5f0" alt="비밀번호 찾기" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/53a094b8-fcab-480a-aaff-ee9bd2c38dc9" alt="로그인 배경 플로팅 이미지" width="#"></p>
  </details>

  <!-- 일반 유저: 홈/마켓 -->
<details>
  <summary><b>② 거래소 홈 및 거래소 시장 </b></summary>
    
<p><b>홈 화면</b></p>
<p>실시간 랭킹/히트맵: 추가 예정</p>
    
<p><b>주식 목록 및 랭킹</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/0de1fbab-c2fa-4df9-a6dd-fa0667dc6823" alt="조건별 순위" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/9846c3eb-8204-4d27-8692-24897d5f6bd4" alt="실시간 반영" width="#"></p>
    
<p><b>검색</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/b3e43b24-438b-4bee-84a8-00c9b1da4132" alt="전체 검색" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/fbc7ebac-38a5-477c-97b9-a02bc479e540" alt="종목 검색" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/7803cb58-8bec-4ad7-b928-f54c9e9e1625" alt="뉴스 검색" width="#"></p>
<p>공시 검색: 추가 예정</p>
  </details>

  <!-- 일반 유저: 거래 -->
<details>
<summary><b>③ 거래</b></summary>
    
<p><b>거래 화면</b></p>
<p>주문/호가/차트/체결: 추가 예정</p>
    
<p><b>주식 상세</b></p>
<p>종목 상세/재무제표/재무비율: 추가 예정</p>
    
<p><b>포트폴리오</b></p>
 <p align="center"><img src="https://github.com/user-attachments/assets/01dd7dd4-eb33-4708-a34d-c51c5da595cc" alt="보유 주식 실시간 변화" width="#"></p>
 <p>포트폴리오 상세: 추가 예정</p>
    
 <p><b>즐겨찾기</b></p>
 <p align="center"><img src="https://github.com/user-attachments/assets/7f1f5f04-e0fc-4d23-9977-4a1fb8de9ea1" alt="주식 즐겨찾기 해제" width="#"></p>
</details>

  <!-- 일반 유저: 공모/청약 -->
<details>
 <summary><b>④ 공모 및 청약</b></summary>
<blockquote>
※ 본 섹션은 ‘공모~상장’ 전체 흐름을 한눈에 보여주기 위해  
기업 관리자·거래소 관리자·일반 투자자 화면을 함께 배치했습니다.
</blockquote>
<!-- <p>공모 목록/필터/검색: 추가 예정</p> -->
    
<p><b>공모 신청</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/ce926fc0-bd2f-4097-8446-bbacb9a4212e" alt="공모 신청" width="#"></p>

<p><b>공모 승인</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/cc16afad-9143-4f47-9de1-6550ad1f0e0e" alt="거래소 관리자 공모 승인" width="#"></p>

<p><b>기관 투자자 수요예측 참여</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/1b2e32ca-18e0-4b38-95bf-4729f4b432f8" alt="기관 투자자 수요 예측 참여" width="#"></p>

<p><b>기업 투자자 공모 참여</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/78d750f9-af78-437a-8a04-45d3933b9165" alt="기업 투자자 공모 참여" width="#"></p>

<p><b>일반 투자자 공모 참여</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/befc1f52-d306-4641-bfb5-3e96edd58a77" alt="일반 투자자 공모 참여" width="#"></p>

<p><b>공모 주관 기업 청약 내역 확인</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/b683512d-1f71-47d6-abe2-28fddf5b8607" alt="공모 주관 기업 청약 내역 확인" width="#"></p>

<p><b>기업 투자자 공모 정산</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/97861c4d-4d73-4de0-9d96-55cfe8e00ca1" alt="기업 투자자 공모 정산" width="#"></p>

<p><b>일반 투자자 공모 정산</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/a18cea9f-4cc5-4e33-9218-c5fe9aa45901" alt="일반 투자자 공모 정산" width="#"></p>

<p><b>공모 주관 기업 청약 배정 실행</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/f7aec9ba-258f-4c3b-b67d-d0aafbf6caa2" alt="공모 주관 기업 청약 배정 실행" width="#"></p>

<p><b>발행사 송금 및 상장 승인</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/8cfa2cfc-6fc1-477f-98d0-14d0477c67cc" alt="발행사 송금 및 상장 승인" width="#"></p>

<p><b>보유 주식 추가</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/3bc29a10-cf5c-4572-8606-b02cbeadf00a" alt="일반 투자자 보유 주식 추가" width="#"></p>

<p><b>공모 주관 기업 입금 확인</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/4c6158a1-d494-4662-9ce0-8f4e3bf26bcd" alt="주관사 공모 금액 확인" width="#"></p>

<!-- <p><b>공모 상세 및 청약</b></p>
<p>공모 상세/청약 신청: 추가 예정</p>
    
<p><b>내 청약 내역</b></p>
<p>청약 목록/추가납입/환불: 추가 예정</p> -->
  </details>

<!-- 일반 유저: 뉴스/공시/커뮤니티 -->
<details>
 <summary><b>⑤ 뉴스/공시/커뮤니티</b></summary>
    
<p><b>뉴스</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/7491c743-9b60-4f1b-8a93-8dd46c533a3a" alt="전체 뉴스" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/78a33a5e-0360-4cdb-88d9-1e064ab8128f" alt="보유주식 뉴스" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/a073961c-26cf-496c-99aa-dd4608191123" alt="즐겨찾기 뉴스" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/aab79ec3-3ab6-42eb-93cf-53e0286d8595" alt="뉴스 공유 링크 자동 생성" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/c2846054-55de-4c2b-8f82-007192ec9192" alt="관련 주식" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/a2d55ef3-89b4-419c-9268-8031399c72ea" alt="인기순" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/baddde83-9376-49ce-af6b-dc3b6b117676" alt="뉴스 상세 조회" width="#"></p>
    
<p><b>공시</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/45a66ae7-9ebd-469b-8fe6-401e6225010a" alt="전체 공시" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/985a02a5-b1f9-4768-9a32-06617a764a8b" alt="보유주식 공시" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/925834ea-812a-4bd8-b855-a1b534d4969c" alt="즐겨찾기 공시" width="#"></p>
    
<p><b>커뮤니티</b></p>
<p>포럼/투표: 추가 예정</p>
</details>

  <!-- 일반 유저: 계좌/자산 -->
<details>
<summary><b>⑥ 계좌 및 자산</b></summary>
    
<p><b>계좌 관리</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/493e59d6-1ac6-45f2-9d51-b8033e8f079a" alt="입금" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/72dec4f5-e74a-4b26-91f1-f62818773ad4" alt="출금" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/43a33e62-7c25-4b71-8558-0e25463c3988" alt="계좌이체" width="#"></p>
    
<p><b>거래내역</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/260b011d-976c-4199-8934-eb4dd24fb217" alt="거래내역 카테고리별 필터" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/dcd11207-4e08-40c1-a8fa-b857842bea54" alt="거래내역 상세 조회 모달" width="#"></p>
<p>날짜별 조회: 추가 예정</p>
    
<p><b>내 정보</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/73375e95-b15a-44fb-a268-33c341c8cd9b" alt="내 정보 페이지" width="#"></p>
  </details>

  <!-- 기업 관리자 -->
<details>
<summary><b>⑦ 기업 관리자</b></summary>
    
<p><b>대시보드</b></p>
<p>대시보드: 추가 예정</p>
    
<p><b>직상장 신청</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/c5ce43b2-2577-4878-85fd-3b2a6bb34c3a" alt="직상장 신청" width="#"></p>

<p><b>공모 상장 신청</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/ed7752b0-2189-426f-8df3-900fba929154" alt="공모 상장 신청" width="#"></p>

<p><b>공모 신청</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/ce926fc0-bd2f-4097-8446-bbacb9a4212e" alt="공모 신청" width="#"></p>

<p><b>공모 주관 기업 청약 내역 확인</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/b683512d-1f71-47d6-abe2-28fddf5b8607" alt="공모 주관 기업 청약 내역 확인" width="#"></p>

<p><b>기업 투자자 공모 정산</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/97861c4d-4d73-4de0-9d96-55cfe8e00ca1" alt="기업 투자자 공모 정산" width="#"></p>

<p><b>기관 투자자 수요예측 참여</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/1b2e32ca-18e0-4b38-95bf-4729f4b432f8" alt="기관 투자자 수요 예측 참여" width="#"></p>

<p><b>기업 투자자 공모 참여</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/78d750f9-af78-437a-8a04-45d3933b9165" alt="기업 투자자 공모 참여" width="#"></p>
    
<p><b>공시 관리</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/e1b66a3f-e77e-49f7-8f5c-8caa12746c7e" alt="공시 목록 페이지 및 필터" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/ddf11478-db2a-41e1-8d94-23ccd5f9f0a2" alt="본공시" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/16859a87-65f2-45fc-af1e-a235bd658f52" alt="정정공시" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/b26f95e7-226f-444a-98f7-9b1d216f4f14" alt="추가공시" width="#"></p>
    
<p><b>주식 관리</b></p>
<p align="center"><img src="#" alt="주식 관리 페이지" width="#"></p>
<p>공시 재제출: 추가 예정</p>
    
<p><b>자산 관리</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/f4985708-472e-4d42-94ce-2c6067077c5a" alt="계좌 등록 신청" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/c7d60ce3-7e84-4d36-9937-f2b70967d023" alt="입금" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/c8d7debc-fb07-4134-a8b2-43291c5e316e" alt="출금" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/a264bd19-e4a0-4ec6-a6d7-e988ee8cdba2" alt="거래내역 조회" width="#"></p>
  </details>

  <!-- 증권사 관리자 -->
<details>
<summary><b>⑧ 증권사 관리자</b></summary>
    
<p><b>대시보드</b></p>
<p>대시보드 페이지: 추가 예정</p>
<p align="center"><img src="https://github.com/user-attachments/assets/2828caf8-4c30-4cb3-a5b9-b16ffd8f5ed1" alt="실시간 고객 활동 모달" width="#"></p>
    
<p><b>고객 관리</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/8734df66-a3b8-4e07-93fa-8ae0503b55dd" alt="고객 관리 페이지" width="#"></p>
    
<p><b>거래 내역</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/43be5fc1-f3f3-4304-9299-bcd07a4075d5" alt="거래 내역 조회" width="#"></p>
    
<p><b>수익 관리</b></p>
<p>수수료 관리/설정: 추가 예정</p>
    
<p><b>자금 관리</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/3de56f76-1344-4024-92ca-5ab1de2d2d2b" alt="입금" width="#"></p>
<p>출금/거래내역: 추가 예정</p>
  </details>

  <!-- 거래소 관리자 -->
<details>
<summary><b>⑨ 거래소 관리자</b></summary>
    
<p><b>대시보드</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/842737a9-11fc-402a-a1bf-d80a92a21b0e" alt="대시보드 페이지" width="#"></p>
    
<p><b>기업 관리</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/c2658d77-aed2-47b0-8b5d-d1ddec288992" alt="기업 등록 승인" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/2bf63c5c-3df9-45e6-9884-804e93593eb6" alt="기업 등록 거절" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/5c8548fb-9cba-404c-879f-41666ca915ac" alt="기업 가입 필터전체" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/10b995e5-a32f-44fc-bf0f-0da74189d0bd" alt="기업 상태별 필터" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/dd8cbd07-3064-4715-b87d-2ac69fea0e4f" alt="기업 가입 상장폐지 화면" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/4f253115-5586-4406-84d9-5e9268a70ffd" alt="기업 가입 거절필터 화면" width="#"></p>
    
<p><b>증권사 관리</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/96165c3b-7618-4c7a-8880-2c7a721a570e" alt="증권사 등록 승인" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/67b3c1d2-a411-4b37-8f79-0f353a0339aa" alt="증권사 등록 거절" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/1463cc14-2c2b-4ae8-82f1-5320a6e2835d" alt="증권사 상태별 필터" width="#"></p>
<p>증권사 검색/상세: 추가 예정</p>
    
<p><b>사용자 관리</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/eb805ef9-cff5-43b0-ad97-eca9a5430861" alt="사용자 관리 페이지" width="#"></p>
<p>사용자 상세/정지: 추가 예정</p>
    
<p><b>계좌 관리</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/0b5f17cc-0c3e-4e37-957a-6d8463021ebc" alt="계좌 관리 페이지" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/ac1e0537-961f-4418-a99a-d5e700a4a26b" alt="기업 계좌 관리 페이지" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/494b208f-6895-4a66-ac59-0473a1b189df" alt="증권사 계좌 관리 페이지" width="#"></p>
<p>계좌 등록 승인/거절/필터/검색: 추가 예정</p>
    
<p><b>IPO 관리</b></p>
<p>발행사 송금 및 상장 승인</p>
<p align="center"><img src="https://github.com/user-attachments/assets/8cfa2cfc-6fc1-477f-98d0-14d0477c67cc" alt="발행사 송금 및 상장 승인" width="#"></p>

<p>IPO 거절/필터/검색: 추가 예정</p>

<p><b>공모 승인</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/cc16afad-9143-4f47-9de1-6550ad1f0e0e" alt="거래소 관리자 공모 승인" width="#"></p>
<p>공모 요청 거절/필터/검색/배정 심사·확정: 추가 예정</p>

    
<p><b>공시 승인</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/818a7b5b-ad5e-4d0e-9d91-e9faee838e1a" alt="공시 승인 페이지" width="#"></p>
<p>공시 거절/사유/필터/검색: 추가 예정</p>
    
<p><b>상장 주식 관리</b></p>
<p>통합관리/필터/검색: 추가 예정</p>
<p>폐지 위험/진행/종료 관리: 추가 예정</p>
<p>기준 관리: 추가 예정</p>
    
<p><b>거래소 자금 관리</b></p>
<p align="center"><img src="https://github.com/user-attachments/assets/1b2a6c88-6382-414b-897b-ac3e46142eb2" alt="거래소 입금" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/8f72ef19-5792-43de-b775-f0f28805df9a" alt="거래소 출금" width="#"></p>
<p align="center"><img src="https://github.com/user-attachments/assets/59576687-e531-4426-a8b9-9aa96d686333" alt="최근 입금 내역" width="#"></p>
  </details>
</section>


<section id="ui-ux-test">
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



<!--
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
-->
  <hr/>

  <footer class="muted" style="margin-top:28px">
    <p align="right"> © MKX. All rights reserved. </p>
  </footer>

</main>
</body>
</html>

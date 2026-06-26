# IDE 전체 코드 에러 표시 원인 및 해결 정리

## 발생 상황

IntelliJ에서 프로젝트를 열었을 때 전체 코드에 빨간 줄이 표시되는 문제가 발생했다.

처음에는 코드 컴파일 문제처럼 보였지만, 실제 Gradle 빌드를 확인한 결과 각 서비스의 Java 컴파일은 정상적으로 성공했다.

## 확인한 내용

### 1. 실제 컴파일 결과

아래 서비스들의 `compileJava` 작업을 각각 실행해 확인했다.

- `apigateway`
- `community`
- `eureka`
- `marketdata`
- `matching-engine`
- `mkx-platform`
- `ordering`
- `trading-bot`

모든 서비스에서 `BUILD SUCCESSFUL`이 확인되었다.

따라서 문제는 Java 코드 자체의 컴파일 에러가 아니라 IDE 프로젝트 인식 문제로 판단했다.

### 2. 프로젝트 구조

현재 저장소는 루트 하나의 Gradle 멀티모듈 프로젝트가 아니라, 서비스별로 독립된 Gradle 프로젝트가 존재하는 구조다.

예시:

```text
apigateway/settings.gradle
community/settings.gradle
eureka/settings.gradle
marketdata/settings.gradle
matching-engine/settings.gradle
mkx-platform/settings.gradle
ordering/settings.gradle
trading-bot/settings.gradle
```

하지만 루트 디렉터리에는 `settings.gradle`이 없었다.

이 상태에서 IntelliJ가 루트 폴더 `MKX-Backend`를 열면, 각 서비스의 Gradle 의존성을 제대로 연결하지 못하고 단순 Java 프로젝트처럼 인식할 수 있다.

그 결과 Spring, Lombok, JPA 등 외부 의존성 import가 전부 깨져 보이면서 전체 코드에 에러 줄이 표시될 수 있다.

### 3. IntelliJ 설정 상태

루트 `.idea/modules.xml`에는 실제 서비스 모듈들이 아니라 빈 Java 모듈 하나만 등록되어 있었다.

```text
.idea/MKX-Backend.iml
```

해당 `.iml` 파일도 소스 폴더나 Gradle 라이브러리 정보를 가지고 있지 않았다.

즉, IntelliJ가 Gradle 프로젝트로 동기화된 상태가 아니었다.

## 적용한 해결

### 1. 루트 `settings.gradle` 추가

루트에서 모든 서비스를 Gradle composite build로 인식할 수 있도록 `settings.gradle`을 추가했다.

```gradle
rootProject.name = 'mkx-backend'

includeBuild 'apigateway'
includeBuild 'community'
includeBuild 'eureka'
includeBuild 'marketdata'
includeBuild 'matching-engine'
includeBuild 'mkx-platform'
includeBuild 'ordering'
includeBuild 'trading-bot'
```

이 설정을 통해 루트 폴더를 열어도 IntelliJ와 Gradle이 각 서비스를 included build로 인식할 수 있다.

### 2. 루트 Gradle 인식 확인

다음 명령으로 루트에서 included build가 정상 인식되는지 확인했다.

```bash
apigateway/gradlew -p . projects
```

결과:

```text
Included builds:

+--- Included build ':apigateway'
+--- Included build ':community'
+--- Included build ':eureka'
+--- Included build ':marketdata'
+--- Included build ':matching-engine'
+--- Included build ':mkx-platform'
+--- Included build ':ordering'
\--- Included build ':trading-bot'
```

루트 Gradle 설정이 정상적으로 동작하는 것을 확인했다.

### 3. `.gitignore` 정리

Gradle 로컬 캐시와 빌드 결과물이 Git에 새로 잡히지 않도록 루트 `.gitignore`에 다음 항목을 추가했다.

```gitignore
.gradle/
**/.gradle/
build/
**/build/
```

## 남아있는 참고 사항

`eureka/.gradle/...` 파일들은 이미 Git 추적 대상에 들어가 있어서, `.gitignore`를 추가해도 기존 변경사항에는 계속 표시된다.

예시:

```text
eureka/.gradle/8.14.3/executionHistory/executionHistory.bin
eureka/.gradle/8.14.3/fileHashes/fileHashes.bin
eureka/.gradle/buildOutputCleanup/outputFiles.bin
```

이 파일들은 코드 파일이 아니라 Gradle 로컬 캐시 파일이다.

추후 별도 작업으로 Git 추적 대상에서 제거하는 것이 좋다.

## IntelliJ에서 해야 할 조치

변경사항 적용 후 IntelliJ에서 아래 중 하나를 수행하면 된다.

1. 우측 Gradle 탭에서 Refresh 실행
2. 프로젝트를 닫고 루트 폴더 `MKX-Backend`를 다시 열기
3. 그래도 빨간 줄이 남아 있으면 `File > Invalidate Caches... > Invalidate and Restart` 실행

## 결론

전체 코드에 표시되던 에러 줄의 원인은 코드 컴파일 실패가 아니라 루트 프로젝트의 Gradle 인식 문제였다.

루트 `settings.gradle`을 추가해 서비스별 Gradle 프로젝트를 composite build로 연결했고, Gradle 기준으로 included build 인식 및 각 서비스 컴파일 성공을 확인했다.

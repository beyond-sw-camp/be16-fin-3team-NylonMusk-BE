# syntax=docker/dockerfile:1
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# gradle wrapper + 소스 복사
COPY gradle gradle
COPY gradlew .
COPY build.gradle settings.gradle ./
COPY src src

# 캐시 없이도 동작. (원하면 --no-daemon 붙이기)
RUN chmod +x gradlew && ./gradlew --no-daemon clean bootJar

# 런타임은 JRE만
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]


# 도커 이미지 빌드
# docker build -t ordersystem:v1.0 .
# 도커컨테이너 실행
# docker 내부에서 localhost를 찾는 설정은 루프백 문제 발생
# docker run --name my-ordersystem -d -p 8080:8080 ordersystem:v1.0

# 도커컨테이너 실행시점에 docker.host.internal을 환경변수로 주입
# docker run --name my-ordersystem -d -p 8080:8080 
#                   -e SPRING_REDIS_HOST = host.docker.internal 
#                   -e SPRING_DATASOURCE_URL=jdbc:mariadb://host.docker.internal:3307/ordersystem
#                       ordersystem:v1.0
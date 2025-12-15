# =====================================================
# NEXUS Java 백엔드 - 멀티 스테이지 Dockerfile
# Spring Boot 3.x + Java 17
# AWS ECS/ECR 배포 최적화 (ARM64/AMD64 호환)
# =====================================================

# -----------------------------------------------------
# 1단계: 빌드
# -----------------------------------------------------
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Maven 설치
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# 의존성 먼저 다운로드 (레이어 캐싱 최적화)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 소스 복사 및 빌드
COPY src ./src

# JAR 파일 빌드 (테스트 스킵)
RUN mvn clean package -DskipTests -B

# -----------------------------------------------------
# 2단계: 프로덕션
# -----------------------------------------------------
FROM eclipse-temurin:17-jre AS production

WORKDIR /app

# 필요한 패키지 설치
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# 한국 시간대 설정
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 보안을 위한 non-root 사용자 생성
RUN groupadd -g 1001 appgroup && \
    useradd -r -u 1001 -g appgroup -d /app -s /sbin/nologin appuser

# 빌드된 JAR 파일 복사
COPY --from=builder /app/target/*.jar app.jar

# 업로드 디렉토리 생성 및 권한 설정
RUN mkdir -p /app/uploads/documents && \
    chown -R appuser:appgroup /app

# non-root 사용자로 전환
USER appuser

# 환경변수 기본값 설정
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=3000

# 3000 포트 노출
EXPOSE 3000

# 헬스체크 설정
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:3000/api/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

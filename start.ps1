# NEXUS Java Backend 시작 스크립트
# Spring Boot 애플리케이션을 빌드하고 실행합니다

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  NEXUS Java Backend 시작 중..." -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# 현재 스크립트 위치로 이동
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptPath

# Java 버전 확인
Write-Host "[1/5] Java 버전 확인 중..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-String "version" | ForEach-Object { $_.ToString() }
    Write-Host "✓ $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ Java가 설치되어 있지 않습니다!" -ForegroundColor Red
    Write-Host "  Java 17 이상을 설치해주세요." -ForegroundColor Red
    exit 1
}

# Maven 버전 확인
Write-Host "`n[2/5] Maven 버전 확인 중..." -ForegroundColor Yellow
try {
    $mavenVersion = mvn -version | Select-String "Apache Maven" | ForEach-Object { $_.ToString() }
    Write-Host "✓ $mavenVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ Maven이 설치되어 있지 않습니다!" -ForegroundColor Red
    Write-Host "  Maven을 설치해주세요." -ForegroundColor Red
    exit 1
}

# .env 파일 확인
Write-Host "`n[3/5] 환경변수 파일 확인 중..." -ForegroundColor Yellow
if (Test-Path ".env") {
    Write-Host "✓ .env 파일이 존재합니다." -ForegroundColor Green
} else {
    Write-Host "✗ .env 파일이 없습니다!" -ForegroundColor Red
    Write-Host "  .env.example을 복사하여 .env 파일을 생성하고 환경변수를 설정해주세요." -ForegroundColor Red
    exit 1
}

# PostgreSQL 연결 확인 (선택사항)
Write-Host "`n[4/5] 데이터베이스 연결 확인 중..." -ForegroundColor Yellow
Write-Host "  (PostgreSQL 연결 실패 시에도 계속 진행됩니다)" -ForegroundColor Gray

# 의존성 설치 및 빌드
Write-Host "`n[5/5] Maven 의존성 설치 및 빌드 중..." -ForegroundColor Yellow
Write-Host "  (처음 실행 시 시간이 오래 걸릴 수 있습니다)" -ForegroundColor Gray

$buildProcess = Start-Process -FilePath "mvn" -ArgumentList "clean", "install", "-DskipTests" -NoNewWindow -PassThru -Wait

if ($buildProcess.ExitCode -eq 0) {
    Write-Host "✓ 빌드 완료!" -ForegroundColor Green
} else {
    Write-Host "✗ 빌드 실패!" -ForegroundColor Red
    Write-Host "  Maven 빌드 중 오류가 발생했습니다." -ForegroundColor Red
    exit 1
}

# Spring Boot 애플리케이션 실행
Write-Host "`n=====================================" -ForegroundColor Cyan
Write-Host "  Spring Boot 애플리케이션 실행 중..." -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "포트: 3000" -ForegroundColor Green
Write-Host "API: http://localhost:3000/api" -ForegroundColor Green
Write-Host ""
Write-Host "종료하려면 Ctrl+C를 누르세요." -ForegroundColor Yellow
Write-Host ""

# Spring Boot 실행
mvn spring-boot:run

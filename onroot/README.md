# OnRoot AI Backend

LLM 기반 자격증 학습 플래너 백엔드 서버

## 기술 스택

- **Java 21 / Spring Boot 3.5**
- **MySQL 8**
- **Gemini API** (gemini-2.5-flash-lite) — 학습 계획 생성
- **Q-Net 공공 API** — 국가기술자격 시험 일정 동기화

## 주요 도메인

| 도메인 | 설명 |
|---|---|
| `ai` | Gemini 기반 학습 플랜 자동 생성 |
| `plan` | 생성된 학습 플랜 관리 |
| `task` | 플랜 내 일별 학습 태스크 |
| `examschedule` | Q-Net 시험 일정 데이터 |
| `dday` | D-Day 관리 |
| `streak` | 학습 연속 달성 기록 |
| `user` | 사용자 인증/관리 |

## 실행 방법

```bash
# 환경변수 설정 (application-local.properties)
DB_PASSWORD=...
QNET_API_KEY=...
GEMINI_API_KEY=...

# 빌드 및 실행
./gradlew build -x test
java -jar build/libs/onroot-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

## AI 플랜 생성 로직

`POST /api/ai/generate` — 사용자 자연어 입력을 받아 일별 학습 태스크를 생성합니다.

### 시험 일정 매핑 규칙

사용자가 입력한 목표 기간과 DB에 저장된 Q-Net 시험 일정을 비교해 플랜 유형을 결정합니다.

| 입력 패턴 | 동작 |
|---|---|
| `"정처기 필기 합격하고싶어"` | 필기 시험 일정만 매핑 → 필기일까지 필기 과목 플랜 |
| `"정처기 실기 합격하고싶어"` | 실기 시험 일정만 매핑 → 실기일까지 실기 과목 플랜 (필기 합격 가정) |
| `"정처기 합격하고싶어"` | 실기→필기 순으로 매핑. 목표일 ±14일 이내 시험이 없으면 일반 학습 계획 생성 |

- 실기는 잡혔으나 그 앞 필기 일정이 없으면 일반 학습 계획으로 fallback (필기 없이 실기 불가)
- 오차 허용 범위: **±14일**

### 학습 시간 파싱

자연어에서 평일/주말 가용 시간을 추출합니다. 조사("에", "에는" 등)가 붙어도 정상 파싱됩니다.

- `"평일에 2시간 주말에 5시간"` → 평일 2h / 주말 5h
- `"하루 3시간"` → 평일·주말 모두 3h
- 미입력 시 기본값: 평일 2h / 주말 2h

### RAG (과목 주입)

`src/main/resources/exam-subjects.json`에 125개 자격증의 필기/실기 과목이 정의되어 있으며, 플랜 생성 시 해당 자격증 과목 목록이 Gemini 프롬프트에 자동 주입됩니다.

## API 문서

서버 실행 후 Swagger UI: `http://localhost:8080/swagger-ui/index.html`

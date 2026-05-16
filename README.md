# OnRoot AI Backend

LLM 기반 자격증 학습 플래너 백엔드 서버

## 기술 스택

- Java 21 / Spring Boot 3.5
- MySQL / Spring Data JPA
- Gemini API (Google AI)
- Q-Net 공공데이터 API
- Swagger (SpringDoc)

## 로컬 실행 방법

### 1. 설정 파일 준비

```bash
cp src/main/resources/application-local.properties.example \
   src/main/resources/application-local.properties
```

`application-local.properties`에 아래 값을 채워주세요:

| 키 | 설명 | 발급처 |
|---|---|---|
| `DB_PASSWORD` | MySQL 비밀번호 | 로컬 DB |
| `QNET_API_KEY` | Q-Net 공공데이터 API 키 | [data.go.kr](https://www.data.go.kr) |
| `GEMINI_API_KEY` | Gemini API 키 | [Google AI Studio](https://aistudio.google.com) |

### 2. Active Profile 설정

IntelliJ → Run Configuration → Active profiles: `local`

또는 실행 시 VM 옵션:
```
-Dspring.profiles.active=local
```

### 3. DB 준비

```sql
CREATE DATABASE OnRoot CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

서버 실행 시 JPA `ddl-auto=update`로 테이블 자동 생성됩니다.

---

## 주요 기능 및 로직

### 시험 일정 동기화 (`POST /api/exam-schedules/sync`)

Q-Net 공공데이터 API에서 자격증 시험 일정을 가져와 DB에 저장합니다.

```
Q-Net API 호출
  └── getEList  → 기사 계열 회차별 필기/실기 일정
  └── getPEList → 기술사 계열 회차별 필기/실기 일정
        ↓
ExamSchedule 테이블에 저장
  - examName: "정보처리기사 필기 3회"
  - examDate, applicationStart, applicationEnd, resultDate
```

> 동기화 전 기존 Plan의 시험 일정 FK를 null로 초기화 후 전체 재저장합니다.

---

### AI 학습 계획 생성 (`POST /api/ai/generate`)

사용자 자연어 입력을 받아 개인화된 일별 학습 계획을 생성합니다.

#### 전체 흐름

```
유저 입력: "정처기 6개월 합격 루틴 평일 2시간 주말 5시간"
        ↓
1. 파싱
   - 기간 추출: "6개월" → targetDate = 오늘 + 6개월
   - 가용 시간 추출: 평일 2시간, 주말 5시간
   - 시험 키워드 추출: "정처기" → EXAM_ALIAS → "정보처리기사"

2. DB에서 시험 일정 조회
   - "정보처리기사" 포함 & 오늘 이후 일정 조회
   - 필기: targetDate 이전 중 가장 늦은 것 → 3회차 (8/7)
   - 실기: targetDate 이전 중 가장 늦은 것 → 3회차 (10/24)
   - targetDate를 실기 시험일(10/24)로 재조정

3. Gemini에 프롬프트 전송
   - 시험 일정, 기간, 가용 시간 포함
   - AI는 날짜별 task가 아닌 세그먼트(구간별 주제) 단위로 반환
   [
     { startDate, endDate, weekdayTopics: [월~금 소주제 5개], weekendTopic }
   ]

4. 서버에서 일별 task 확장
   오늘 ~ 실기 시험일까지 날짜를 순회하며 task 생성:
   - 시험일        → "정보처리기사 필기 3회 응시"
   - 원서접수 기간  → "필기 원서접수" / "실기 원서접수"
   - 주말          → weekendTopic + (5시간)
   - 평일          → weekdayTopics[요일 % 5] + (2시간)
   세그먼트가 커버 안 하는 날도 fallback 주제로 채워 날짜 누락 방지

5. DB 저장 및 응답
   Plan (weekdayHours, weekendHours, writtenExamSchedule, practicalExamSchedule 포함)
   Task (매일 1개, 총 시작일~시험일 일수만큼)
   AiGenerationLog (원본 프롬프트·응답 보관)
```

#### 시험 키워드 별칭 (EXAM_ALIAS)

| 입력 | 매핑 |
|---|---|
| 정처기 | 정보처리기사 |
| 정산기 | 정보처리산업기사 |
| 전기산기 | 전기산업기사 |
| 산안기 | 산업안전기사 |

#### 가용 시간 파싱 규칙

| 입력 예시 | weekdayHours | weekendHours |
|---|---|---|
| `평일 2시간 주말 5시간` | 2 | 5 |
| `하루 3시간` | 3 | 3 |
| (미입력) | 2 | 2 |

---

## API 문서

서버 실행 후 Swagger UI: `http://localhost:8080/swagger-ui/index.html`
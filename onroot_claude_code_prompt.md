# OnRoot 프로젝트 개발 컨텍스트

## 프로젝트 개요
- LLM을 이용해 AI 학습 계획을 생성해주는 플래너 앱의 백엔드
- Spring Boot 3.5.14 / Java 21 / Gradle / MySQL
- 패키지명: com.OnRoot.onroot

## ERD (테이블 구조)

```sql
CREATE TABLE `USER` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT,
    `email`         VARCHAR(100)    NOT NULL,
    `password_hash` VARCHAR(255)    NOT NULL,
    `nickname`      VARCHAR(50)     NOT NULL,
    `provider`      VARCHAR(20)     NOT NULL DEFAULT 'local', -- local/kakao/google
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_user_email` (`email`)
);

CREATE TABLE `EXAM_SCHEDULE` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT,
    `exam_name`         VARCHAR(100)    NOT NULL,
    `subject`           VARCHAR(100)    NOT NULL,
    `application_start` DATE,
    `application_end`   DATE,
    `exam_date`         DATE            NOT NULL,
    `result_date`       DATE,
    PRIMARY KEY (`id`)
);

CREATE TABLE `PLAN` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT,
    `user_id`           BIGINT          NOT NULL,
    `exam_schedule_id`  BIGINT          NULL,
    `title`             VARCHAR(100)    NOT NULL,
    `category`          VARCHAR(20)     NOT NULL DEFAULT '기타', -- 자격증/취업/어학/기타
    `target_date`       DATE            NOT NULL,
    `status`            VARCHAR(20)     NOT NULL DEFAULT 'IN_PROGRESS', -- IN_PROGRESS/COMPLETED/ABANDONED
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`user_id`) REFERENCES `USER`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`exam_schedule_id`) REFERENCES `EXAM_SCHEDULE`(`id`) ON DELETE SET NULL
);

CREATE TABLE `TASK` (
    `id`             BIGINT          NOT NULL AUTO_INCREMENT,
    `plan_id`        BIGINT          NOT NULL,
    `title`          VARCHAR(200)    NOT NULL,
    `scheduled_date` DATE            NOT NULL,
    `order_index`    INT             NOT NULL DEFAULT 0,
    `completed_at`   DATETIME        NULL, -- NULL이면 미완료, 값 있으면 완료
    PRIMARY KEY (`id`),
    FOREIGN KEY (`plan_id`) REFERENCES `PLAN`(`id`) ON DELETE CASCADE
);

CREATE TABLE `DDAY` (
    `id`          BIGINT          NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT          NOT NULL,
    `title`       VARCHAR(100)    NOT NULL,
    `target_date` DATE            NOT NULL,
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`user_id`) REFERENCES `USER`(`id`) ON DELETE CASCADE
);

CREATE TABLE `STREAK` (
    `id`                 BIGINT          NOT NULL AUTO_INCREMENT,
    `user_id`            BIGINT          NOT NULL,
    `current_streak`     INT             NOT NULL DEFAULT 0,
    `last_activity_date` DATE            NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_streak_user` (`user_id`),
    FOREIGN KEY (`user_id`) REFERENCES `USER`(`id`) ON DELETE CASCADE
);

CREATE TABLE `AI_GENERATION_LOG` (
    `id`             BIGINT          NOT NULL AUTO_INCREMENT,
    `user_id`        BIGINT          NOT NULL,
    `plan_id`        BIGINT          NULL,
    `user_input`     TEXT            NOT NULL,
    `generated_json` JSON            NOT NULL,
    `llm_model`      VARCHAR(50)     NOT NULL,
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`user_id`) REFERENCES `USER`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`plan_id`) REFERENCES `PLAN`(`id`) ON DELETE SET NULL
);
```

## 공통 규칙
- 모든 API는 `/api` prefix 사용
- 로그인 필요한 API는 Header에 `Authorization: Bearer {accessToken}` 포함
- 공통 에러 응답 형식: `{ "message": "에러 메시지" }`
- PLAN status는 Java enum으로 관리 (IN_PROGRESS / COMPLETED / ABANDONED)
- TASK 완료 여부는 completed_at NULL 여부로 판단 (is_completed 컬럼 없음)

## API 명세

### USER

#### 회원가입
- Method: POST
- URL: /api/users/signup
- 설명: 이메일, 비밀번호, 닉네임을 입력받아 신규 사용자를 등록합니다.
- Request Body:
```json
{
  "email": "onroot@gmail.com",
  "password": "password123",
  "nickname": "온루트"
}
```
- Response 201:
```json
{
  "id": 1,
  "email": "onroot@gmail.com",
  "nickname": "온루트"
}
```
- Response 400: `{ "message": "모든 필드는 필수 입력값입니다." }`
- Response 409: `{ "message": "이미 사용 중인 이메일입니다." }`

#### 로그인
- Method: POST
- URL: /api/users/login
- 설명: 이메일과 비밀번호를 입력받아 로그인하고 액세스 토큰을 반환합니다.
- Request Body:
```json
{
  "email": "onroot@gmail.com",
  "password": "password123"
}
```
- Response 200:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```
- Response 400: `{ "message": "모든 필드는 필수 입력값입니다." }`
- Response 401: `{ "message": "이메일 또는 비밀번호가 올바르지 않습니다." }`

#### 내 정보 조회
- Method: GET
- URL: /api/users/me
- 설명: 로그인한 사용자의 정보를 반환합니다.
- Response 200:
```json
{
  "id": 1,
  "email": "onroot@gmail.com",
  "nickname": "온루트",
  "provider": "local",
  "createdAt": "2026-05-15T10:00:00"
}
```
- Response 401: `{ "message": "로그인이 필요합니다." }`

#### 내 정보 수정
- Method: PATCH
- URL: /api/users/me
- 설명: 로그인한 사용자의 닉네임을 수정합니다.
- Request Body:
```json
{
  "nickname": "새닉네임"
}
```
- Response 200:
```json
{
  "id": 1,
  "nickname": "새닉네임"
}
```
- Response 400: `{ "message": "모든 필드는 필수 입력값입니다." }`
- Response 401: `{ "message": "로그인이 필요합니다." }`

#### 회원 탈퇴
- Method: DELETE
- URL: /api/users/me
- 설명: 로그인한 사용자의 계정을 삭제합니다.
- Response 204: {}
- Response 401: `{ "message": "로그인이 필요합니다." }`

---

### PLAN

#### 계획 생성
- Method: POST
- URL: /api/plans
- 설명: AI에게 학습 목표를 입력받아 계획과 세부 할일을 생성합니다.
- Request Body:
```json
{
  "userInput": "정보처리기사, 2개월, 하루 2시간",
  "category": "자격증",
  "targetDate": "2026-08-31",
  "examScheduleId": 1
}
```
- Response 201:
```json
{
  "id": 1,
  "title": "정보처리기사 취득",
  "category": "자격증",
  "targetDate": "2026-08-31",
  "status": "IN_PROGRESS",
  "createdAt": "2026-05-15T10:00:00"
}
```
- Response 400: `{ "message": "모든 필드는 필수 입력값입니다." }`
- Response 401: `{ "message": "로그인이 필요합니다." }`

#### 계획 목록 조회
- Method: GET
- URL: /api/plans
- 설명: 로그인한 사용자의 전체 계획 목록을 반환합니다.
- Response 200:
```json
[
  {
    "id": 1,
    "title": "정보처리기사 취득",
    "category": "자격증",
    "targetDate": "2026-08-31",
    "status": "IN_PROGRESS",
    "createdAt": "2026-05-15T10:00:00"
  }
]
```
- Response 401: `{ "message": "로그인이 필요합니다." }`

#### 계획 단건 조회
- Method: GET
- URL: /api/plans/{planId}
- 설명: 특정 계획의 상세 정보와 세부 할일 목록을 반환합니다.
- Response 200:
```json
{
  "id": 1,
  "title": "정보처리기사 취득",
  "category": "자격증",
  "targetDate": "2026-08-31",
  "status": "IN_PROGRESS",
  "createdAt": "2026-05-15T10:00:00",
  "tasks": [
    {
      "id": 1,
      "title": "필기 이론 학습",
      "scheduledDate": "2026-05-20",
      "completedAt": null,
      "orderIndex": 1
    }
  ]
}
```
- Response 401: `{ "message": "로그인이 필요합니다." }`
- Response 404: `{ "message": "계획을 찾을 수 없습니다." }`

#### 계획 수정
- Method: PATCH
- URL: /api/plans/{planId}
- 설명: 특정 계획의 제목, 카테고리, 목표 완료일, 상태를 수정합니다.
- Request Body:
```json
{
  "title": "정보처리기사 취득",
  "category": "자격증",
  "targetDate": "2026-08-31",
  "status": "COMPLETED"
}
```
- Response 200:
```json
{
  "id": 1,
  "title": "정보처리기사 취득",
  "category": "자격증",
  "targetDate": "2026-08-31",
  "status": "COMPLETED",
  "createdAt": "2026-05-15T10:00:00"
}
```
- Response 401: `{ "message": "로그인이 필요합니다." }`
- Response 404: `{ "message": "계획을 찾을 수 없습니다." }`

#### 계획 삭제
- Method: DELETE
- URL: /api/plans/{planId}
- 설명: 특정 계획과 하위 할일을 모두 삭제합니다.
- Response 204: {}
- Response 401: `{ "message": "로그인이 필요합니다." }`
- Response 404: `{ "message": "계획을 찾을 수 없습니다." }`

---

### TASK

#### TASK 생성
- Method: POST
- URL: /api/plans/{planId}/tasks
- 설명: 특정 계획에 세부 할일을 추가합니다.
- Request Body:
```json
{
  "title": "필기 이론 학습",
  "scheduledDate": "2026-05-20",
  "orderIndex": 1
}
```
- Response 201:
```json
{
  "id": 1,
  "planId": 1,
  "title": "필기 이론 학습",
  "scheduledDate": "2026-05-20",
  "completedAt": null,
  "orderIndex": 1
}
```
- Response 400: `{ "message": "모든 필드는 필수 입력값입니다." }`
- Response 401: `{ "message": "로그인이 필요합니다." }`
- Response 404: `{ "message": "계획을 찾을 수 없습니다." }`

#### TASK 목록 조회
- Method: GET
- URL: /api/plans/{planId}/tasks
- 설명: 특정 계획의 세부 할일 목록을 반환합니다.
- Response 200:
```json
[
  {
    "id": 1,
    "planId": 1,
    "title": "필기 이론 학습",
    "scheduledDate": "2026-05-20",
    "completedAt": null,
    "orderIndex": 1
  }
]
```
- Response 401: `{ "message": "로그인이 필요합니다." }`
- Response 404: `{ "message": "계획을 찾을 수 없습니다." }`

#### TASK 완료 처리
- Method: PATCH
- URL: /api/plans/{planId}/tasks/{taskId}/complete
- 설명: 특정 할일을 완료 처리합니다. 완료 시 completedAt에 현재 시각이 저장되고 STREAK이 갱신됩니다.
- Response 200:
```json
{
  "id": 1,
  "planId": 1,
  "title": "필기 이론 학습",
  "scheduledDate": "2026-05-20",
  "completedAt": "2026-05-20T14:00:00",
  "orderIndex": 1
}
```
- Response 401: `{ "message": "로그인이 필요합니다." }`
- Response 404: `{ "message": "할일을 찾을 수 없습니다." }`

#### TASK 수정
- Method: PATCH
- URL: /api/plans/{planId}/tasks/{taskId}
- 설명: 특정 할일의 제목, 예정 날짜, 정렬 순서를 수정합니다.
- Request Body:
```json
{
  "title": "필기 이론 학습",
  "scheduledDate": "2026-05-25",
  "orderIndex": 1
}
```
- Response 200:
```json
{
  "id": 1,
  "planId": 1,
  "title": "필기 이론 학습",
  "scheduledDate": "2026-05-25",
  "completedAt": null,
  "orderIndex": 1
}
```
- Response 401: `{ "message": "로그인이 필요합니다." }`
- Response 404: `{ "message": "할일을 찾을 수 없습니다." }`

#### TASK 삭제
- Method: DELETE
- URL: /api/plans/{planId}/tasks/{taskId}
- 설명: 특정 할일을 삭제합니다.
- Response 204: {}
- Response 401: `{ "message": "로그인이 필요합니다." }`
- Response 404: `{ "message": "할일을 찾을 수 없습니다." }`

---

### DDAY

#### D-day 등록
- Method: POST
- URL: /api/ddays
- 설명: 새로운 D-day 항목을 등록합니다.
- Request Body:
```json
{
  "title": "카카오 면접",
  "targetDate": "2026-06-01"
}
```
- Response 201:
```json
{
  "id": 1,
  "title": "카카오 면접",
  "targetDate": "2026-06-01",
  "dDay": -17,
  "createdAt": "2026-05-15T10:00:00"
}
```
- Response 400: `{ "message": "모든 필드는 필수 입력값입니다." }`
- Response 401: `{ "message": "로그인이 필요합니다." }`

#### D-day 목록 조회
- Method: GET
- URL: /api/ddays
- 설명: 로그인한 사용자의 전체 D-day 목록을 반환합니다. dDay 값은 백엔드에서 계산하여 반환합니다.
- Response 200:
```json
[
  {
    "id": 1,
    "title": "카카오 면접",
    "targetDate": "2026-06-01",
    "dDay": -17,
    "createdAt": "2026-05-15T10:00:00"
  }
]
```
- Response 401: `{ "message": "로그인이 필요합니다." }`

#### D-day 수정
- Method: PATCH
- URL: /api/ddays/{ddayId}
- 설명: 특정 D-day의 제목과 목표 날짜를 수정합니다.
- Request Body:
```json
{
  "title": "카카오 면접",
  "targetDate": "2026-06-05"
}
```
- Response 200:
```json
{
  "id": 1,
  "title": "카카오 면접",
  "targetDate": "2026-06-05",
  "dDay": -21,
  "createdAt": "2026-05-15T10:00:00"
}
```
- Response 401: `{ "message": "로그인이 필요합니다." }`
- Response 404: `{ "message": "D-day를 찾을 수 없습니다." }`

#### D-day 삭제
- Method: DELETE
- URL: /api/ddays/{ddayId}
- 설명: 특정 D-day 항목을 삭제합니다.
- Response 204: {}
- Response 401: `{ "message": "로그인이 필요합니다." }`
- Response 404: `{ "message": "D-day를 찾을 수 없습니다." }`

---

### EXAM_SCHEDULE

#### 시험 일정 목록 조회
- Method: GET
- URL: /api/exam-schedules
- 설명: 공공데이터포털에서 동기화된 국가시험 일정 목록을 반환합니다.
- Response 200:
```json
[
  {
    "id": 1,
    "examName": "정보처리기사",
    "subject": "IT",
    "applicationStart": "2026-06-01",
    "applicationEnd": "2026-06-10",
    "examDate": "2026-07-15",
    "resultDate": "2026-08-20"
  }
]
```
- Response 401: `{ "message": "로그인이 필요합니다." }`

---

### STREAK

#### STREAK 조회
- Method: GET
- URL: /api/streaks
- 설명: 로그인한 사용자의 연속 학습 현황을 반환합니다.
- Response 200:
```json
{
  "id": 1,
  "currentStreak": 5,
  "lastActivityDate": "2026-05-15"
}
```
- Response 401: `{ "message": "로그인이 필요합니다." }`

---

### AI

#### AI 계획 생성
- Method: POST
- URL: /api/ai/generate
- 설명: 사용자의 목표를 입력받아 AI가 학습 계획을 생성하고 PLAN과 TASK를 저장합니다.
- Request Body:
```json
{
  "userInput": "정보처리기사, 2개월, 하루 2시간",
  "category": "자격증",
  "targetDate": "2026-08-31",
  "examScheduleId": 1
}
```
- Response 201:
```json
{
  "logId": 1,
  "planId": 1,
  "title": "정보처리기사 취득",
  "tasks": [
    {
      "id": 1,
      "title": "필기 이론 학습",
      "scheduledDate": "2026-05-20",
      "orderIndex": 1
    }
  ]
}
```
- Response 400: `{ "message": "모든 필드는 필수 입력값입니다." }`
- Response 401: `{ "message": "로그인이 필요합니다." }`
- Response 502: `{ "message": "AI 서비스 호출에 실패했습니다. 잠시 후 다시 시도해주세요." }`

-- insert_dummy_user.sql
-- DB: OnRoot
-- 더미 사용자(id=1)를 삽입합니다. 이미 존재하면 아무 작업도 하지 않습니다.

-- 방법 1: 존재 여부 확인 후 삽입 (안전)
INSERT INTO `USER` (id, email, password_hash, nickname, provider, created_at)
SELECT 1, 'demo@onroot.local', 'dummy-password-hash', 'demo', 'local', NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `USER` WHERE id = 1);

-- 방법 2: 덮어쓰기 (필요 시 사용)
-- REPLACE INTO `USER` (id, email, password_hash, nickname, provider, created_at)
-- VALUES (1, 'demo@onroot.local', 'dummy-password-hash', 'demo', 'local', NOW());

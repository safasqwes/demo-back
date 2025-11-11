-- Function Usage Log Table
-- This table tracks all function usage by users (both authenticated and guest users)
-- Note: Function configurations are NOT stored in database, they are code-based in FunctionConfig enum

CREATE TABLE IF NOT EXISTS `tb_function_usage_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `user_id` BIGINT NULL COMMENT 'User ID (NULL for guest users)',
    `fingerprint` VARCHAR(255) NULL COMMENT 'Browser fingerprint (for guest users)',
    `function_type` INT NOT NULL COMMENT 'Function type ID (e.g., 1001=demo-test, 1002=ai-chat, 1003=image-gen, 1004=premium-analysis)',
    `points_type` TINYINT NOT NULL DEFAULT 0 COMMENT 'Points type: 0=Trial/Guest, 1=Free Points (Silver), 2=Fixed Points (Gold)',
    `points_cost` INT DEFAULT 0 COMMENT 'Points deducted (0 for guest users)',
    `request_params` TEXT NULL COMMENT 'Request parameters (JSON format)',
    `response_status` VARCHAR(20) DEFAULT 'success' COMMENT 'Response status: success, failed, error',
    `error_message` VARCHAR(500) NULL COMMENT 'Error message if failed',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_fingerprint` (`fingerprint`),
    INDEX `idx_function_type` (`function_type`),
    INDEX `idx_points_type` (`points_type`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Function usage log';

-- Daily Function Statistics Table
-- Aggregated statistics for reporting and analytics
CREATE TABLE IF NOT EXISTS `tb_function_daily_stats` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `stat_date` DATE NOT NULL COMMENT 'Statistics date',
    `function_type` INT NOT NULL COMMENT 'Function type ID (e.g., 1001, 1002, 1003, 1004)',
    `total_calls` INT DEFAULT 0 COMMENT 'Total function calls',
    `authenticated_calls` INT DEFAULT 0 COMMENT 'Calls by authenticated users',
    `guest_calls` INT DEFAULT 0 COMMENT 'Calls by guest users (trial)',
    `free_points_consumed` BIGINT DEFAULT 0 COMMENT 'Total silver coins (free points) consumed',
    `fixed_points_consumed` BIGINT DEFAULT 0 COMMENT 'Total gold coins (fixed points) consumed',
    `unique_users` INT DEFAULT 0 COMMENT 'Unique authenticated users',
    `unique_guests` INT DEFAULT 0 COMMENT 'Unique guest fingerprints',
    `success_calls` INT DEFAULT 0 COMMENT 'Successful calls',
    `failed_calls` INT DEFAULT 0 COMMENT 'Failed calls',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_date_function` (`stat_date`, `function_type`),
    INDEX `idx_function_type` (`function_type`),
    INDEX `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Daily function statistics';

-- ============================================================================
-- ENUM TYPE REFERENCES (Defined in code, not stored in database)
-- ============================================================================

-- Function Type Reference (Integer ID):
-- 1001 = demo-test         | Scene: chat     | Model: gpt-3.5-turbo
-- 1002 = ai-chat           | Scene: chat     | Model: gpt-4
-- 1003 = image-gen         | Scene: image    | Model: dall-e-3
-- 1004 = premium-analysis  | Scene: analysis | Model: gpt-4-turbo

-- Points Type Reference (Integer Code):
-- 0 = Trial/Guest (no points required)
-- 1 = Free Points (Silver Coins)
-- 2 = Fixed Points (Gold Coins)
-- 3 = Mixed Points (Both accepted)

-- Scene Type Reference (String Code):
-- chat, dialogue, text-generation, writing, translation, summarization
-- image, image-edit, image-analysis, video, audio
-- analysis, research, code, math
-- search, qa, rag
-- customer-service, marketing, education, healthcare
-- embedding, moderation, classification
-- demo, other

-- Model Type Reference (String Code):
-- OpenAI: gpt-3.5-turbo, gpt-4, gpt-4-turbo, gpt-4o, dall-e-2, dall-e-3
-- Anthropic: claude-3-opus, claude-3-sonnet, claude-3-haiku
-- Google: gemini-pro, gemini-pro-vision
-- Stability AI: stable-diffusion-xl
-- Other: custom, none

-- Example queries for analytics:

-- 1. Get total usage by function type (last 7 days)
-- SELECT 
--     function_type,
--     COUNT(*) as total_calls,
--     SUM(CASE WHEN user_id IS NOT NULL THEN 1 ELSE 0 END) as authenticated_calls,
--     SUM(CASE WHEN user_id IS NULL THEN 1 ELSE 0 END) as guest_calls,
--     SUM(CASE WHEN points_type = 1 THEN points_cost ELSE 0 END) as free_points_used,
--     SUM(CASE WHEN points_type = 2 THEN points_cost ELSE 0 END) as fixed_points_used
-- FROM tb_function_usage_log
-- WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
-- GROUP BY function_type;

-- 2. Get user's function usage history
-- SELECT 
--     function_type,
--     points_type,
--     points_cost,
--     response_status,
--     created_at
-- FROM tb_function_usage_log
-- WHERE user_id = ?
-- ORDER BY created_at DESC
-- LIMIT 100;

-- 3. Get daily stats for a specific function type
-- SELECT 
--     stat_date,
--     function_type,
--     total_calls,
--     guest_calls,
--     authenticated_calls,
--     free_points_consumed,
--     fixed_points_consumed
-- FROM tb_function_daily_stats
-- WHERE function_type = 1002  -- ai-chat
--   AND stat_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
-- ORDER BY stat_date DESC;

-- 4. Get points consumption by points type
-- SELECT 
--     points_type,
--     COUNT(*) as usage_count,
--     SUM(points_cost) as total_points_cost,
--     AVG(points_cost) as avg_points_cost
-- FROM tb_function_usage_log
-- WHERE user_id IS NOT NULL
--   AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
-- GROUP BY points_type;

-- 5. Get most popular functions
-- SELECT 
--     function_type,
--     COUNT(*) as total_calls,
--     COUNT(DISTINCT user_id) as unique_users,
--     COUNT(DISTINCT fingerprint) as unique_guests
-- FROM tb_function_usage_log
-- WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
-- GROUP BY function_type
-- ORDER BY total_calls DESC;


CREATE TABLE IF NOT EXISTS user_category_reports (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    category VARCHAR(50) NOT NULL,
    total_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    transaction_count INT NOT NULL DEFAULT 0,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_category UNIQUE (user_id, category)
);

CREATE INDEX IF NOT EXISTS idx_reports_user_id ON user_category_reports(user_id);

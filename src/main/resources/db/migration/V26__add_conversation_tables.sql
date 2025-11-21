-- V26: Add Conversation Tables
-- Drop existing scenarios table and recreate with proper Flyway management
-- Add conversation_sessions and conversation_messages tables

-- Drop existing scenarios table (will lose existing data)
DROP TABLE IF EXISTS scenarios CASCADE;

-- Create scenarios table (conversation templates)
CREATE TABLE scenarios (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    scenario_text TEXT NOT NULL,
    language VARCHAR(10) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    category VARCHAR(50) NOT NULL,
    roles JSONB NOT NULL,
    required_terminology JSONB NOT NULL,
    project_ids JSONB NOT NULL,
    schedule_ids JSONB NOT NULL,
    document_ids JSONB NOT NULL,
    auto_generated BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_scenarios_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for scenarios
CREATE INDEX ix_scenarios_user_id ON scenarios(user_id);
CREATE INDEX ix_scenarios_language ON scenarios(language);
CREATE INDEX ix_scenarios_difficulty ON scenarios(difficulty);
CREATE INDEX ix_scenarios_created_at ON scenarios(created_at);

-- Create conversation_sessions table (actual conversation practice sessions)
CREATE TABLE conversation_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    scenario_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    user_role VARCHAR(100),
    ai_role VARCHAR(100),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    ended_at TIMESTAMP WITH TIME ZONE,
    total_messages INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_conversation_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_conversation_sessions_scenario FOREIGN KEY (scenario_id) REFERENCES scenarios(id) ON DELETE CASCADE,
    CONSTRAINT check_status CHECK (status IN ('active', 'completed', 'abandoned'))
);

-- Create indexes for conversation_sessions
CREATE INDEX ix_conversation_sessions_user_id ON conversation_sessions(user_id);
CREATE INDEX ix_conversation_sessions_scenario_id ON conversation_sessions(scenario_id);
CREATE INDEX ix_conversation_sessions_status ON conversation_sessions(status);
CREATE INDEX ix_conversation_sessions_started_at ON conversation_sessions(started_at);

-- Create conversation_messages table (individual messages in a session)
CREATE TABLE conversation_messages (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    sender VARCHAR(10) NOT NULL,
    message_text TEXT NOT NULL,
    translated_text TEXT,
    detected_terms JSONB,
    feedback TEXT,
    sequence_number INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_conversation_messages_session FOREIGN KEY (session_id) REFERENCES conversation_sessions(id) ON DELETE CASCADE,
    CONSTRAINT check_sender CHECK (sender IN ('user', 'ai'))
);

-- Create indexes for conversation_messages
CREATE INDEX ix_conversation_messages_session_id ON conversation_messages(session_id);
CREATE INDEX ix_conversation_messages_created_at ON conversation_messages(created_at);
CREATE INDEX ix_conversation_messages_sequence ON conversation_messages(session_id, sequence_number);

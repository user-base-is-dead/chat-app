CREATE TABLE app_platforms (
    platform TEXT PRIMARY KEY CHECK (platform IN ('android', 'web')),
    min_version INTEGER NOT NULL DEFAULT 0,
    blocked_versions INTEGER[] NOT NULL DEFAULT '{}',
    flags JSONB NOT NULL DEFAULT '{}'
);

INSERT INTO app_platforms (platform) VALUES ('android'), ('web');

CREATE TABLE app_releases (
    id BIGSERIAL PRIMARY KEY,
    platform TEXT NOT NULL REFERENCES app_platforms (platform),
    version_code INTEGER NOT NULL,
    version_name TEXT NOT NULL,
    notes TEXT NOT NULL DEFAULT '',
    download_url TEXT NOT NULL DEFAULT '',
    play_url TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (platform, version_code)
);

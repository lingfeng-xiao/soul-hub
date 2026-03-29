create table if not exists runtime_model_config (
    id bigint primary key,
    provider varchar(64) not null,
    model_name varchar(255) not null,
    api_key text,
    base_url varchar(2048),
    temperature_value double precision not null,
    max_tokens integer not null,
    updated_at timestamp not null
);

create table if not exists autonomy_policy (
    id bigint primary key,
    mode varchar(64) not null,
    paused boolean not null,
    allow_internal boolean not null,
    allow_readonly boolean not null,
    allow_mutating boolean not null,
    whitelist_json text,
    updated_at timestamp not null
);

create table if not exists life_journal_entries (
    id bigint auto_increment primary key,
    entry_type varchar(64) not null,
    title varchar(255) not null,
    detail text not null,
    payload_json text,
    created_at timestamp not null
);

create index if not exists idx_life_journal_entries_created_at
    on life_journal_entries (created_at desc);

create table if not exists life_command_executions (
    id bigint auto_increment primary key,
    command_id varchar(128) not null unique,
    command_type varchar(64) not null,
    content text not null,
    context_json text,
    source varchar(64) not null,
    summary text not null,
    detail text not null,
    success boolean not null,
    impact_json text,
    created_at timestamp not null
);

create index if not exists idx_life_command_executions_created_at
    on life_command_executions (created_at desc);

create table if not exists life_runtime_state (
    id bigint primary key,
    identity_json text not null,
    self_json text not null,
    relationship_json text not null,
    goals_json text not null,
    updated_at timestamp not null
);

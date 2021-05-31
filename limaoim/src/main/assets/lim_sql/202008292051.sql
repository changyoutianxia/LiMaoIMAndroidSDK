create table chat_msg_tab
(
    client_seq INTEGER PRIMARY KEY AUTOINCREMENT,
    message_id TEXT,
    message_seq BIGINT default 0,
    channel_id TEXT,
    channel_type int default 0,
    timestamp INTEGER,
    from_uid text,
    type int default 0,
    revoke int default 0,
    content text,
    status int default 0,
    voice_status int default 0,
    unread_count int default 0,
    readed_count int default 0,
    readed int default 0,
    created_at text,
    updated_at text,
    revoker text,
    searchable_word text,
    client_msg_no text,
    is_deleted int default 0,
    receipt int default 0,
    order_seq BIGINT default 0,
    extra_version BIGINT default 0,
    extra text
);

CREATE INDEX msg_channel_index ON chat_msg_tab (channel_id,channel_type);
CREATE UNIQUE INDEX IF NOT EXISTS msg_client_msg_no_index ON chat_msg_tab (client_msg_no);
CREATE INDEX searchable_word_index ON chat_msg_tab (searchable_word);
CREATE INDEX type_index ON chat_msg_tab (type);

create table chat_msg_conversation_tab
(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    channel_id text,
    channel_type int default 0,
    last_msg_id text,
    last_msg_content text,
    last_client_msg_no text,
    last_msg_timestamp INTEGER,
    type int default 0,
    status int default 0,
    unread_count int default 0,
    client_seq BIGINT int default 0,
    browse_to BIGINT int default 0,
    from_uid text,
    reminders text,
    is_deleted int default 0,
    version BIGINT default 0,
    extra text
);

CREATE UNIQUE INDEX IF NOT EXISTS conversation_msg_index_channel ON chat_msg_conversation_tab (channel_id, channel_type);
CREATE INDEX conversation_msg_index_time ON chat_msg_conversation_tab (last_msg_timestamp);

create table channel_tab
(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    channel_id text,
    channel_type int default 0,
    show_nick int default 0,
    channel_name text,
    channel_remark text,
    top int default 0,
    mute int default 0,
    save int default 0,
    forbidden int default 0,
    follow int default 0,
    is_deleted int default 0,
    receipt int default 0,
    status int default 1,
    invite int default 0,
    version BIGINT default 0,
    online smallint not null default 0,
    last_offline INTEGER not null default 0,
    avatar text,
    category text,
    extra text,
    created_at text,
    updated_at text
);

CREATE UNIQUE INDEX IF NOT EXISTS channel_index ON channel_tab (channel_id, channel_type);

create table channel_members_tab
(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    channel_id text,
    channel_type int default 0,
    member_uid text,
    member_name text,
    member_remark text,
    member_avatar text,
    role int default 0,
    status int default 1,
    is_deleted int default 0,
    version BIGINT default 0,
    created_at text,
    updated_at text,
    extra text
);

CREATE UNIQUE INDEX IF NOT EXISTS channel_members_index ON channel_members_tab (channel_id,channel_type,member_uid);


create table chat_msg_reaction_tab
(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    channel_id text,
    channel_type int default 0,
    uid text,
    name text,
    emoji text,
    message_id text,
    seq BIGINT default 0,
    is_deleted int default 0,
    created_at text
);

CREATE UNIQUE INDEX IF NOT EXISTS chat_msg_reaction_index ON chat_msg_reaction_tab (message_id,uid,emoji);

-- Transactional Outbox: o evento nasce na mesma transação da mudança de negócio.
create table outbox_event (
    id              uuid         primary key,
    event_type      varchar(200) not null,
    schema_version  integer      not null,
    occurred_at     timestamptz  not null,
    correlation_id  varchar(100),
    aggregate_id    varchar(100),
    payload         text         not null,
    created_at      timestamptz  not null default now(),
    published_at    timestamptz
);

-- Só as linhas pendentes entram no índice, que permanece pequeno.
create index ix_outbox_event_pendentes on outbox_event (occurred_at, id) where published_at is null;

-- Consumidor idempotente: um evento produz efeito uma única vez por consumidor.
create table processed_event (
    event_id      uuid         not null,
    consumer      varchar(200) not null,
    processed_at  timestamptz  not null default now(),
    primary key (event_id, consumer)
);

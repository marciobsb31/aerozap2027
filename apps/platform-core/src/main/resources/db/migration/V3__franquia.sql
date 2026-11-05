-- Franquia de consultas (AZ-022, ADR-0004). Schema próprio do módulo (ADR-0002).
create schema if not exists franquia;

-- Plano. A franquia e a duração são dados, não constantes no código (RF-018): mudar o Free de 5
-- para 3 é decisão de negócio, e decisão de negócio presa em código vira fila de engenharia.
create table franquia.plano (
    codigo        varchar(40)  primary key,
    nome          varchar(120) not null,
    consultas     integer      not null check (consultas >= 0),
    tipo_periodo  varchar(30)  not null,
    ativo         boolean      not null default true,
    criado_em     timestamptz  not null default now()
);

insert into franquia.plano (codigo, nome, consultas, tipo_periodo)
values ('FREE', 'Free', 5, 'TRIMESTRE_CALENDARIO');

-- Período de uso: a janela em que a franquia vale, com o contador do que já saiu.
--
-- O contador fica aqui, e não numa contagem de linhas de reserva, por um motivo de concorrência:
-- `update ... set consumidas = consumidas + 1 where consumidas < franquia` é uma decisão atômica
-- do banco. Contar linhas e decidir na aplicação deixa uma janela entre a contagem e a gravação, e
-- é por essa janela que a última consulta é debitada duas vezes.
create table franquia.periodo_de_uso (
    id            uuid        primary key,
    usuario_id    uuid        not null,
    plano_codigo  varchar(40) not null references franquia.plano (codigo),
    inicio        timestamptz not null,
    fim           timestamptz not null,
    franquia      integer     not null check (franquia >= 0),
    consumidas    integer     not null default 0,
    criado_em     timestamptz not null default now(),
    -- A rede de segurança: mesmo que a aplicação erre, o banco recusa saldo negativo ou estourado.
    constraint ck_consumidas_dentro_da_franquia
        check (consumidas >= 0 and consumidas <= franquia)
);

-- Um período por usuário por janela. É o que impede duas viradas de trimestre simultâneas de
-- criarem dois períodos e dobrarem a franquia.
create unique index ux_periodo_por_usuario_e_inicio on franquia.periodo_de_uso (usuario_id, inicio);
create index ix_periodo_vigente on franquia.periodo_de_uso (usuario_id, fim);

create table franquia.reserva_de_uso (
    id                 uuid         primary key,
    periodo_id         uuid         not null references franquia.periodo_de_uso (id),
    usuario_id         uuid         not null,
    pesquisa_id        varchar(100) not null,
    chave_idempotencia varchar(200) not null,
    estado             varchar(20)  not null,
    criada_em          timestamptz  not null default now(),
    expira_em          timestamptz  not null,
    resolvida_em       timestamptz,
    motivo_liberacao   varchar(30),
    constraint ck_estado_da_reserva
        check (estado in ('RESERVED', 'CONFIRMED', 'RELEASED', 'EXPIRED'))
);

-- Dois eixos de unicidade, como o contrato ReservationRequest diz: a chave protege a repetição da
-- chamada (reentrega, retentativa, worker reiniciado) e a pesquisa garante uma reserva por
-- pesquisa. Quem decide é o índice, não uma consulta anterior.
create unique index ux_reserva_por_chave on franquia.reserva_de_uso (usuario_id, chave_idempotencia);
create unique index ux_reserva_por_pesquisa on franquia.reserva_de_uso (usuario_id, pesquisa_id);

-- Só as reservas em aberto entram no índice que o expirador varre; ele permanece pequeno mesmo
-- depois de milhões de pesquisas.
create index ix_reserva_em_aberto on franquia.reserva_de_uso (expira_em)
    where estado = 'RESERVED';

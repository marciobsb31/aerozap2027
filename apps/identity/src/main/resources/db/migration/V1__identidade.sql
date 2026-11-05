-- Identidade (AZ-020): conta, aceite de termos, sessão e recuperação de acesso.

create table usuario (
    id                  uuid         primary key,
    email               varchar(320) not null,
    -- O e-mail é comparado em minúsculas: "Ana@x.com" e "ana@x.com" são a mesma pessoa, e deixar
    -- isso para a aplicação é como se criam duas contas para o mesmo e-mail.
    email_normalizado   varchar(320) not null,
    senha_hash          varchar(100) not null,
    nome                varchar(200),
    idioma              varchar(10)  not null default 'pt-BR',
    estado              varchar(20)  not null default 'ATIVO',
    criado_em           timestamptz  not null default now(),
    atualizado_em       timestamptz  not null default now()
);

-- É o banco que garante a unicidade, não uma consulta antes do insert: entre a consulta e a
-- gravação cabe outro cadastro com o mesmo e-mail.
create unique index ux_usuario_email on usuario (email_normalizado);

create table papel_do_usuario (
    usuario_id  uuid        not null references usuario (id) on delete cascade,
    papel       varchar(40) not null,
    concedido_em timestamptz not null default now(),
    primary key (usuario_id, papel)
);

-- Aceite versionado: guarda o que a pessoa aceitou, não só que aceitou (RF-099).
create table aceite_de_termos (
    id            uuid         primary key,
    usuario_id    uuid         not null references usuario (id) on delete cascade,
    documento     varchar(40)  not null,
    versao        varchar(40)  not null,
    aceito_em     timestamptz  not null default now(),
    origem_ip     varchar(64)
);

create index ix_aceite_por_usuario on aceite_de_termos (usuario_id, documento);

-- Sessão = uma família de refresh tokens. Rotação cria uma linha nova na mesma família; reusar um
-- token já rotacionado derruba a família inteira, porque ou o token vazou ou está sendo repetido.
create table sessao (
    id             uuid         primary key,
    familia_id     uuid         not null,
    usuario_id     uuid         not null references usuario (id) on delete cascade,
    token_hash     varchar(64)  not null,
    criado_em      timestamptz  not null default now(),
    expira_em      timestamptz  not null,
    usado_em       timestamptz,
    revogado_em    timestamptz,
    motivo_revogacao varchar(40),
    user_agent     varchar(300),
    origem_ip      varchar(64)
);

-- O token nunca é guardado em claro: o que fica no banco é o hash. Vazamento da tabela não dá
-- sessão a ninguém.
create unique index ux_sessao_token on sessao (token_hash);
create index ix_sessao_familia on sessao (familia_id);
create index ix_sessao_usuario_ativa on sessao (usuario_id) where revogado_em is null;

-- Recuperação de acesso: token de uso único, guardado como hash, com expiração curta.
create table recuperacao_de_acesso (
    id          uuid        primary key,
    usuario_id  uuid        not null references usuario (id) on delete cascade,
    token_hash  varchar(64) not null,
    criado_em   timestamptz not null default now(),
    expira_em   timestamptz not null,
    usado_em    timestamptz
);

create unique index ux_recuperacao_token on recuperacao_de_acesso (token_hash);

-- Limitação de tentativas de entrada, por identificador e por origem (RF-002).
-- A contagem é incrementada por UPDATE condicional, pela mesma razão da franquia: a decisão é do
-- banco, não de uma janela entre ler e gravar.
create table tentativa_de_entrada (
    chave        varchar(200) primary key,
    tentativas   integer      not null default 0,
    janela_abre  timestamptz  not null default now(),
    bloqueado_ate timestamptz
);

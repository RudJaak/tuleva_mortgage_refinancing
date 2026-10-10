create table borrower
(
    id            uuid primary key,
    personal_code varchar(11) unique,
    first_name    text,
    last_name     text,
    synthetic     boolean     not null default false,
    created_at    timestamptz not null
);

comment on column borrower.personal_code is 'Estonian isikukood from eID; null for synthetic demo borrowers';
comment on column borrower.synthetic is 'Seeded demo data, never a real person';

create table commitment
(
    id                uuid primary key,
    borrower_id       uuid          not null references borrower (id),
    committed_balance numeric(14, 2) not null check (committed_balance > 0),
    current_margin    numeric(5, 3) not null check (current_margin >= 0),
    status            varchar(16)   not null check (status in ('SIGNED', 'WITHDRAWN')),
    signed_at         timestamptz   not null,
    withdrawn_at      timestamptz
);

comment on column commitment.committed_balance is 'Remaining principal at signing (ESMA RMBS RREL30 current principal balance)';
comment on column commitment.current_margin is 'Borrower''s current margin over Euribor in percent (ESMA RMBS RREL46 margin)';

create unique index commitment_one_signed_per_borrower on commitment (borrower_id) where status = 'SIGNED';
create index commitment_status on commitment (status);

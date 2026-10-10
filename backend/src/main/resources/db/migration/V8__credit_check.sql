create table credit_check
(
    id              uuid primary key,
    borrower_id     uuid        not null unique references borrower (id),
    result          varchar(16) not null check (result in ('CLEAR', 'DEFAULTS_FOUND')),
    active_defaults integer     not null,
    source          text        not null,
    checked_at      timestamptz not null
);

comment on table credit_check is 'Outcome of a payment-default query made with the borrower''s consent; only the result is kept, never the report';

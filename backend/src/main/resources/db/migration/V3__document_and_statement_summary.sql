create table document
(
    id                 uuid primary key,
    borrower_id        uuid        not null references borrower (id),
    type               varchar(32) not null,
    sha256             varchar(64) not null,
    verification_level varchar(32) not null,
    sealed_by_bank     text,
    sealed_at          timestamptz,
    status             varchar(16) not null check (status in ('RECEIVED', 'PROCESSED', 'FAILED')),
    created_at         timestamptz not null
);

comment on table document is 'Upload metadata only; the original bytes are never stored';
comment on column document.verification_level is 'BANK_SEALED, UNTRUSTED_SEAL, INVALID_SEAL, UNSIGNED_CONTAINER or PLAIN_PDF';

create index document_borrower on document (borrower_id);

create table statement_summary
(
    id                     uuid primary key,
    document_id            uuid          not null unique references document (id),
    borrower_id            uuid          not null references borrower (id),
    period_start           date,
    period_end             date,
    average_monthly_income numeric(12, 2) not null,
    monthly_obligations    numeric(12, 2) not null,
    summary                jsonb         not null,
    created_at             timestamptz   not null
);

comment on table statement_summary is 'Derived screening figures; raw transactions are discarded after analysis';

create index statement_summary_borrower on statement_summary (borrower_id);

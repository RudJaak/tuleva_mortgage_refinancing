create table loan
(
    id                uuid primary key,
    borrower_id       uuid           not null unique references borrower (id),
    loan_contract_id  uuid references loan_contract (id),
    lender            text,
    contract_number   text,
    contract_date     date,
    principal         numeric(14, 2),
    margin            numeric(5, 3),
    base_rate         text,
    rate_type         varchar(16)    not null,
    fixed_rate_until  date,
    maturity_date     date,
    monthly_payment   numeric(12, 2),
    repayment_type    varchar(16)    not null,
    balance           numeric(14, 2),
    property_value    numeric(14, 2),
    provenance        jsonb          not null,
    confirmed_at      timestamptz    not null
);

comment on table loan is 'The borrower''s current mortgage as they confirmed it; one per borrower';
comment on column loan.provenance is 'Per field: DOCUMENT when confirmed as read from the contract, DECLARED when entered or corrected by the borrower';
comment on column loan.margin is 'ESMA RMBS RREL46 current interest rate margin';
comment on column loan.maturity_date is 'ESMA RMBS RREL24 maturity date';
comment on column loan.principal is 'ESMA RMBS RREL29 original principal balance';
comment on column loan.balance is 'ESMA RMBS RREL30 current principal balance, when stated by the borrower';
comment on column loan.property_value is 'ESMA RMBS RREC13 original valuation amount, as used for the loan';

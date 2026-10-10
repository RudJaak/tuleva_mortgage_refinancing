create table loan_contract
(
    id                uuid primary key,
    document_id       uuid           not null unique references document (id),
    borrower_id       uuid           not null references borrower (id),
    lender            text,
    margin            numeric(5, 3),
    maturity_date     date,
    principal         numeric(14, 2),
    balance           numeric(14, 2),
    balance_estimated boolean        not null,
    contract          jsonb          not null,
    summary           jsonb          not null,
    created_at        timestamptz    not null
);

comment on table loan_contract is 'Fields read from an uploaded loan contract, with page and quote evidence; the file itself is not kept';
comment on column loan_contract.margin is 'Margin over the base rate in percent (ESMA RMBS RREL46 current interest rate margin)';
comment on column loan_contract.maturity_date is 'ESMA RMBS RREL24 maturity date';
comment on column loan_contract.principal is 'Original principal (ESMA RMBS RREL29 original principal balance)';
comment on column loan_contract.balance is 'Current principal (ESMA RMBS RREL30); estimated from the instalment when balance_estimated';

create index loan_contract_borrower on loan_contract (borrower_id);

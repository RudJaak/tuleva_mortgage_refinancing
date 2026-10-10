alter table statement_summary
    add column account_holder text,
    add column account        text;

comment on column statement_summary.account_holder is 'Holder name printed on the statement, to group several accounts per borrower';
comment on column statement_summary.account is 'Masked account number (country code and last four digits), to count each account once';

alter table document
    add column signed_by_borrower boolean;

comment on column document.signed_by_borrower is 'For a SIGNED container: whether the logged-in borrower is one of the signers; signer names are not stored';

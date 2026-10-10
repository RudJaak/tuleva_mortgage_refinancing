import { useState } from 'react'
import { FormattedList, FormattedMessage, useIntl } from 'react-intl'
import { Link } from 'react-router-dom'
import { ApiError } from '../../api/client'
import { DocumentUpload } from '../documents/DocumentUpload'
import { paths } from '../paths'
import {
  useStatementDocuments,
  useStatementsOverview,
  useUploadStatement,
  type StatementTotals,
} from './api'
import { StatementCard } from './StatementCard'

const unknownHolder = '?'

/**
 * Every borrower on the loan adds six months of statements for every bank account they have.
 * Statements are grouped per person, with each person's and the household's monthly totals.
 */
export function StatementsStep() {
  const documents = useStatementDocuments()
  const overview = useStatementsOverview(documents.data)
  const upload = useUploadStatement()
  const [uploading, setUploading] = useState(false)

  if (documents.error instanceof ApiError && documents.error.status === 401) {
    return (
      <p className="alert alert-info mb-0">
        <FormattedMessage
          id="documents.loginRequired"
          values={{ link: (text) => <Link to={paths.login}>{text}</Link> }}
        />
      </p>
    )
  }

  const uploadAll = async (files: File[]) => {
    setUploading(true)
    try {
      for (const file of files) {
        await upload.mutateAsync(file).catch(() => undefined)
      }
    } finally {
      setUploading(false)
    }
  }

  const persons = overview.data?.persons ?? []
  const borrowersOnLoan = persons.filter((person) => person.onLoan)
  const grouped = new Set(persons.flatMap((person) => person.documentIds))
  const byId = new Map(documents.data?.map((d) => [d.documentId, d]))
  const ungrouped = documents.data?.filter((d) => !grouped.has(d.documentId)) ?? []
  const personsWithStatements = persons.filter((person) => person.documentIds.length > 0)

  return (
    <div className="d-flex flex-column gap-4">
      {borrowersOnLoan.length > 1 && (
        <p className="alert alert-info mb-0" data-testid="co-borrowers">
          <FormattedMessage
            id="statements.coBorrowers"
            values={{
              count: borrowersOnLoan.length,
              names: (
                <FormattedList type="conjunction" value={borrowersOnLoan.map((p) => p.name)} />
              ),
            }}
          />
        </p>
      )}
      <DocumentUpload
        hintId="statements.upload.hint"
        chooseId="statements.upload.choose"
        multiple
        onSelect={uploadAll}
        pending={uploading}
        error={upload.error}
      />

      {ungrouped.length > 0 && (
        <section className="d-flex flex-column gap-3">
          <h2 className="h5 text-navy mb-0">
            <FormattedMessage id="statements.processing.heading" />
          </h2>
          {ungrouped.map((document) => (
            <StatementCard key={document.documentId} document={document} />
          ))}
        </section>
      )}

      {persons.map((person) => (
        <section
          key={person.name}
          className="d-flex flex-column gap-3"
          data-testid="statement-person"
        >
          <div>
            <h2 className="h5 text-navy mb-0">
              {person.name === unknownHolder ? (
                <FormattedMessage id="statements.unknownHolder" />
              ) : (
                person.name
              )}
            </h2>
            <div className="small text-body-secondary">
              <FormattedMessage
                id="statements.person.count"
                values={{ count: person.documentIds.length }}
              />
              {!person.onLoan && person.name !== unknownHolder && (
                <>
                  {' · '}
                  <FormattedMessage id="statements.person.notOnLoan" />
                </>
              )}
              {person.documentIds.length > 0 && (
                <>
                  {' · '}
                  <Totals totals={person.totals} />
                </>
              )}
            </div>
          </div>
          {person.documentIds.map((id) => {
            const document = byId.get(id)
            return document ? <StatementCard key={id} document={document} /> : null
          })}
        </section>
      ))}

      {personsWithStatements.length > 1 && overview.data && (
        <section className="card rounded-4 p-4 bg-blue-2" data-testid="household-totals">
          <h2 className="h5 text-navy">
            <FormattedMessage id="statements.household.heading" />
          </h2>
          <div>
            <Totals totals={overview.data.household} />
          </div>
        </section>
      )}
    </div>
  )
}

function Totals({ totals }: { totals: StatementTotals }) {
  const intl = useIntl()
  const euro = (amount: number) =>
    intl.formatNumber(amount, { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 })
  return (
    <FormattedMessage
      id="statements.totals"
      values={{
        income: euro(totals.monthlyIncome),
        obligations: euro(totals.monthlyObligations),
        ratio:
          totals.debtServiceRatio === null
            ? '—'
            : intl.formatNumber(totals.debtServiceRatio, { style: 'percent' }),
      }}
    />
  )
}

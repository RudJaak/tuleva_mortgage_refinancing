import { FormattedMessage } from 'react-intl'
import { Link } from 'react-router-dom'
import { ApiError } from '../../api/client'
import { DocumentUpload } from '../documents/DocumentUpload'
import { paths } from '../paths'
import { useStatementDocuments, useUploadStatement } from './api'
import { StatementCard } from './StatementCard'

export function StatementsStep() {
  const documents = useStatementDocuments()
  const upload = useUploadStatement()

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

  return (
    <div className="d-flex flex-column gap-4">
      <DocumentUpload
        hintId="statements.upload.hint"
        chooseId="statements.upload.choose"
        onSelect={(file) => upload.mutate(file)}
        pending={upload.isPending}
        error={upload.error}
      />
      {documents.data?.map((document) => (
        <StatementCard key={document.documentId} document={document} />
      ))}
    </div>
  )
}

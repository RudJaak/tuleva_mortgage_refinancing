import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../../api/client'

export type DocumentType = 'ACCOUNT_STATEMENT' | 'LOAN_CONTRACT'

export type VerificationLevel =
  'BANK_SEALED' | 'UNTRUSTED_SEAL' | 'SIGNED' | 'INVALID_SEAL' | 'UNSIGNED_CONTAINER' | 'PLAIN_PDF'

export type Verification = {
  level: VerificationLevel
  bankName: string | null
  sealedBy: string | null
  sealedAt: string | null
  signers: { name: string; kind: 'PERSON' | 'ORGANISATION' }[]
  signedByBorrower: boolean | null
}

export type UploadedDocument<Result> = {
  documentId: string
  type: DocumentType
  status: 'RECEIVED' | 'PROCESSED' | 'FAILED'
  verification: Verification
  createdAt: string
  result: Result | null
  failure: { reason: string; message: string } | null
}

export const documentPollMs = 1_500

const documentsKey = (type: DocumentType) => ['documents', type]

/** The borrower's uploads of one type, newest first; polls while any is still being read. */
export function useDocuments<Result>(type: DocumentType) {
  return useQuery({
    queryKey: documentsKey(type),
    queryFn: () => api.get<UploadedDocument<Result>[]>(`/v1/documents?type=${type}`),
    refetchInterval: (query) =>
      query.state.data?.some((document) => document.status === 'RECEIVED') ? documentPollMs : false,
  })
}

export function useUploadDocument<Result>(type: DocumentType) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (file: File) => {
      const form = new FormData()
      form.append('type', type)
      form.append('file', file)
      return api.upload<UploadedDocument<Result>>('/v1/documents', form)
    },
    onSuccess: (accepted) => {
      queryClient.setQueryData<UploadedDocument<Result>[]>(documentsKey(type), (current = []) => [
        accepted,
        ...current.filter((document) => document.documentId !== accepted.documentId),
      ])
    },
  })
}

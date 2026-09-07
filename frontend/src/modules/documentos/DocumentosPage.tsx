import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { SkeletonRows } from '../../components/Motion'
import { PdfModal } from '../../components/Overlay'
import { Alert, EmptyState, PageHeader } from '../../components/ui'
import api, { getErrorMessage } from '../../services/api'
import type { Document } from '../../types'
import { formatDate } from '../../utils/format'

export default function DocumentosPage() {
  const [pdfId, setPdfId] = useState<number | null>(null)

  const { data: documents = [], isLoading, error } = useQuery({
    queryKey: ['documentos'],
    queryFn: async () => {
      const { data } = await api.get<Document[]>('/documentos')
      return data
    },
  })

  const selected = documents.find((d) => d.id === pdfId) || null

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Archivo"
        title="Documentos"
        subtitle="Contratos, remisiones y facturas generados"
      />

      {error && <Alert>{getErrorMessage(error, 'No se pudieron cargar los documentos')}</Alert>}

      <section className="panel overflow-hidden">
        {isLoading ? (
          <SkeletonRows rows={6} />
        ) : documents.length === 0 ? (
          <EmptyState title="Sin documentos" hint="Se generan al emitir un alquiler." />
        ) : (
          <ul className="divide-y divide-[var(--border)]">
            {documents.map((doc) => (
              <li key={doc.id}>
                <button
                  type="button"
                  onClick={() => setPdfId(doc.id)}
                  className="w-full text-left px-5 py-3.5 hover:bg-[#f9f9fb] transition-colors flex items-center justify-between gap-3"
                >
                  <div className="min-w-0">
                    <div className="font-semibold text-[var(--navy)]">{doc.documentType}</div>
                    <div className="text-xs text-[var(--text-muted)] mt-1">
                      {doc.serialNumber || `Doc #${doc.id}`} · Alquiler #{doc.rentalId} ·{' '}
                      {formatDate(doc.generatedAt)}
                    </div>
                  </div>
                  <span className="text-xs font-bold text-[var(--accent)] shrink-0">Ver PDF →</span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>

      <PdfModal
        open={pdfId != null}
        onClose={() => setPdfId(null)}
        documentId={pdfId}
        title={selected ? `${selected.documentType} · #${selected.id}` : 'Documento'}
      />
    </div>
  )
}

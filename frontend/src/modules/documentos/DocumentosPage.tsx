import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Button, Input, Modal, PageHeader, Pagination, Panel } from '../../components/ui'
import { api } from '../../services/api'
import type { DocumentRow, PageResponse } from '../../types/api'
import { datetimeEs } from '../../lib/format'

export function DocumentosPage() {
  const [q, setQ] = useState('')
  const [page, setPage] = useState(0)
  const [pdfUrl, setPdfUrl] = useState<string | null>(null)
  const list = useQuery({
    queryKey: ['documentos', q, page],
    queryFn: async () => (await api.get<PageResponse<DocumentRow>>('/documentos', { params: { buscar: q || undefined, page, size: 20 } })).data,
  })

  return (
    <div>
      <PageHeader eyebrow="Papeleria" title="Documentos" subtitle="Contratos, remisiones, devoluciones y cuentas de cobro." />
      <Panel>
        <Input label="Buscar" value={q} onChange={(e) => { setQ(e.target.value); setPage(0) }} />
        <div className="mt-4 space-y-2">
          {(list.data?.content ?? []).map((doc) => (
            <div key={doc.id} className="flex items-center justify-between rounded-lg bg-slate-50 px-3 py-2 text-sm">
              <div>
                <p className="font-medium text-navy">{doc.serialNumber} · {doc.documentTypeLabel}</p>
                <p className="text-xs text-steel-muted">{doc.customerName} · {datetimeEs(doc.generatedAt)}</p>
              </div>
              <Button variant="ghost" onClick={async () => {
                const blob = await api.get(`/documentos/${doc.id}/contenido`, { responseType: 'blob' })
                setPdfUrl(URL.createObjectURL(blob.data))
              }}>Ver</Button>
            </div>
          ))}
        </div>
        <Pagination page={page} totalPages={list.data?.totalPages ?? 0} onPage={setPage} />
      </Panel>
      <Modal open={Boolean(pdfUrl)} title="Visor PDF" onClose={() => setPdfUrl(null)}>
        {pdfUrl && <iframe title="PDF" src={pdfUrl} className="h-[70vh] w-full rounded" />}
      </Modal>
    </div>
  )
}

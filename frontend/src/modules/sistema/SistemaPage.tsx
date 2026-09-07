import { useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import { ModuleTabs, PageHeader } from '../../components/ui'
import AdminPage from '../admin/AdminPage'
import ImportPage from '../importacion/ImportPage'
import IntegracionesPage from '../integraciones/IntegracionesPage'

type Tab = 'conexiones' | 'importar' | 'usuarios'

const tabs: { id: Tab; label: string }[] = [
  { id: 'conexiones', label: 'Conexiones' },
  { id: 'importar', label: 'Importar' },
  { id: 'usuarios', label: 'Usuarios' },
]

export default function SistemaPage() {
  const [params, setParams] = useSearchParams()
  const tab = useMemo<Tab>(() => {
    const raw = params.get('tab')
    if (raw === 'importar' || raw === 'usuarios' || raw === 'conexiones') return raw
    return 'conexiones'
  }, [params])

  function setTab(next: Tab) {
    setParams(next === 'conexiones' ? {} : { tab: next })
  }

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Infraestructura"
        title="Sistema"
        subtitle="Conexiones externas, carga masiva y usuarios del patio"
      />

      <ModuleTabs tabs={tabs} value={tab} onChange={(id) => setTab(id as Tab)} />

      <div className="anim-panel" key={tab}>
        {tab === 'conexiones' && <IntegracionesPage embedded />}
        {tab === 'importar' && <ImportPage embedded />}
        {tab === 'usuarios' && <AdminPage embedded />}
      </div>
    </div>
  )
}

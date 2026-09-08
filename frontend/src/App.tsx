import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { ProtectedRoute } from './components/ProtectedRoute'
import { ToastProvider } from './components/Toast'
import { P } from './lib/permissions'
import { AuthProvider } from './modules/auth/AuthContext'
import { LoginPage } from './modules/auth/LoginPage'
import { AlquileresPage } from './modules/alquileres/AlquileresPage'
import { WizardPage } from './modules/alquileres/WizardPage'
import { ClientesPage } from './modules/clientes/ClientesPage'
import { DashboardPage } from './modules/dashboard/DashboardPage'
import { DocumentosPage } from './modules/documentos/DocumentosPage'
import { FinanzasPage } from './modules/finanzas/FinanzasPage'
import { InventarioPage } from './modules/inventario/InventarioPage'
import { AnaliticasPage } from './modules/analiticas/AnaliticasPage'
import { SistemaPage } from './modules/sistema/SistemaPage'

const queryClient = new QueryClient({
  defaultOptions: { queries: { refetchOnWindowFocus: false, retry: 1 } },
})

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <ToastProvider>
          <BrowserRouter>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route element={<ProtectedRoute />}>
                <Route element={<Layout />}>
                  <Route element={<ProtectedRoute permission={P.DASHBOARD_VER} />}>
                    <Route path="/" element={<DashboardPage />} />
                  </Route>
                  <Route element={<ProtectedRoute permission={P.ALQUILERES_VER} />}>
                    <Route path="/alquileres" element={<AlquileresPage />} />
                  </Route>
                  <Route element={<ProtectedRoute permission={P.ALQUILERES_GESTIONAR} />}>
                    <Route path="/alquileres/nuevo" element={<WizardPage />} />
                  </Route>
                  <Route element={<ProtectedRoute permission={P.CLIENTES_VER} />}>
                    <Route path="/clientes" element={<ClientesPage />} />
                  </Route>
                  <Route element={<ProtectedRoute permission={P.INVENTARIO_VER} />}>
                    <Route path="/inventario" element={<InventarioPage />} />
                  </Route>
                  <Route element={<ProtectedRoute permission={P.DOCUMENTOS_VER} />}>
                    <Route path="/documentos" element={<DocumentosPage />} />
                  </Route>
                  <Route element={<ProtectedRoute permission={P.FINANZAS_VER} />}>
                    <Route path="/finanzas" element={<FinanzasPage />} />
                  </Route>
                  <Route element={<ProtectedRoute permission={P.ANALITICAS_VER} />}>
                    <Route path="/analiticas" element={<AnaliticasPage />} />
                  </Route>
                  <Route path="/sistema" element={<SistemaPage />} />
                </Route>
              </Route>
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </BrowserRouter>
        </ToastProvider>
      </AuthProvider>
    </QueryClientProvider>
  )
}

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { CommandPalette } from './components/CommandPalette'
import ErrorBoundary from './components/ErrorBoundary'
import Layout from './components/Layout'
import ProtectedRoute from './components/ProtectedRoute'
import { ToastProvider } from './components/Toast'
import { AuthProvider } from './modules/auth/AuthContext'
import LoginPage from './modules/auth/LoginPage'
import DashboardPage from './modules/dashboard/DashboardPage'
import ClientesPage from './modules/clientes/ClientesPage'
import InventarioPage from './modules/inventario/InventarioPage'
import AlquileresPage from './modules/alquileres/AlquileresPage'
import WizardPage from './modules/alquileres/WizardPage'
import DocumentosPage from './modules/documentos/DocumentosPage'
import FinanzasPage from './modules/finanzas/FinanzasPage'
import SistemaPage from './modules/sistema/SistemaPage'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      staleTime: 30_000,
    },
  },
})

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <ToastProvider>
          <BrowserRouter>
            <CommandPalette />
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route element={<ProtectedRoute />}>
                <Route
                  element={
                    <ErrorBoundary>
                      <Layout />
                    </ErrorBoundary>
                  }
                >
                  <Route index element={<DashboardPage />} />
                  <Route path="clientes" element={<ClientesPage />} />
                  <Route path="inventario" element={<InventarioPage />} />
                  <Route path="alquileres" element={<AlquileresPage />} />
                  <Route path="alquileres/nuevo" element={<WizardPage />} />
                  <Route path="documentos" element={<DocumentosPage />} />
                  <Route path="finanzas" element={<FinanzasPage />} />
                  <Route path="sistema" element={<SistemaPage />} />
                  <Route path="analiticas" element={<Navigate to="/" replace />} />
                  <Route path="antecedentes" element={<Navigate to="/clientes?panel=antecedentes" replace />} />
                  <Route path="integraciones" element={<Navigate to="/sistema?tab=conexiones" replace />} />
                  <Route path="importacion" element={<Navigate to="/sistema?tab=importar" replace />} />
                  <Route path="admin" element={<Navigate to="/sistema?tab=usuarios" replace />} />
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

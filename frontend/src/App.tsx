import { Navigate, Route, Routes } from 'react-router-dom'
import { AppLayout } from '@/components/layout/AppLayout'
import { ProtectedRoute } from '@/components/layout/ProtectedRoute'
import { ChangePasswordPage, ForgotPasswordPage, LoginPage } from '@/features/auth'
import {
  AdministrationPage,
  AuditPage,
  ConfigurationPage,
  ReinitialisationsPage,
  UtilisateursPage,
} from '@/features/administration'
import { ClientsPage } from '@/features/clients'
import { CommerciauxPage } from '@/features/commerciaux'
import { DashboardPage } from '@/features/dashboard'
import { DemandesPage } from '@/features/demandes'
import { DocumentsPage } from '@/features/documents'
import { EcheancesPage } from '@/features/echeances'
import { NotificationsPage } from '@/features/notifications'
import { PaiementsPage } from '@/features/paiements'
import { PlansPaiementPage } from '@/features/plan-paiement'
import { RapportsPage } from '@/features/rapports'
import { RecherchePage } from '@/features/recherche'
import { ResponsablesPage } from '@/features/responsables'
import { ServicesPage } from '@/features/services'

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/mot-de-passe-oublie" element={<ForgotPasswordPage />} />

      <Route element={<ProtectedRoute />}>
        <Route path="/changer-mot-de-passe" element={<ChangePasswordPage />} />
        <Route element={<AppLayout />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/clients" element={<ClientsPage />} />
          <Route path="/responsables" element={<ResponsablesPage />} />
          <Route path="/commerciaux" element={<CommerciauxPage />} />
          <Route path="/services" element={<ServicesPage />} />
          <Route path="/demandes" element={<DemandesPage />} />
          <Route path="/plans-paiement" element={<PlansPaiementPage />} />
          <Route path="/echeances" element={<EcheancesPage />} />
          <Route path="/paiements" element={<PaiementsPage />} />
          <Route path="/documents" element={<DocumentsPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
          <Route path="/rapports" element={<RapportsPage />} />
          <Route path="/recherche" element={<RecherchePage />} />
          <Route path="/administration" element={<AdministrationPage />} />
          <Route path="/administration/utilisateurs" element={<UtilisateursPage />} />
          <Route path="/administration/audit" element={<AuditPage />} />
          <Route path="/administration/reinitialisations" element={<ReinitialisationsPage />} />
          <Route path="/administration/configuration" element={<ConfigurationPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}

export default App
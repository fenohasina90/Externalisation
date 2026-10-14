import { Navigate, Route, Routes } from 'react-router-dom'
import { AppLayout } from '@/components/layout/AppLayout'
import { ProtectedRoute } from '@/components/layout/ProtectedRoute'
import { ForbiddenPage, NotFoundPage } from '@/components/shared/ErrorPage'
import { ChangePasswordPage, ForgotPasswordPage, LoginPage } from '@/features/auth'
import {
  AdministrationPage,
  AuditPage,
  ConfigurationPage,
  ReinitialisationsPage,
  RolesPermissionsPage,
  UtilisateursPage,
} from '@/features/administration'
import { ClientDetailPage, ClientsPage } from '@/features/clients'
import { CommerciauxPage } from '@/features/commerciaux'
import { DashboardPage } from '@/features/dashboard'
import { DemandeDetailPage, DemandesPage, NouvelleDemandePage } from '@/features/demandes'
import { DocumentsPage } from '@/features/documents'
import { EcheancesPage } from '@/features/echeances'
import { NotificationsPage } from '@/features/notifications'
import {
  EcheancierDetailPage,
  EcheanciersPage,
  HistoriquePaiementsPage,
  NouveauPaiementPage,
  PaiementsPage,
} from '@/features/paiements'
import { PlansPaiementPage } from '@/features/plan-paiement'
import { RapportsPage } from '@/features/rapports'
import { RecherchePage } from '@/features/recherche'
import { ResponsablesPage } from '@/features/responsables'
import { DomainesPage, ServicesPage } from '@/features/services'

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/mot-de-passe-oublie" element={<ForgotPasswordPage />} />
      <Route path="/404" element={<NotFoundPage />} />
      <Route path="/403" element={<ForbiddenPage />} />

      <Route element={<ProtectedRoute />}>
        <Route path="/changer-mot-de-passe" element={<ChangePasswordPage />} />
        <Route element={<AppLayout />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/clients" element={<ClientsPage />} />
          <Route path="/clients/:code" element={<ClientDetailPage />} />
          <Route path="/responsables" element={<ResponsablesPage />} />
          <Route path="/commerciaux" element={<CommerciauxPage />} />
          <Route path="/services" element={<ServicesPage />} />
          <Route path="/domaines" element={<DomainesPage />} />
          <Route path="/demandes" element={<DemandesPage />} />
          <Route path="/demandes/nouveau" element={<NouvelleDemandePage />} />
          <Route path="/demandes/:reference" element={<DemandeDetailPage />} />
          <Route path="/plans-paiement" element={<PlansPaiementPage />} />
          <Route path="/echeances" element={<EcheancesPage />} />
          <Route path="/paiements" element={<PaiementsPage />} />
          <Route path="/paiements/nouveau" element={<NouveauPaiementPage />} />
          <Route path="/paiements/echeanciers" element={<EcheanciersPage />} />
          <Route path="/paiements/echeanciers/:reference" element={<EcheancierDetailPage />} />
          <Route path="/paiements/historique" element={<HistoriquePaiementsPage />} />
          <Route path="/documents" element={<DocumentsPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
          <Route path="/rapports" element={<RapportsPage />} />
          <Route path="/recherche" element={<RecherchePage />} />
          <Route path="/administration" element={<AdministrationPage />} />
          <Route path="/administration/utilisateurs" element={<UtilisateursPage />} />
          <Route path="/administration/roles" element={<RolesPermissionsPage />} />
          <Route path="/administration/audit" element={<AuditPage />} />
          <Route path="/administration/reinitialisations" element={<ReinitialisationsPage />} />
          <Route path="/administration/configuration" element={<ConfigurationPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/404" replace />} />
      </Route>
    </Routes>
  )
}

export default App
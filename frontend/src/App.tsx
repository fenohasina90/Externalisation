import { Navigate, Route, Routes } from 'react-router-dom'
import { AppLayout } from '@/components/layout/AppLayout'
import { ProtectedRoute } from '@/components/layout/ProtectedRoute'
import { RequirePermission } from '@/components/layout/RequirePermission'
import { ForbiddenPage, NotFoundPage } from '@/components/shared/ErrorPage'
import { ChangePasswordPage, ForgotPasswordPage, LoginPage } from '@/features/auth'
import {
  AdministrationPage,
  AuditPage,
  ConfigurationPage,
  ModesPaiementPage,
  ReinitialisationsPage,
  RolesPermissionsPage,
  StatutsPage,
  UtilisateursPage,
} from '@/features/administration'
import { SauvegardesPage } from '@/features/backup'
import { ClientDetailPage, ClientsPage } from '@/features/clients'
import { CommerciauxPage } from '@/features/commerciaux'
import { DashboardPage } from '@/features/dashboard'
import {
  DemandeDetailPage,
  DemandesPage,
  DossierEntreprisePage,
  DossierTransactionImmobilierePage,
  NouvelleDemandePage,
} from '@/features/demandes'
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

const ADMIN: string[] = ['USER_VIEW', 'AUDIT_VIEW', 'CONFIG_VIEW', 'PASSWORD_RESET_MANAGE']
const DOCUMENTS: string[] = ['DOCUMENT_DEMANDE_VIEW', 'DOCUMENT_PAIEMENT_VIEW']
const ECHEANCES: string[] = ['DEMANDE_VIEW', 'PAIEMENT_VIEW']
const RAPPORTS: string[] = ['RAPPORT_DEMANDE_VIEW', 'RAPPORT_PAIEMENT_VIEW']

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
          <Route
            path="/"
            element={
              <RequirePermission permission="DASHBOARD_VIEW">
                <DashboardPage />
              </RequirePermission>
            }
          />
          <Route
            path="/clients"
            element={
              <RequirePermission permission="CLIENT_VIEW">
                <ClientsPage />
              </RequirePermission>
            }
          />
          <Route
            path="/clients/:reference"
            element={
              <RequirePermission permission="CLIENT_VIEW">
                <ClientDetailPage />
              </RequirePermission>
            }
          />
          <Route
            path="/responsables"
            element={
              <RequirePermission permission="RESPONSABLE_VIEW">
                <ResponsablesPage />
              </RequirePermission>
            }
          />
          <Route
            path="/commerciaux"
            element={
              <RequirePermission permission="COMMERCIAL_VIEW">
                <CommerciauxPage />
              </RequirePermission>
            }
          />
          <Route
            path="/services"
            element={
              <RequirePermission permission="SERVICE_VIEW">
                <ServicesPage />
              </RequirePermission>
            }
          />
          <Route
            path="/domaines"
            element={
              <RequirePermission permission="DOMAINE_VIEW">
                <DomainesPage />
              </RequirePermission>
            }
          />
          <Route
            path="/demandes"
            element={
              <RequirePermission permission="DEMANDE_VIEW">
                <DemandesPage />
              </RequirePermission>
            }
          />
          <Route
            path="/demandes/nouveau"
            element={
              <RequirePermission permission="DEMANDE_CREATE">
                <NouvelleDemandePage />
              </RequirePermission>
            }
          />
          <Route
            path="/demandes/:reference"
            element={
              <RequirePermission permission="DEMANDE_VIEW">
                <DemandeDetailPage />
              </RequirePermission>
            }
          />
          <Route
            path="/demandes/:reference/dossier-entreprise"
            element={
              <RequirePermission permission={['DOSSIER_VIEW', 'DOSSIER_CREATE', 'DOSSIER_UPDATE']}>
                <DossierEntreprisePage />
              </RequirePermission>
            }
          />
          <Route
            path="/demandes/:reference/dossier-transaction-immobiliere"
            element={
              <RequirePermission permission={['DOSSIER_VIEW', 'DOSSIER_CREATE', 'DOSSIER_UPDATE']}>
                <DossierTransactionImmobilierePage />
              </RequirePermission>
            }
          />
          <Route
            path="/plans-paiement"
            element={
              <RequirePermission permission={ECHEANCES}>
                <PlansPaiementPage />
              </RequirePermission>
            }
          />
          <Route
            path="/echeances"
            element={
              <RequirePermission permission={ECHEANCES}>
                <EcheancesPage />
              </RequirePermission>
            }
          />
          <Route
            path="/paiements"
            element={
              <RequirePermission permission="PAIEMENT_VIEW">
                <PaiementsPage />
              </RequirePermission>
            }
          />
          <Route
            path="/paiements/nouveau"
            element={
              <RequirePermission permission="PAIEMENT_CREATE">
                <NouveauPaiementPage />
              </RequirePermission>
            }
          />
          <Route
            path="/paiements/echeanciers"
            element={
              <RequirePermission permission="PAIEMENT_VIEW">
                <EcheanciersPage />
              </RequirePermission>
            }
          />
          <Route
            path="/paiements/echeanciers/:reference"
            element={
              <RequirePermission permission="PAIEMENT_VIEW">
                <EcheancierDetailPage />
              </RequirePermission>
            }
          />
          <Route
            path="/paiements/historique"
            element={
              <RequirePermission permission="PAIEMENT_VIEW">
                <HistoriquePaiementsPage />
              </RequirePermission>
            }
          />
          <Route
            path="/documents"
            element={
              <RequirePermission permission={DOCUMENTS}>
                <DocumentsPage />
              </RequirePermission>
            }
          />
          <Route path="/notifications" element={<NotificationsPage />} />
          <Route
            path="/rapports"
            element={
              <RequirePermission permission={RAPPORTS}>
                <RapportsPage />
              </RequirePermission>
            }
          />
          <Route path="/recherche" element={<RecherchePage />} />
          <Route
            path="/administration"
            element={
              <RequirePermission permission={ADMIN}>
                <AdministrationPage />
              </RequirePermission>
            }
          />
          <Route
            path="/administration/utilisateurs"
            element={
              <RequirePermission permission="USER_VIEW">
                <UtilisateursPage />
              </RequirePermission>
            }
          />
          <Route
            path="/administration/roles"
            element={
              <RequirePermission permission="USER_VIEW">
                <RolesPermissionsPage />
              </RequirePermission>
            }
          />
          <Route
            path="/administration/audit"
            element={
              <RequirePermission permission="AUDIT_VIEW">
                <AuditPage />
              </RequirePermission>
            }
          />
          <Route
            path="/administration/reinitialisations"
            element={
              <RequirePermission permission="PASSWORD_RESET_MANAGE">
                <ReinitialisationsPage />
              </RequirePermission>
            }
          />
          <Route
            path="/administration/configuration"
            element={
              <RequirePermission permission="CONFIG_VIEW">
                <ConfigurationPage />
              </RequirePermission>
            }
          />
          <Route
            path="/administration/statuts"
            element={
              <RequirePermission permission="CONFIG_VIEW">
                <StatutsPage />
              </RequirePermission>
            }
          />
          <Route
            path="/administration/modes-paiement"
            element={
              <RequirePermission permission="MODE_PAIEMENT_VIEW">
                <ModesPaiementPage />
              </RequirePermission>
            }
          />
          <Route
            path="/administration/sauvegardes"
            element={
              <RequirePermission permission="BACKUP_VIEW">
                <SauvegardesPage />
              </RequirePermission>
            }
          />
        </Route>
        <Route path="*" element={<Navigate to="/404" replace />} />
      </Route>
    </Routes>
  )
}

export default App

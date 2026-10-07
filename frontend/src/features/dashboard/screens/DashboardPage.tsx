import { Alert, PageHeader, Spinner } from '@/components/ui'
import { formatMontant } from '@/utils/format'
import { Section, StatCard } from '../components'
import { useDashboardSummary } from '../hooks'

export function DashboardPage() {
  const { data, loading, error } = useDashboardSummary()

  if (loading) return <Spinner label="Chargement du tableau de bord…" />
  if (error) return <Alert tone="danger">{error}</Alert>
  if (!data) return null

  const chiffre = (value: number | null | undefined): string => formatMontant(value ?? null)

  return (
    <>
      <PageHeader titre="Tableau de bord" sousTitre="Vue d’ensemble de l’activité" />

      <div className="stats-grid">
        <StatCard label="Demandes en attente" valeur={data.demandesEnAttente ?? 0} accent="warning" />
        <StatCard label="Demandes en cours" valeur={data.demandesEnCours ?? 0} accent="info" />
        <StatCard label="Clients" valeur={data.nombreClients ?? 0} accent="success" />
        <StatCard label="Services" valeur={data.nombreServices ?? 0} accent="neutral" />
      </div>

      <div className="stats-grid">
        <StatCard label="Montant total" valeur={chiffre(data.montantTotal)} accent="info" />
        <StatCard label="Montant payé" valeur={chiffre(data.montantPaye)} accent="success" />
        <StatCard label="Reste à payer" valeur={chiffre(data.resteAPayer)} accent="danger" />
        <StatCard label="Échéances en retard" valeur={data.echeancesEnRetard ?? 0} accent="danger" />
      </div>

      <Section titre="Informations complémentaires">
        <p className="section-note">
          Échéances proches : {data.echeancesProches ?? 0} · Demandes totales :{' '}
          {data.nombreDemandes ?? 0}
        </p>
      </Section>
    </>
  )
}
import type { JSX } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '@/features/auth/hooks/useAuth'
import {
  BarresPaiementsMensuels,
  CartesAlertes,
  CourbeEvolution,
  DonutStatuts,
  MontantServices,
  TopServices,
  TranchesRetard,
} from '../components/charts'
import {
  ALERTES_ACCUEIL,
  ALERTES_ECONOMAT,
  ALERTES_SUPER_ADMIN,
} from '../data/mockDashboard'

function KpiDemandes({
  icon,
  tag,
  valeur,
  label,
  highlight = false,
}: {
  icon: 'users' | 'doc' | 'layers' | 'card' | 'check' | 'clock'
  tag?: string
  valeur: string
  label: string
  highlight?: boolean
}) {
  const icones: Record<string, JSX.Element> = {
    users: (
      <svg className="sv" viewBox="0 0 24 24">
        <circle cx="9" cy="8" r="3.5" />
        <path d="M2.5 20c0-3.5 3-6 6.5-6s6.5 2.5 6.5 6M16 4.8a3.5 3.5 0 0 1 0 6.4M18 14.3c2 .7 3.5 2.6 3.5 5.2" />
      </svg>
    ),
    doc: (
      <svg className="sv" viewBox="0 0 24 24">
        <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8l-5-5zM14 3v5h5M9 13h6M9 17h6" />
      </svg>
    ),
    layers: (
      <svg className="sv" viewBox="0 0 24 24">
        <path d="M12 3l9 5-9 5-9-5 9-5zM3 13l9 5 9-5M3 17l9 5 9-5" />
      </svg>
    ),
    card: (
      <svg className="sv" viewBox="0 0 24 24">
        <rect x="3" y="5" width="18" height="14" rx="2" />
        <path d="M3 10h18" />
      </svg>
    ),
    check: (
      <svg className="sv" viewBox="0 0 24 24">
        <circle cx="12" cy="12" r="9" />
        <path d="M8 12l3 3 5-6" />
      </svg>
    ),
    clock: (
      <svg className="sv" viewBox="0 0 24 24">
        <circle cx="12" cy="12" r="9" />
        <path d="M12 7v5l3 3" />
      </svg>
    ),
  }
  return (
    <div className={`card kpi-card${highlight ? ' hl' : ''}`}>
      {tag && <span className="kpi-tag">{tag}</span>}
      <div className="kpi-icon">{icones[icon]}</div>
      <div>
        <div className="kpi-num">{valeur}</div>
        <div className="kpi-label">{label}</div>
      </div>
    </div>
  )
}

/**
 * Tableau de bord personnalisé selon le rôle (données statiques).
 * ACCUEIL : demandes · ECONOMAT : paiements · SUPER_ADMIN/ADMIN : ensemble.
 */
export function DashboardPage() {
  const { utilisateur } = useAuth()
  const role = utilisateur?.role.nomRole ?? 'ACCUEIL'

  const vue =
    role === 'ECONOMAT' ? 'ECONOMAT' : role === 'ACCUEIL' ? 'ACCUEIL' : 'SUPER_ADMIN'

  const sousTitre =
    vue === 'ACCUEIL'
      ? 'Suivi des clients, des demandes et des services.'
      : vue === 'ECONOMAT'
        ? 'Suivi des paiements, des échéances et des montants à encaisser.'
        : 'Vue d’ensemble de vos clients, demandes et paiements d’externalisation.'

  return (
    <div className="app-page responsables-page">
      <header className="page-header page-header-sticky">
        <div>
          <h1>Tableau de bord</h1>
          <p>{sousTitre}</p>
        </div>
        <div className="actions">
          <span className="pill ok">Vue {vue}</span>
          <button type="button" className="btn-ghost" onClick={() => window.print()}>
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M12 3v12m0 0l-4-4m4 4l4-4M4 21h16" />
            </svg>
            Exporter
          </button>
        </div>
      </header>

      {vue === 'ACCUEIL' && (
        <>
          <section className="kpi-grid">
            <KpiDemandes icon="users" tag="+ 12 ce mois" valeur="248" label="Nombre de clients" />
            <KpiDemandes icon="doc" tag="+ 33 ce mois" valeur="126" label="Nombre de demandes" highlight />
            <KpiDemandes icon="layers" tag="21 actifs" valeur="22" label="Nombre de services" />
          </section>

          <div className="dash-grid">
            <section className="card card-pad">
              <h3>Évolution des demandes</h3>
              <CourbeEvolution />
            </section>
            <section className="card card-pad">
              <h3>Demandes par statut</h3>
              <DonutStatuts />
            </section>
            <section className="card card-pad">
              <h3>Services les plus demandés</h3>
              <TopServices />
            </section>
          </div>

          <section className="card card-pad">
            <h3>Alertes</h3>
            <CartesAlertes alertes={ALERTES_ACCUEIL} />
          </section>
        </>
      )}

      {vue === 'ECONOMAT' && (
        <>
          <section className="kpi-grid">
            <KpiDemandes icon="card" valeur="84 500 000 MGA" label="Montant total à payer" />
            <KpiDemandes icon="check" tag="62 %" valeur="52 300 000 MGA" label="Montant total payé" highlight />
            <KpiDemandes icon="clock" tag="38 %" valeur="32 200 000 MGA" label="Montant restant à payer" />
          </section>

          <div className="dash-grid">
            <section className="card card-pad">
              <h3>Paiements par mois</h3>
              <BarresPaiementsMensuels />
            </section>
            <section className="card card-pad">
              <div className="dash-card-head">
                <h3>Échéances en retard</h3>
                <Link to="/paiements" className="lk">Voir les retards ›</Link>
              </div>
              <TranchesRetard />
            </section>
            <section className="card card-pad">
              <h3>Montant des services</h3>
              <MontantServices />
            </section>
          </div>

          <section className="card card-pad">
            <h3>Alertes</h3>
            <CartesAlertes alertes={ALERTES_ECONOMAT} />
          </section>
        </>
      )}

      {vue === 'SUPER_ADMIN' && (
        <>
          <section className="kpi-grid kpi-grid--5">
            <KpiDemandes icon="users" tag="+ 12 ce mois" valeur="248" label="Nombre de clients" />
            <KpiDemandes icon="doc" tag="+ 33 ce mois" valeur="126" label="Nombre de demandes" highlight />
            <KpiDemandes icon="layers" tag="21 actifs" valeur="22" label="Nombre de services" />
            <KpiDemandes icon="card" valeur="84 500 000 MGA" label="Montant total à payer" />
            <KpiDemandes icon="check" tag="62 %" valeur="52 300 000 MGA" label="Montant total payé" highlight />
            <KpiDemandes icon="clock" tag="38 %" valeur="32 200 000 MGA" label="Montant restant à payer" />
          </section>

          <div className="dash-grid">
            <section className="card card-pad">
              <h3>Évolution des demandes</h3>
              <CourbeEvolution />
            </section>
            <section className="card card-pad">
              <h3>Demandes par statut</h3>
              <DonutStatuts />
            </section>
            <section className="card card-pad">
              <h3>Montant des services</h3>
              <MontantServices />
            </section>
            <section className="card card-pad">
              <h3>Paiements par mois</h3>
              <BarresPaiementsMensuels />
            </section>
            <section className="card card-pad">
              <div className="dash-card-head">
                <h3>Échéances en retard</h3>
                <Link to="/paiements" className="lk">Voir les retards ›</Link>
              </div>
              <TranchesRetard />
            </section>
            <section className="card card-pad">
              <h3>Services les plus demandés</h3>
              <TopServices />
            </section>
          </div>

          <section className="card card-pad">
            <h3>Alertes</h3>
            <CartesAlertes alertes={ALERTES_SUPER_ADMIN} />
          </section>
        </>
      )}
    </div>
  )
}

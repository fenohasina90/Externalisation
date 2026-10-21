import { useState, type JSX } from 'react'
import { Link } from 'react-router-dom'
import { Spinner } from '@/components/ui'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { formatMontantMGA } from '@/utils/format'
import {
  BarresPaiementsMensuels,
  CartesAlertes,
  CourbeEvolution,
  DonutStatuts,
  MontantServices,
  TopServices,
  TranchesRetard,
} from '../components/charts'
import { PerformancesPanel } from '../components/Performances'
import { useDashboardData, useDashboardFiltres, type VueDashboard } from '../hooks'
import {
  FILTRES_DASHBOARD_VIDES,
  type AlerteDashboard,
  type DashboardFiltres,
  type DashboardSummary,
} from '../types'

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

function construireAlertes(vue: VueDashboard, summary: DashboardSummary, montantEnRetard: number) {
  const alertes: AlerteDashboard[] = []
  const enRetard = summary.echeancesEnRetard ?? 0
  const proches = summary.echeancesProches ?? 0
  const enAttente = summary.demandesEnAttente ?? 0

  if (vue !== 'ACCUEIL') {
    if (proches > 0) {
      alertes.push({
        ton: 'orange',
        etiquette: 'Échéances proches',
        titre: `${proches} échéance${proches > 1 ? 's' : ''} sous 3 jours`,
        detail: 'Relancez les clients concernés',
        lien: '/paiements/echeanciers',
      })
    }
    if (enRetard > 0) {
      alertes.push({
        ton: 'rouge',
        etiquette: 'Paiements en retard',
        titre: `${enRetard} échéance${enRetard > 1 ? 's' : ''} dépassée${enRetard > 1 ? 's' : ''}`,
        detail: montantEnRetard > 0 ? `${formatMontantMGA(montantEnRetard)} concernés` : 'À relancer',
        lien: '/paiements',
      })
    }
    if ((summary.resteAPayer ?? 0) > 0) {
      alertes.push({
        ton: 'vert',
        etiquette: 'Montants restant à payer',
        titre: formatMontantMGA(summary.resteAPayer),
        detail: `sur ${formatMontantMGA(summary.montantTotal)} au total`,
        lien: '/paiements',
      })
    }
  }

  if (vue !== 'ECONOMAT' && enAttente > 0) {
    alertes.push({
      ton: 'gris',
      etiquette: 'Demandes en attente',
      titre: `${enAttente} demande${enAttente > 1 ? 's' : ''} à traiter`,
      detail: 'En attente de validation ou de premier paiement',
      lien: '/demandes',
    })
  }

  return alertes
}

/** Tableau de bord personnalisé et branché sur `/api/dashboard/*`. */
export function DashboardPage() {
  const { utilisateur } = useAuth()
  const role = utilisateur?.role.nomRole ?? 'ACCUEIL'

  const vue: VueDashboard =
    role === 'ECONOMAT' ? 'ECONOMAT' : role === 'ACCUEIL' ? 'ACCUEIL' : 'SUPER_ADMIN'

  const sousTitre =
    vue === 'ACCUEIL'
      ? 'Suivi des clients, des demandes et des services.'
      : vue === 'ECONOMAT'
        ? 'Suivi des paiements, des échéances et des montants à encaisser.'
        : 'Vue d’ensemble de vos clients, demandes et paiements d’externalisation.'

  const [filtres, setFiltres] = useState<DashboardFiltres>(FILTRES_DASHBOARD_VIDES)
  const options = useDashboardFiltres()
  const { data, loading, error } = useDashboardData(vue, filtres)
  const summary = data?.summary
  const filtresActifs = Object.values(filtres).filter(Boolean).length

  function changerDateDebut(valeur: string) {
    setFiltres((actuels) => ({
      ...actuels,
      dateDebut: valeur,
      dateFin:
        actuels.dateFin && valeur && actuels.dateFin < valeur ? valeur : actuels.dateFin,
    }))
  }

  function changerDateFin(valeur: string) {
    setFiltres((actuels) => ({
      ...actuels,
      dateFin: valeur,
      dateDebut:
        actuels.dateDebut && valeur && valeur < actuels.dateDebut
          ? valeur
          : actuels.dateDebut,
    }))
  }
  const montantEnRetard =
    data?.echeances?.enRetard.reduce((total, e) => total + e.reste, 0) ?? 0
  const pourcentagePaye =
    summary && (summary.montantTotal ?? 0) > 0
      ? Math.min(100, Math.round(((summary.montantPaye ?? 0) / (summary.montantTotal ?? 1)) * 100))
      : 0

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

      {loading && !data && <Spinner label="Chargement du tableau de bord…" />}

      {error && (
        <div className="alert alert-danger" role="alert">
          {error}
        </div>
      )}

      <section className="card card-pad dash-filters">
        <div className="dash-filters__head">
          <div>
            <h3>Filtres</h3>
            <p className="section-note">
              Période, client et service communs à tous les indicateurs.
            </p>
          </div>
          <div className="dash-filters__actions">
            {loading && data && <span className="section-note">Mise à jour…</span>}
            {filtresActifs > 0 && (
              <span className="pill wt">
                {filtresActifs} filtre{filtresActifs > 1 ? 's' : ''} actif
                {filtresActifs > 1 ? 's' : ''}
              </span>
            )}
            <button
              type="button"
              className="btn-ghost"
              onClick={() => setFiltres(FILTRES_DASHBOARD_VIDES)}
              disabled={filtresActifs === 0}
            >
              Réinitialiser
            </button>
          </div>
        </div>

        <div className="filters-grid">
          <label className="field">
            Période — du
            <input
              type="date"
              value={filtres.dateDebut}
              max={filtres.dateFin || undefined}
              onChange={(e) => changerDateDebut(e.target.value)}
            />
          </label>
          <label className="field">
            Période — au
            <input
              type="date"
              value={filtres.dateFin}
              min={filtres.dateDebut || undefined}
              onChange={(e) => changerDateFin(e.target.value)}
            />
          </label>
          <label className="field">
            Client
            <select
              value={filtres.clientId}
              onChange={(e) => setFiltres((f) => ({ ...f, clientId: e.target.value }))}
              disabled={options.loading}
            >
              <option value="">Tous les clients</option>
              {options.data?.clients.map((client) => (
                <option key={client.id} value={client.id}>
                  {client.libelle ?? `Client #${client.id}`}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            Service
            <select
              value={filtres.serviceId}
              onChange={(e) => setFiltres((f) => ({ ...f, serviceId: e.target.value }))}
              disabled={options.loading}
            >
              <option value="">Tous les services</option>
              {options.data?.services.map((service) => (
                <option key={service.id} value={service.id}>
                  {service.libelle ?? `Service #${service.id}`}
                </option>
              ))}
            </select>
          </label>
        </div>
        {options.error && (
          <p className="field-hint">Options de filtres indisponibles : {options.error}</p>
        )}
      </section>

      {data && summary && (
        <>
          {vue === 'ACCUEIL' && (
            <section className="kpi-grid">
              <KpiDemandes
                icon="users"
                valeur={(summary.nombreClients ?? 0).toLocaleString('fr-FR')}
                label="Nombre de clients"
              />
              <KpiDemandes
                icon="doc"
                tag={`${summary.demandesEnAttente ?? 0} en attente`}
                valeur={(summary.nombreDemandes ?? 0).toLocaleString('fr-FR')}
                label="Nombre de demandes"
                highlight
              />
              <KpiDemandes
                icon="layers"
                valeur={(summary.nombreServices ?? 0).toLocaleString('fr-FR')}
                label="Nombre de services"
              />
            </section>
          )}

          {vue === 'ECONOMAT' && (
            <section className="kpi-grid">
              <KpiDemandes
                icon="card"
                valeur={formatMontantMGA(summary.montantTotal)}
                label="Montant total à payer"
              />
              <KpiDemandes
                icon="check"
                tag={`${pourcentagePaye} %`}
                valeur={formatMontantMGA(summary.montantPaye)}
                label="Montant total payé"
                highlight
              />
              <KpiDemandes
                icon="clock"
                tag={`${100 - pourcentagePaye} %`}
                valeur={formatMontantMGA(summary.resteAPayer)}
                label="Montant restant à payer"
              />
            </section>
          )}

          {vue === 'SUPER_ADMIN' && (
            <section className="kpi-grid kpi-grid--5">
              <KpiDemandes
                icon="users"
                valeur={(summary.nombreClients ?? 0).toLocaleString('fr-FR')}
                label="Nombre de clients"
              />
              <KpiDemandes
                icon="doc"
                tag={`${summary.demandesEnAttente ?? 0} en attente`}
                valeur={(summary.nombreDemandes ?? 0).toLocaleString('fr-FR')}
                label="Nombre de demandes"
                highlight
              />
              <KpiDemandes
                icon="layers"
                valeur={(summary.nombreServices ?? 0).toLocaleString('fr-FR')}
                label="Nombre de services"
              />
              <KpiDemandes
                icon="card"
                valeur={formatMontantMGA(summary.montantTotal)}
                label="Montant total à payer"
              />
              <KpiDemandes
                icon="check"
                tag={`${pourcentagePaye} %`}
                valeur={formatMontantMGA(summary.montantPaye)}
                label="Montant total payé"
                highlight
              />
              <KpiDemandes
                icon="clock"
                tag={`${100 - pourcentagePaye} %`}
                valeur={formatMontantMGA(summary.resteAPayer)}
                label="Montant restant à payer"
              />
            </section>
          )}

          <section className="card card-pad">
            <div className="dash-card-head">
              <h3>Indicateurs de performance</h3>
              {filtresActifs > 0 && <span className="pill wt">Périmètre filtré</span>}
            </div>
            <PerformancesPanel donnees={data.performances} vue={vue} />
          </section>

          <div className="dash-grid">
            {vue !== 'ECONOMAT' && (
              <section className="card card-pad">
                <h3>Évolution des demandes</h3>
                <CourbeEvolution donnees={data.demandesMois} />
              </section>
            )}
            {vue !== 'ECONOMAT' && (
              <section className="card card-pad">
                <h3>Demandes par statut</h3>
                <DonutStatuts donnees={data.statuts} />
              </section>
            )}
            {vue !== 'ACCUEIL' && (
              <section className="card card-pad">
                <h3>Montant des services</h3>
                <MontantServices donnees={data.servicesRevenu} />
              </section>
            )}
            {vue !== 'ACCUEIL' && (
              <section className="card card-pad">
                <h3>Paiements par mois</h3>
                <BarresPaiementsMensuels donnees={data.paiements?.parMois ?? []} />
              </section>
            )}
            {vue !== 'ACCUEIL' && (
              <section className="card card-pad">
                <div className="dash-card-head">
                  <h3>Échéances en retard</h3>
                  <Link to="/paiements" className="lk">
                    Voir les retards ›
                  </Link>
                </div>
                <TranchesRetard echeances={data.echeances?.enRetard ?? []} />
              </section>
            )}
            {vue !== 'ECONOMAT' && (
              <section className="card card-pad">
                <h3>Services les plus demandés</h3>
                <TopServices donnees={data.servicesTop} />
              </section>
            )}
          </div>

          <section className="card card-pad">
            <h3>Alertes</h3>
            <CartesAlertes alertes={construireAlertes(vue, summary, montantEnRetard)} />
          </section>
        </>
      )}
    </div>
  )
}

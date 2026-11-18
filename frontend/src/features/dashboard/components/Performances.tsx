import type { VueDashboard } from '../hooks'
import type { PerformancesDashboard } from '../types'

function formatTaux(valeur: number): string {
  return `${valeur.toLocaleString('fr-FR', { maximumFractionDigits: 1 })} %`
}

function libelleMois(mois: string): string {
  const [annee, numero] = mois.split('-').map(Number)
  if (!annee || !numero) return mois
  return new Date(annee, numero - 1, 1).toLocaleDateString('fr-FR', { month: 'short' })
}

interface CartePerformance {
  label: string
  valeur: string
  detail?: string
}

interface PerformancesPanelProps {
  donnees: PerformancesDashboard
  vue: VueDashboard
}

/** Indicateurs de performance, limités aux champs autorisés par le rôle. */
export function PerformancesPanel({ donnees, vue }: PerformancesPanelProps) {
  const cartes: CartePerformance[] = []

  if (donnees.demandesTerminees !== null) {
    cartes.push({
      label: 'Demandes terminées',
      valeur: donnees.demandesTerminees.toLocaleString('fr-FR'),
      detail:
        donnees.tauxDemandesTerminees === null
          ? undefined
          : `${formatTaux(donnees.tauxDemandesTerminees)} des demandes`,
    })
  }
  if (donnees.servicePlusDemande !== null) {
    cartes.push({
      label: 'Service le plus demandé',
      valeur: donnees.servicePlusDemande.nomService,
      detail: `${donnees.servicePlusDemande.nombreDemandes.toLocaleString('fr-FR')} demande${
        donnees.servicePlusDemande.nombreDemandes > 1 ? 's' : ''
      }`,
    })
  }
  if (donnees.commercialTop !== null) {
    cartes.push({
      label: 'Commercial le plus actif',
      valeur: donnees.commercialTop.nomCommercial,
      detail: `${donnees.commercialTop.nombreDemandes.toLocaleString('fr-FR')} demande${
        donnees.commercialTop.nombreDemandes > 1 ? 's' : ''
      }`,
    })
  }

  const parMois = donnees.demandesTermineesParMois.slice(-6)
  const maxMois = Math.max(1, ...parMois.map((m) => m.nombre))

  if (cartes.length === 0 && parMois.length === 0) {
    return <p className="section-note">Aucun indicateur de performance à afficher.</p>
  }

  return (
    <div className="perf-panel">
      {cartes.length > 0 && (
        <div className="perf-grid">
          {cartes.map((carte) => (
            <div key={carte.label} className="perf-card">
              <span className="perf-card__label">{carte.label}</span>
              <strong className="perf-card__valeur">{carte.valeur}</strong>
              {carte.detail && <small className="perf-card__detail">{carte.detail}</small>}
            </div>
          ))}
        </div>
      )}

      {vue !== 'ECONOMAT' && parMois.length > 0 && (
        <div className="perf-mois">
          <h4>Demandes terminées par mois</h4>
          <div className="perf-mois__barres" role="img" aria-label="Demandes terminées par mois">
            {parMois.map((m) => (
              <div key={m.mois} className="perf-mois__colonne">
                <span className="perf-mois__valeur">{m.nombre}</span>
                <span
                  className="perf-mois__barre"
                  style={{ height: `${Math.max(6, Math.round((m.nombre / maxMois) * 80))}px` }}
                />
                <small>{libelleMois(m.mois)}</small>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

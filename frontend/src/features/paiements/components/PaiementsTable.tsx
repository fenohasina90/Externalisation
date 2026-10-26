import { Link } from 'react-router-dom'
import { formatMontantMGA } from '@/utils/format'
import { classeEtatEcheance, getLibelleEtatEcheance } from '@/features/echeances/constants/labels'
import { SituationPaiementPill, StatutDemandePill } from '@/features/demandes/components'
import type { Demande } from '@/features/demandes/types'
import { classeMode } from '../constants/labels'
import type { SituationPlan } from '../types'

export { StatutDemandePill, SituationPaiementPill }

/** La situation de règlement est calculée par le backend. */
export function SituationPlanPill({ situation }: { situation: SituationPlan }) {
  return <SituationPaiementPill situation={situation} />
}

export function ModePill({ code }: { code: string | null | undefined }) {
  return <span className={classeMode(code)}>● {code ?? '—'}</span>
}

export function EcheancePill({ statut }: { statut: string }) {
  return (
    <span className={classeEtatEcheance(statut)}>● {getLibelleEtatEcheance(statut)}</span>
  )
}

interface Props {
  items: Demande[]
}

/** Suivi financier par demande — montants agrégés par le backend. */
export function SuiviTable({ items }: Props) {
  if (items.length === 0)
    return <div className="card-pad text-muted">Aucune demande à afficher.</div>

  return (
    <table className="table">
      <thead>
        <tr>
          <th>Référence</th>
          <th>Client</th>
          <th>Services</th>
          <th>Montant total</th>
          <th>Montant payé</th>
          <th>Reste à payer</th>
          <th>Statut</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {items.map((d) => {
          const nbServices = d.lignes?.length ?? 0
          const noms = (d.lignes ?? [])
            .map((l) => l.nomService)
            .filter((n): n is string => n !== null)
          const premier = noms[0] ?? '—'
          return (
            <tr key={d.id}>
              <td className="fw-700">{d.referenceDemande}</td>
              <td>{d.nomSociete ?? '—'}</td>
              <td>
                <span className="fw-600">{premier}</span>
                {nbServices > 1 && (
                  <div className="cell-hint">
                    + {nbServices - 1} autre{nbServices > 2 ? 's' : ''}
                  </div>
                )}
              </td>
              <td className="cell-number">{formatMontantMGA(d.montantTotal ?? 0)}</td>
              <td className="cell-number">{formatMontantMGA(d.montantPaye)}</td>
              <td className="cell-number fw-700">{formatMontantMGA(d.resteAPayer)}</td>
              <td>
                <StatutDemandePill statut={d.statutActuel ?? '—'} />
              </td>
              <td>
                <Link
                  to={`/paiements/echeanciers/${d.referenceDemande}`}
                  className="btn-link success"
                >
                  Échéancier ›
                </Link>
              </td>
            </tr>
          )
        })}
      </tbody>
    </table>
  )
}
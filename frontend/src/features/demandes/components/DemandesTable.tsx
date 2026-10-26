import { Link } from 'react-router-dom'
import { formatMontantMGA } from '@/utils/format'
import type { Demande, SituationPaiement } from '../types'
import {
  classeSituationPaiement,
  getLibelleSituationPaiement,
} from '../constants/labels'

export function StatutDemandePill({ statut }: { statut: string }) {
  const classe =
    statut === 'EN_COURS' ? 'pill info'
    : statut === 'TERMINEE' || statut === 'VALIDATION' ? 'pill ok'
    : statut === 'ANNULEE' ? 'pill danger'
    : 'pill wt'
  const libelles: Record<string, string> = {
    EN_ATTENTE: 'En attente',
    VALIDATION: 'Validation',
    EN_COURS: 'En cours',
    TERMINEE: 'Terminée',
    ANNULEE: 'Annulée',
  }
  return <span className={classe}>● {libelles[statut] ?? statut}</span>
}

export function PaiementPill({ paye, total }: { paye: number; total: number }) {
  const situation = total <= 0 || paye >= total ? 'Payé' : paye > 0 ? 'Partiel' : 'En attente'
  const classe =
    situation === 'Payé' ? 'pill ok'
    : situation === 'Partiel' ? 'pill warning'
    : 'pill wt'
  return <span className={classe}>● {situation}</span>
}

/** Situation de règlement calculée par le backend (toutes lignes confondues). */
export function SituationPaiementPill({ situation }: { situation: string }) {
  const valeur = situation as SituationPaiement
  return (
    <span className={classeSituationPaiement(valeur)}>
      ● {getLibelleSituationPaiement(situation)}
    </span>
  )
}

interface Props {
  items: Demande[]
}

/** Tableau des demandes multi-services — action Voir vers le détail. */
export function DemandesTable({ items }: Props) {
  if (items.length === 0)
    return <div className="card-pad text-muted">Aucune demande trouvée.</div>

  return (
    <table className="table">
      <thead>
        <tr>
          <th>Référence</th>
          <th>Client</th>
          <th>Services</th>
          <th>Montant total</th>
          <th>Payé / Reste</th>
          <th>Statut</th>
          <th>Situation du paiement</th>
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
                {nbServices > 1 && <div className="cell-hint">+ {nbServices - 1} autre{nbServices > 2 ? 's' : ''}</div>}
              </td>
              <td className="fw-600">{formatMontantMGA(d.montantTotal)}</td>
              <td className="text-secondary">
                {formatMontantMGA(d.montantPaye)} / {formatMontantMGA(d.resteAPayer)}
              </td>
              <td>
                <StatutDemandePill statut={d.statutActuel} />
              </td>
              <td>
                {d.situationPaiement ? (
                  <SituationPaiementPill situation={d.situationPaiement} />
                ) : (
                  <PaiementPill paye={d.montantPaye} total={d.montantTotal ?? 0} />
                )}
              </td>
              <td>
                <Link to={`/demandes/${d.referenceDemande}`} className="lk">
                  Voir ›
                </Link>
              </td>
            </tr>
          )
        })}
      </tbody>
    </table>
  )
}

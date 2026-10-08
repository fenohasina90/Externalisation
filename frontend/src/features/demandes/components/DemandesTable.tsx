import { Link } from 'react-router-dom'
import { STATUT_LABEL, type SituationPaiement, type StatutDemandeUI } from '../data/mockDemandes'

export function StatutDemandePill({ statut }: { statut: StatutDemandeUI }) {
  const classe =
    statut === 'EN_COURS' ? 'pill info'
    : statut === 'TERMINEE' || statut === 'VALIDATION' ? 'pill ok'
    : statut === 'ANNULEE' ? 'pill danger'
    : 'pill wt'
  return <span className={classe}>● {STATUT_LABEL[statut]}</span>
}

export function PaiementPill({ situation }: { situation: SituationPaiement }) {
  const classe =
    situation === 'Payé' ? 'pill ok'
    : situation === 'Partiel' ? 'pill warning'
    : situation === 'En retard' ? 'pill danger'
    : 'pill wt'
  return <span className={classe}>● {situation}</span>
}

interface Props {
  items: import('../data/mockDemandes').DemandeResume[]
}

/** Tableau des demandes — action Voir vers le détail. */
export function DemandesTable({ items }: Props) {
  if (items.length === 0)
    return <div className="card-pad text-muted">Aucune demande trouvée.</div>

  return (
    <table className="table">
      <thead>
        <tr>
          <th>Référence</th>
          <th>Client</th>
          <th>Service</th>
          <th>Date de début</th>
          <th>Date de fin</th>
          <th>Statut</th>
          <th>Situation du paiement</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {items.map((d) => (
          <tr key={d.reference}>
            <td className="fw-700">{d.reference}</td>
            <td>{d.client}</td>
            <td>
              <span className="fw-600">{d.service}</span>
              <div className="cell-hint">{d.domaine}</div>
            </td>
            <td className="text-secondary">{d.dateDebut}</td>
            <td className="text-secondary">{d.dateFin}</td>
            <td>
              <StatutDemandePill statut={d.statut} />
            </td>
            <td>
              <PaiementPill situation={d.situationPaiement} />
            </td>
            <td>
              <Link to={`/demandes/${d.reference}`} className="lk">
                Voir ›
              </Link>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

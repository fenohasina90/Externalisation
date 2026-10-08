import { Link } from 'react-router-dom'
import { formatMGA, type ModePaiement, type PlanPaiement, type StatutPlan } from '../data/mockPaiements'

export function StatutPlanPill({ statut }: { statut: StatutPlan }) {
  const classe =
    statut === 'Payé' ? 'pill ok'
    : statut === 'Partiel' ? 'pill warning'
    : statut === 'En retard' ? 'pill danger'
    : 'pill wt'
  return <span className={classe}>● {statut}</span>
}

export function ModePill({ mode }: { mode: ModePaiement }) {
  return <span className={mode === 'Virement' ? 'pill info' : 'pill wt'}>● {mode}</span>
}

export function StatutEcheancePill({ attendu, paye }: { attendu: number; paye: number }) {
  const reste = attendu - paye
  if (reste === 0) return <span className="pill ok">● Payée</span>
  if (paye > 0) return <span className="pill warning">● Partielle</span>
  return <span className="pill wt">● À venir</span>
}

/** Tableau des plans de paiement — action vers l'échéancier. */
export function PlansTable({ items }: { items: PlanPaiement[] }) {
  if (items.length === 0)
    return <div className="card-pad text-muted">Aucun paiement trouvé.</div>

  return (
    <table className="table">
      <thead>
        <tr>
          <th>Référence</th>
          <th>Client</th>
          <th>Demande</th>
          <th>Montant total</th>
          <th>Montant payé</th>
          <th>Reste à payer</th>
          <th>Statut</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {items.map((p) => (
          <tr key={p.reference}>
            <td className="fw-700">{p.reference}</td>
            <td>{p.client}</td>
            <td className="text-secondary">{p.demandeRef}</td>
            <td className="cell-number">{formatMGA(p.montantTotal)}</td>
            <td className="cell-number">{formatMGA(p.montantPaye)}</td>
            <td className="cell-number fw-700">{formatMGA(p.montantTotal - p.montantPaye)}</td>
            <td>
              <StatutPlanPill statut={p.statut} />
            </td>
            <td>
              <Link to={`/paiements/echeanciers/${p.demandeRef}`} className="btn-link success">
                Échéancier ›
              </Link>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

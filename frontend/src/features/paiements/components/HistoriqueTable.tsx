import { formatMGA, type PaiementHistorique } from '../data/mockPaiements'
import { ModePill } from './PaiementsTable'

/** Tableau de l'historique des règlements. */
export function HistoriqueTable({ items }: { items: PaiementHistorique[] }) {
  if (items.length === 0)
    return <div className="card-pad text-muted">Aucun paiement trouvé.</div>

  return (
    <table className="table">
      <thead>
        <tr>
          <th>Référence</th>
          <th>Demande</th>
          <th>Client</th>
          <th>Date</th>
          <th>Montant</th>
          <th>Mode de paiement</th>
          <th>Utilisateur</th>
          <th>Réf. virement</th>
        </tr>
      </thead>
      <tbody>
        {items.map((p) => (
          <tr key={p.reference}>
            <td className="fw-700">{p.reference}</td>
            <td className="text-secondary">{p.demandeRef}</td>
            <td>{p.client}</td>
            <td className="text-secondary">{p.date}</td>
            <td className="cell-number fw-700">{formatMGA(p.montant)}</td>
            <td>
              <ModePill mode={p.mode} />
            </td>
            <td>{p.utilisateur}</td>
            <td className="text-secondary">{p.refVirement ?? '—'}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

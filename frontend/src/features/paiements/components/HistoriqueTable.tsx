import { formatDate, formatMontantMGA } from '@/utils/format'
import { Can } from '@/components/shared/Can'
import { paiementsApi } from '../services/paiements.service'
import type { Paiement } from '../types'
import { ModePill } from './PaiementsTable'

/** Historique des règlements enregistrés. */
export function HistoriqueTable({ items }: { items:Paiement[] }) {
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
          <th>Reçu</th>
        </tr>
      </thead>
      <tbody>
        {items.map((p) => (
          <tr key={p.id}>
            <td className="fw-700">{p.referencePaiement}</td>
            <td className="text-secondary">{p.referenceDemande}</td>
            <td>{p.nomSociete ?? '—'}</td>
            <td className="text-secondary">{formatDate(p.datePaiement)}</td>
            <td className="cell-number fw-700">{formatMontantMGA(p.montantPaye)}</td>
            <td>
              <ModePill code={p.libelleMode} />
            </td>
            <td>{p.createdByName ?? '—'}</td>
            <td className="text-secondary">{p.referenceVirement ?? '—'}</td>
            <td>
              <Can permission="DOCUMENT_PAIEMENT_VIEW">
                <a
                  className="btn-link success"
                  href={paiementsApi.recepisseUrl(p.id)}
                  target="_blank"
                  rel="noreferrer"
                >
                  PDF ›
                </a>
              </Can>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
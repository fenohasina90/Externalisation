import { Can } from '@/components/shared/Can'
import type { ModePaiementAdmin } from '../types'

interface Props {
  items: ModePaiementAdmin[]
  loading?: boolean
  onEdit: (mode: ModePaiementAdmin) => void
  onToggleActif: (id: number, actif: boolean) => void
}

/** Tableau des modes de paiement — mêmes classes que les autres pages liste. */
export function ModesPaiementTable({ items, loading = false, onEdit, onToggleActif }: Props) {
  if (loading) return <div className="card-pad text-muted">Chargement des modes...</div>
  if (items.length === 0)
    return <div className="card-pad text-muted">Aucun mode de paiement trouvé.</div>

  return (
    <table className="table">
      <thead>
        <tr>
          <th>Code</th>
          <th>Libellé</th>
          <th>État</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {items.map((m) => (
          <tr key={m.id}>
            <td className="fw-700">{m.code}</td>
            <td>{m.libelle}</td>
            <td>
              {m.actif ? (
                <span className="pill ok">Actif</span>
              ) : (
                <span className="pill wt">Inactif</span>
              )}
            </td>
            <td>
              <Can permission="MODE_PAIEMENT_UPDATE">
                <div className="table-actions">
                  <button type="button" className="btn-link" onClick={() => onEdit(m)}>
                    Modifier
                  </button>
                  {m.actif ? (
                    <button
                      type="button"
                      className="btn-link danger"
                      onClick={() => onToggleActif(m.id, false)}
                    >
                      Désactiver
                    </button>
                  ) : (
                    <button
                      type="button"
                      className="btn-link success"
                      onClick={() => onToggleActif(m.id, true)}
                    >
                      Activer
                    </button>
                  )}
                </div>
              </Can>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

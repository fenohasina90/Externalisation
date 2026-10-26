import { Can } from '@/components/shared/Can'
import type { StatutDemandeConfig } from '../types'

interface Props {
  items: StatutDemandeConfig[]
  loading?: boolean
  onEdit: (statut: StatutDemandeConfig) => void
  onToggleActif: (id: number, actif: boolean) => void
}

/** Tableau des statuts de demande — mêmes classes que les autres pages liste. */
export function StatutsTable({ items, loading = false, onEdit, onToggleActif }: Props) {
  if (loading) return <div className="card-pad text-muted">Chargement des statuts...</div>
  if (items.length === 0)
    return <div className="card-pad text-muted">Aucun statut trouvé.</div>

  return (
    <table className="table">
      <thead>
        <tr>
          <th>Ordre</th>
          <th>Statut</th>
          <th>Description</th>
          <th>État</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {items.map((s) => (
          <tr key={s.id}>
            <td className="cell-number">{s.ordre}</td>
            <td className="fw-700">{s.statut}</td>
            <td className="truncate text-secondary" title={s.description ?? ''}>
              {s.description ?? '—'}
            </td>
            <td>
              {s.actif ? (
                <span className="pill ok">Actif</span>
              ) : (
                <span className="pill wt">Inactif</span>
              )}
            </td>
            <td>
              <Can permission="CONFIG_UPDATE">
                <div className="table-actions">
                  <button type="button" className="btn-link" onClick={() => onEdit(s)}>
                    Modifier
                  </button>
                  {s.actif ? (
                    <button
                      type="button"
                      className="btn-link danger"
                      onClick={() => onToggleActif(s.id, false)}
                    >
                      Désactiver
                    </button>
                  ) : (
                    <button
                      type="button"
                      className="btn-link success"
                      onClick={() => onToggleActif(s.id, true)}
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

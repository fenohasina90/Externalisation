import type { Domaine } from '../types'

interface Props {
  items: Domaine[]
  loading?: boolean
  onEdit: (d: Domaine) => void
  onToggleActif: (id: number, actif: boolean) => void
}

/** Tableau des domaines de société — mêmes classes que les autres pages liste. */
export function DomainesTable({ items, loading = false, onEdit, onToggleActif }: Props) {
  if (loading) return <div className="card-pad text-muted">Chargement des domaines...</div>
  if (items.length === 0)
    return <div className="card-pad text-muted">Aucun domaine trouvé.</div>

  return (
    <table className="table">
      <thead>
        <tr>
          <th>Nom du domaine</th>
          <th>Nombre de sociétés associées</th>
          <th>Statut</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {items.map((d) => (
          <tr key={d.id}>
            <td className="fw-700">{d.nomDomaine}</td>
            <td className="cell-number">{d.nombreSocietes}</td>
            <td>
              {d.actif ? (
                <span className="pill ok">Actif</span>
              ) : (
                <span className="pill wt">Inactif</span>
              )}
            </td>
            <td>
              <div className="table-actions">
                <button type="button" className="btn-link" onClick={() => onEdit(d)}>
                  Modifier
                </button>
                {d.actif ? (
                  <button
                    type="button"
                    className="btn-link danger"
                    onClick={() => onToggleActif(d.id, false)}
                  >
                    Désactiver
                  </button>
                ) : (
                  <button
                    type="button"
                    className="btn-link success"
                    onClick={() => onToggleActif(d.id, true)}
                  >
                    Activer
                  </button>
                )}
              </div>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

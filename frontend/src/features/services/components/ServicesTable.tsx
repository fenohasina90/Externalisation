import type { Service } from '../types'

interface Props {
  items: Service[]
  loading?: boolean
  onView: (s: Service) => void
  onEdit: (s: Service) => void
  onArchive: (id: number) => void
}

/** Tableau des services — mêmes classes que les autres pages liste. */
export function ServicesTable({ items, loading = false, onView, onEdit, onArchive }: Props) {
  if (loading) return <div className="card-pad text-muted">Chargement des services...</div>
  if (items.length === 0)
    return <div className="card-pad text-muted">Aucun service trouvé.</div>

  return (
    <table className="table">
      <thead>
        <tr>
          <th>Service</th>
          <th>Code</th>
          <th>Description</th>
          <th>Statut</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {items.map((s) => (
          <tr key={s.id}>
            <td className="fw-700">{s.nomService}</td>
            <td className="text-secondary">{s.codeService ?? '—'}</td>
            <td className="text-secondary" style={{ whiteSpace: 'normal', minWidth: 220 }}>
              {s.description ?? '—'}
            </td>
            <td>
              {s.actif ? (
                <span className="pill ok">Actif</span>
              ) : (
                <span className="pill wt">Archivé</span>
              )}
            </td>
            <td>
              <div className="table-actions">
                <button type="button" className="btn-link" onClick={() => onView(s)}>
                  Consulter
                </button>
                <button type="button" className="btn-link" onClick={() => onEdit(s)}>
                  Modifier
                </button>
                <button
                  type="button"
                  className="btn-link danger"
                  onClick={() => onArchive(s.id)}
                  disabled={!s.actif}
                >
                  Archiver
                </button>
              </div>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

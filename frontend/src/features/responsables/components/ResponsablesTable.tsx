import type { Responsable } from '../types'

interface Props {
  items: Responsable[]
  loading: boolean
  onEdit: (r: Responsable) => void
  onToggleActif: (id: number, actif: boolean) => void
}

function initials(nomClient: string) {
  const parts = (nomClient || '').trim()
  if (!parts) return '—'
  const first = parts.split(/\s+/)[0]
  return first[0]?.toUpperCase() || '—'
}

export function ResponsablesTable({ items, loading, onEdit, onToggleActif }: Props) {
  if (loading) {
    return <div className="card-pad text-muted">Chargement des responsables...</div>
  }

  if (items.length === 0) {
    return <div className="card-pad text-muted">Aucun responsable trouvé.</div>
  }

  return (
    <table className="table">
      <thead>
        <tr>
          <th>Société</th>
          <th>Nom</th>
          <th>Fonction</th>
          <th>Téléphone</th>
          <th>Email</th>
          <th>Statut</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {items.map((r) => (
          <tr key={r.id}>
            <td>
              <b className="fw-700">{r.nomSociete ?? '—'}</b>
            </td>
            <td>
              <span className="avatar">{initials(r.nomClient)}</span>
              <span className="fw-600">{r.nomClient}</span>
            </td>
            <td className="text-secondary">{r.fonction ?? '—'}</td>
            <td className="text-secondary">{r.contact ?? '—'}</td>
            <td className="truncate text-secondary" title={r.email ?? ''}>
              {r.email ?? '—'}
            </td>
            <td>
              {r.actif ? <span className="pill ok">Actif</span> : <span className="pill wt">Inactif</span>}
            </td>
            <td>
              <div className="table-actions">
                <button type="button" className="btn-link" onClick={() => onEdit(r)}>
                  Modifier
                </button>
                {r.actif ? (
                  <button
                    type="button"
                    className="btn-link danger"
                    onClick={() => onToggleActif(r.id, false)}
                  >
                    Désactiver
                  </button>
                ) : (
                  <button
                    type="button"
                    className="btn-link success"
                    onClick={() => onToggleActif(r.id, true)}
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

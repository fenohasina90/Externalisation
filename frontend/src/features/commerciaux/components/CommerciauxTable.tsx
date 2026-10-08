import type { Commercial } from '../types'

interface Props {
  items: Commercial[]
  /** Optionnel pour réutilisation avec l'API (étape 2). */
  loading?: boolean
  onEdit: (c: Commercial) => void
  onToggleActif: (id: number, actif: boolean) => void
}

/** Initiales "JD" pour Jean Dupont (cf. maquette). */
export function commercialInitials(nom: string): string {
  const parts = (nom || '').trim().split(/\s+/).filter(Boolean)
  if (parts.length === 0) return '—'
  if (parts.length === 1) return (parts[0][0] ?? '—').toUpperCase()
  return `${parts[0][0] ?? ''}${parts[parts.length - 1][0] ?? ''}`.toUpperCase()
}

/** Tableau des commerciaux — mêmes classes que ResponsablesTable. */
export function CommerciauxTable({ items, loading = false, onEdit, onToggleActif }: Props) {
  if (loading) {
    return <div className="card-pad text-muted">Chargement des commerciaux...</div>
  }

  if (items.length === 0) {
    return <div className="card-pad text-muted">Aucun commercial trouvé.</div>
  }

  return (
    <table className="table">
      <thead>
        <tr>
          <th>Nom</th>
          <th>Matricule</th>
          <th>Contact</th>
          <th>Email</th>
          <th>Statut</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {items.map((c) => (
          <tr key={c.id}>
            <td>
              <span className="avatar">{commercialInitials(c.nomCommercial)}</span>
              <span className="fw-600">{c.nomCommercial}</span>
            </td>
            <td className="text-secondary">{c.immatricule ?? '—'}</td>
            <td className="text-secondary">{c.contact ?? '—'}</td>
            <td className="truncate text-secondary" title={c.email ?? ''}>
              {c.email ?? '—'}
            </td>
            <td>
              {c.actif ? (
                <span className="pill ok">Actif</span>
              ) : (
                <span className="pill wt">Inactif</span>
              )}
            </td>
            <td>
              <div className="table-actions">
                <button type="button" className="btn-link" onClick={() => onEdit(c)}>
                  Modifier
                </button>
                {c.actif ? (
                  <button
                    type="button"
                    className="btn-link danger"
                    onClick={() => onToggleActif(c.id, false)}
                  >
                    Désactiver
                  </button>
                ) : (
                  <button
                    type="button"
                    className="btn-link success"
                    onClick={() => onToggleActif(c.id, true)}
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

import type { Utilisateur } from '@/features/auth/types'

interface Props {
  items: Utilisateur[]
  loading?: boolean
  onEdit: (u: Utilisateur) => void
  onResetPassword: (u: Utilisateur) => void
  onToggleActif: (id: number, actif: boolean) => void
}

function initials(u: Utilisateur): string {
  const p = u.prenom?.trim()?.[0] ?? ''
  const n = u.nom?.trim()?.[0] ?? ''
  return `${p}${n}`.toUpperCase() || '—'
}

function rolePill(nomRole: string): string {
  switch (nomRole) {
    case 'SUPER_ADMIN':
      return 'pill ok'
    case 'ACCUEIL':
      return 'pill info'
    case 'ECONOMAT':
      return 'pill warning'
    default:
      return 'pill wt'
  }
}

/** Tableau des utilisateurs — mêmes classes que les autres pages liste. */
export function UtilisateursTable({ items, loading = false, onEdit, onResetPassword, onToggleActif }: Props) {
  if (loading) return <div className="card-pad text-muted">Chargement des utilisateurs...</div>
  if (items.length === 0)
    return <div className="card-pad text-muted">Aucun utilisateur trouvé.</div>

  return (
    <table className="table">
      <thead>
        <tr>
          <th>Nom</th>
          <th>Email</th>
          <th>Rôle</th>
          <th>Statut</th>
          <th>Dernière connexion</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {items.map((u) => (
          <tr key={u.id}>
            <td>
              <span className="avatar">{initials(u)}</span>
              <span className="fw-600">
                {u.prenom} {u.nom}
              </span>
            </td>
            <td className="truncate text-secondary" title={u.email}>
              {u.email}
            </td>
            <td>
              <span className={rolePill(u.role.nomRole)}>● {u.role.nomRole}</span>
            </td>
            <td>
              {u.actif ? (
                <span className="pill ok">Actif</span>
              ) : (
                <span className="pill wt">Inactif</span>
              )}
            </td>
            <td className="text-secondary">{u.derniereConnexion ?? '—'}</td>
            <td>
              <div className="table-actions">
                <button type="button" className="btn-link" onClick={() => onEdit(u)}>
                  Modifier
                </button>
                <button type="button" className="btn-link" onClick={() => onResetPassword(u)}>
                  Réinit. mot de passe
                </button>
                {u.actif ? (
                  <button
                    type="button"
                    className="btn-link danger"
                    onClick={() => onToggleActif(u.id, false)}
                  >
                    Désactiver
                  </button>
                ) : (
                  <button
                    type="button"
                    className="btn-link success"
                    onClick={() => onToggleActif(u.id, true)}
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

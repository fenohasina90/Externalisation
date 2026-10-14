import { Link } from 'react-router-dom'
import { codeSociete, statutSociete, type Societe } from '../types'

interface Props {
  items: Societe[]
  onEdit: (c: Societe) => void
  onArchiver: (id: number) => void
}

function initiales(nomComplet: string): string {
  const parts = nomComplet.trim().split(/\s+/).filter(Boolean)
  if (parts.length === 0) return '—'
  if (parts.length === 1) return (parts[0][0] ?? '—').toUpperCase()
  return `${parts[0][0] ?? ''}${parts[parts.length - 1][0] ?? ''}`.toUpperCase()
}

function statutPill(statut: string): string {
  if (statut === 'Actif') return 'pill ok'
  return 'pill wt'
}

/** Tableau des clients — Consulter, Modifier, Archiver (définitif). */
export function ClientsTable({ items, onEdit, onArchiver }: Props) {
  if (items.length === 0)
    return <div className="card-pad text-muted">Aucun client trouvé.</div>

  return (
    <table className="table">
      <thead>
        <tr>
          <th>Client</th>
          <th>Activité</th>
          <th>Contact</th>
          <th>Responsable</th>
          <th>Nombre de demandes</th>
          <th>Statut</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {items.map((c) => {
          const statut = statutSociete(c)
          return (
            <tr key={c.id}>
              <td>
                <span className="fw-700">{c.nomSociete}</span>
                <div className="cell-hint">{codeSociete(c.id)}</div>
              </td>
              <td className="text-secondary">{c.activite ?? '—'}</td>
              <td className="text-secondary">
                {c.email ?? '—'}
                <div className="cell-hint">{c.contact ?? ''}</div>
              </td>
              <td>
                {c.responsablePrincipal ? (
                  <>
                    <span className="avatar">{initiales(c.responsablePrincipal)}</span>
                    <span className="fw-600">{c.responsablePrincipal}</span>
                  </>
                ) : (
                  <span className="text-muted">—</span>
                )}
              </td>
              <td className="cell-number fw-700">{c.nombreDemandes}</td>
              <td>
                <span className={statutPill(statut)}>● {statut}</span>
              </td>
              <td>
                <div className="table-actions">
                  <Link to={`/clients/${c.id}`} className="btn-link">
                    Consulter
                  </Link>
                  <button type="button" className="btn-link" onClick={() => onEdit(c)}>
                    Modifier
                  </button>
                  {statut !== 'Archivé' && (
                    <button
                      type="button"
                      className="btn-link danger"
                      onClick={() => onArchiver(c.id)}
                    >
                      Archiver
                    </button>
                  )}
                </div>
              </td>
            </tr>
          )
        })}
      </tbody>
    </table>
  )
}

import { Link } from 'react-router-dom'
import { initialesClient, type ClientResume } from '../data/mockClients'

interface Props {
  items: ClientResume[]
  onEdit: (c: ClientResume) => void
  onChangerStatut: (code: string, statut: ClientResume['statut']) => void
}

function statutPill(statut: ClientResume['statut']): string {
  if (statut === 'Actif') return 'pill ok'
  if (statut === 'Inactif') return 'pill wt'
  return 'pill wt'
}

/** Tableau des clients — Consulter, Modifier, Archiver/Restaurer. */
export function ClientsTable({ items, onEdit, onChangerStatut }: Props) {
  if (items.length === 0)
    return <div className="card-pad text-muted">Aucun client trouvé.</div>

  return (
    <table className="table">
      <thead>
        <tr>
          <th>Client ⇅</th>
          <th>Activité ⇅</th>
          <th>Contact</th>
          <th>Responsable ⇅</th>
          <th>Nombre de demandes ⇅</th>
          <th>Statut ⇅</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {items.map((c) => (
          <tr key={c.code}>
            <td>
              <span className="fw-700">{c.nom}</span>
              <div className="cell-hint">{c.code}</div>
            </td>
            <td className="text-secondary">{c.activite}</td>
            <td className="text-secondary">
              {c.email}
              <div className="cell-hint">{c.telephone}</div>
            </td>
            <td>
              <span className="avatar">{initialesClient(c.responsable)}</span>
              <span className="fw-600">{c.responsable}</span>
            </td>
            <td className="cell-number fw-700">{c.nbDemandes}</td>
            <td>
              <span className={statutPill(c.statut)}>● {c.statut}</span>
            </td>
            <td>
              <div className="table-actions">
                <Link to={`/clients/${c.code}`} className="btn-link">
                  Consulter
                </Link>
                <button type="button" className="btn-link" onClick={() => onEdit(c)}>
                  Modifier
                </button>
                {c.statut !== 'Archivé' && (
                  <button
                    type="button"
                    className="btn-link danger"
                    onClick={() => onChangerStatut(c.code, 'Archivé')}
                  >
                    Archiver
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

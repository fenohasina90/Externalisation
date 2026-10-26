import { getLibelleActionAudit } from '../constants/labels'
import type { AuditEntry } from '../types'
import { formatDateHeure, formatValeurAudit } from '@/utils/format'

const PILL_ACTION: Record<string, string> = {
  CREATE: 'pill ok',
  UPDATE: 'pill info',
  DELETE: 'pill danger',
  ARCHIVE: 'pill warning',
  LOGIN: 'pill wt',
  LOGOUT: 'pill wt',
  PAYMENT: 'pill ok',
  STATUS_CHANGE: 'pill info',
  RESET_REQUEST: 'pill warning',
  RESET_APPROVE: 'pill info',
  RESET_SEND: 'pill info',
  RESET_REJECT: 'pill danger',
  RESET_DONE: 'pill ok',
}

function pill(action: string): string {
  return PILL_ACTION[action] ?? 'pill wt'
}

interface Props {
  items: AuditEntry[]
  onView: (id: number) => void
}

/** Tableau du journal d'audit (clic sur une ligne = détail). */
export function AuditTable({ items, onView }: Props) {
  if (items.length === 0) return <div className="card-pad text-muted">Aucune entrée trouvée.</div>

  return (
    <table className="table">
      <thead>
        <tr>
          <th>Date et heure</th>
          <th>Utilisateur</th>
          <th>Action</th>
          <th>Module</th>
          <th>Entité</th>
          <th>ID entité</th>
          <th>Ancienne valeur</th>
          <th>Nouvelle valeur</th>
          <th>Adresse IP</th>
        </tr>
      </thead>
      <tbody>
        {items.map((e) => (
          <tr
            key={e.id}
            onClick={() => onView(e.id)}
            style={{ cursor: 'pointer' }}
            title="Voir le détail"
          >
            <td className="text-secondary">{formatDateHeure(e.createdAt)}</td>
            <td className="fw-600">{e.nomUtilisateur ?? '—'}</td>
            <td>
              <span className={pill(e.action)}>● {getLibelleActionAudit(e.action)}</span>
            </td>
            <td>{e.module}</td>
            <td>{e.entite ?? '—'}</td>
            <td className="text-secondary">{e.entiteId ?? '—'}</td>
            <td className="truncate text-secondary" title={formatValeurAudit(e.ancienneValeur)}>
              {formatValeurAudit(e.ancienneValeur)}
            </td>
            <td className="truncate" title={formatValeurAudit(e.nouvelleValeur)}>
              {formatValeurAudit(e.nouvelleValeur)}
            </td>
            <td className="text-secondary">{e.adresseIp ?? '—'}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

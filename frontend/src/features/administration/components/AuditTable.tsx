import { MOCK_AUDIT_REFS } from '../data/mockAudit'
import type { AuditEntry } from '../types'

const ACTION_PILL: Record<string, { classe: string; label: string }> = {
  CREATE: { classe: 'pill ok', label: 'Création' },
  UPDATE: { classe: 'pill info', label: 'Modification' },
  DELETE: { classe: 'pill danger', label: 'Désactivation' },
  LOGIN: { classe: 'pill wt', label: 'Connexion' },
  LOGOUT: { classe: 'pill wt', label: 'Déconnexion' },
  PAYMENT: { classe: 'pill ok', label: 'Paiement' },
  STATUS_CHANGE: { classe: 'pill info', label: 'Changement de statut' },
  ARCHIVE: { classe: 'pill warning', label: 'Archivage' },
}

function pill(action: string): { classe: string; label: string } {
  return ACTION_PILL[action] ?? { classe: 'pill wt', label: action }
}

/** Tableau du journal d'audit (lecture seule). */
export function AuditTable({ items }: { items: AuditEntry[] }) {
  if (items.length === 0)
    return <div className="card-pad text-muted">Aucune entrée trouvée.</div>

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
        {items.map((e) => {
          const p = pill(e.action)
          return (
            <tr key={e.id}>
              <td className="text-secondary">{e.createdAt}</td>
              <td className="fw-600">{e.nomUtilisateur ?? '—'}</td>
              <td>
                <span className={p.classe}>● {p.label}</span>
              </td>
              <td>{e.module}</td>
              <td>{e.entite ?? '—'}</td>
              <td className="text-secondary">{MOCK_AUDIT_REFS[e.id] ?? '—'}</td>
              <td className="text-secondary">{e.ancienneValeur ?? '—'}</td>
              <td>{e.nouvelleValeur ?? '—'}</td>
              <td className="text-secondary">{e.adresseIp ?? '—'}</td>
            </tr>
          )
        })}
      </tbody>
    </table>
  )
}

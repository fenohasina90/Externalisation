import { Badge, Button } from '@/components/ui'

import type { Responsable } from '../types'

interface ResponsablesTableProps {
  items: Responsable[]
  onEdit: (responsable: Responsable) => void
  onToggle: (responsable: Responsable) => void
}

export function ResponsablesTable({ items, onEdit, onToggle }: ResponsablesTableProps) {
  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr>
            <th>Nom</th>
            <th>Société</th>
            <th>Fonction</th>
            <th>Téléphone</th>
            <th>Email</th>
            <th>Statut</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {items.map((responsable) => (
            <tr key={responsable.id}>
              <td className="cell-strong">{responsable.nomClient}</td>
              <td>{responsable.nomSociete ?? '—'}</td>
              <td>{responsable.fonction ?? '—'}</td>
              <td>{responsable.contact ?? '—'}</td>
              <td>{responsable.email ?? '—'}</td>
              <td>
                <Badge tone={responsable.actif ? 'success' : 'neutral'}>
                  {responsable.actif ? 'Actif' : 'Inactif'}
                </Badge>
              </td>
              <td>
                <div className="table-actions">
                  <Button variant="ghost" onClick={() => onEdit(responsable)}>
                    Modifier
                  </Button>
                  <Button
                    variant={responsable.actif ? 'danger' : 'secondary'}
                    onClick={() => onToggle(responsable)}
                  >
                    {responsable.actif ? 'Désactiver' : 'Activer'}
                  </Button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
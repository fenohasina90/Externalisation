import { Badge, Card, EmptyState, Pagination } from '@/components/ui'
import { formatDate, formatMontant } from '@/utils/format'
import type { Societe } from '../types'

interface ClientsTableProps {
  clients: Societe[]
  page: number
  totalPages: number
  onPageChange: (page: number) => void
}

export function ClientsTable({ clients, page, totalPages, onPageChange }: ClientsTableProps) {
  if (clients.length === 0) {
    return (
      <Card>
        <EmptyState
          titre="Aucun client trouvé"
          hint="Modifiez votre recherche ou ajoutez de nouveaux clients."
        />
      </Card>
    )
  }

  return (
    <Card className="table-card">
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>Société</th>
              <th>Domaine</th>
              <th>Contact</th>
              <th>E-mail</th>
              <th>Créée le</th>
              <th>CA mensuel</th>
              <th>Statut</th>
            </tr>
          </thead>
          <tbody>
            {clients.map((client) => (
              <tr key={client.id}>
                <td className="cell-strong">{client.nomSociete}</td>
                <td>{client.nomDomaine ?? '—'}</td>
                <td>{client.contact ?? '—'}</td>
                <td>{client.email ?? '—'}</td>
                <td>{formatDate(client.dateCreation)}</td>
                <td className="cell-number">{formatMontant(client.chiffreAffaireMensuel)}</td>
                <td>
                  <Badge tone={client.actif ? 'success' : 'neutral'}>
                    {client.actif ? 'Actif' : 'Inactif'}
                  </Badge>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pagination page={page} totalPages={totalPages} onPageChange={onPageChange} />
    </Card>
  )
}
import { Alert, Input, PageHeader, Spinner } from '@/components/ui'
import { ClientsTable } from '../components'
import { useClientsList } from '../hooks'

export function ClientsPage() {
  const { items, total, page, totalPages, loading, error, search, setSearch, setPage } =
    useClientsList()

  return (
    <>
      <PageHeader titre="Clients" sousTitre={`${total} société(s) enregistrée(s)`} />

      <div className="toolbar">
        <Input
          type="search"
          placeholder="Rechercher par nom, e-mail, immatriculation…"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
      </div>

      {loading ? (
        <Spinner label="Chargement des clients…" />
      ) : error ? (
        <Alert tone="danger">{error}</Alert>
      ) : (
        <ClientsTable
          clients={items}
          page={page}
          totalPages={totalPages}
          onPageChange={setPage}
        />
      )}
    </>
  )
}
import { useState } from 'react'
import { Alert, Button, Card, EmptyState, Input, PageHeader, Pagination, Select, Spinner } from '@/components/ui'
import { ResponsableFormModal, ResponsablesTable } from '../components'
import { responsablesApi } from '../services/responsables.service'
import { useResponsablesList } from '../hooks/useResponsablesList'
import type { Responsable, StatutFilter } from '../types'

export function ResponsablesPage() {
  const {
    items,
    total,
    totalPages,
    page,
    loading,
    error,
    reload,
    setPage,
    search,
    setSearch,
    statut,
    setStatut,
  } = useResponsablesList(8)

  const [editing, setEditing] = useState<Responsable | 'new' | null>(null)
  const [toggling, setToggling] = useState<Responsable | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [toggleError, setToggleError] = useState<string | null>(null)
  const [toggleBusy, setToggleBusy] = useState(false)

  function handleSaved(message: string): void {
    setEditing(null)
    setNotice(message)
    reload()
  }

  async function confirmToggle(): Promise<void> {
    if (!toggling) return
    setToggleBusy(true)
    setToggleError(null)
    try {
      await responsablesApi.setActif(toggling.id, !toggling.actif)
      setNotice(toggling.actif ? 'Responsable désactivé.' : 'Responsable activé.')
      setToggling(null)
      reload()
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Une erreur est survenue.'
      setToggleError(message)
    } finally {
      setToggleBusy(false)
    }
  }

  const from = total === 0 ? 0 : page * 8 + 1
  const to = page * 8 + items.length

  return (
    <>
      <PageHeader
        titre="Responsables clients"
        sousTitre={`${total} responsable(s) au total`}
        actions={
          <Button onClick={() => setEditing('new')}>
            Ajouter un responsable
          </Button>
        }
      />

      {notice ? <Alert tone="success">{notice}</Alert> : null}

      <div className="toolbar">
        <Input
          type="search"
          placeholder="Nom, société, email…"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
        <Select
          value={statut}
          onChange={(event) => setStatut(event.target.value as StatutFilter)}
        >
          <option value="tous">Tous</option>
          <option value="actifs">Actifs</option>
          <option value="inactifs">Inactifs</option>
        </Select>
      </div>

      {loading ? (
        <Spinner label="Chargement des responsables…" />
      ) : error ? (
        <Alert tone="danger">{error}</Alert>
      ) : items.length === 0 ? (
        <Card>
          <EmptyState
            titre="Aucun responsable trouvé"
            hint="Modifiez votre recherche ou ajoutez un responsable."
          />
        </Card>
      ) : (
        <Card className="table-card">
          <ResponsablesTable
            items={items}
            onEdit={(responsable) => {
              setNotice(null)
              setEditing(responsable)
            }}
            onToggle={(responsable) => {
              setNotice(null)
              setToggleError(null)
              setToggling(responsable)
            }}
          />
          <Pagination
            page={page}
            totalPages={totalPages}
            onPageChange={setPage}
          />
          <div className="pagination-info" style={{ padding: '10px 16px' }}>
            Affichage de {from} à {to} sur {total} responsables
          </div>
        </Card>
      )}

      {editing ? (
        <ResponsableFormModal
          key={editing === 'new' ? 'new' : editing.id}
          responsable={editing === 'new' ? undefined : editing}
          onClose={() => setEditing(null)}
          onSaved={handleSaved}
        />
      ) : null}

      {toggling ? (
        <div className="modal-backdrop" onClick={() => setToggling(null)}>
          <div className="modal" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
            <div className="modal__header">
              <h3 className="modal__title">
                {toggling.actif ? 'Désactiver le responsable' : 'Activer le responsable'}
              </h3>
              <button type="button" className="modal__close" onClick={() => setToggling(null)} aria-label="Fermer">
                ×
              </button>
            </div>
            <div className="modal__body">
              <p>
                {toggling.actif ? 'Voulez-vous désactiver' : 'Voulez-vous réactiver'} <strong>{toggling.nomClient}</strong> ({toggling.nomSociete ?? '—'}) ?
                Le responsable reste dans la liste avec le statut {toggling.actif ? 'Inactif' : 'Actif'}.
              </p>
              {toggleError ? <Alert tone="danger">{toggleError}</Alert> : null}
            </div>
            <div className="modal__actions">
              <Button variant="secondary" onClick={() => setToggling(null)}>
                Annuler
              </Button>
              <Button
                variant={toggling.actif ? 'danger' : 'primary'}
                loading={toggleBusy}
                loadingLabel="En cours…"
                onClick={() => void confirmToggle()}
              >
                {toggling.actif ? 'Désactiver' : 'Activer'}
              </Button>
            </div>
          </div>
        </div>
      ) : null}
    </>
  )
}
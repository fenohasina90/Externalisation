import { useEffect, useState } from 'react'
import { ConfirmDialog } from '@/components/shared/ConfirmDialog'
import { Alert, Spinner } from '@/components/ui'
import { ServiceDetailDrawer, ServiceFormModal, ServicesTable } from '../components'
import { useServicesList, type StatutServiceFilter } from '../hooks/useServicesList'
import { servicesApi } from '../services/services.service'
import type { CreateServicePayload, Service, UpdateServicePayload } from '../types'

const PAGE_SIZE = 8

/**
 * Liste des services branchée sur l'API : recherche (nom ou code),
 * filtre statut, pagination serveur, code auto-généré (SRV-0001, …).
 */
export function ServicesPage() {
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Service | null>(null)
  const [detail, setDetail] = useState<Service | null>(null)
  const [confirmArchive, setConfirmArchive] = useState<Service | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const { items, total, totalPages, page, loading, error, setPage, search, setSearch, statut, setStatut, reload } =
    useServicesList(PAGE_SIZE)

  const actifs = items.filter((s) => s.actif).length
  const archives = items.filter((s) => !s.actif).length
  const pctActifs = total === 0 ? 0 : Math.round((actifs / total) * 100)

  const handleSave = async (payload: CreateServicePayload | UpdateServicePayload, id?: number) => {
    if (id != null) {
      const maj = await servicesApi.update(id, payload as UpdateServicePayload)
      setDetail((d) => (d?.id === id ? maj : d))
    } else {
      await servicesApi.create(payload as CreateServicePayload)
    }
    setModalOpen(false)
    setEditing(null)
    reload()
  }

  const handleArchive = async (id: number) => {
    setActionError(null)
    try {
      await servicesApi.setArchive(id, true)
      setDetail((d) => (d?.id === id ? { ...d, actif: false } : d))
      reload()
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "Échec de l'archivage")
    }
  }

  const demanderArchive = (id: number) => {
    const cible = items.find((s) => s.id === id) ?? null
    setConfirmArchive(cible)
  }

  const openEdit = (s: Service) => {
    setDetail(null)
    setEditing(s)
    setModalOpen(true)
  }

  useEffect(() => {
    document.body.classList.add('page-responsables')
    return () => {
      document.body.classList.remove('page-responsables')
    }
  }, [])

  return (
    <div className="app-page responsables-page">
      <header className="page-header page-header-sticky">
        <div>
          <h1>Services</h1>
          <p>Catalogue des services d&apos;externalisation proposés.</p>
        </div>
        <div className="actions">
          <button
            type="button"
            className="btn-primary"
            onClick={() => {
              setEditing(null)
              setModalOpen(true)
            }}
          >
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M12 5v14M5 12h14" />
            </svg>
            Ajouter un service
          </button>
        </div>
      </header>

      {actionError && (
        <div className="alert alert-danger" role="alert">
          {actionError}
        </div>
      )}

      <section className="kpi-grid">
        <div className="card kpi-card">
          <span className="kpi-tag">Total</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M12 3l9 5-9 5-9-5 9-5zM3 13l9 5 9-5M3 17l9 5 9-5" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{total}</div>
            <div className="kpi-label">Services</div>
          </div>
        </div>

        <div className="card kpi-card hl">
          <span className="kpi-tag">{pctActifs} %</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="9" />
              <path d="M8 12l3 3 5-6" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{actifs}</div>
            <div className="kpi-label">Actifs</div>
          </div>
        </div>

        <div className="card kpi-card">
          <span className="kpi-tag">Masqués</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="9" />
              <path d="M12 7v5l3 3" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{archives}</div>
            <div className="kpi-label">Archivés</div>
          </div>
        </div>
      </section>

      <section className="card card-pad">
        <form onSubmit={(e) => e.preventDefault()} className="filters-inline">
          <label className="field filter-field">
            Recherche
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Nom ou code…"
              />
          </label>
          <label className="field filter-field">
            Statut
            <select value={statut} onChange={(e) => setStatut(e.target.value as StatutServiceFilter)}>
              <option value="tous">Tous</option>
              <option value="actifs">Actifs</option>
              <option value="archives">Archivés</option>
            </select>
          </label>
          {/* <button type="submit" className="btn-primary">
            Rechercher
          </button> */}
        </form>
      </section>

      <section className="card">
        {loading && items.length === 0 ? (
          <Spinner label="Chargement des services…" />
        ) : error ? (
          <div className="card-pad">
            <Alert tone="danger">{error}</Alert>
          </div>
        ) : (
          <div className="table-scroll-container no-global-scroll">
            <div className="table-wrap">
              <ServicesTable
                items={items}
                onView={setDetail}
                onEdit={openEdit}
                onArchive={demanderArchive}
              />
            </div>
            {total > 0 && (
              <div className="table-footer">
              <span>
                Affichage de {page * PAGE_SIZE + 1} à {Math.min((page + 1) * PAGE_SIZE, total)} sur{' '}
                {total} services
              </span>
              <div className="pagination">
                <button
                  type="button"
                  className="pagination-item"
                  onClick={() => setPage(Math.max(0, page - 1))}
                  disabled={page <= 0}
                >
                  ‹
                </button>
                {Array.from({ length: totalPages }).map((_, i) => (
                  <button
                    key={i}
                    type="button"
                    className={`pagination-item ${i === page ? 'active' : ''}`}
                    onClick={() => setPage(i)}
                  >
                    {i + 1}
                  </button>
                ))}
                <button
                  type="button"
                  className="pagination-item"
                  onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                  disabled={page >= totalPages - 1}
                >
                  ›
                </button>
              </div>
            </div>
            )}
          </div>
        )}
      </section>

      <ServiceFormModal
        open={modalOpen}
        service={editing}
        onClose={() => {
          setModalOpen(false)
          setEditing(null)
        }}
        onSave={handleSave}
      />

      <ServiceDetailDrawer
        service={detail}
        onClose={() => setDetail(null)}
        onEdit={openEdit}
        onArchive={(id) => {
          setDetail(null)
          demanderArchive(id)
        }}
      />

      <ConfirmDialog
        open={confirmArchive !== null}
        titre="Archiver ce service ?"
        message={`« ${confirmArchive?.nomService ?? ''} » sera masqué du catalogue. Les demandes existantes et l’historique des tarifs sont conservés.`}
        confirmer="Archiver"
        ton="danger"
        onAnnuler={() => setConfirmArchive(null)}
        onConfirmer={() => {
          if (!confirmArchive) return
          const id = confirmArchive.id
          setConfirmArchive(null)
          void handleArchive(id)
        }}
      />
    </div>
  )
}

import { useEffect, useMemo, useState } from 'react'
import { ServiceDetailDrawer, ServiceFormModal, ServicesTable } from '../components'
import { MOCK_SERVICES, type StatutServiceFilter } from '../data/mockServices'
import { useServicesListStatic } from '../hooks'
import type { Service } from '../types'

const PAGE_SIZE = 8

/**
 * Liste des services — étape 1 : interface seule, données statiques.
 * Tableau + modale Ajouter/Modifier + panneau de détail.
 */
export function ServicesPage() {
  const [services, setServices] = useState<Service[]>(MOCK_SERVICES)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Service | null>(null)
  const [detail, setDetail] = useState<Service | null>(null)

  const { items, total, totalPages, page, setPage, search, setSearch, statut, setStatut } =
    useServicesListStatic({ source: services, size: PAGE_SIZE })

  const kpis = useMemo(() => {
    const actifs = services.filter((s) => s.actif).length
    const totalCount = services.length
    return {
      total: totalCount,
      actifs,
      pctActifs: totalCount === 0 ? 0 : Math.round((actifs / totalCount) * 100),
      archives: totalCount - actifs,
    }
  }, [services])

  const handleSave = (payload: { nomService: string; description: string | null }, id?: number) => {
    if (id != null) {
      // Modification : le statut existant est conservé (géré par l'action Archiver).
      setServices((prev) =>
        prev.map((s) =>
          s.id === id ? { ...s, nomService: payload.nomService, description: payload.description } : s,
        ),
      )
      setDetail((d) =>
        d?.id === id ? { ...d, nomService: payload.nomService, description: payload.description } : d,
      )
    } else {
      // Création : service actif par défaut.
      const nextId = Math.max(0, ...services.map((s) => s.id)) + 1
      setServices((prev) => [
        ...prev,
        { id: nextId, nomService: payload.nomService, tarifService: 0, description: payload.description, actif: true },
      ])
    }
    setModalOpen(false)
    setEditing(null)
  }

  const handleArchive = (id: number) => {
    setServices((prev) => prev.map((s) => (s.id === id ? { ...s, actif: false } : s)))
    setDetail((d) => (d?.id === id ? { ...d, actif: false } : d))
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

      <section className="kpi-grid">
        <div className="card kpi-card">
          <span className="kpi-tag">Total</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M12 3l9 5-9 5-9-5 9-5zM3 13l9 5 9-5M3 17l9 5 9-5" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{kpis.total}</div>
            <div className="kpi-label">Services</div>
          </div>
        </div>

        <div className="card kpi-card hl">
          <span className="kpi-tag">{kpis.pctActifs} %</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="9" />
              <path d="M8 12l3 3 5-6" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{kpis.actifs}</div>
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
            <div className="kpi-num">{kpis.archives}</div>
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
              placeholder="Nom du service…"
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
        <div className="table-scroll-container no-global-scroll">
          <div className="table-wrap">
            <ServicesTable
              items={items}
              onView={setDetail}
              onEdit={openEdit}
              onArchive={handleArchive}
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
          handleArchive(id)
          setDetail(null)
        }}
      />
    </div>
  )
}

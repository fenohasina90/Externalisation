import { useEffect, useMemo, useState } from 'react'
import { AuditTable } from '../components'
import { MOCK_AUDIT } from '../data/mockAudit'
import { useAuditListStatic } from '../hooks'

const PAGE_SIZE = 8

/** Journal d'audit — étape 1 : interface seule, données statiques. */
export function AuditPage() {
  const [dateDebut, setDateDebut] = useState('')
  const [dateFin, setDateFin] = useState('')

  const { items, total, totalPages, page, setPage, ...filtres } = useAuditListStatic({
    source: MOCK_AUDIT,
    size: PAGE_SIZE,
  })

  const utilisateurs = useMemo(
    () => [...new Set(MOCK_AUDIT.map((e) => e.nomUtilisateur).filter(Boolean))] as string[],
    [],
  )
  const actions = useMemo(() => [...new Set(MOCK_AUDIT.map((e) => e.action))], [])
  const modules = useMemo(() => [...new Set(MOCK_AUDIT.map((e) => e.module))], [])
  const entites = useMemo(
    () => [...new Set(MOCK_AUDIT.map((e) => e.entite).filter(Boolean))] as string[],
    [],
  )

  useEffect(() => {
    document.body.classList.add('page-responsables')
    return () => {
      document.body.classList.remove('page-responsables')
    }
  }, [])

  const handleExport = () => {
    const header = 'Date;Utilisateur;Action;Module;Entite;Ancienne;Nouvelle;IP'
    const rows = items.map((e) =>
      [e.createdAt, e.nomUtilisateur, e.action, e.module, e.entite, e.ancienneValeur, e.nouvelleValeur, e.adresseIp]
        .map((v) => `"${(v ?? '').toString().replace(/"/g, '""')}"`)
        .join(';'),
    )
    const blob = new Blob([[header, ...rows].join('\n')], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'journal-audit.csv'
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div className="app-page responsables-page">
      <header className="page-header page-header-sticky">
        <div>
          <h1>Journal d&apos;audit</h1>
          <p>Historique des actions effectuées dans l&apos;application.</p>
        </div>
        <div className="actions">
          <button type="button" className="btn-ghost" onClick={handleExport}>
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M12 3v12m0 0l-4-4m4 4l4-4M4 21h16" />
            </svg>
            Exporter
          </button>
        </div>
      </header>

      <section className="card card-pad">
        <form onSubmit={(e) => e.preventDefault()} className="filters-inline">
          <label className="field filter-field">
            Utilisateur
            <select
              value={filtres.utilisateur}
              onChange={(e) => filtres.setUtilisateur(e.target.value)}
            >
              <option value="tous">Tous</option>
              {utilisateurs.map((u) => (
                <option key={u} value={u}>
                  {u}
                </option>
              ))}
            </select>
          </label>
          <label className="field filter-field">
            Action
            <select value={filtres.action} onChange={(e) => filtres.setAction(e.target.value)}>
              <option value="toutes">Toutes</option>
              {actions.map((a) => (
                <option key={a} value={a}>
                  {a}
                </option>
              ))}
            </select>
          </label>
          <label className="field filter-field">
            Module
            <select value={filtres.module} onChange={(e) => filtres.setModule(e.target.value)}>
              <option value="tous">Tous</option>
              {modules.map((m) => (
                <option key={m} value={m}>
                  {m}
                </option>
              ))}
            </select>
          </label>
          <label className="field filter-field">
            Entité
            <select value={filtres.entite} onChange={(e) => filtres.setEntite(e.target.value)}>
              <option value="toutes">Toutes</option>
              {entites.map((e) => (
                <option key={e} value={e}>
                  {e}
                </option>
              ))}
            </select>
          </label>
          <label className="field filter-field">
            Période (début)
            <input type="date" value={dateDebut} onChange={(e) => setDateDebut(e.target.value)} />
          </label>
          <label className="field filter-field">
            Période (fin)
            <input type="date" value={dateFin} onChange={(e) => setDateFin(e.target.value)} />
          </label>
          <button type="submit" className="btn-primary">
            Appliquer
          </button>
        </form>
      </section>

      <section className="card">
        <div className="table-scroll-container no-global-scroll">
          <div className="table-wrap">
            <AuditTable items={items} />
          </div>
          {total > 0 && (
            <div className="table-footer">
              <span>
                Affichage de {page * PAGE_SIZE + 1} à {Math.min((page + 1) * PAGE_SIZE, total)} sur{' '}
                {total} entrées
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
    </div>
  )
}

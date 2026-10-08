import { useState } from 'react'
import { CategoriesRapports, DemandesParStatut, RapportDomaineTable } from '../components'
import {
  CATEGORIES_RAPPORT,
  FILTRE_OPTIONS,
  MOCK_RAPPORT,
  formatMGA,
  type CategorieRapport,
} from '../data/mockRapports'

/**
 * Rapports — étape 1 : interface seule, données statiques.
 * PDF ouvre l'impression, Excel/CSV téléchargent le tableau affiché.
 */
export function RapportsPage() {
  const [categorie, setCategorie] = useState<CategorieRapport>('general')
  const [dateDebut, setDateDebut] = useState('2026-09-01')
  const [dateFin, setDateFin] = useState('2026-10-04')
  const [client, setClient] = useState('tous')
  const [service, setService] = useState('tous')
  const [domaine, setDomaine] = useState('tous')
  const [commercial, setCommercial] = useState('tous')
  const [statut, setStatut] = useState('tous')

  const labelCategorie =
    CATEGORIES_RAPPORT.find((c) => c.id === categorie)?.label ?? 'Rapport général'
  const tauxPaye = Math.round((MOCK_RAPPORT.montantPaye / MOCK_RAPPORT.montantTotal) * 100)

  const telechargerCSV = () => {
    const header = 'Domaine;Demandes;Montant total;Montant paye;Reste a payer'
    const rows = MOCK_RAPPORT.parDomaine.map((l) =>
      [l.domaine, l.demandes, l.montantTotal, l.montantPaye, l.resteAPayer].join(';'),
    )
    const blob = new Blob([[header, ...rows].join('\n')], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `rapport-${categorie}.csv`
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div className="app-page responsables-page">
      <header className="page-header page-header-sticky">
        <div>
          <h1>Rapports</h1>
          <p>Analysez l&apos;activité par catégorie et exportez les résultats.</p>
        </div>
        <div className="actions">
          <button type="button" className="btn-ghost" onClick={() => window.print()}>
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M12 3v12m0 0l-4-4m4 4l4-4M4 21h16" />
            </svg>
            PDF
          </button>
          <button
            type="button"
            className="btn-ghost"
            onClick={telechargerCSV}
            title="Format CSV, ouvrable dans Excel"
          >
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M12 3v12m0 0l-4-4m4 4l4-4M4 21h16" />
            </svg>
            Excel
          </button>
          <button type="button" className="btn-ghost" onClick={telechargerCSV}>
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M12 3v12m0 0l-4-4m4 4l4-4M4 21h16" />
            </svg>
            CSV
          </button>
        </div>
      </header>

      <CategoriesRapports selection={categorie} onSelect={setCategorie} />

      <section className="card card-pad">
        <form onSubmit={(e) => e.preventDefault()} className="filters-inline">
          <label className="field filter-field">
            Période (début)
            <input type="date" value={dateDebut} onChange={(e) => setDateDebut(e.target.value)} />
          </label>
          <label className="field filter-field">
            Période (fin)
            <input type="date" value={dateFin} onChange={(e) => setDateFin(e.target.value)} />
          </label>
          <label className="field filter-field">
            Client
            <select value={client} onChange={(e) => setClient(e.target.value)}>
              <option value="tous">Tous les clients</option>
              {FILTRE_OPTIONS.clients.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </label>
          <label className="field filter-field">
            Service
            <select value={service} onChange={(e) => setService(e.target.value)}>
              <option value="tous">Tous les services</option>
              {FILTRE_OPTIONS.services.map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
          </label>
          <label className="field filter-field">
            Domaine
            <select value={domaine} onChange={(e) => setDomaine(e.target.value)}>
              <option value="tous">Tous les domaines</option>
              {FILTRE_OPTIONS.domaines.map((d) => (
                <option key={d} value={d}>
                  {d}
                </option>
              ))}
            </select>
          </label>
          <label className="field filter-field">
            Commercial
            <select value={commercial} onChange={(e) => setCommercial(e.target.value)}>
              <option value="tous">Tous</option>
              {FILTRE_OPTIONS.commerciaux.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </label>
          <label className="field filter-field">
            Statut
            <select value={statut} onChange={(e) => setStatut(e.target.value)}>
              <option value="tous">Tous</option>
              <option value="EN_ATTENTE">En attente</option>
              <option value="VALIDATION">Validation</option>
              <option value="EN_COURS">En cours</option>
              <option value="TERMINEE">Terminée</option>
              <option value="ANNULEE">Annulée</option>
            </select>
          </label>
          <button type="submit" className="btn-primary">
            Appliquer
          </button>
        </form>
      </section>

      <section className="kpi-grid">
        <div className="card kpi-card">
          <span className="kpi-tag">Total</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="9" cy="8" r="3.5" />
              <path d="M2.5 20c0-3.5 3-6 6.5-6s6.5 2.5 6.5 6M16 4.8a3.5 3.5 0 0 1 0 6.4M18 14.3c2 .7 3.5 2.6 3.5 5.2" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{MOCK_RAPPORT.clients}</div>
            <div className="kpi-label">Clients</div>
          </div>
        </div>

        <div className="card kpi-card">
          <span className="kpi-tag">Période</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8l-5-5zM14 3v5h5M9 13h6M9 17h6" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{MOCK_RAPPORT.demandes}</div>
            <div className="kpi-label">Demandes</div>
          </div>
        </div>

        <div className="card kpi-card">
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <rect x="3" y="5" width="18" height="14" rx="2" />
              <path d="M3 10h18" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{formatMGA(MOCK_RAPPORT.montantTotal)}</div>
            <div className="kpi-label">Montant total</div>
          </div>
        </div>

        <div className="card kpi-card hl">
          <span className="kpi-tag">{tauxPaye} %</span>
          <div className="kpi-icon">
            <svg className="sv" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="9" />
              <path d="M8 12l3 3 5-6" />
            </svg>
          </div>
          <div>
            <div className="kpi-num">{formatMGA(MOCK_RAPPORT.montantPaye)}</div>
            <div className="kpi-label">Montant payé</div>
          </div>
        </div>
      </section>

      <div className="rapport-grid">
        <section className="card">
          <div className="card-pad" style={{ paddingBottom: 0 }}>
            <h3 style={{ margin: 0 }}>{labelCategorie} — par domaine</h3>
          </div>
          <div className="table-scroll-container no-global-scroll">
            <div className="table-wrap">
              <RapportDomaineTable lignes={MOCK_RAPPORT.parDomaine} />
            </div>
          </div>
        </section>

        <section className="card card-pad">
          <h3>Demandes par statut</h3>
          <DemandesParStatut items={MOCK_RAPPORT.parStatut} />
        </section>
      </div>
    </div>
  )
}

import type { JSX } from 'react'
import { CATEGORIES_RAPPORT, type CategorieMeta } from '../constants'
import type { CategorieRapport } from '../types'

const ICONES: Record<CategorieRapport, JSX.Element> = {
  general: (
    <svg className="sv" viewBox="0 0 24 24">
      <path d="M4 20V10M10 20V4M16 20v-7M22 20H2" />
    </svg>
  ),
  financier: (
    <svg className="sv" viewBox="0 0 24 24">
      <rect x="3" y="5" width="18" height="14" rx="2" />
      <path d="M3 10h18" />
    </svg>
  ),
  clients: (
    <svg className="sv" viewBox="0 0 24 24">
      <circle cx="9" cy="8" r="3.5" />
      <path d="M2.5 20c0-3.5 3-6 6.5-6s6.5 2.5 6.5 6M16 4.8a3.5 3.5 0 0 1 0 6.4M18 14.3c2 .7 3.5 2.6 3.5 5.2" />
    </svg>
  ),
  services: (
    <svg className="sv" viewBox="0 0 24 24">
      <path d="M12 3l9 5-9 5-9-5 9-5zM3 13l9 5 9-5M3 17l9 5 9-5" />
    </svg>
  ),
  demandes: (
    <svg className="sv" viewBox="0 0 24 24">
      <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8l-5-5zM14 3v5h5M9 13h6M9 17h6" />
    </svg>
  ),
  paiements: (
    <svg className="sv" viewBox="0 0 24 24">
      <circle cx="12" cy="12" r="9" />
      <path d="M8 12l3 3 5-6" />
    </svg>
  ),
}

interface Props {
  selection: CategorieRapport
  onSelect: (c: CategorieRapport) => void
  /** Catégories autorisées pour l'utilisateur connecté. */
  categories?: CategorieMeta[]
}

/** Cartes de sélection de la catégorie de rapport. */
export function CategoriesRapports({
  selection,
  onSelect,
  categories = CATEGORIES_RAPPORT,
}: Props) {
  return (
    <div className="cat-grid" role="tablist" aria-label="Catégorie de rapport">
      {categories.map((c) => (
        <button
          key={c.id}
          type="button"
          role="tab"
          aria-selected={selection === c.id}
          onClick={() => onSelect(c.id)}
          className={`card cat-card${selection === c.id ? ' cat-card--selected' : ''}`}
        >
          <span className="kpi-icon">{ICONES[c.id]}</span>
          <span className="cat-label">{c.label}</span>
        </button>
      ))}
    </div>
  )
}

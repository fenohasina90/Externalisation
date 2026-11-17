import type { KeyboardEvent } from 'react'

const MOIS_FR = [
  'Janvier',
  'Février',
  'Mars',
  'Avril',
  'Mai',
  'Juin',
  'Juillet',
  'Août',
  'Septembre',
  'Octobre',
  'Novembre',
  'Décembre',
] as const

interface MonthPickerProps {
  value: string
  onChange: (valeur: string) => void
  id?: string
  required?: boolean
}

function decouper(valeur: string): { annee: number; mois: number } | null {
  const m = /^(\d{4})-(\d{2})$/.exec(valeur)
  if (!m) return null
  const annee = Number(m[1])
  const mois = Number(m[2])
  if (Number.isNaN(annee) || mois < 1 || mois > 12) return null
  return { annee, mois: mois - 1 }
}

function assembler(annee: number, mois: number): string {
  return `${annee}-${String(mois + 1).padStart(2, '0')}`
}

function etiketer(annee: number, mois: number): string {
  return `${MOIS_FR[mois]} ${annee}`
}

/**
 * Sélecteur de mois « professionnel » : en-tête « Novembre 2026 » avec
 * navigation fine (pas d'un an), grille des 12 mois, sélecteur d'année large
 * et bouton « Ce mois-ci ». Conserve une valeur ISO « AAAA-MM ».
 */
export function MonthPicker({ value, onChange, id, required = false }: MonthPickerProps) {
  const now = new Date()
  const courant = decouper(value) ?? { annee: now.getFullYear(), mois: now.getMonth() }
  const selection = etiketer(courant.annee, courant.mois)

  // Années proposées : 10 ans en arrière, l'année courante et 3 ans en avant.
  const baseAnnee = now.getFullYear()
  const annees: number[] = []
  for (let a = baseAnnee - 10; a <= baseAnnee + 3; a += 1) {
    annees.push(a)
  }

  function bouger(deltaMois: number) {
    const d = new Date(courant.annee, courant.mois + deltaMois, 1)
    onChange(assembler(d.getFullYear(), d.getMonth()))
  }

  function choisirMois(mois: number) {
    onChange(assembler(courant.annee, mois))
  }

  function choisirAnnee(annee: string) {
    const a = Number(annee)
    if (!Number.isNaN(a)) {
      onChange(assembler(a, courant.mois))
    }
  }

  function allerCeMois() {
    onChange(assembler(now.getFullYear(), now.getMonth()))
  }

  function onKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') {
      event.preventDefault()
      bouger(-1)
    } else if (event.key === 'ArrowRight' || event.key === 'ArrowDown') {
      event.preventDefault()
      bouger(1)
    }
  }

  return (
    <div className="month-picker" id={id} onKeyDown={onKeyDown}>
      <div className="month-picker-head">
        <button
          type="button"
          className="month-picker-nav"
          aria-label="Mois précédent"
          onClick={() => bouger(-1)}
        >
          ‹
        </button>
        <div className="month-picker-head-center">
          <strong className="month-picker-selected" aria-live="polite" aria-atomic="true">
            {selection}
          </strong>
          <div className="month-picker-subrow">
            <label className="month-picker-annee">
              <span className="sr-only">Année</span>
              <select className="month-picker-annee-select" value={courant.annee}
                onChange={(e) => choisirAnnee(e.target.value)}>
                {annees.map((a) => (
                  <option key={a} value={a}>{a}</option>
                ))}
              </select>
            </label>
            <button type="button" className="month-picker-today" onClick={allerCeMois}>
              Ce mois-ci
            </button>
          </div>
        </div>
        <button
          type="button"
          className="month-picker-nav"
          aria-label="Mois suivant"
          onClick={() => bouger(1)}
        >
          ›
        </button>
      </div>
      <div className="month-grid" role="group" aria-label="Choisir le mois">
        {MOIS_FR.map((libelle, i) => {
          const actif = i === courant.mois
          return (
            <button
              key={libelle}
              type="button"
              className={`month-cell${actif ? ' month-cell--actif' : ''}`}
              aria-pressed={actif}
              onClick={() => choisirMois(i)}
            >
              {libelle}
            </button>
          )
        })}
      </div>
      <p className="month-picker-note">
        Mois facturé : <b>{selection}</b>{' '}
        {required ? <span aria-hidden="true">*</span> : null}
      </p>
    </div>
  )
}

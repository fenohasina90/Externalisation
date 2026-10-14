import { useEffect, useId, useRef, useState, type ReactNode } from 'react'

export interface AutocompleteOption {
  id: number | string
  label: string
}

interface AutocompleteProps {
  value: string
  onTextChange: (text: string) => void
  /** Appelé avec l'option choisie, ou null quand le texte ne correspond à aucun choix. */
  onPick: (option: AutocompleteOption | null) => void
  options: AutocompleteOption[]
  placeholder?: string
  required?: boolean
  disabled?: boolean
  loading?: boolean
  /** Ligne affichée en bas du panneau (ex. indice de création). */
  footer?: ReactNode
  name?: string
  maxSuggestions?: number
}

/**
 * Champ texte avec suggestions filtrables, clavier (↑↓/Entrée/Échap) et
 * sélection à la souris. Sans correspondance : le texte reste libre et
 * `onPick(null)` est émis (le parent décide : créer ou refuser).
 */
export function Autocomplete({
  value,
  onTextChange,
  onPick,
  options,
  placeholder,
  required,
  disabled,
  loading,
  footer,
  name,
  maxSuggestions = 8,
}: AutocompleteProps) {
  const listId = useId()
  const [open, setOpen] = useState(false)
  const [highlight, setHighlight] = useState(0)
  const closeTimer = useRef<number | null>(null)

  const terme = value.trim().toLowerCase()
  const suggestions =
    terme === ''
      ? options.slice(0, maxSuggestions)
      : options
          .filter((o) => o.label.toLowerCase().includes(terme))
          .slice(0, maxSuggestions)

  useEffect(
    () => () => {
      if (closeTimer.current !== null) window.clearTimeout(closeTimer.current)
    },
    [],
  )

  const choisir = (option: AutocompleteOption) => {
    onTextChange(option.label)
    onPick(option)
    setOpen(false)
  }

  const fermerDoux = () => {
    // Laisse le clic sur une suggestion aboutir avant de fermer.
    closeTimer.current = window.setTimeout(() => setOpen(false), 120)
  }

  return (
    <span className="autocomplete">
      <input
        type="text"
        name={name}
        value={value}
        placeholder={placeholder}
        required={required}
        disabled={disabled}
        autoComplete="off"
        role="combobox"
        aria-expanded={open}
        aria-controls={listId}
        aria-autocomplete="list"
        onChange={(e) => {
          onTextChange(e.target.value)
          onPick(null)
          setHighlight(0)
          setOpen(true)
        }}
        onFocus={() => setOpen(true)}
        onBlur={fermerDoux}
        onKeyDown={(e) => {
          if (e.key === 'ArrowDown' && suggestions.length > 0) {
            e.preventDefault()
            setHighlight((h) => (h + 1) % suggestions.length)
          } else if (e.key === 'ArrowUp' && suggestions.length > 0) {
            e.preventDefault()
            setHighlight((h) => (h - 1 + suggestions.length) % suggestions.length)
          } else if (e.key === 'Enter' && open && suggestions.length > 0) {
            e.preventDefault()
            choisir(suggestions[highlight] ?? suggestions[0])
          } else if (e.key === 'Escape') {
            setOpen(false)
          }
        }}
      />
      {open && !disabled && (
        <span className="autocomplete__panel" role="listbox" id={listId}>
          {loading && <span className="autocomplete__info">Recherche…</span>}
          {!loading &&
            suggestions.map((o, i) => (
              <span
                key={o.id}
                role="option"
                aria-selected={i === highlight}
                className={`autocomplete__option${i === highlight ? ' autocomplete__option--active' : ''}`}
                onMouseDown={(e) => {
                  e.preventDefault()
                  choisir(o)
                }}
                onMouseEnter={() => setHighlight(i)}
              >
                {o.label}
              </span>
            ))}
          {!loading && suggestions.length === 0 && (
            <span className="autocomplete__info">Aucune suggestion</span>
          )}
          {footer}
        </span>
      )}
    </span>
  )
}

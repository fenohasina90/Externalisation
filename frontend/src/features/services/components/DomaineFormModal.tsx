import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import type { Domaine } from '../types'

interface Props {
  open: boolean
  /** Domaine à modifier, null pour une création. */
  domaine: Domaine | null
  onClose: () => void
  /** Création : actif par défaut. Modification : statut conservé (géré par l'action Activer/Désactiver). */
  onSave: (payload: { nomDomaine: string }, id?: number) => void
}

const emptyForm = { nomDomaine: '' }

type FormState = typeof emptyForm

/** Modale Ajouter / Modifier un domaine (cf. maquette). */
export function DomaineFormModal({ open, domaine, onClose, onSave }: Props) {
  const [form, setForm] = useState<FormState>(emptyForm)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!open) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setForm(emptyForm)
      setError(null)
      return
    }
    setForm(domaine ? { nomDomaine: domaine.nomDomaine } : emptyForm)
    setError(null)
  }, [open, domaine])

  if (!open) return null

  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target
    setForm((f) => ({ ...f, [name]: value }))
  }

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    if (!form.nomDomaine.trim()) {
      setError('Le nom du domaine est obligatoire')
      return
    }
    onSave({ nomDomaine: form.nomDomaine.trim() }, domaine?.id)
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="modal-card"
        style={{ maxWidth: 560 }}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-label={domaine ? 'Modifier un domaine' : 'Ajouter un domaine'}
      >
        <header className="modal-header">
          <div>
            <h3 className="modal-title">
              {domaine ? 'Modifier un domaine' : 'Ajouter un domaine'}
            </h3>
          </div>
          <button type="button" className="btn-ghost" onClick={onClose} aria-label="Fermer">
            ✕
          </button>
        </header>

        <form onSubmit={handleSubmit} className="modal-body">
          {error && <div style={{ color: 'var(--danger)', fontSize: 14 }}>{error}</div>}

          <div className="form-grid">
            <label className="field form-grid__full">
              Nom du domaine <span className="required">*</span>
              <input
                name="nomDomaine"
                value={form.nomDomaine}
                onChange={handleChange}
                placeholder="Ex. Comptabilité"
                required
              />
            </label>
          </div>

          <footer className="modal-footer">
            <button type="button" className="btn-ghost" onClick={onClose}>
              Annuler
            </button>
            <button type="submit" className="btn-primary">
              Enregistrer
            </button>
          </footer>
        </form>
      </div>
    </div>
  )
}

import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import type { Service } from '../types'

interface Props {
  open: boolean
  /** Service à modifier, null pour une création. */
  service: Service | null
  onClose: () => void
  /** Création : actif par défaut (sans champ statut). Modification : statut conservé. */
  onSave: (payload: { nomService: string; description: string | null }, id?: number) => void
}

const emptyForm = { nomService: '', description: '' }

type FormState = typeof emptyForm

/** Modale Ajouter / Modifier un service (cf. maquette, sans champ statut). */
export function ServiceFormModal({ open, service, onClose, onSave }: Props) {
  const [form, setForm] = useState<FormState>(emptyForm)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!open) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setForm(emptyForm)
      setError(null)
      return
    }
    setForm(
      service
        ? { nomService: service.nomService, description: service.description ?? '' }
        : emptyForm,
    )
    setError(null)
  }, [open, service])

  if (!open) return null

  const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target
    setForm((f) => ({ ...f, [name]: value }))
  }

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    if (!form.nomService.trim()) {
      setError('Le nom du service est obligatoire')
      return
    }
    onSave(
      { nomService: form.nomService.trim(), description: form.description.trim() || null },
      service?.id,
    )
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="modal-card"
        style={{ maxWidth: 560 }}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-label={service ? 'Modifier un service' : 'Ajouter un service'}
      >
        <header className="modal-header">
          <div>
            <h3 className="modal-title">
              {service ? 'Modifier un service' : 'Ajouter un service'}
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
              Service <span className="required">*</span>
              <input
                name="nomService"
                value={form.nomService}
                onChange={handleChange}
                placeholder="Ex. Audit comptable"
                required
              />
            </label>

            <label className="field form-grid__full">
              Description
              <textarea
                name="description"
                value={form.description}
                onChange={handleChange}
                placeholder="Décrivez le service"
                rows={4}
                style={{ height: 92 }}
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

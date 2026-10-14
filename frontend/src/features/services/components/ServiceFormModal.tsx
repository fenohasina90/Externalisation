import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import type { CreateServicePayload, Service, UpdateServicePayload } from '../types'

interface Props {
  open: boolean
  /** Service à modifier, null pour une création (code auto-généré, actif par défaut). */
  service: Service | null
  onClose: () => void
  onSave: (payload: CreateServicePayload | UpdateServicePayload, id?: number) => Promise<void>
}

const emptyForm = { nomService: '', description: '' }

type FormState = typeof emptyForm

/** Modale Ajouter / Modifier un service (sans champ statut ni code). */
export function ServiceFormModal({ open, service, onClose, onSave }: Props) {
  const [form, setForm] = useState<FormState>(emptyForm)
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!open) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setForm(emptyForm)
      setError(null)
      setSaving(false)
      return
    }
    setForm(
      service
        ? {
            nomService: service.nomService,
            description: service.description ?? '',
          }
        : emptyForm,
    )
    setError(null)
  }, [open, service])

  if (!open) return null

  const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target
    setForm((f) => ({ ...f, [name]: value }))
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (!form.nomService.trim()) {
      setError('Le nom du service est obligatoire')
      return
    }
    setSaving(true)
    setError(null)
    try {
      const payload = {
        nomService: form.nomService.trim(),
        description: form.description.trim() || null,
      }
      await onSave(payload, service?.id)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Échec de l'enregistrement")
    } finally {
      setSaving(false)
    }
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

          {service?.codeService && (
            <p className="modal-subtitle">
              Code : <strong>{service.codeService}</strong> (généré automatiquement,
              non modifiable)
            </p>
          )}

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

          {!service && (
            <p className="modal-subtitle form-grid__full" style={{ margin: 0 }}>
              Le code sera généré automatiquement (SRV-0001, …).
            </p>
          )}

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
            <button type="button" className="btn-ghost" onClick={onClose} disabled={saving}>
              Annuler
            </button>
            <button type="submit" className="btn-primary" disabled={saving}>
              {saving ? 'Enregistrement…' : 'Enregistrer'}
            </button>
          </footer>
        </form>
      </div>
    </div>
  )
}

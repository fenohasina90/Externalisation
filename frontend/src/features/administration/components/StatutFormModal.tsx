import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { messageErreur } from '@/constants/messages'
import type { CreateStatutPayload, StatutDemandeConfig } from '../types'

interface Props {
  open: boolean
  /** Statut à modifier, null pour une création (actif par défaut). */
  statut: StatutDemandeConfig | null
  onClose: () => void
  onSave: (payload: CreateStatutPayload, id?: number) => Promise<void>
}

const emptyForm = { statut: '', description: '', ordre: '0' }

type FormState = typeof emptyForm

/** Modale Ajouter / Modifier un statut de demande. */
export function StatutFormModal({ open, statut, onClose, onSave }: Props) {
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
      statut
        ? {
            statut: statut.statut,
            description: statut.description ?? '',
            ordre: String(statut.ordre),
          }
        : emptyForm,
    )
    setError(null)
  }, [open, statut])

  if (!open) return null

  const handleChange = (
    e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>,
  ) => {
    const { name, value } = e.target
    setForm((f) => ({ ...f, [name]: value }))
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (!form.statut.trim()) {
      setError('Le statut est obligatoire')
      return
    }
    setSaving(true)
    setError(null)
    try {
      await onSave(
        {
          statut: form.statut.trim(),
          description: form.description.trim() || null,
          ordre: Number(form.ordre) || 0,
        },
        statut?.id,
      )
    } catch (err) {
      setError(messageErreur(err))
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
        aria-label={statut ? 'Modifier un statut' : 'Ajouter un statut'}
      >
        <header className="modal-header">
          <div>
            <h3 className="modal-title">
              {statut ? 'Modifier un statut' : 'Ajouter un statut'}
            </h3>
            <p className="modal-subtitle">
              Le code est enregistré en majuscules et doit rester unique.
            </p>
          </div>
          <button type="button" className="btn-ghost" onClick={onClose} aria-label="Fermer">
            ✕
          </button>
        </header>

        <form onSubmit={handleSubmit} className="modal-body">
          {error && <div style={{ color: 'var(--danger)', fontSize: 14 }}>{error}</div>}

          <div className="form-grid">
            <label className="field">
              Statut <span className="required">*</span>
              <input
                name="statut"
                value={form.statut}
                onChange={handleChange}
                placeholder="Ex. EN_ATTENTE"
                required
              />
            </label>

            <label className="field">
              Ordre d&apos;affichage
              <input
                type="number"
                name="ordre"
                min={0}
                value={form.ordre}
                onChange={handleChange}
              />
            </label>

            <label className="field form-grid__full">
              Description
              <textarea
                name="description"
                value={form.description}
                onChange={handleChange}
                placeholder="Décrivez ce statut"
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

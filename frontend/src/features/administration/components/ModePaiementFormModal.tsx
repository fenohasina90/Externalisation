import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { messageErreur } from '@/constants/messages'
import type { CreateModePaiementPayload, ModePaiementAdmin } from '../types'

interface Props {
  open: boolean
  /** Mode à modifier, null pour une création (actif par défaut). */
  mode: ModePaiementAdmin | null
  onClose: () => void
  onSave: (payload: CreateModePaiementPayload, id?: number) => Promise<void>
}

const emptyForm = { code: '', libelle: '' }

type FormState = typeof emptyForm

/** Modale Ajouter / Modifier un mode de paiement. */
export function ModePaiementFormModal({ open, mode, onClose, onSave }: Props) {
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
    setForm(mode ? { code: mode.code, libelle: mode.libelle } : emptyForm)
    setError(null)
  }, [open, mode])

  if (!open) return null

  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target
    setForm((f) => ({ ...f, [name]: value }))
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (!form.code.trim()) {
      setError('Le code est obligatoire')
      return
    }
    if (!form.libelle.trim()) {
      setError('Le libellé est obligatoire')
      return
    }
    setSaving(true)
    setError(null)
    try {
      await onSave(
        { code: form.code.trim(), libelle: form.libelle.trim() },
        mode?.id,
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
        style={{ maxWidth: 520 }}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-label={mode ? 'Modifier un mode de paiement' : 'Ajouter un mode de paiement'}
      >
        <header className="modal-header">
          <div>
            <h3 className="modal-title">
              {mode ? 'Modifier un mode de paiement' : 'Ajouter un mode de paiement'}
            </h3>
            <p className="modal-subtitle">
              Le code est enregistré en majuscules (ex. VIREMENT) et doit rester unique.
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
              Code <span className="required">*</span>
              <input
                name="code"
                value={form.code}
                onChange={handleChange}
                placeholder="Ex. VIREMENT"
                required
              />
            </label>

            <label className="field">
              Libellé <span className="required">*</span>
              <input
                name="libelle"
                value={form.libelle}
                onChange={handleChange}
                placeholder="Ex. Virement bancaire"
                required
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

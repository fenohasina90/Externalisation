import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import type { Commercial, CreateCommercialPayload } from '../types'

interface Props {
  open: boolean
  /** Commercial à modifier, null pour une création. */
  commercial: Commercial | null
  onClose: () => void
  /** Sauvegarde via l'API (rejette en cas d'erreur serveur). */
  onSave: (payload: CreateCommercialPayload, id?: number) => Promise<void>
}

const emptyForm = {
  nomCommercial: '',
  contact: '',
  email: '',
}

type FormState = typeof emptyForm

/**
 * Modale Ajouter / Modifier un commercial.
 * Le matricule est généré automatiquement à la création et non modifiable :
 * aucun champ de saisie, affiché en lecture seule en modification.
 */
export function CommercialFormModal({ open, commercial, onClose, onSave }: Props) {
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
      commercial
        ? {
            nomCommercial: commercial.nomCommercial,
            contact: commercial.contact ?? '',
            email: commercial.email ?? '',
          }
        : emptyForm,
    )
    setError(null)
  }, [open, commercial])

  if (!open) return null

  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target
    setForm((f) => ({ ...f, [name]: value }))
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (!form.nomCommercial.trim()) {
      setError('Le nom est obligatoire')
      return
    }
    // if (!form.email.trim()) {
    //   setError("L'adresse e-mail est obligatoire")
    //   return
    // }
    setSaving(true)
    setError(null)
    try {
      await onSave(
        {
          nomCommercial: form.nomCommercial.trim(),
          contact: form.contact.trim() || null,
          email: form.email.trim(),
        },
        commercial?.id,
      )
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
        style={{ maxWidth: 580 }}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-label={commercial ? 'Modifier un commercial' : 'Ajouter un commercial'}
      >
        <header className="modal-header">
          <div>
            <h3 className="modal-title">
              {commercial ? 'Modifier un commercial' : 'Ajouter un commercial'}
            </h3>
            <p className="modal-subtitle">
              Le formulaire de modification est identique, pré-rempli.
            </p>
          </div>
          <button type="button" className="btn-ghost" onClick={onClose} aria-label="Fermer">
            ✕
          </button>
        </header>

        <form onSubmit={handleSubmit} className="modal-body">
          {error && <div style={{ color: 'var(--danger)', fontSize: 14 }}>{error}</div>}

          {commercial?.immatricule && (
            <p className="modal-subtitle">
              Matricule : <strong>{commercial.immatricule}</strong> (généré automatiquement,
              non modifiable)
            </p>
          )}

          <div className="form-grid">
            <label className="field form-grid__full">
              Nom <span className="required">*</span>
              <input
                name="nomCommercial"
                value={form.nomCommercial}
                onChange={handleChange}
                placeholder="Nom et prénom"
                required
              />
            </label>

            <label className="field form-grid__full">
              Contact
              <input
                type="tel"
                name="contact"
                value={form.contact}
                onChange={handleChange}
                placeholder="+261 34 00 000 00"
              />
            </label>

            <label className="field form-grid__full">
              Email 
              {/* <span className="required">*</span> */}
              <input
                type="email"
                name="email"
                value={form.email}
                onChange={handleChange}
                placeholder="nom@cabinet-excellent.mg"
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

import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import type { Domaine } from '../types'

interface Props {
  open: boolean
  /** Domaine à modifier, null pour une création. */
  domaine: Domaine | null
  onClose: () => void
  /** Création : actif par défaut. Modification : statut conservé (géré par l'action Activer/Désactiver). */
  onSave: (payload: { nomDomaine: string; codeDomaine: string }, id?: number) => Promise<void>
}

const emptyForm = { nomDomaine: '', codeDomaine: '' }

type FormState = typeof emptyForm

/** Modale Ajouter / Modifier un domaine (cf. maquette). */
export function DomaineFormModal({ open, domaine, onClose, onSave }: Props) {
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
    setForm(domaine ? { nomDomaine: domaine.nomDomaine, codeDomaine: domaine.codeDomaine ?? '' } : emptyForm)
    setError(null)
  }, [open, domaine])

  if (!open) return null

  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target
    setForm((f) => ({ ...f, [name]: value }))
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (!form.nomDomaine.trim()) {
      setError('Le nom du domaine est obligatoire')
      return
    }
    if (!form.codeDomaine.trim()) {
      setError('Le code domaine est obligatoire')
      return
    }
    setSaving(true)
    setError(null)
    try {
      await onSave({
        nomDomaine: form.nomDomaine.trim(),
        codeDomaine: form.codeDomaine.trim().toUpperCase(),
      }, domaine?.id)
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

            <label className="field form-grid__full">
              Code domaine <span className="required">*</span>
              <input
                name="codeDomaine"
                value={form.codeDomaine}
                onChange={handleChange}
                placeholder="Ex. COMP"
                required
                // maxLength="50"
              />
              {/*
              <span className="form-help">
                Saisi manuellement, utilisé pour préfixer le code des services liés (ex. SRV-COMP-0007).
              </span>
              */}
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

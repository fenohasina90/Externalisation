import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { responsablesApi } from '../services/responsables.service'
import type { Responsable } from '../types'

interface Props {
  open: boolean
  responsable: Responsable | null
  onClose: () => void
  onSaved: () => void
}

const emptyForm = {
  societeId: 0,
  nomClient: '',
  contact: '',
  email: '',
  fonction: '',
}

type FormState = typeof emptyForm

export function ResponsableFormModal({ open, responsable, onClose, onSaved }: Props) {
  const [form, setForm] = useState<FormState>(emptyForm)
  const [societes, setSocietes] = useState<{ id: number; nom: string }[]>([])
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!open) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setForm(emptyForm)
      setError(null)
      setSocietes([])
      return
    }
    let mounted = true
    ;(async () => {
      try {
        const sList = await responsablesApi.societes()
        if (mounted) setSocietes(sList)
      } catch (e) {
        console.error(e)
      }
    })()

    // Initialize form synchronously based on current props
    const initialForm = responsable
      ? {
          societeId: responsable.societeId ?? 0,
          nomClient: responsable.nomClient,
          contact: responsable.contact ?? '',
          email: responsable.email ?? '',
          fonction: responsable.fonction ?? '',
        }
      : emptyForm
    setForm(initialForm)
    setError(null)

    return () => {
      mounted = false
    }
  }, [open, responsable])

  if (!open) return null

  const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target
    setForm((f) => ({
      ...f,
      [name]: value,
    }))
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      if (responsable) {
        await responsablesApi.update(responsable.id, {
          nomClient: form.nomClient,
          contact: form.contact || null,
          email: form.email || null,
          fonction: form.fonction || null,
        })
      } else {
        if (!form.societeId) throw new Error('Veuillez sélectionner une société')
        await responsablesApi.create({
          societeId: form.societeId,
          nomClient: form.nomClient,
          contact: form.contact || null,
          email: form.email || null,
          fonction: form.fonction || null,
        })
      }
      onSaved()
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } }; message?: string }
      const msg = e?.response?.data?.message ?? e?.message ?? "Erreur lors de l'enregistrement"
      setError(msg)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <header className="modal-header">
          <div>
            <h3 className="modal-title">
              {responsable ? 'Modifier un responsable' : 'Ajouter un responsable'}
            </h3>
            <p className="modal-subtitle">Renseignez les informations de l’interlocuteur client.</p>
          </div>
          <button type="button" className="btn-ghost" onClick={onClose} aria-label="Fermer">
            ✕
          </button>
        </header>

        <form onSubmit={handleSubmit} className="modal-body">
          {error && <div style={{ color: 'var(--danger)', fontSize: 14 }}>{error}</div>}

          <div className="form-grid">
            <label className="field">
              Société <span className="required">*</span>
              <select
                name="societeId"
                value={form.societeId}
                onChange={handleChange}
                required
                disabled={!!responsable}
              >
                <option value={0}>Sélectionner une société</option>
                {societes.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.nom}
                  </option>
                ))}
              </select>
            </label>

            <label className="field">
              Nom complet / libellé <span className="required">*</span>
              <input name="nomClient" value={form.nomClient} onChange={handleChange} required />
            </label>

            <label className="field">
              Fonction
              <input name="fonction" value={form.fonction} onChange={handleChange} />
            </label>

            <label className="field">
              Téléphone
              <input name="contact" value={form.contact} onChange={handleChange} />
            </label>

            <label className="field">
              Email
              <input type="email" name="email" value={form.email} onChange={handleChange} />
            </label>
          </div>

          <footer className="modal-footer">
            <button type="button" className="btn-ghost" onClick={onClose} disabled={saving}>
              Annuler
            </button>
            <button type="submit" className="btn-primary" disabled={saving}>
              {saving ? 'Enregistrement...' : responsable ? 'Enregistrer' : 'Créer'}
            </button>
          </footer>
        </form>
      </div>
    </div>
  )
}

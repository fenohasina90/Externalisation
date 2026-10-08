import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { CLIENT_OPTIONS, type ClientResume } from '../data/mockClients'

interface Props {
  open: boolean
  /** Client à modifier, null pour une création. */
  client: ClientResume | null
  onClose: () => void
  /** Création : actif par défaut (sans champ statut). */
  onSave: (payload: {
    nom: string
    activite: string
    responsable: string
    telephone: string
    email: string
  }) => void
}

const emptyForm = { nom: '', activite: '', responsable: '', telephone: '', email: '' }

type FormState = typeof emptyForm

/** Modale Ajouter / Modifier un client (cf. maquette, sans champ statut). */
export function ClientFormModal({ open, client, onClose, onSave }: Props) {
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
      client
        ? {
            nom: client.nom,
            activite: client.activite,
            responsable: client.responsable,
            telephone: client.telephone,
            email: client.email,
          }
        : emptyForm,
    )
    setError(null)
  }, [open, client])

  if (!open) return null

  const responsablesOptions = [...new Set([form.responsable, ...CLIENT_OPTIONS.responsables].filter(Boolean))]

  const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target
    setForm((f) => ({ ...f, [name]: value }))
  }

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    if (!form.nom.trim()) {
      setError('Le nom de la société est obligatoire')
      return
    }
    if (!form.responsable) {
      setError('Le responsable est obligatoire')
      return
    }
    onSave({
      nom: form.nom.trim(),
      activite: form.activite.trim(),
      responsable: form.responsable,
      telephone: form.telephone.trim(),
      email: form.email.trim(),
    })
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="modal-card"
        style={{ maxWidth: 560 }}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-label={client ? 'Modifier un client' : 'Ajouter un client'}
      >
        <header className="modal-header">
          <div>
            <h3 className="modal-title">{client ? 'Modifier un client' : 'Ajouter un client'}</h3>
            <p className="modal-subtitle">
              La modification utilise le même formulaire, pré-rempli.
            </p>
          </div>
          <button type="button" className="btn-ghost" onClick={onClose} aria-label="Fermer">
            ✕
          </button>
        </header>

        <form onSubmit={handleSubmit} className="modal-body">
          {error && <div style={{ color: 'var(--danger)', fontSize: 14 }}>{error}</div>}

          <div className="form-grid form-grid--2">
            <label className="field form-grid__full">
              Client <span className="required">*</span>
              <input
                name="nom"
                value={form.nom}
                onChange={handleChange}
                placeholder="Nom de la société"
                required
              />
            </label>

            <label className="field form-grid__full">
              Activité
              <input
                name="activite"
                value={form.activite}
                onChange={handleChange}
                placeholder="Ex. Services financiers"
              />
            </label>

            <label className="field form-grid__full">
              Responsable <span className="required">*</span>
              <select name="responsable" value={form.responsable} onChange={handleChange} required>
                <option value="">Sélectionner un responsable</option>
                {responsablesOptions.map((r) => (
                  <option key={r} value={r}>
                    {r}
                  </option>
                ))}
              </select>
            </label>

            <label className="field form-grid__full">
              Téléphone
              <input
                type="tel"
                name="telephone"
                value={form.telephone}
                onChange={handleChange}
                placeholder="+261 34 00 000 00"
              />
            </label>

            <label className="field form-grid__full">
              Email
              <input
                type="email"
                name="email"
                value={form.email}
                onChange={handleChange}
                placeholder="contact@societe.mg"
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

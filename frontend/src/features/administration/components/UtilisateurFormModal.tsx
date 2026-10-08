import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import type { Utilisateur } from '@/features/auth/types'
import type { RoleFilter } from '../data/mockUtilisateurs'

interface Props {
  open: boolean
  /** Utilisateur à modifier, null pour une création. */
  utilisateur: Utilisateur | null
  onClose: () => void
  onSave: (payload: {
    nom: string
    prenom: string
    username: string
    email: string
    role: RoleFilter
  }) => void
}

const emptyForm = {
  nom: '',
  prenom: '',
  username: '',
  email: '',
  role: 'ACCUEIL' as RoleFilter,
}

type FormState = typeof emptyForm

/** Modale Créer / Modifier un utilisateur (cf. maquette). */
export function UtilisateurFormModal({ open, utilisateur, onClose, onSave }: Props) {
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
      utilisateur
        ? {
            nom: utilisateur.nom,
            prenom: utilisateur.prenom,
            username: utilisateur.username,
            email: utilisateur.email,
            role: utilisateur.role.nomRole as RoleFilter,
          }
        : emptyForm,
    )
    setError(null)
  }, [open, utilisateur])

  if (!open) return null

  const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target
    setForm((f) => ({ ...f, [name]: value }))
  }

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    if (!form.nom.trim() || !form.prenom.trim()) {
      setError('Le nom et le prénom sont obligatoires')
      return
    }
    if (!form.username.trim()) {
      setError("L'identifiant est obligatoire")
      return
    }
    if (!form.email.trim()) {
      setError("L'adresse e-mail est obligatoire")
      return
    }
    onSave({
      nom: form.nom.trim(),
      prenom: form.prenom.trim(),
      username: form.username.trim(),
      email: form.email.trim(),
      role: form.role,
    })
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="modal-card"
        style={{ maxWidth: 580 }}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-label={utilisateur ? 'Modifier un utilisateur' : 'Créer un utilisateur'}
      >
        <header className="modal-header">
          <div>
            <h3 className="modal-title">
              {utilisateur ? 'Modifier un utilisateur' : 'Créer un utilisateur'}
            </h3>
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

          <div className="form-grid">
            <label className="field">
              Nom <span className="required">*</span>
              <input
                name="nom"
                value={form.nom}
                onChange={handleChange}
                placeholder="Nom"
                required
              />
            </label>

            <label className="field">
              Prénom <span className="required">*</span>
              <input
                name="prenom"
                value={form.prenom}
                onChange={handleChange}
                placeholder="Prénom"
                required
              />
            </label>

            <label className="field">
              Identifiant <span className="required">*</span>
              <input
                name="username"
                value={form.username}
                onChange={handleChange}
                placeholder="Ex. prenom.nom"
                required
              />
            </label>

            <label className="field">
              Email <span className="required">*</span>
              <input
                type="email"
                name="email"
                value={form.email}
                onChange={handleChange}
                placeholder="nom@cabinet-excellent.mg"
                required
              />
            </label>

            <label className="field form-grid__full">
              Rôle <span className="required">*</span>
              <select name="role" value={form.role} onChange={handleChange} required>
                <option value="SUPER_ADMIN">SUPER_ADMIN</option>
                <option value="ACCUEIL">ACCUEIL</option>
                <option value="ECONOMAT">ECONOMAT</option>
              </select>
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

import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import type { Utilisateur } from '@/features/auth/types'

export interface RoleOption {
  id: number
  nom: string
}

interface Props {
  open: boolean
  /** Utilisateur à modifier, null pour une création (compte actif par défaut). */
  utilisateur: Utilisateur | null
  roles: RoleOption[]
  onClose: () => void
  onSave: (payload: {
    nom: string
    prenom: string
    username: string
    email: string
    password?: string
    roleId: number
  }) => Promise<void>
}

const emptyForm = {
  nom: '',
  prenom: '',
  username: '',
  email: '',
  password: '',
  roleId: 0,
}

type FormState = typeof emptyForm

/** Modale Créer / Modifier un utilisateur (mot de passe exigé à la création). */
export function UtilisateurFormModal({ open, utilisateur, roles, onClose, onSave }: Props) {
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
      utilisateur
        ? {
            nom: utilisateur.nom,
            prenom: utilisateur.prenom,
            username: utilisateur.username,
            email: utilisateur.email,
            password: '',
            roleId: utilisateur.role.id,
          }
        : emptyForm,
    )
    setError(null)
  }, [open, utilisateur])

  // Rôle par défaut en création : ne complète que si rien n'est sélectionné,
  // sans écraser les autres champs (les roles arrivent après l'ouverture).
  useEffect(() => {
    if (!open || utilisateur !== null) return
    const premierRole = roles[0]?.id
    if (premierRole === undefined) return
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setForm((f) => (f.roleId === 0 ? { ...f, roleId: premierRole } : f))
  }, [open, utilisateur, roles])

  if (!open) return null

  const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target
    setForm((f) => ({ ...f, [name]: name === 'roleId' ? Number(value) : value }))
  }

  const handleSubmit = async (e: FormEvent) => {
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
    if (!utilisateur && form.password.length < 6) {
      setError('Le mot de passe doit contenir au moins 6 caractères')
      return
    }
    if (!form.roleId) {
      setError('Le rôle est obligatoire')
      return
    }
    setSaving(true)
    setError(null)
    try {
      await onSave({
        nom: form.nom.trim(),
        prenom: form.prenom.trim(),
        username: form.username.trim(),
        email: form.email.trim(),
        ...(utilisateur ? {} : { password: form.password }),
        roleId: form.roleId,
      })
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

            {!utilisateur && (
              <label className="field">
                Mot de passe <span className="required">*</span>
                <input
                  type="password"
                  name="password"
                  value={form.password}
                  onChange={handleChange}
                  placeholder="6 caractères minimum"
                  required
                  minLength={6}
                />
              </label>
            )}

            <label className="field">
              Rôle <span className="required">*</span>
              <select
                name="roleId"
                value={form.roleId}
                onChange={handleChange}
                required
              >
                <option value={0}>Sélectionner un rôle</option>
                {roles.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.nom}
                  </option>
                ))}
              </select>
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

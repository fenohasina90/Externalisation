import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { Autocomplete } from '@/components/ui'
import type { CreateSocietePayload, Societe } from '../types'

export interface DomaineOption {
  id: number
  nom: string
}

interface Props {
  open: boolean
  /** Client à modifier, null pour une création (actif par défaut, sans champ statut). */
  client: Societe | null
  /** Responsable principal actuel (édition). */
  responsableActuel?: string | null
  domaines: DomaineOption[]
  /** Résout un nom de domaine (sélection ou création si inexistant). */
  resolveDomaine: (nom: string) => Promise<number>
  onClose: () => void
  onSave: (
    payload: CreateSocietePayload & { responsable: string },
    id?: number,
  ) => Promise<void>
}

const emptyForm = {
  nomSociete: '',
  activite: '',
  domaine: '',
  responsable: '',
  contact: '',
  email: '',
}

type FormState = typeof emptyForm

function correspondExact(nom: string, domaines: DomaineOption[]): DomaineOption | null {
  const cherche = nom.trim().toLowerCase()
  if (!cherche) return null
  return domaines.find((d) => d.nom.toLowerCase() === cherche) ?? null
}

/** Modale Ajouter / Modifier un client (domaine en autocomplétion, sans champ statut). */
export function ClientFormModal({ open, client, responsableActuel, domaines, resolveDomaine, onClose, onSave }: Props) {
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
    const nomDomaine = client
      ? (domaines.find((d) => d.id === client.domaineId)?.nom ?? '')
      : ''
    setForm(
      client
        ? {
            nomSociete: client.nomSociete,
            activite: client.activite ?? '',
            domaine: nomDomaine,
            responsable: responsableActuel ?? client.responsablePrincipal ?? '',
            contact: client.contact ?? '',
            email: client.email ?? '',
          }
        : emptyForm,
    )
    setError(null)
  }, [open, client, responsableActuel, domaines])

  if (!open) return null

  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target
    setForm((f) => ({ ...f, [name]: value }))
  }

  const domaineExiste = correspondExact(form.domaine, domaines) !== null

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (!form.nomSociete.trim()) {
      setError('Le nom de la société est obligatoire')
      return
    }
    if (!form.domaine.trim()) {
      setError('Le domaine est obligatoire')
      return
    }
    if (!form.responsable.trim()) {
      setError('Le responsable est obligatoire')
      return
    }
    setSaving(true)
    setError(null)
    try {
      const domaineId = await resolveDomaine(form.domaine.trim())
      await onSave(
        {
          nomSociete: form.nomSociete.trim(),
          activite: form.activite.trim() || null,
          contact: form.contact.trim() || null,
          email: form.email.trim() || null,
          domaineId,
          responsable: form.responsable.trim(),
        },
        client?.id,
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
                name="nomSociete"
                value={form.nomSociete}
                onChange={handleChange}
                placeholder="Nom de la société"
                required
              />
            </label>

            <label className="field">
              Activité
              <input
                name="activite"
                value={form.activite}
                onChange={handleChange}
                placeholder="Ex. Services financiers"
              />
            </label>

            <div className="field">
              <span className="field-label">
                Domaine <span className="required">*</span>
              </span>
              <Autocomplete
                value={form.domaine}
                placeholder="Ex. Services financiers"
                onTextChange={(texte) => setForm((f) => ({ ...f, domaine: texte }))}
                onPick={() => undefined}
                options={domaines.map((d) => ({ id: d.id, label: d.nom }))}
                footer={
                  form.domaine.trim() !== '' && !domaineExiste ? (
                    <span className="autocomplete__hint">
                      « {form.domaine.trim()} » sera créé comme nouveau domaine.
                    </span>
                  ) : undefined
                }
              />
            </div>

            <label className="field">
              Responsable <span className="required">*</span>
              <input
                name="responsable"
                value={form.responsable}
                onChange={handleChange}
                placeholder="Nom et prénom"
                required
              />
            </label>

            <label className="field">
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

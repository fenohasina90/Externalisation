import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import type { CreateServicePayload, Domaine, Service, UpdateServicePayload } from '../types'

interface Props {
  open: boolean
  /** Service à modifier, null pour une création (code auto-généré, actif par défaut). */
  service: Service | null
  /** Liste des domaines proposés pour le choix du domaine du service. */
  domaines?: Domaine[]
  onClose: () => void
  onSave: (payload: CreateServicePayload | UpdateServicePayload, id?: number) => Promise<void>
}

const emptyForm = { nomService: '', typeService: 'STANDARD', description: '', domaineId: '' }

type FormState = typeof emptyForm

/** Modale Ajouter / Modifier un service (sans champ statut ni code). */
export function ServiceFormModal({ open, service, domaines = [], onClose, onSave }: Props) {
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
            typeService: service.typeService ?? 'STANDARD',
            description: service.description ?? '',
            domaineId: service.domaineId != null ? String(service.domaineId) : '',
          }
        : emptyForm,
    )
    setError(null)
  }, [open, service, domaines])

  if (!open) return null

  const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
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
      const payload: CreateServicePayload | UpdateServicePayload = {
        nomService: form.nomService.trim(),
        typeService: form.typeService,
        description: form.description.trim() || null,
        domaineId: form.domaineId.trim() ? Number(form.domaineId) : null,
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

          {/* {!service && (
            <p className="modal-subtitle form-grid__full" style={{ margin: 0 }}>
              Le code sera généré automatiquement au format SRV-CODE_DOMAINE-XXXX.
            </p>
          )} */}

            <label className="field form-grid__full">
              Domaine
              <select
                name="domaineId"
                value={form.domaineId}
                onChange={handleChange}
              >
                <option value="">Aucun (code SRV-XXXX)</option>
                {domaines.map((d) => (
                  <option key={d.id} value={String(d.id)}>
                    {d.nomDomaine} ({d.codeDomaine})
                  </option>
                ))}
              </select>
              {/* <span className="form-help">
                Le domaine détermine le préfixe du code de service (ex. SRV-COMP-0007).
              </span> */}
            </label>

            <label className="field form-grid__full">
              Type de service
              <select
                name="typeService"
                value={form.typeService}
                onChange={handleChange}
              >
                <option value="STANDARD">Standard</option>
                <option value="CREATION">Création d&apos;entreprise ou de société</option>
                <option value="TRANSACTION_IMMOBILIERE">Transaction immobilière</option>
              </select>
              {/* <span className="form-help">
                Les services de création et de transaction immobilière portent une fiche dédiée.
              </span> */}
            </label>

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

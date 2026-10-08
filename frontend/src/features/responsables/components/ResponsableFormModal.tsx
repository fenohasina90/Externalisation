import { useState, type FormEvent } from 'react'
import { Button, Field, Input, Select } from '@/components/ui'
import { MESSAGES } from '@/constants/messages'
import { useApi } from '@/hooks/useApi'
import type { ApiError } from '@/types'
import { responsablesApi } from '../services/responsables.service'
import type { CreateResponsablePayload, Responsable, UpdateResponsablePayload } from '../types'

interface ResponsableFormModalProps {
  /** Absent : création. Présent : modification. */
  responsable?: Responsable
  onClose: () => void
  onSaved: (message: string) => void
}

interface Values {
  societeId: string
  nomClient: string
  fonction: string
  contact: string
  email: string
}

type Errors = Partial<Record<keyof Values, string>>

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function ResponsableFormModal({
  responsable,
  onClose,
  onSaved,
}: ResponsableFormModalProps) {
  const isEdit = Boolean(responsable)
  const societes = useApi(() => responsablesApi.societes(), [])

  const [values, setValues] = useState<Values>({
    societeId: responsable ? String(responsable.societeId ?? '') : '',
    nomClient: responsable?.nomClient ?? '',
    fonction: responsable?.fonction ?? '',
    contact: responsable?.contact ?? '',
    email: responsable?.email ?? '',
  })
  const [errors, setErrors] = useState<Errors>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  function updateField(field: keyof Values, value: string): void {
    setValues((previous) => ({ ...previous, [field]: value }))
    setErrors((previous) => ({ ...previous, [field]: undefined }))
  }

  const bind = (field: keyof Values) => ({
    value: values[field],
    onChange: (event: { target: { value: string } }) => updateField(field, event.target.value),
  })

  function validate(): Errors {
    const result: Errors = {}
    if (!isEdit && !values.societeId) result.societeId = 'La société est obligatoire.'
    if (!values.nomClient.trim()) result.nomClient = 'Le nom du responsable est obligatoire.'
    if (values.email.trim() && !EMAIL_PATTERN.test(values.email.trim())) {
      result.email = 'L\'email doit être valide.'
    }
    return result
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault()
    setFormError(null)

    const found = validate()
    setErrors(found)
    if (Object.keys(found).length > 0) return

    setSaving(true)
    try {
      if (responsable) {
        const payload: UpdateResponsablePayload = {
          nomClient: values.nomClient.trim(),
          fonction: values.fonction.trim() || null,
          contact: values.contact.trim() || null,
          email: values.email.trim() || null,
        }
        await responsablesApi.update(responsable.id, payload)
        onSaved('Responsable modifié.')
      } else {
        const payload: CreateResponsablePayload = {
          societeId: Number(values.societeId),
          nomClient: values.nomClient.trim(),
          fonction: values.fonction.trim() || null,
          contact: values.contact.trim() || null,
          email: values.email.trim() || null,
        }
        await responsablesApi.create(payload)
        onSaved('Responsable ajouté.')
      }
    } catch (error) {
      const apiError = error as ApiError
      if (apiError.fieldErrors) {
        const newErrors: Errors = {}
        Object.entries(apiError.fieldErrors).forEach(([key, message]) => {
          const mapped = key === 'nomClient' ? 'nomClient' : key as keyof Values
          if (mapped in values) newErrors[mapped] = message
        })
        setErrors((previous) => ({ ...previous, ...newErrors }))
        setFormError('Corrigez les champs signalés.')
      } else {
        setFormError(apiError.message || MESSAGES.generic)
      }
      setSaving(false)
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
        <div className="modal__header">
          <h3 className="modal__title">
            {isEdit ? 'Modifier le responsable' : 'Ajouter un responsable'}
          </h3>
          <button type="button" className="modal__close" onClick={onClose} aria-label="Fermer">
            ×
          </button>
        </div>
        <form className="modal__body form-grid" onSubmit={(event) => void handleSubmit(event)} noValidate>
          {formError ? (
            <div className="form-alert form-grid__full" role="alert">
              {formError}
            </div>
          ) : null}

          <div className="form-grid__full">
            <Field label="Société *" id="societeId" error={errors.societeId}>
              <Select id="societeId" {...bind('societeId')} disabled={isEdit || societes.loading}>
                <option value="">
                  {societes.loading ? 'Chargement…' : 'Sélectionner une société'}
                </option>
                {societes.data?.map((societe) => (
                  <option key={societe.id} value={societe.id}>
                    {societe.nom}
                  </option>
                ))}
              </Select>
            </Field>
            {societes.error ? <p className="field-error">{societes.error}</p> : null}
          </div>

          <Field label="Nom du responsable *" id="nomClient" error={errors.nomClient}>
            <Input id="nomClient" placeholder="Nom et prénom" autoComplete="off" {...bind('nomClient')} />
          </Field>
          <Field label="Fonction" id="fonction" error={errors.fonction}>
            <Input id="fonction" placeholder="Ex. Directeur financier" {...bind('fonction')} />
          </Field>
          <Field label="Téléphone" id="contact" error={errors.contact}>
            <Input id="contact" type="tel" placeholder="+261 34 00 000 00" {...bind('contact')} />
          </Field>
          <Field label="Email" id="email" error={errors.email}>
            <Input id="email" type="email" placeholder="nom@societe.mg" {...bind('email')} />
          </Field>

          <div className="modal__actions form-grid__full">
            <Button type="button" variant="secondary" onClick={onClose}>
              Annuler
            </Button>
            <Button type="submit" loading={saving} loadingLabel="Enregistrement…">
              Enregistrer
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
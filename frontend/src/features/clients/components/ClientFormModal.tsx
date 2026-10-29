import { useState, type ChangeEvent } from 'react'
// import { Autocomplete } from '@/components/ui'
import { FormEtapes } from '@/components/shared/FormEtapes'
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
  onClose: () => void
  onSave: (
    payload: CreateSocietePayload & { responsable: string },
    id?: number,
  ) => Promise<void>
}

const emptyForm = {
  nomSociete: '',
  activite: '',
  // domaine: '',
  responsable: '',
  contact: '',
  email: '',
  fb: '',
  adresse: '',
  dateCreation: '',
  nombrePersonnel: '',
  chiffreAffaireMensuel: '',
}

type FormState = typeof emptyForm

// function correspondExact(nom: string, domaines: DomaineOption[]): DomaineOption | null {
//   const cherche = nom.trim().toLowerCase()
//   if (!cherche) return null
//   return domaines.find((d) => d.nom.toLowerCase() === cherche) ?? null
// }

function formulaireInitial(
  client: Societe | null,
  responsableActuel: string | null | undefined,
  // domaines: DomaineOption[],
): FormState {
  if (!client) return { ...emptyForm }
  return {
    nomSociete: client.nomSociete,
    activite: client.activite ?? '',
    // domaine: domaines.find((d) => d.id === client.domaineId)?.nom ?? '',
    responsable: responsableActuel ?? client.responsablePrincipal ?? '',
    contact: client.contact ?? '',
    email: client.email ?? '',
    fb: client.fb ?? '',
    adresse: client.adresse ?? '',
    dateCreation: client.dateCreation ?? '',
    nombrePersonnel:
      client.nombrePersonnel === null || client.nombrePersonnel === undefined
        ? ''
        : String(client.nombrePersonnel),
    chiffreAffaireMensuel:
      client.chiffreAffaireMensuel === null || client.chiffreAffaireMensuel === undefined
        ? ''
        : String(client.chiffreAffaireMensuel),
  }
}

/**
 * Modale Ajouter / Modifier un client en deux étapes (FormEtapes) :
 * 1. informations générales (nom, activité, domaine, responsable, contact, email),
 * 2. détails de la société (facebook, adresse, date de création, personnel,
 *    chiffre d'affaires mensuel — tous facultatifs).
 */
export function ClientFormModal({ open, client, responsableActuel, onClose, onSave }: Props) {
  const [form, setForm] = useState<FormState>(emptyForm)
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  // const [domaineCree, setDomaineCree] = useState<string | null>(null)
  // const [creationDomaineEnCours, setCreationDomaineEnCours] = useState(false)
  const [formKey, setFormKey] = useState<string | null>(null)

  // `key` remontée à chaque ouverture : réinitialise l'étape et le formulaire.
  const resetKey = open ? `ouvert-${client?.id ?? 'nouveau'}` : 'ferme'

  // Réinitialisation pendant le rendu (pas dans un effet : pas de rendus en cascade).
  if (formKey !== resetKey) {
    setFormKey(resetKey)
    if (open) {
      setForm(formulaireInitial(client, responsableActuel))
      setError(null)
      setSaving(false)
      // setDomaineCree(null)
    }
  }

  if (!open) return null

  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target
    setForm((f) => ({ ...f, [name]: value }))
  }

  // const domaineExiste = correspondExact(form.domaine, domaines) !== null
  // const texteDomainePropre = form.domaine.trim()

  // const handleCreerDomaine = async () => {
  //   if (texteDomainePropre === '' || domaineExiste) return
  //   setCreationDomaineEnCours(true)
  //   try {
  //     await (onCreerDomaine ?? resolveDomaine)(texteDomainePropre)
  //     setDomaineCree(texteDomainePropre)
  //   } catch (err) {
  //     setError(err instanceof Error ? err.message : 'Échec de la création du domaine')
  //   } finally {
  //     setCreationDomaineEnCours(false)
  //   }
  // }

  /** Étape 1 : champs obligatoires de l'identité du client. */
  const validerInformations = (): string | null => {
    if (!form.nomSociete.trim()) return 'Le nom de la société est obligatoire'
    // if (!form.domaine.trim()) return 'Le domaine est obligatoire'
    if (!form.responsable.trim()) return 'Le responsable est obligatoire'
    return null
  }

  /** Étape 2 : les détails sont facultatifs mais doivent être cohérents s'ils sont remplis. */
  const validerDetails = (): string | null => {
    if (form.dateCreation) {
      const date = new Date(form.dateCreation)
      if (Number.isNaN(date.getTime())) return 'La date de création est invalide'
      const aujourdHui = new Date()
      aujourdHui.setHours(0, 0, 0, 0)
      if (date > aujourdHui) return 'La date de création ne peut pas être dans le futur'
    }
    if (form.nombrePersonnel) {
      const nombre = Number(form.nombrePersonnel)
      if (!Number.isInteger(nombre) || nombre < 1) {
        return 'Le nombre de personnel doit être un entier positif'
      }
    }
    if (form.chiffreAffaireMensuel) {
      const montant = Number(form.chiffreAffaireMensuel)
      if (Number.isNaN(montant) || montant < 0) {
        return "Le chiffre d'affaires mensuel doit être positif ou nul"
      }
    }
    return null
  }

  const validerEtape = (index: number): string | null =>
    index === 0 ? validerInformations() : validerDetails()

  const handleTerminer = async () => {
    setSaving(true)
    setError(null)
    try {
      // const domaineId = await resolveDomaine(form.domaine.trim())
      await onSave(
        {
          nomSociete: form.nomSociete.trim(),
          activite: form.activite.trim() || null,
          contact: form.contact.trim() || null,
          email: form.email.trim() || null,
          fb: form.fb.trim() || null,
          adresse: form.adresse.trim() || null,
          dateCreation: form.dateCreation || null,
          nombrePersonnel: form.nombrePersonnel ? Number(form.nombrePersonnel) : null,
          chiffreAffaireMensuel: form.chiffreAffaireMensuel
            ? Number(form.chiffreAffaireMensuel)
            : null,
          // domaineId,
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
              {client
                ? 'La modification utilise le même formulaire, pré-rempli.'
                : 'Créez le client en deux étapes.'}
            </p>
          </div>
          <button type="button" className="btn-ghost" onClick={onClose} aria-label="Fermer">
            ✕
          </button>
        </header>

        <div className="modal-body">
          <FormEtapes
            key={resetKey}
            etapes={[
              { titre: 'Informations', description: 'Identité du client et premier contact.' },
              { titre: 'Détails', description: 'Société : tous les champs sont facultatifs.' },
            ]}
            validerEtape={validerEtape}
            onTerminer={() => void handleTerminer()}
            onAnnuler={onClose}
            enregistrement={saving}
            erreur={error}
          >
            {(etape) =>
              etape === 0 ? (
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

                  <label className="field form-grid__full">
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
                    Activité
                    <input
                      name="activite"
                      value={form.activite}
                      onChange={handleChange}
                      placeholder="Ex. Services financiers"
                    />
                  </label>

                  <div className="field">
                    <label className="field">
                    Contact du société
                    <input
                      type="tel"
                      name="contact"
                      value={form.contact}
                      onChange={handleChange}
                      placeholder="+261 34 00 000 00"
                    />
                  </label>
                  </div>

                  <label className="field form-grid__full">
                    Email du société
                    <input
                      type="email"
                      name="email"
                      value={form.email}
                      onChange={handleChange}
                      placeholder="contact@societe.mg"
                    />
                  </label>
                </div>
              ) : (
                <div className="form-grid form-grid--2">
                  <label className="field form-grid__full">
                    Facebook
                    <input
                      name="fb"
                      value={form.fb}
                      onChange={handleChange}
                      placeholder="Lien ou nom de la page Facebook"
                    />
                  </label>

                  <label className="field form-grid__full">
                    Adresse
                    <input
                      name="adresse"
                      value={form.adresse}
                      onChange={handleChange}
                      placeholder="Adresse du siège ou des bureaux"
                    />
                  </label>

                  <label className="field">
                    Date de création de la société
                    <input
                      type="date"
                      name="dateCreation"
                      value={form.dateCreation}
                      onChange={handleChange}
                    />
                  </label>

                  <label className="field">
                    Nombre de personnel
                    <input
                      type="number"
                      min={1}
                      step={1}
                      name="nombrePersonnel"
                      value={form.nombrePersonnel}
                      onChange={handleChange}
                      placeholder="Ex. 12"
                    />
                  </label>

                  <label className="field form-grid__full">
                    Chiffre d&apos;affaires mensuel
                    <input
                      type="number"
                      min={0}
                      step="0.01"
                      name="chiffreAffaireMensuel"
                      value={form.chiffreAffaireMensuel}
                      onChange={handleChange}
                      placeholder="Ex. 1500000"
                    />
                  </label>
                </div>
              )
            }
          </FormEtapes>
        </div>
      </div>
    </div>
  )
}

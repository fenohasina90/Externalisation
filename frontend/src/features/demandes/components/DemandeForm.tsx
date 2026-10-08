import { useEffect, useRef, useState, type FormEvent } from 'react'
import {
  COMMERCIAUX_OPTIONS,
  DEMANDE_OPTIONS,
  RESPONSABLES_PAR_CLIENT,
  TARIF_PAR_SERVICE,
} from '../data/mockDemandes'

export interface DemandeFormValues {
  client: string
  responsable: string
  commercial: string
  domaine: string
  service: string
  dateDemande: string
  debutService: string
  finService: string
  tarif: number
  quantite: number
  commentaire: string
}

interface Props {
  initial: DemandeFormValues
  submitLabel: string
  onCancel: () => void
  onSubmit: (v: DemandeFormValues) => void
  /** Affiche la zone de dépôt de documents (page de création uniquement). */
  avecDocuments?: boolean
  /** Remonte les valeurs saisies (récapitulatif de la page de création). */
  onChange?: (v: DemandeFormValues) => void
}

export const DEMANDE_FORM_VIDE: DemandeFormValues = {
  client: 'Atlas Conseil',
  responsable: 'Amina Neme',
  commercial: 'Jean Dupont',
  domaine: 'Comptabilité',
  service: 'Audit comptable',
  dateDemande: '2026-09-02',
  debutService: '2026-09-01',
  finService: '2027-02-28',
  tarif: 500000,
  quantite: 2,
  commentaire: '',
}

/** Convertit aaaa-mm-jj → jj/mm/aaaa pour l'enregistrement statique. */
export function versDateFR(iso: string): string {
  const [a, m, j] = iso.split('-')
  return `${j}/${m}/${a}`
}

/** Formulaire partagé création / modification d'une demande. */
export function DemandeForm({ initial, submitLabel, onCancel, onSubmit, avecDocuments = false, onChange }: Props) {
  const [form, setForm] = useState<DemandeFormValues>(initial)
  const [error, setError] = useState<string | null>(null)
  const onChangeRef = useRef(onChange)
  onChangeRef.current = onChange

  useEffect(() => {
    onChangeRef.current?.(form)
  }, [form])

  const set = (cle: keyof DemandeFormValues, valeur: string | number) => {
    setForm((f) => {
      const next = { ...f, [cle]: valeur }
      // Tarif courant récupéré automatiquement au changement de service.
      if (cle === 'service' && typeof valeur === 'string' && TARIF_PAR_SERVICE[valeur] != null) {
        next.tarif = TARIF_PAR_SERVICE[valeur]
      }
      // Responsable réinitialisé au changement de client.
      if (cle === 'client' && typeof valeur === 'string') {
        const premiers = RESPONSABLES_PAR_CLIENT[valeur] ?? []
        next.responsable = premiers[0] ?? ''
      }
      return next
    })
  }

  const montantTotal = form.tarif * form.quantite

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    if (!form.client || !form.responsable || !form.commercial || !form.service) {
      setError('Client, responsable, commercial et service sont obligatoires')
      return
    }
    if (!form.dateDemande || !form.debutService || !form.finService) {
      setError('Les dates de demande et de service sont obligatoires')
      return
    }
    if (form.finService < form.debutService) {
      setError('La fin du service doit être postérieure au début')
      return
    }
    if (form.tarif <= 0 || form.quantite <= 0) {
      setError('Le tarif et la quantité doivent être positifs')
      return
    }
    onSubmit({ ...form, commentaire: form.commentaire.trim() })
  }

  const responsables = RESPONSABLES_PAR_CLIENT[form.client] ?? []

  return (
    <form onSubmit={handleSubmit}>
      {error && (
        <div className="alert alert-danger" role="alert">
          {error}
        </div>
      )}

      <div className="form-grid">
        <label className="field">
          Client <span className="required">*</span>
          <select value={form.client} onChange={(e) => set('client', e.target.value)} required>
            {DEMANDE_OPTIONS.clients.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          Responsable <span className="required">*</span>
          <select value={form.responsable} onChange={(e) => set('responsable', e.target.value)} required>
            {responsables.map((r) => (
              <option key={r} value={r}>
                {r}
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          Commercial <span className="required">*</span>
          <select value={form.commercial} onChange={(e) => set('commercial', e.target.value)} required>
            {COMMERCIAUX_OPTIONS.map((c) => (
              <option key={c.nom} value={c.nom}>
                {c.nom} ({c.matricule})
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          Domaine <span className="required">*</span>
          <select value={form.domaine} onChange={(e) => set('domaine', e.target.value)} required>
            {DEMANDE_OPTIONS.domaines.map((d) => (
              <option key={d} value={d}>
                {d}
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          Service <span className="required">*</span>
          <select value={form.service} onChange={(e) => set('service', e.target.value)} required>
            {DEMANDE_OPTIONS.services.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          Date de demande <span className="required">*</span>
          <input type="date" value={form.dateDemande} onChange={(e) => set('dateDemande', e.target.value)} required />
        </label>

        <label className="field">
          Début du service <span className="required">*</span>
          <input type="date" value={form.debutService} onChange={(e) => set('debutService', e.target.value)} required />
        </label>

        <label className="field">
          Fin du service <span className="required">*</span>
          <input type="date" value={form.finService} onChange={(e) => set('finService', e.target.value)} required />
        </label>

        <label className="field">
          Tarif (MGA) <span className="required">*</span>
          <input
            type="number"
            min={1}
            value={form.tarif}
            onChange={(e) => set('tarif', Number(e.target.value))}
            required
          />
          <span className="form-help">
            Tarif courant du service récupéré automatiquement ; vous pouvez le modifier.
          </span>
        </label>

        <label className="field">
          Quantité <span className="required">*</span>
          <input
            type="number"
            min={1}
            value={form.quantite}
            onChange={(e) => set('quantite', Number(e.target.value))}
            required
          />
        </label>

        <label className="field form-grid__full">
          Montant total (MGA)
          <input type="text" value={montantTotal.toLocaleString('fr-FR')} readOnly disabled />
          <span className="form-help">Tarif × quantité</span>
        </label>

        <label className="field form-grid__full">
          Commentaire
          <textarea
            value={form.commentaire}
            onChange={(e) => set('commentaire', e.target.value)}
            placeholder="Information complémentaire (facultatif)"
            rows={3}
            style={{ height: 92 }}
          />
        </label>

        {avecDocuments && (
          <div className="field form-grid__full">
            <span className="field-label">Documents</span>
            <div className="dz" role="button" tabIndex={0} aria-label="Ajouter des documents">
              <div>↑</div>
              <div>
                <strong className="lk">Parcourir</strong> ou glisser des fichiers ici
              </div>
              <small>PDF, JPG, PNG ou DOCX</small>
            </div>
          </div>
        )}
      </div>

      <footer className="modal-footer" style={{ borderTop: 'none', padding: '16px 0 0' }}>
        <button type="button" className="btn-ghost" onClick={onCancel}>
          Annuler
        </button>
        <button type="submit" className="btn-primary">
          {submitLabel}
        </button>
      </footer>
    </form>
  )
}

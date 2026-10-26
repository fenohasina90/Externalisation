import { useEffect, useState, type FormEvent } from 'react'
import { StatutDemandePill } from './DemandesTable'
import { TRANSITIONS_MANUELLES, type StatutDemande } from '../types'

const LIBELLES: Record<string, string> = {
  EN_ATTENTE: 'En attente',
  VALIDATION: 'Validation',
  EN_COURS: 'En cours',
  TERMINEE: 'Terminée',
  ANNULEE: 'Annulée',
}

interface Props {
  open: boolean
  statutActuel: string
  onClose: () => void
  onConfirm: (nouveau: StatutDemande, commentaire: string) => void
  enregistrement?: boolean
}

/** Changement manuel de statut d'une demande (contrôles backend). */
export function ChangerStatutModal({ open, statutActuel, onClose, onConfirm, enregistrement = false }: Props) {
  const cibles = TRANSITIONS_MANUELLES[statutActuel] ?? []
  const [nouveauStatut, setNouveauStatut] = useState<StatutDemande>('TERMINEE')
  const [commentaire, setCommentaire] = useState('')

  useEffect(() => {
    if (open) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setNouveauStatut(cibles[0] ?? 'TERMINEE')
      setCommentaire('')
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open])

  if (!open) return null

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    onConfirm(nouveauStatut, commentaire)
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="modal-card"
        style={{ maxWidth: 480 }}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-label="Changer le statut"
      >
        <header className="modal-header">
          <div>
            <h3 className="modal-title">Changer le statut</h3>
            <p className="modal-subtitle">
              Statut actuel : <StatutDemandePill statut={statutActuel} />
            </p>
          </div>
          <button type="button" className="btn-ghost" onClick={onClose} aria-label="Fermer">
            ✕
          </button>
        </header>

        {cibles.length === 0 ? (
          <div className="modal-body">
            <p className="section-note">Aucune transition manuelle possible depuis ce statut.</p>
            <footer className="modal-footer">
              <button type="button" className="btn-ghost" onClick={onClose}>
                Fermer
              </button>
            </footer>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="modal-body">
            <label className="field">
              Nouveau statut <span className="required">*</span>
              <select
                value={nouveauStatut}
                onChange={(e) => setNouveauStatut(e.target.value as StatutDemande)}
              >
                {cibles.map((s) => (
                  <option key={s} value={s}>
                    {LIBELLES[s] ?? s}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              Commentaire
              <input
                value={commentaire}
                onChange={(e) => setCommentaire(e.target.value)}
                placeholder="Précisions éventuelles"
              />
            </label>
            {statutActuel === 'EN_ATTENTE' && nouveauStatut === 'VALIDATION' && (
              <p className="section-note">
                La validation exige : factures validées et plan de paiement établi pour chaque
                service (contrôlé par le backend).
              </p>
            )}
            <footer className="modal-footer">
              <button type="button" className="btn-ghost" onClick={onClose} disabled={enregistrement}>
                Annuler
              </button>
              <button type="submit" className="btn-primary" disabled={enregistrement}>
                {enregistrement ? 'Enregistrement…' : 'Confirmer'}
              </button>
            </footer>
          </form>
        )}
      </div>
    </div>
  )
}

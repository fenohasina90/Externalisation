import { useEffect, useState, type FormEvent } from 'react'
import { STATUT_LABEL, type DemandeResume, type StatutDemandeUI } from '../data/mockDemandes'
import { StatutDemandePill } from './DemandesTable'

/** Transitions manuelles autorisées depuis chaque statut. */
export const TRANSITIONS_MANUELLES: Record<StatutDemandeUI, StatutDemandeUI[]> = {
  EN_ATTENTE: ['ANNULEE'],
  VALIDATION: ['ANNULEE'],
  EN_COURS: ['TERMINEE', 'ANNULEE'],
  TERMINEE: [],
  ANNULEE: [],
}

const INDICATION: Record<StatutDemandeUI, string | null> = {
  EN_ATTENTE: null,
  VALIDATION: null,
  EN_COURS: 'Autre option depuis « En cours » : Annulée.',
  TERMINEE: null,
  ANNULEE: null,
}

interface Props {
  open: boolean
  demande: DemandeResume | null
  onClose: () => void
  onConfirm: (nouveauStatut: StatutDemandeUI, commentaire: string) => void
}

/** Modale « Changer le statut » (cf. maquette). */
export function ChangerStatutModal({ open, demande, onClose, onConfirm }: Props) {
  const cibles = demande ? TRANSITIONS_MANUELLES[demande.statut] : []
  const [nouveauStatut, setNouveauStatut] = useState<StatutDemandeUI>('TERMINEE')
  const [commentaire, setCommentaire] = useState('')

  useEffect(() => {
    if (open && demande) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setNouveauStatut(TRANSITIONS_MANUELLES[demande.statut][0] ?? 'TERMINEE')
      setCommentaire('')
    }
  }, [open, demande])

  if (!open || !demande) return null

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    onConfirm(nouveauStatut, commentaire.trim())
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="modal-card"
        style={{ maxWidth: 520 }}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-label="Changer le statut"
      >
        <header className="modal-header">
          <div>
            <h3 className="modal-title">Changer le statut</h3>
          </div>
          <button type="button" className="btn-ghost" onClick={onClose} aria-label="Fermer">
            ✕
          </button>
        </header>

        <form onSubmit={handleSubmit} className="modal-body">
          <p className="section-note">
            {demande.reference} · statut actuel : <StatutDemandePill statut={demande.statut} />
          </p>

          {cibles.length === 0 ? (
            <div className="alert alert-info">
              Aucune transition manuelle possible depuis « {STATUT_LABEL[demande.statut]} ».
            </div>
          ) : (
            <>
              <label className="field">
                Nouveau statut <span className="required">*</span>
                <select
                  value={nouveauStatut}
                  onChange={(e) => setNouveauStatut(e.target.value as StatutDemandeUI)}
                  required
                >
                  {cibles.map((s) => (
                    <option key={s} value={s}>
                      {STATUT_LABEL[s]}
                    </option>
                  ))}
                </select>
              </label>
              {INDICATION[demande.statut] && (
                <p className="form-help">{INDICATION[demande.statut]}</p>
              )}

              <label className="field">
                Commentaire
                <textarea
                  value={commentaire}
                  onChange={(e) => setCommentaire(e.target.value)}
                  placeholder="Motif ou précision (facultatif)"
                  rows={3}
                  style={{ height: 92 }}
                />
              </label>

              <div className="nt">
                Utilisateur, date et heure sont enregistrés automatiquement dans
                l&apos;historique de la demande.
              </div>
            </>
          )}

          <footer className="modal-footer">
            <button type="button" className="btn-ghost" onClick={onClose}>
              Annuler
            </button>
            {cibles.length > 0 && (
              <button type="submit" className="btn-primary">
                Confirmer le changement
              </button>
            )}
          </footer>
        </form>
      </div>
    </div>
  )
}

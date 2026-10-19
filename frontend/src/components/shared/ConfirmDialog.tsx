import { createPortal } from 'react-dom'
import type { ReactNode } from 'react'

interface ConfirmDialogProps {
  open: boolean
  titre: string
  message: string
  /** Libellé du bouton de confirmation (défaut : « Confirmer »). */
  confirmer?: string
  /** Couleur du bouton de confirmation. */
  ton?: 'danger' | 'success' | 'primary'
  /** Contenu additionnel sous le message (récapitulatif, avertissement…). */
  details?: ReactNode
  /** Message d'erreur survenue pendant l'action confirmée. */
  erreur?: string | null
  /** Action en cours : désactive la fermeture et le bouton de confirmation. */
  enCours?: boolean
  onAnnuler: () => void
  onConfirmer: () => void
}

/**
 * Modale de confirmation réutilisable : activation / désactivation,
 * archivage / restauration, déconnexion, validation d'un paiement.
 */
export function ConfirmDialog({
  open,
  titre,
  message,
  confirmer = 'Confirmer',
  ton = 'danger',
  details,
  erreur = null,
  enCours = false,
  onAnnuler,
  onConfirmer,
}: ConfirmDialogProps) {
  if (!open) return null

  const classeBouton =
    ton === 'danger' ? 'btn-danger' : ton === 'success' ? 'btn-primary' : 'btn-primary'

  // Portail vers <body> : la modale recouvre tout l'écran quel que soit
  // l'endroit où elle est déclarée (ex. à l'intérieur de la sidebar).
  return createPortal(
    <div
      className="modal-backdrop modal-backdrop--center"
      onClick={enCours ? undefined : onAnnuler}
    >
      <div
        className="modal-card"
        style={{ maxWidth: 440 }}
        onClick={(e) => e.stopPropagation()}
        role="alertdialog"
        aria-modal="true"
        aria-label={titre}
      >
        <header className="modal-header">
          <h3 className="modal-title">{titre}</h3>
          <button
            type="button"
            className="btn-ghost"
            onClick={onAnnuler}
            aria-label="Fermer"
            disabled={enCours}
          >
            ✕
          </button>
        </header>

        <div className="modal-body">
          <p className="section-note" style={{ fontSize: 15 }}>
            {message}
          </p>
          {details}
          {erreur && (
            <div className="alert alert-danger" role="alert" style={{ marginTop: 12 }}>
              {erreur}
            </div>
          )}
        </div>

        <footer className="modal-footer">
          <button type="button" className="btn-ghost" onClick={onAnnuler} disabled={enCours}>
            Annuler
          </button>
          <button
            type="button"
            className={classeBouton}
            onClick={onConfirmer}
            autoFocus
            disabled={enCours}
          >
            {enCours ? 'Enregistrement…' : confirmer}
          </button>
        </footer>
      </div>
    </div>,
    document.body,
  )
}

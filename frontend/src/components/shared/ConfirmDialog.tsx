import { createPortal } from 'react-dom'

interface ConfirmDialogProps {
  open: boolean
  titre: string
  message: string
  /** Libellé du bouton de confirmation (défaut : « Confirmer »). */
  confirmer?: string
  /** Couleur du bouton de confirmation. */
  ton?: 'danger' | 'success' | 'primary'
  onAnnuler: () => void
  onConfirmer: () => void
}

/**
 * Modale de confirmation réutilisable : activation / désactivation,
 * archivage / restauration, déconnexion.
 */
export function ConfirmDialog({
  open,
  titre,
  message,
  confirmer = 'Confirmer',
  ton = 'danger',
  onAnnuler,
  onConfirmer,
}: ConfirmDialogProps) {
  if (!open) return null

  const classeBouton =
    ton === 'danger' ? 'btn-danger' : ton === 'success' ? 'btn-primary' : 'btn-primary'

  // Portail vers <body> : la modale recouvre tout l'écran quel que soit
  // l'endroit où elle est déclarée (ex. à l'intérieur de la sidebar).
  return createPortal(
    <div className="modal-backdrop modal-backdrop--center" onClick={onAnnuler}>
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
          <button type="button" className="btn-ghost" onClick={onAnnuler} aria-label="Fermer">
            ✕
          </button>
        </header>

        <div className="modal-body">
          <p className="section-note" style={{ fontSize: 15 }}>
            {message}
          </p>
        </div>

        <footer className="modal-footer">
          <button type="button" className="btn-ghost" onClick={onAnnuler}>
            Annuler
          </button>
          <button type="button" className={classeBouton} onClick={onConfirmer} autoFocus>
            {confirmer}
          </button>
        </footer>
      </div>
    </div>,
    document.body,
  )
}

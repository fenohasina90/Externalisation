import { useState, type ReactNode } from 'react'

export interface EtapeFormulaire {
  /** Titre court affiché dans l'indicateur (ex. « Informations »). */
  titre: string
  /** Description affichée sous l'indicateur pour l'étape courante. */
  description?: string
}

interface FormEtapesProps {
  /** Étapes dans l'ordre (au moins une). */
  etapes: EtapeFormulaire[]
  /**
   * Valide l'étape d'index donné : retourne un message d'erreur si elle ne
   * peut pas être quittée, `null` sinon. Appelée sur « Suivant » et « Terminer ».
   */
  validerEtape: (index: number) => string | null
  /** Action finale, appelée depuis la dernière étape après validation. */
  onTerminer: () => void
  onAnnuler: () => void
  /** Enregistrement en cours : désactive les boutons. */
  enregistrement?: boolean
  /** Erreur extérieure (ex. échec d'enregistrement) affichée sous l'indicateur. */
  erreur?: string | null
  /** Libellé du bouton final. */
  libelleTerminer?: string
  /** Contenu de l'étape courante (fonction du numéro d'étape, base 0). */
  children: (etape: number) => ReactNode
}

/**
 * Formulaire progressif réutilisable : indicateur d'étapes cliquables
 * (retour arrière uniquement), navigation Précédent / Suivant et bouton
 * final. Pour réinitialiser, remontez avec une `key` différente.
 */
export function FormEtapes({
  etapes,
  validerEtape,
  onTerminer,
  onAnnuler,
  enregistrement = false,
  erreur = null,
  libelleTerminer = 'Enregistrer',
  children,
}: FormEtapesProps) {
  const [etape, setEtape] = useState(0)
  const [erreurLocale, setErreurLocale] = useState<string | null>(null)
  const derniere = etape === etapes.length - 1

  const allerSuivant = () => {
    const probleme = validerEtape(etape)
    if (probleme) {
      setErreurLocale(probleme)
      return
    }
    setErreurLocale(null)
    setEtape(etape + 1)
  }

  const allerPrecedent = () => {
    setErreurLocale(null)
    setEtape(Math.max(0, etape - 1))
  }

  const gererTerminer = () => {
    const probleme = validerEtape(etape)
    if (probleme) {
      setErreurLocale(probleme)
      return
    }
    setErreurLocale(null)
    onTerminer()
  }

  const message = erreurLocale ?? erreur

  return (
    <div className="form-etapes">
      <ol className="form-etapes__steps" aria-label="Progression du formulaire">
        {etapes.map((e, i) => {
          const terminee = i < etape
          const active = i === etape
          return (
            <li
              key={e.titre}
              className={`form-etapes__step${active ? ' form-etapes__step--actif' : ''}${terminee ? ' form-etapes__step--terminee' : ''}`}
              aria-current={active ? 'step' : undefined}
            >
              {terminee ? (
                <button
                  type="button"
                  className="form-etapes__pastille"
                  onClick={() => {
                    setErreurLocale(null)
                    setEtape(i)
                  }}
                  aria-label={`Retour à l'étape ${i + 1} : ${e.titre}`}
                >
                  ✓
                </button>
              ) : (
                <span className="form-etapes__pastille" aria-hidden="true">
                  {i + 1}
                </span>
              )}
              <span className="form-etapes__titre">{e.titre}</span>
            </li>
          )
        })}
      </ol>

      {etapes[etape]?.description && <p className="section-note">{etapes[etape].description}</p>}
      {message && (
        <div className="alert alert-danger" role="alert">
          {message}
        </div>
      )}

      <div className="form-etapes__contenu">{children(etape)}</div>

      <footer className="modal-footer">
        <button type="button" className="btn-ghost" onClick={onAnnuler} disabled={enregistrement}>
          Annuler
        </button>
        <span style={{ display: 'inline-flex', gap: 8 }}>
          {etape > 0 && (
            <button type="button" className="btn-ghost" onClick={allerPrecedent} disabled={enregistrement}>
              ‹ Précédent
            </button>
          )}
          {!derniere ? (
            <button type="button" className="btn-primary" onClick={allerSuivant} disabled={enregistrement}>
              Suivant ›
            </button>
          ) : (
            <button type="button" className="btn-primary" onClick={gererTerminer} disabled={enregistrement}>
              {enregistrement ? 'Enregistrement…' : libelleTerminer}
            </button>
          )}
        </span>
      </footer>
    </div>
  )
}

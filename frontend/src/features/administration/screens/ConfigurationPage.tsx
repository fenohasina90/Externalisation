import { useMemo, useState } from 'react'
import { Alert, Spinner } from '@/components/ui'
import { useApi } from '@/hooks/useApi'
import { configurationApi } from '../services/administration.service'
import type { ApplicationConfiguration } from '../types'

interface SectionDef {
  /** Identifiant d'ancre pour la navigation latérale. */
  id: string
  icone: string
  titre: string
  sousTitre: string
  cles: string[]
}

const SECTIONS: SectionDef[] = [
  {
    id: 'application',
    icone: '🌐',
    titre: 'Application',
    sousTitre: 'Identité de l’application affichée sur les documents et dans l’onglet du navigateur.',
    cles: ['NOM_APPLICATION', 'DEVISE', 'URL_APPLICATION'],
  },
  {
    id: 'alertes',
    icone: '🔔',
    titre: 'Alertes paiements',
    sousTitre:
      'Échéances proches, retards de paiement et rappel de création de la facture mensuelle. Toutes les valeurs sont exprimées en jours.',
    cles: [
      'JOURS_AVANCE_ALERTE_ECHEANCE',
      'JOURS_RETARD_PRESTATION_DEBOURS',
      'JOURS_RETARD_ABONNEMENT',
      'JOUR_CREATION_FACTURE_MENSUELLE',
      'JOURS_RAPPEL_AVANT_CREATION_MENSUELLE',
      'TAUX_TVA',
    ],
  },
  {
    id: 'sauvegarde',
    icone: '💾',
    titre: 'Sauvegarde automatique',
    sousTitre: 'Heure quotidienne d’exécution de la sauvegarde automatique de la base (heure du serveur).',
    cles: ['HEURE_SAUVEGARDE_AUTOMATIQUE'],
  },
  {
    id: 'technique',
    icone: '⚙️',
    titre: 'Fichiers, session et affichage',
    sousTitre: 'Limites techniques de l’application et pagination des listes.',
    cles: ['TAILLE_MAX_FICHIER', 'DUREE_SESSION_MINUTES', 'PAGINATION_TAILLE_DEFAUT'],
  },
  {
    id: 'securite',
    icone: '🔒',
    titre: 'Sécurité',
    sousTitre: 'Durée de validité des demandes et des mots de passe temporaires (en minutes).',
    cles: ['DUREE_VALIDITE_DEMANDE_RESET', 'DUREE_VALIDITE_MDP_TEMPORAIRE'],
  },
]

/** Libellés lisibles par clé (sans les texte bruts) et unités de mesure. */
const LIBELLES: Record<string, { label: string; unite?: string; aide?: string }> = {
  NOM_APPLICATION: { label: 'Nom de l’application', aide: 'Titre affiché sur les documents PDF (factures, reçu…).' },
  DEVISE: { label: 'Devise', aide: 'Code ISO affiché sur les factures et dans l’interface (ex. MGA, EUR, USD).' },
  URL_APPLICATION: { label: 'URL du frontend', aide: 'Base utilisée pour construire les liens envoyés par courriel.' },

  JOURS_AVANCE_ALERTE_ECHEANCE: {
    label: 'Échéance proche',
    unite: 'jours',
    aide: 'Alerte préalable avant une échéance de plan de paiement non soldée.',
  },
  JOURS_RETARD_PRESTATION_DEBOURS: {
    label: 'Retard prestation / débours',
    unite: 'jours',
    aide: 'Facture validée (prestation, débours) non soldée après X jours → notification « Retard de paiement ».',
  },
  JOURS_RETARD_ABONNEMENT: {
    label: 'Retard abonnement mensuel',
    unite: 'jours',
    aide: 'Facture mensuelle non soldée après X jours depuis la date d’émission → notification « Retard de paiement ».',
  },
  JOUR_CREATION_FACTURE_MENSUELLE: {
    label: 'Jour de création mensuelle',
    unite: 'jours',
    aide: 'Rappel uniquement (ex. 15) — la facture du mois peut être créée à n’importe quelle date.',
  },
  JOURS_RAPPEL_AVANT_CREATION_MENSUELLE: {
    label: 'Rappel avant création mensuelle',
    unite: 'jours',
    aide: 'Le jour J − X, un rappel est envoyé (ex. si X = 5 et le jour cible est le 15, rappel le 10). Le jour J lui-même, un rappel est toujours émis.',
  },
  TAUX_TVA: { label: 'Taux TVA', unite: '%', aide: 'Taux appliqué par défaut sur les documents si aucun taux n’est indiqué.' },

  HEURE_SAUVEGARDE_AUTOMATIQUE: { label: 'Heure de la sauvegarde', aide: 'Déclenchement de la sauvegarde automatique quotidienne.' },

  TAILLE_MAX_FICHIER: { label: 'Taille maximale des fichiers', unite: 'octets', aide: 'Taille maximale d’un fichier pièce jointe (≈ 10 Mo par défaut).' },
  DUREE_SESSION_MINUTES: { label: 'Durée de session', unite: 'minutes', aide: 'Durée avant expiration automatique d’une session inactive.' },
  PAGINATION_TAILLE_DEFAUT: { label: 'Taille de page par défaut', unite: 'éléments', aide: 'Nombre de résultats affichés par page dans les listes.' },

  DUREE_VALIDITE_DEMANDE_RESET: {
    label: 'Validité de la demande de réinitialisation',
    unite: 'minutes',
    aide: 'Durée de validité d’un lien de réinitialisation du mot de passe.',
  },
  DUREE_VALIDITE_MDP_TEMPORAIRE: {
    label: 'Durée de validité du mot de passe temporaire',
    unite: 'minutes',
    aide: 'Durée avant que le mot de passe temporaire attribué ne soit plus valide.',
  },
}

/** Section de type decimal → saisie décimale ; TIME → saisie heure; sinon entier. */
function inputType(champ: ApplicationConfiguration): 'text' | 'number' | 'time' {
  if (champ.typeValeur === 'TIME') return 'time'
  if (champ.typeValeur === 'STRING') return 'text'
  return 'number'
}

function champNumberStep(champ: ApplicationConfiguration): string | undefined {
  if (champ.typeValeur === 'DECIMAL' || champ.typeValeur === 'NUMERIC') return 'any'
  return '1'
}

function champMin(champ: ApplicationConfiguration): number | undefined {
  return champ.typeValeur === 'DECIMAL' || champ.typeValeur === 'NUMERIC' ? undefined : 0
}

/** Configuration branchée sur l'API : GET /configuration, PUT /configuration/{cle}. */
export function ConfigurationPage() {
  const { data, loading, error, reload } = useApi<ApplicationConfiguration[]>(
    () => configurationApi.list(),
    [],
  )
  const [valeurs, setValeurs] = useState<Record<string, string>>({})
  const [base, setBase] = useState<Record<string, string> | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)
  const [enregistrement, setEnregistrement] = useState(false)

  // Initialise le formulaire à la réception des données (une seule fois par chargement).
  const empreinte = (data ?? []).map((c) => `${c.cle}=${c.valeur}`).join('|')
  if (data && base === null) {
    const map: Record<string, string> = {}
    for (const c of data) map[c.cle] = c.valeur
    setBase(map)
    setValeurs(map)
  }

  const sections = useMemo(() => {
    const parCle = new Map((data ?? []).map((c) => [c.cle, c]))
    const rangees = new Set<string>()
    const bloc = SECTIONS.map((s) => ({
      ...s,
      champs: s.cles.flatMap((cle) => {
        const c = parCle.get(cle)
        if (!c) return []
        rangees.add(cle)
        return [c]
      }),
    })).filter((s) => s.champs.length > 0)
    const autres = (data ?? []).filter((c) => !rangees.has(c.cle))
    if (autres.length > 0) {
      bloc.push({
        id: 'autres',
        icone: '🗂️',
        titre: 'Autres paramètres',
        sousTitre: 'Paramètres supplémentaires présents en base (non classés).',
        cles: [],
        champs: autres,
      })
    }
    return bloc
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [empreinte])

  const diffs = base != null
    ? Object.keys(valeurs).filter((cle) => valeurs[cle] !== base[cle])
    : []
  const modifie = diffs.length > 0

  const handleAnnuler = () => {
    if (base) setValeurs(base)
    setMessage(null)
    setErreur(null)
  }

  const handleEnregistrer = async () => {
    if (!base) return
    setEnregistrement(true)
    setMessage(null)
    setErreur(null)
    try {
      for (const cle of diffs) {
        await configurationApi.update(cle, valeurs[cle])
      }
      setMessage(
        diffs.length === 0
          ? 'Aucune modification à enregistrer.'
          : `Configuration enregistrée (${diffs.length} paramètre${diffs.length > 1 ? 's' : ''}).`,
      )
      reload()
      setBase(null)
    } catch (err) {
      setErreur(err instanceof Error ? err.message : 'Échec de l’enregistrement')
    } finally {
      setEnregistrement(false)
    }
  }

  if (loading) return <Spinner label="Chargement de la configuration…" />
  if (error || !data) {
    return (
      <div className="app-page">
        <Alert tone="danger">{error ?? 'Impossible de charger la configuration.'}</Alert>
      </div>
    )
  }

  return (
    <div className="app-page administration-page">
      <header className="page-header page-header-sticky">
        <div>
          <h1>Configuration</h1>
          <p>
            Les paramètres directement utilisés par l&apos;application. Chaque paramètre est
            enregistré individuellement&nbsp;; seules les valeurs modifiées sont envoyées au serveur.
          </p>
        </div>
        <div className="actions">
          <button type="button" className="btn-ghost" onClick={handleAnnuler} disabled={!modifie}>
            Annuler
          </button>
          <button
            type="button"
            className="btn-primary"
            onClick={() => void handleEnregistrer()}
            disabled={enregistrement || !modifie}
          >
            {enregistrement
              ? 'Enregistrement…'
              : modifie
                ? `Enregistrer (${diffs.length})`
                : 'Enregistrer'}
          </button>
        </div>
      </header>

      {message && <div className="alert alert-success" role="status">{message}</div>}
      {erreur && <div className="alert alert-danger" role="alert">{erreur}</div>}

      <div className="config-layout">
        <div className="config-content">
          {sections.map((section) => (
            <section id={section.id} key={section.id} className="card config-card">
              <header className="config-card__head">
                <span className="config-card__icone" aria-hidden="true">{section.icone}</span>
                <div className="config-card__titre">
                  <h2>{section.titre}</h2>
                  <p className="section-note">{section.sousTitre}</p>
                </div>
                <span className="config-card__compte">
                  {section.champs.length}<span className="config-card__compte-suffixe">&nbsp;param.</span>
                </span>
              </header>

              <div className="form-grid form-grid--2 config-grille">
                {section.champs.map((champ) => {
                  const meta = LIBELLES[champ.cle]
                  const label = meta?.label ?? champ.description ?? champ.cle
                  const unite = meta?.unite
                  const typeValeur = champ.typeValeur
                  const modifiable = champ.modifiable !== false
                  return (
                    <label key={champ.cle} className="field config-row">
                      <div className="config-row__tete">
                        <span className="field-label">{label}</span>
                        {!modifiable && <span className="pill wt">Verrouillé</span>}
                      </div>
                      <div className="config-row__saisie">
                        <input
                          type={inputType(champ)}
                          step={champNumberStep(champ)}
                          min={champMin(champ)}
                          inputMode={typeValeur !== 'STRING' ? 'numeric' : undefined}
                          value={valeurs[champ.cle] ?? ''}
                          disabled={!modifiable}
                          onChange={(e) => {
                            setValeurs((v) => ({ ...v, [champ.cle]: e.target.value }))
                            setMessage(null)
                            setErreur(null)
                          }}
                          aria-label={label}
                        />
                        {unite && <span className="config-row__unite">{unite}</span>}
                      </div>
                      {meta?.aide && <span className="form-help">{meta.aide}</span>}
                      <span className="form-help form-help--code">
                        <code className="code-inline">{champ.cle}</code>
                        {!modifiable && ' — paramètre système non modifiable depuis l’interface'}
                      </span>
                    </label>
                  )
                })}
              </div>

              {section.id === 'alertes' && (
                <div className="config-apercu">
                  <span className="config-apercu__icone" aria-hidden="true">💡</span>
                  <p>
                    <strong>Aperçu des notifications associées.</strong>
                    {' '}Une facture non soldée déclenche un <em>rappel de retard</em> au-delà de
                    <strong> {valeurs.JOURS_RETARD_PRESTATION_DEBOURS ?? 5} jours</strong> pour
                    les prestations/débours (validation de la facture) et de
                    <strong> {valeurs.JOURS_RETARD_ABONNEMENT ?? 7} jours</strong> pour l’abonnement
                    mensuel (date d’émission de la facture).
                    {' '}La facture mensuelle propose également un rappel
                    <strong> {valeurs.JOURS_RAPPEL_AVANT_CREATION_MENSUELLE ?? 5} jours avant</strong>
                    {' '}et le jour J (<strong>le {valeurs.JOUR_CREATION_FACTURE_MENSUELLE ?? 15} du mois</strong>).
                    {' '}Ces valeurs ne sont que des rappels ; la création de la facture reste possible à n’importe quelle date.
                  </p>
                </div>
              )}
            </section>
          ))}
        </div>
      </div>
    </div>
  )
}

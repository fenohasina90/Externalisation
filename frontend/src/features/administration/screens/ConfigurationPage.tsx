import { useMemo, useState } from 'react'
import { Alert, Spinner } from '@/components/ui'
import { useApi } from '@/hooks/useApi'
import { configurationApi } from '../services/administration.service'
import type { ApplicationConfiguration } from '../types'

const SECTIONS: { titre: string; sousTitre: string; cles: string[] }[] = [
  {
    titre: 'Application',
    sousTitre: 'Identité et devise.',
    cles: ['NOM_APPLICATION', 'DEVISE', 'URL_APPLICATION'],
  },
  {
    titre: 'Paiements et alertes',
    sousTitre: 'Échéances et fiscalité.',
    cles: ['JOURS_AVANCE_ALERTE_ECHEANCE', 'TAUX_TVA'],
  },
  {
    titre: 'Sauvegardes',
    sousTitre: 'Heure quotidienne de la sauvegarde automatique (heure du serveur).',
    cles: ['HEURE_SAUVEGARDE_AUTOMATIQUE'],
  },
  {
    titre: 'Fichiers, session et affichage',
    sousTitre: 'Limites techniques.',
    cles: ['TAILLE_MAX_FICHIER', 'DUREE_SESSION_MINUTES', 'PAGINATION_TAILLE_DEFAUT'],
  },
  {
    titre: 'Sécurité',
    sousTitre: 'Validité des demandes et mots de passe temporaires (en minutes).',
    cles: ['DUREE_VALIDITE_DEMANDE_RESET', 'DUREE_VALIDITE_MDP_TEMPORAIRE'],
  },
]

/** Configuration branchée sur l'API : lecture GET /configuration, écriture PUT /configuration/{cle}. */
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
      bloc.push({ titre: 'Autres paramètres', sousTitre: 'Paramètres supplémentaires.', cles: [], champs: autres })
    }
    return bloc
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [empreinte])

  const modifie = base !== null && JSON.stringify(valeurs) !== JSON.stringify(base)

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
      const changees = Object.keys(valeurs).filter((cle) => valeurs[cle] !== base[cle])
      for (const cle of changees) {
        await configurationApi.update(cle, valeurs[cle])
      }
      setMessage(
        changees.length === 0
          ? 'Aucune modification à enregistrer.'
          : `Configuration enregistrée (${changees.length} paramètre${changees.length > 1 ? 's' : ''}).`,
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
    <div className="app-page responsables-page">
      <header className="page-header page-header-sticky">
        <div>
          <h1>Configuration</h1>
          <p>Paramètres généraux de l&apos;application.</p>
        </div>
        <div className="actions">
          <button type="button" className="btn-ghost" onClick={handleAnnuler} disabled={!modifie}>
            Annuler
          </button>
          <button
            type="button"
            className="btn-primary"
            onClick={() => void handleEnregistrer()}
            disabled={enregistrement}
          >
            {enregistrement ? 'Enregistrement…' : 'Enregistrer'}
          </button>
        </div>
      </header>

      {message && (
        <div className="alert alert-success" role="status">
          {message}
        </div>
      )}
      {erreur && (
        <div className="alert alert-danger" role="alert">
          {erreur}
        </div>
      )}

      {sections.map((section) => (
        <section key={section.titre} className="card card-pad">
          <h3 style={{ margin: '0 0 2px' }}>{section.titre}</h3>
          <p className="section-note" style={{ marginBottom: 14 }}>
            {section.sousTitre}
          </p>
          <div className="form-grid">
            {section.champs.map((champ) => (
              <label key={champ.cle} className="field">
                {champ.description ?? champ.cle}
                <input
                  type={
                    champ.typeValeur === 'TIME'
                      ? 'time'
                      : champ.typeValeur === 'STRING'
                        ? 'text'
                        : 'number'
                  }
                  value={valeurs[champ.cle] ?? ''}
                  disabled={!champ.modifiable}
                  onChange={(e) => {
                    setValeurs((v) => ({ ...v, [champ.cle]: e.target.value }))
                    setMessage(null)
                    setErreur(null)
                  }}
                />
                <span className="form-help">
                  Clé : <code className="code-inline">{champ.cle}</code>
                  {!champ.modifiable && ' — non modifiable'}
                </span>
              </label>
            ))}
          </div>
        </section>
      ))}
    </div>
  )
}

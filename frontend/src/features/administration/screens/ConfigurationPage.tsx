import { useMemo, useState } from 'react'
import { CONFIG_SECTIONS } from '../data/mockConfiguration'

/** Configuration — étape 1 : interface seule, valeurs statiques modifiables localement. */
export function ConfigurationPage() {
  const initial = useMemo(() => {
    const map: Record<string, string> = {}
    for (const s of CONFIG_SECTIONS) for (const c of s.champs) map[c.cle] = c.valeur
    return map
  }, [])

  const [valeurs, setValeurs] = useState<Record<string, string>>(initial)
  const [message, setMessage] = useState<string | null>(null)

  const modifie = JSON.stringify(valeurs) !== JSON.stringify(initial)

  const handleAnnuler = () => {
    setValeurs(initial)
    setMessage(null)
  }

  const handleEnregistrer = () => {
    setMessage('Configuration enregistrée (démonstration, sans backend).')
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
          <button type="button" className="btn-primary" onClick={handleEnregistrer}>
            Enregistrer
          </button>
        </div>
      </header>

      {message && (
        <div className="alert alert-success" role="status">
          {message}
        </div>
      )}

      {CONFIG_SECTIONS.map((section) => (
        <section key={section.titre} className="card card-pad">
          <h3 style={{ margin: '0 0 2px' }}>{section.titre}</h3>
          <p className="section-note" style={{ marginBottom: 14 }}>
            {section.sousTitre}
          </p>
          <div className="form-grid">
            {section.champs.map((champ) => (
              <label key={champ.cle} className="field">
                {champ.label}
                {champ.type === 'select' ? (
                  <select
                    value={valeurs[champ.cle]}
                    onChange={(e) => {
                      setValeurs((v) => ({ ...v, [champ.cle]: e.target.value }))
                      setMessage(null)
                    }}
                  >
                    {champ.options?.map((o) => (
                      <option key={o.valeur} value={o.valeur}>
                        {o.label}
                      </option>
                    ))}
                  </select>
                ) : (
                  <input
                    type={champ.type === 'number' ? 'number' : 'text'}
                    value={valeurs[champ.cle]}
                    onChange={(e) => {
                      setValeurs((v) => ({ ...v, [champ.cle]: e.target.value }))
                      setMessage(null)
                    }}
                  />
                )}
                {champ.hint && <span className="form-help">{champ.hint}</span>}
              </label>
            ))}
          </div>
        </section>
      ))}
    </div>
  )
}

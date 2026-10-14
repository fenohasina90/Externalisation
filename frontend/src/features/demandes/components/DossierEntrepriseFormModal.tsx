import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import {
  DOSSIER_VIDE,
  FORMES_JURIDIQUES,
  HEBERGEMENTS,
  REGIMES_ACTIVITE,
  type AssocieDossier,
  type DossierEntreprise,
  type FormeJuridique,
  type RegimeActivite,
  type TypeHebergement,
} from '../data/mockDossierEntreprise'

interface Props {
  open: boolean
  /** Dossier à modifier (pré-rempli) ou vide pour la première saisie. */
  dossier: DossierEntreprise
  onClose: () => void
  onSave: (d: DossierEntreprise) => void
}

/** Formulaire du dossier de création d'entreprise (cf. fiche papier). */
export function DossierEntrepriseFormModal({ open, dossier, onClose, onSave }: Props) {
  const [form, setForm] = useState<DossierEntreprise>(dossier)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (open) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setForm(dossier)
      setError(null)
    }
  }, [open, dossier])

  if (!open) return null

  const set = <K extends keyof DossierEntreprise>(cle: K, valeur: DossierEntreprise[K]) => {
    setForm((f) => ({ ...f, [cle]: valeur }))
  }

  const setAssocie = (index: number, cle: keyof AssocieDossier, valeur: string) => {
    setForm((f) => ({
      ...f,
      associes: f.associes.map((a, i) => (i === index ? { ...a, [cle]: valeur } : a)),
    }))
  }

  const ajouterAssocie = () => {
    setForm((f) => ({
      ...f,
      associes: [...f.associes, { nom: '', partSociale: '', email: '', contact: '' }],
    }))
  }

  const retirerAssocie = (index: number) => {
    setForm((f) => ({ ...f, associes: f.associes.filter((_, i) => i !== index) }))
  }

  const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value, type } = e.target
    if (type === 'checkbox') {
      set(name as keyof DossierEntreprise, (e.target as HTMLInputElement).checked as never)
    } else if (name === 'capitalSocial') {
      set('capitalSocial', value === '' ? null : Number(value))
    } else if (name === 'nombreAssocies') {
      set('nombreAssocies', Number(value))
    } else {
      set(name as keyof DossierEntreprise, value as never)
    }
  }

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    if (!form.denomination.trim()) {
      setError('La dénomination est obligatoire')
      return
    }
    if (form.formeJuridique === 'AUTRE' && !form.formePrecision.trim()) {
      setError('Précisez la forme juridique (Autre)')
      return
    }
    // Société (tout sauf Individuelle) : associés obligatoires.
    if (form.formeJuridique !== 'INDIVIDUELLE') {
      const renseignes = form.associes.filter((a) => a.nom.trim() !== '')
      if (renseignes.length === 0) {
        setError('Une société doit avoir au moins un associé renseigné')
        return
      }
    }
    onSave({ ...form, denomination: form.denomination.trim() })
  }

  const vides = form.associes.length === 0

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="modal-card"
        style={{ maxWidth: 760 }}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-label="Dossier de création d’entreprise"
      >
        <header className="modal-header">
          <div>
            <h3 className="modal-title">Dossier de création d’entreprise</h3>
            <p className="modal-subtitle">
              Date et N° IM repris de la demande d’origine. Associés obligatoires pour une société.
            </p>
          </div>
          <button type="button" className="btn-ghost" onClick={onClose} aria-label="Fermer">
            ✕
          </button>
        </header>

        <form onSubmit={handleSubmit} className="modal-body">
          {error && (
            <div className="alert alert-danger" role="alert">
              {error}
            </div>
          )}

          <h4 className="form-section">Société</h4>
          <div className="form-grid form-grid--2">
            <label className="field">
              <span className="field-label">
                Forme juridique <span className="required">*</span>
              </span>
              <select
                name="formeJuridique"
                value={form.formeJuridique}
                onChange={(e) => set('formeJuridique', e.target.value as FormeJuridique)}
                required
              >
                {FORMES_JURIDIQUES.map((f) => (
                  <option key={f.valeur} value={f.valeur}>
                    {f.label}
                  </option>
                ))}
              </select>
            </label>
            {form.formeJuridique === 'AUTRE' && (
              <label className="field">
                <span className="field-label">
                  Préciser <span className="required">*</span>
                </span>
                <input name="formePrecision" value={form.formePrecision} onChange={handleChange} required />
              </label>
            )}
            <label className="field form-grid__full">
              <span className="field-label">
                Dénomination <span className="required">*</span>
              </span>
              <input name="denomination" value={form.denomination} onChange={handleChange} required />
            </label>
            <label className="field">
              Siège social actuel
              <input name="siegeSocialActuel" value={form.siegeSocialActuel} onChange={handleChange} />
            </label>
            <label className="field">
              Capital social (MGA)
              <input
                type="number"
                min={0}
                name="capitalSocial"
                value={form.capitalSocial ?? ''}
                onChange={handleChange}
              />
            </label>
          </div>

          <h4 className="form-section">
            Associés{' '}
            {form.formeJuridique !== 'INDIVIDUELLE' && <span className="required">*</span>}
          </h4>
          {form.associes.map((a, i) => (
            <div key={i} className="form-grid form-grid--2" style={{ marginBottom: 12 }}>
              <label className="field">
              <span className="field-label">
                Nom des associés{' '}
                {form.formeJuridique !== 'INDIVIDUELLE' && <span className="required">*</span>}
              </span>
                <input value={a.nom} onChange={(e) => setAssocie(i, 'nom', e.target.value)} />
              </label>
              <label className="field">
                Part sociale
                <input
                  value={a.partSociale}
                  onChange={(e) => setAssocie(i, 'partSociale', e.target.value)}
                  placeholder="Ex. 50 %"
                />
              </label>
              <label className="field">
                Adresse mail
                <input
                  type="email"
                  value={a.email}
                  onChange={(e) => setAssocie(i, 'email', e.target.value)}
                />
              </label>
              <label className="field">
                Contact
                <input value={a.contact} onChange={(e) => setAssocie(i, 'contact', e.target.value)} />
              </label>
              <div className="form-grid__full">
                <button
                  type="button"
                  className="btn-link danger"
                  onClick={() => retirerAssocie(i)}
                  disabled={form.associes.length <= 1}
                >
                  Retirer
                </button>
              </div>
            </div>
          ))}
          {vides && <p className="form-help">Aucun associé (entreprise individuelle).</p>}
          <button type="button" className="btn-ghost" onClick={ajouterAssocie}>
            + Ajouter un associé
          </button>

          <h4 className="form-section">Gérance</h4>
          <div className="form-grid form-grid--2">
            <label className="field">
              Nom du gérant
              <input name="nomGerant" value={form.nomGerant} onChange={handleChange} />
            </label>
            <label className="field">
              Fonction
              <input name="fonctionGerant" value={form.fonctionGerant} onChange={handleChange} />
            </label>
            <label className="field form-grid__full" style={{ flexDirection: 'row', alignItems: 'center', gap: 10 }}>
              <input
                type="checkbox"
                name="locationGerance"
                checked={form.locationGerance}
                onChange={handleChange}
                style={{ width: 18, height: 18, accentColor: 'var(--g)' }}
              />
              Location de gérance
            </label>
          </div>

          <h4 className="form-section">Siège et établissement</h4>
          <div className="form-grid form-grid--2">
            <label className="field">
              Secteur d’activité
              <input name="secteurActivite" value={form.secteurActivite} onChange={handleChange} />
            </label>
            <label className="field">
              Autorisation spéciale
              <input name="autorisationSpeciale" value={form.autorisationSpeciale} onChange={handleChange} />
            </label>
            <label className="field form-grid__full">
              Adresse
              <input name="adresse" value={form.adresse} onChange={handleChange} />
            </label>
            <label className="field">
              Ville
              <input name="ville" value={form.ville} onChange={handleChange} />
            </label>
            <label className="field">
              Commune
              <input name="commune" value={form.commune} onChange={handleChange} />
            </label>
            <label className="field">
              District
              <input name="district" value={form.district} onChange={handleChange} />
            </label>
            <label className="field">
              Région
              <input name="region" value={form.region} onChange={handleChange} />
            </label>
            <label className="field">
              Hébergement du siège
              <select
                name="hebergement"
                value={form.hebergement}
                onChange={(e) => set('hebergement', e.target.value as TypeHebergement | '')}
              >
                <option value="">Sélectionner…</option>
                {HEBERGEMENTS.map((h) => (
                  <option key={h.valeur} value={h.valeur}>
                    {h.label}
                  </option>
                ))}
              </select>
            </label>
            {form.hebergement === 'AUTRE' && (
              <label className="field">
                Préciser l’hébergement
                <input name="hebergementPrecision" value={form.hebergementPrecision} onChange={handleChange} />
              </label>
            )}
            <label className="field">
              Contrat de bail
              <select
                name="contratBail"
                value={form.contratBail}
                onChange={(e) => set('contratBail', e.target.value as '' | 'oui' | 'non')}
              >
                <option value="">Non renseigné</option>
                <option value="oui">Oui</option>
                <option value="non">Non</option>
              </select>
            </label>
            <label className="field form-grid__full">
              Détail du bail
              <input name="contratBailDetail" value={form.contratBailDetail} onChange={handleChange} />
            </label>
          </div>

          <h4 className="form-section">Activité et situation</h4>
          <div className="form-grid form-grid--2">
            <label className="field form-grid__full">
              Objet social (principales activités)
              <textarea name="objetSocial" value={form.objetSocial} onChange={handleChange} rows={3} style={{ height: 80 }} />
            </label>
            <label className="field">
              Début d’activité souhaité
              <input type="date" name="dateSouhaitee" value={form.dateSouhaitee} onChange={handleChange} />
            </label>
            <label className="field">
              Régime d’activité
              <select
                name="regimeActivite"
                value={form.regimeActivite}
                onChange={(e) => set('regimeActivite', e.target.value as RegimeActivite | '')}
              >
                <option value="">Sélectionner…</option>
                {REGIMES_ACTIVITE.map((r) => (
                  <option key={r.valeur} value={r.valeur}>
                    {r.label}
                  </option>
                ))}
              </select>
            </label>
            <label className="field form-grid__full">
              Établissement / succursale / filiale
              <textarea name="activitesEtablissement" value={form.activitesEtablissement} onChange={handleChange} rows={2} style={{ height: 64 }} />
            </label>
            <label className="field">
              Assujettissement TVA
              <input name="tva" value={form.tva} onChange={handleChange} />
            </label>
            <label className="field">
              Autre société
              <input name="autreSociete" value={form.autreSociete} onChange={handleChange} />
            </label>
            <label className="field">
              Situation matrimoniale
              <input name="situationMatrimoniale" value={form.situationMatrimoniale} onChange={handleChange} />
            </label>
            <label className="field">
              Régime d’imposition
              <input name="regimeImposition" value={form.regimeImposition} onChange={handleChange} />
            </label>
            <label className="field form-grid__full">
              Observations
              <textarea name="observations" value={form.observations} onChange={handleChange} rows={2} style={{ height: 64 }} />
            </label>
          </div>

          <footer className="modal-footer">
            <button type="button" className="btn-ghost" onClick={onClose}>
              Annuler
            </button>
            <button
              type="button"
              className="btn-ghost"
              onClick={() => {
                setForm(DOSSIER_VIDE)
                setError(null)
              }}
            >
              Réinitialiser
            </button>
            <button type="submit" className="btn-primary">
              Enregistrer
            </button>
          </footer>
        </form>
      </div>
    </div>
  )
}

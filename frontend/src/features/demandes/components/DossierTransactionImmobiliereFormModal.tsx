import { useEffect, useState, type ChangeEvent } from 'react'
import { FormEtapes } from '@/components/shared/FormEtapes'
import {
  OBJETS,
  SITUATIONS_TERRAIN,
  TYPES_TERRAIN,
  type DossierTransactionForm,
  type ObjetTransaction,
  type OuiNon,
  type SituationTerrain,
  type TypeTerrain,
} from '../types/dossierTransaction'

interface Props {
  open: boolean
  dossier: DossierTransactionForm
  enregistrement?: boolean
  onClose: () => void
  onSave: (dossier: DossierTransactionForm) => void
}

const NOMBRES = new Set([
  'achatMesureM2',
  'achatMontant',
  'venteMesureM2',
  'venteMontant',
  'nombreCommerciauxTerrain',
  'commissionCn',
])

const ETAPES = [
  { titre: 'Transaction', description: 'Propriétaire, action et objet de la transaction.' },
  { titre: 'Achat / Vente', description: 'Caractéristiques et montants des biens concernés.' },
  { titre: 'Régularisation', description: 'Terrain et documents disponibles.' },
  { titre: 'Suivi', description: 'Commerciaux, commission et prestation.' },
]

function Case({
  name,
  label,
  checked,
  onChange,
}: {
  name: string
  label: string
  checked: boolean
  onChange: (e: ChangeEvent<HTMLInputElement>) => void
}) {
  return (
    <label className="field" style={{ flexDirection: 'row', alignItems: 'center', gap: 10 }}>
      <input
        type="checkbox"
        name={name}
        checked={checked}
        onChange={onChange}
        style={{ width: 18, height: 18, accentColor: 'var(--g)' }}
      />
      {label}
    </label>
  )
}

/** Formulaire de la fiche papier « Transaction immobilière ». */
export function DossierTransactionImmobiliereFormModal({
  open,
  dossier,
  enregistrement = false,
  onClose,
  onSave,
}: Props) {
  const [form, setForm] = useState<DossierTransactionForm>(dossier)

  useEffect(() => {
    if (open) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setForm(dossier)
    }
  }, [open, dossier])

  if (!open) return null

  const set = <K extends keyof DossierTransactionForm>(
    cle: K,
    valeur: DossierTransactionForm[K],
  ) => setForm((f) => ({ ...f, [cle]: valeur }))

  const handleChange = (
    e: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>,
  ) => {
    const { name, value, type } = e.target
    if (type === 'checkbox') {
      set(name as keyof DossierTransactionForm, (e.target as HTMLInputElement).checked as never)
    } else if (NOMBRES.has(name)) {
      set(
        name as keyof DossierTransactionForm,
        (value === '' ? null : Number(value)) as never,
      )
    } else {
      set(name as keyof DossierTransactionForm, value as never)
    }
  }

  const validerEtape = (index: number): string | null => {
    if (index === 0) {
      const action =
        form.actionAchat ||
        form.actionVente ||
        form.actionLocation ||
        form.actionRegularisationPapiers ||
        form.actionGestionImmobiliere ||
        form.actionAutre
      if (!action) return 'Sélectionnez au moins une action.'
      if (form.actionAutre && !form.actionAutrePrecision.trim()) {
        return 'Précisez l’action « Autre ».'
      }
      if (form.objet === 'AUTRE' && !form.objetAutrePrecision.trim()) {
        return 'Précisez l’objet « Autre ».'
      }
    }
    if (index === 1) {
      const nombres = [
        form.achatMesureM2,
        form.achatMontant,
        form.venteMesureM2,
        form.venteMontant,
      ]
      if (nombres.some((n) => n !== null && n < 0)) {
        return 'Les mesures et montants doivent être positifs.'
      }
    }
    if (index === 3) {
      if (form.nombreCommerciauxTerrain !== null && form.nombreCommerciauxTerrain < 0) {
        return 'Le nombre de commerciaux doit être positif.'
      }
      if (form.commissionCn !== null && form.commissionCn < 0) {
        return 'La commission CN doit être positive.'
      }
    }
    return null
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="modal-card"
        style={{ maxWidth: 820 }}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-label="Dossier de transaction immobilière"
      >
        <header className="modal-header">
          <div>
            <h3 className="modal-title">Dossier de transaction immobilière</h3>
            <p className="modal-subtitle">
              Date, commercial et coordonnées du client sont repris de la demande.
            </p>
          </div>
          <button type="button" className="btn-ghost" onClick={onClose} aria-label="Fermer">
            ✕
          </button>
        </header>

        <div className="modal-body">
          <FormEtapes
            etapes={ETAPES}
            validerEtape={validerEtape}
            onTerminer={() => onSave(form)}
            onAnnuler={onClose}
            enregistrement={enregistrement}
            libelleTerminer="Enregistrer"
          >
            {(etape) => (
              <>
                {etape === 0 && (
                  <>
                    <h4 className="form-section">Informations de la transaction</h4>
                    <div className="form-grid form-grid--2">
                      <label className="field form-grid__full">
                        Nom du propriétaire précédent
                        <input
                          name="nomProprietairePrecedent"
                          value={form.nomProprietairePrecedent}
                          onChange={handleChange}
                        />
                      </label>

                      <div className="form-grid__full">
                        <span className="field-label">
                          Actions <span className="required">*</span>
                        </span>
                        <div className="form-grid form-grid--2">
                          <Case name="actionAchat" label="Achat" checked={form.actionAchat} onChange={handleChange} />
                          <Case name="actionVente" label="Vente" checked={form.actionVente} onChange={handleChange} />
                          <Case name="actionLocation" label="Location" checked={form.actionLocation} onChange={handleChange} />
                          <Case
                            name="actionRegularisationPapiers"
                            label="Régularisation des papiers"
                            checked={form.actionRegularisationPapiers}
                            onChange={handleChange}
                          />
                          <Case
                            name="actionGestionImmobiliere"
                            label="Gestion immobilière"
                            checked={form.actionGestionImmobiliere}
                            onChange={handleChange}
                          />
                          <Case name="actionAutre" label="Autre" checked={form.actionAutre} onChange={handleChange} />
                        </div>
                      </div>
                      {form.actionAutre && (
                        <label className="field form-grid__full">
                          Précisez l’action <span className="required">*</span>
                          <input
                            name="actionAutrePrecision"
                            value={form.actionAutrePrecision}
                            onChange={handleChange}
                            required
                          />
                        </label>
                      )}

                      <label className="field">
                        Objet
                        <select
                          name="objet"
                          value={form.objet}
                          onChange={(e) => set('objet', e.target.value as ObjetTransaction | '')}
                        >
                          <option value="">Sélectionner…</option>
                          {OBJETS.map((o) => (
                            <option key={o.valeur} value={o.valeur}>
                              {o.label}
                            </option>
                          ))}
                        </select>
                      </label>
                      {form.objet === 'AUTRE' && (
                        <label className="field">
                          Précisez l’objet <span className="required">*</span>
                          <input
                            name="objetAutrePrecision"
                            value={form.objetAutrePrecision}
                            onChange={handleChange}
                            required
                          />
                        </label>
                      )}
                    </div>
                  </>
                )}

                {etape === 1 && (
                  <>
                    <h4 className="form-section">Achat</h4>
                    {!form.actionAchat && (
                      <p className="section-note">Action « Achat » non sélectionnée.</p>
                    )}
                    <div className="form-grid form-grid--2">
                      <label className="field">
                        Mesure (m²)
                        <input
                          type="number"
                          min={0}
                          step="0.01"
                          name="achatMesureM2"
                          value={form.achatMesureM2 ?? ''}
                          onChange={handleChange}
                        />
                      </label>
                      <label className="field">
                        Position / plan
                        <input name="achatPosition" value={form.achatPosition} onChange={handleChange} />
                      </label>
                      <label className="field form-grid__full">
                        Axe ou emplacement
                        <input
                          name="achatAxeEmplacement"
                          value={form.achatAxeEmplacement}
                          onChange={handleChange}
                        />
                      </label>
                      <label className="field">
                        Montant (MGA)
                        <input
                          type="number"
                          min={0}
                          step="0.01"
                          name="achatMontant"
                          value={form.achatMontant ?? ''}
                          onChange={handleChange}
                        />
                      </label>
                      <label className="field">
                        Si maison : permis de construire
                        <select
                          name="achatPermisConstruire"
                          value={form.achatPermisConstruire}
                          onChange={(e) => set('achatPermisConstruire', e.target.value as OuiNon)}
                        >
                          <option value="">Non renseigné</option>
                          <option value="oui">Oui</option>
                          <option value="non">Non</option>
                        </select>
                      </label>
                    </div>

                    <h4 className="form-section" style={{ marginTop: 20 }}>Vente</h4>
                    {!form.actionVente && (
                      <p className="section-note">Action « Vente » non sélectionnée.</p>
                    )}
                    <div className="form-grid form-grid--2">
                      <label className="field">
                        Mesure (m²)
                        <input
                          type="number"
                          min={0}
                          step="0.01"
                          name="venteMesureM2"
                          value={form.venteMesureM2 ?? ''}
                          onChange={handleChange}
                        />
                      </label>
                      <label className="field">
                        Position / plan
                        <input name="ventePosition" value={form.ventePosition} onChange={handleChange} />
                      </label>
                      <label className="field form-grid__full">
                        Axe ou emplacement
                        <input
                          name="venteAxeEmplacement"
                          value={form.venteAxeEmplacement}
                          onChange={handleChange}
                        />
                      </label>
                      <label className="field">
                        Prise de photo
                        <select
                          name="ventePrisePhoto"
                          value={form.ventePrisePhoto}
                          onChange={(e) => set('ventePrisePhoto', e.target.value as OuiNon)}
                        >
                          <option value="">Non renseigné</option>
                          <option value="oui">Oui</option>
                          <option value="non">Non</option>
                        </select>
                      </label>
                      <label className="field">
                        Visite
                        <select
                          name="venteVisite"
                          value={form.venteVisite}
                          onChange={(e) => set('venteVisite', e.target.value as OuiNon)}
                        >
                          <option value="">Non renseigné</option>
                          <option value="oui">Oui</option>
                          <option value="non">Non</option>
                        </select>
                      </label>
                      <label className="field">
                        Montant (MGA)
                        <input
                          type="number"
                          min={0}
                          step="0.01"
                          name="venteMontant"
                          value={form.venteMontant ?? ''}
                          onChange={handleChange}
                        />
                      </label>
                      <label className="field">
                        Si maison : permis de construire
                        <select
                          name="ventePermisConstruire"
                          value={form.ventePermisConstruire}
                          onChange={(e) => set('ventePermisConstruire', e.target.value as OuiNon)}
                        >
                          <option value="">Non renseigné</option>
                          <option value="oui">Oui</option>
                          <option value="non">Non</option>
                        </select>
                      </label>
                    </div>
                  </>
                )}

                {etape === 2 && (
                  <>
                    <h4 className="form-section">Régularisation de papiers</h4>
                    <div className="form-grid form-grid--2">
                      <label className="field">
                        Type de terrain
                        <select
                          name="regularisationTypeTerrain"
                          value={form.regularisationTypeTerrain}
                          onChange={(e) =>
                            set('regularisationTypeTerrain', e.target.value as TypeTerrain | '')
                          }
                        >
                          <option value="">Non renseigné</option>
                          {TYPES_TERRAIN.map((t) => (
                            <option key={t.valeur} value={t.valeur}>
                              {t.label}
                            </option>
                          ))}
                        </select>
                      </label>
                      <label className="field">
                        Situation du terrain
                        <select
                          name="regularisationSituationTerrain"
                          value={form.regularisationSituationTerrain}
                          onChange={(e) =>
                            set(
                              'regularisationSituationTerrain',
                              e.target.value as SituationTerrain | '',
                            )
                          }
                        >
                          <option value="">Non renseigné</option>
                          {SITUATIONS_TERRAIN.map((s) => (
                            <option key={s.valeur} value={s.valeur}>
                              {s.label}
                            </option>
                          ))}
                        </select>
                      </label>
                    </div>

                    <h4 className="form-section" style={{ marginTop: 20 }}>Documents existants</h4>
                    <div className="form-grid form-grid--2">
                      <Case
                        name="documentPvReconnaissance"
                        label="PV de reconnaissance"
                        checked={form.documentPvReconnaissance}
                        onChange={handleChange}
                      />
                      <Case
                        name="documentNumeroDemandeAcquisition"
                        label="N° de demande d’acquisition"
                        checked={form.documentNumeroDemandeAcquisition}
                        onChange={handleChange}
                      />
                      <Case
                        name="documentActeVentePriveeCommunale"
                        label="Acte de vente privée ou communale"
                        checked={form.documentActeVentePriveeCommunale}
                        onChange={handleChange}
                      />
                      <Case
                        name="documentSituationJuridique"
                        label="Situation juridique"
                        checked={form.documentSituationJuridique}
                        onChange={handleChange}
                      />
                      <Case
                        name="documentActeNotoriete"
                        label="Acte de notoriété"
                        checked={form.documentActeNotoriete}
                        onChange={handleChange}
                      />
                    </div>

                    <label className="field form-grid__full" style={{ marginTop: 16 }}>
                      Observations
                      <textarea
                        name="observations"
                        value={form.observations}
                        onChange={handleChange}
                        rows={4}
                      />
                    </label>
                  </>
                )}

                {etape === 3 && (
                  <>
                    <h4 className="form-section">Suivi de la prestation</h4>
                    <div className="form-grid form-grid--2">
                      <label className="field">
                        Nombre de commerciaux sur terrain voulus
                        <input
                          type="number"
                          min={0}
                          step={1}
                          name="nombreCommerciauxTerrain"
                          value={form.nombreCommerciauxTerrain ?? ''}
                          onChange={handleChange}
                        />
                      </label>
                      <label className="field">
                        Commission du CN (MGA)
                        <input
                          type="number"
                          min={0}
                          step="0.01"
                          name="commissionCn"
                          value={form.commissionCn ?? ''}
                          onChange={handleChange}
                        />
                      </label>
                      <div className="form-grid__full">
                        <Case
                          name="prestationAbonnementMensuel"
                          label="Prestation abonnement mensuel"
                          checked={form.prestationAbonnementMensuel}
                          onChange={handleChange}
                        />
                      </div>
                    </div>
                    <p className="form-help">
                      Les signatures papier restent portées par le document original.
                    </p>
                  </>
                )}
              </>
            )}
          </FormEtapes>
        </div>
      </div>
    </div>
  )
}

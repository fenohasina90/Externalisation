import {
  OBJETS,
  SITUATIONS_TERRAIN,
  TYPES_TERRAIN,
  type DossierTransactionImmobiliere,
} from '../types/dossierTransaction'

function label<T extends string>(
  liste: { valeur: T; label: string }[],
  valeur: T | null,
): string {
  if (!valeur) return '—'
  return liste.find((l) => l.valeur === valeur)?.label ?? valeur
}

function valeur(texte: string | null): string {
  return texte && texte.trim() !== '' ? texte : '—'
}

function ouiNon(vrai: boolean | null): string {
  if (vrai === null || vrai === undefined) return '—'
  return vrai ? 'Oui' : 'Non'
}

function montant(n: number | null): string {
  return n === null || n === undefined ? '—' : `${n.toLocaleString('fr-FR')} MGA`
}

function mesure(n: number | null): string {
  return n === null || n === undefined ? '—' : `${n.toLocaleString('fr-FR')} m²`
}

function formatDate(iso: string | null): string {
  if (!iso) return '—'
  const [a, m, j] = iso.split('-')
  return `${j}/${m}/${a}`
}

const ACTIONS: { cle: keyof DossierTransactionImmobiliere; label: string }[] = [
  { cle: 'actionAchat', label: 'Achat' },
  { cle: 'actionVente', label: 'Vente' },
  { cle: 'actionLocation', label: 'Location' },
  { cle: 'actionRegularisationPapiers', label: 'Régularisation des papiers' },
  { cle: 'actionGestionImmobiliere', label: 'Gestion immobilière' },
  { cle: 'actionAutre', label: 'Autre' },
]

const DOCUMENTS: { cle: keyof DossierTransactionImmobiliere; label: string }[] = [
  { cle: 'documentPvReconnaissance', label: 'PV de reconnaissance' },
  { cle: 'documentNumeroDemandeAcquisition', label: "N° de demande d'acquisition" },
  { cle: 'documentActeVentePriveeCommunale', label: 'Acte de vente privée ou communale' },
  { cle: 'documentSituationJuridique', label: 'Situation juridique' },
  { cle: 'documentActeNotoriete', label: 'Acte de notoriété' },
]

export function DossierTransactionImmobiliereDetail({
  dossier,
}: {
  dossier: DossierTransactionImmobiliere
}) {
  const actions = ACTIONS.filter((a) => Boolean(dossier[a.cle]))

  return (
    <>
      <div className="detail-cards">
        <section className="card card-pad">
          <h3>Demande et client</h3>
          <div className="info-grid">
            <div className="info-item">
              <span>Date</span>
              <b>{formatDate(dossier.dateDemande)}</b>
            </div>
            <div className="info-item">
              <span>Commercial</span>
              <b>{valeur(dossier.nomCommercial)}</b>
            </div>
            <div className="info-item">
              <span>N° matricule</span>
              <b>{valeur(dossier.numeroIm)}</b>
            </div>
            <div className="info-item">
              <span>Client</span>
              <b>{valeur(dossier.nomSociete)}</b>
            </div>
            <div className="info-item">
              <span>Contact</span>
              <b>{valeur(dossier.contactClient)}</b>
            </div>
            <div className="info-item">
              <span>E-mail</span>
              <b>{valeur(dossier.emailClient)}</b>
            </div>
            <div className="info-item">
              <span>Facebook</span>
              <b>{valeur(dossier.fbClient)}</b>
            </div>
            <div className="info-item">
              <span>Adresse</span>
              <b>{valeur(dossier.adresseClient)}</b>
            </div>
          </div>
        </section>

        <section className="card card-pad">
          <h3>Transaction</h3>
          <div className="info-grid">
            <div className="info-item info-item--full">
              <span>Propriétaire précédent</span>
              <b>{valeur(dossier.nomProprietairePrecedent)}</b>
            </div>
            <div className="info-item info-item--full">
              <span>Actions</span>
              <b>
                {actions.length === 0
                  ? '—'
                  : actions.map((a) => a.label).join(', ')}
                {dossier.actionAutre && dossier.actionAutrePrecision
                  ? ` (${dossier.actionAutrePrecision})`
                  : ''}
              </b>
            </div>
            <div className="info-item">
              <span>Objet</span>
              <b>
                {label(OBJETS, dossier.objet)}
                {dossier.objet === 'AUTRE' && dossier.objetAutrePrecision
                  ? ` — ${dossier.objetAutrePrecision}`
                  : ''}
              </b>
            </div>
          </div>
        </section>
      </div>

      <div className="detail-cards" style={{ marginTop: 16 }}>
        <section className="card card-pad">
          <h3>Achat</h3>
          <div className="info-grid">
            <div className="info-item">
              <span>Mesure</span>
              <b>{mesure(dossier.achatMesureM2)}</b>
            </div>
            <div className="info-item">
              <span>Position / plan</span>
              <b>{valeur(dossier.achatPosition)}</b>
            </div>
            <div className="info-item info-item--full">
              <span>Axe ou emplacement</span>
              <b>{valeur(dossier.achatAxeEmplacement)}</b>
            </div>
            <div className="info-item">
              <span>Montant</span>
              <b>{montant(dossier.achatMontant)}</b>
            </div>
            <div className="info-item">
              <span>Permis de construire</span>
              <b>{ouiNon(dossier.achatPermisConstruire)}</b>
            </div>
          </div>
        </section>

        <section className="card card-pad">
          <h3>Vente</h3>
          <div className="info-grid">
            <div className="info-item">
              <span>Mesure</span>
              <b>{mesure(dossier.venteMesureM2)}</b>
            </div>
            <div className="info-item">
              <span>Position / plan</span>
              <b>{valeur(dossier.ventePosition)}</b>
            </div>
            <div className="info-item info-item--full">
              <span>Axe ou emplacement</span>
              <b>{valeur(dossier.venteAxeEmplacement)}</b>
            </div>
            <div className="info-item">
              <span>Prise de photo</span>
              <b>{ouiNon(dossier.ventePrisePhoto)}</b>
            </div>
            <div className="info-item">
              <span>Visite</span>
              <b>{ouiNon(dossier.venteVisite)}</b>
            </div>
            <div className="info-item">
              <span>Montant</span>
              <b>{montant(dossier.venteMontant)}</b>
            </div>
            <div className="info-item">
              <span>Permis de construire</span>
              <b>{ouiNon(dossier.ventePermisConstruire)}</b>
            </div>
          </div>
        </section>
      </div>

      <div className="detail-cards" style={{ marginTop: 16 }}>
        <section className="card card-pad">
          <h3>Régularisation</h3>
          <div className="info-grid">
            <div className="info-item">
              <span>Type de terrain</span>
              <b>{label(TYPES_TERRAIN, dossier.regularisationTypeTerrain)}</b>
            </div>
            <div className="info-item">
              <span>Situation du terrain</span>
              <b>{label(SITUATIONS_TERRAIN, dossier.regularisationSituationTerrain)}</b>
            </div>
            <div className="info-item info-item--full">
              <span>Documents existants</span>
              <b>
                {DOCUMENTS.filter((d) => Boolean(dossier[d.cle]))
                  .map((d) => d.label)
                  .join(', ') || '—'}
              </b>
            </div>
            <div className="info-item info-item--full">
              <span>Observations</span>
              <b>{valeur(dossier.observations)}</b>
            </div>
          </div>
        </section>

        <section className="card card-pad">
          <h3>Suivi</h3>
          <div className="info-grid">
            <div className="info-item">
              <span>Commerciaux sur terrain</span>
              <b>{dossier.nombreCommerciauxTerrain ?? '—'}</b>
            </div>
            <div className="info-item">
              <span>Commission CN</span>
              <b>{montant(dossier.commissionCn)}</b>
            </div>
            <div className="info-item info-item--full">
              <span>Prestation abonnement mensuel</span>
              <b>{ouiNon(dossier.prestationAbonnementMensuel)}</b>
            </div>
          </div>
        </section>
      </div>
    </>
  )
}

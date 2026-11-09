import {
  FORMES_JURIDIQUES,
  HEBERGEMENTS,
  REGIMES_ACTIVITE,
  type DossierEntreprise,
} from '../data/mockDossierEntreprise'

function label<T extends string>(liste: { valeur: T; label: string }[], valeur: T | ''): string {
  if (!valeur) return '—'
  return liste.find((l) => l.valeur === valeur)?.label ?? valeur
}

function ouiNon(vrai: boolean): string {
  return vrai ? 'Oui' : 'Non'
}

function valeur(texte: string | null, suffixe = ''): string {
  if (texte === null || texte === undefined || String(texte).trim() === '') return '—'
  return `${texte}${suffixe}`
}

function formatMontant(n: number | null): string {
  if (n === null || n === undefined) return '—'
  return `${n.toLocaleString('fr-FR')} MGA`
}

function formatDateISO(iso: string): string {
  if (!iso) return '—'
  const [a, m, j] = iso.split('-')
  return `${j}/${m}/${a}`
}

interface Props {
  dossier: DossierEntreprise
  /** Date de la demande d'origine + immatricule du commercial (lus, non stockés). */
  dateDemande: string
  numeroIM: string
}

/** Fiche de lecture du dossier de création d'entreprise (cf. fiche papier). */
export function DossierEntrepriseDetail({ dossier, dateDemande, numeroIM }: Props) {
  return (
    <>
      <div className="detail-cards">
        <section className="card card-pad">
          <h3>Société</h3>
          <div className="info-grid">
            <div className="info-item">
              <span>Date de la fiche</span>
              <b>{dateDemande}</b>
            </div>
            <div className="info-item">
              <span>N° IM du commercial</span>
              <b>{numeroIM}</b>
            </div>
            <div className="info-item">
              <span>Forme juridique</span>
              <b>
                {label(FORMES_JURIDIQUES, dossier.formeJuridique)}
                {dossier.formeJuridique === 'AUTRE' && dossier.formePrecision
                  ? ` (${dossier.formePrecision})`
                  : ''}
              </b>
            </div>
            <div className="info-item info-item--full">
              <span>Dénomination</span>
              <b>{dossier.denomination || '—'}</b>
            </div>
            <div className="info-item info-item--full">
              <span>Siège social actuel</span>
              <b>{valeur(dossier.siegeSocialActuel)}</b>
            </div>
            <div className="info-item">
              <span>Capital social</span>
              <b>{formatMontant(dossier.capitalSocial)}</b>
            </div>
            <div className="info-item">
              <span>Nombre d&apos;associés</span>
              <b>{dossier.associes.filter((a) => a.nom.trim()).length || dossier.nombreAssocies}</b>
            </div>
          </div>
        </section>

        <section className="card card-pad">
          <h3>Gérance</h3>
          <div className="info-grid">
            <div className="info-item">
              <span>Nom du gérant</span>
              <b>{valeur(dossier.nomGerant)}</b>
            </div>
            <div className="info-item">
              <span>Fonction</span>
              <b>{valeur(dossier.fonctionGerant)}</b>
            </div>
            <div className="info-item">
              <span>Location de gérance</span>
              <b>{ouiNon(dossier.locationGerance)}</b>
            </div>
          </div>
        </section>
      </div>

      <div className="detail-cards" style={{ marginTop: 16 }}>
        <section className="card card-pad">
          <h3>Siège et établissement</h3>
          <div className="info-grid">
            <div className="info-item info-item--full">
              <span>Adresse</span>
              <b>{valeur(dossier.adresse)}</b>
            </div>
            <div className="info-item">
              <span>Ville</span>
              <b>{valeur(dossier.ville)}</b>
            </div>
            <div className="info-item">
              <span>Commune</span>
              <b>{valeur(dossier.commune)}</b>
            </div>
            <div className="info-item">
              <span>District</span>
              <b>{valeur(dossier.district)}</b>
            </div>
            <div className="info-item">
              <span>Région</span>
              <b>{valeur(dossier.region)}</b>
            </div>
            <div className="info-item info-item--full">
              <span>Hébergement du siège</span>
              <b>
                {label(HEBERGEMENTS, dossier.hebergement)}
                {dossier.hebergement === 'AUTRE' && dossier.hebergementPrecision
                  ? ` — ${dossier.hebergementPrecision}`
                  : ''}
              </b>
            </div>
            <div className="info-item info-item--full">
              <span>Contrat de bail</span>
              <b>
                {dossier.contratBail === '' ? '—' : dossier.contratBail === 'oui' ? 'Oui' : 'Non'}
                {dossier.contratBailDetail ? ` — ${dossier.contratBailDetail}` : ''}
              </b>
            </div>
          </div>
        </section>

        <section className="card card-pad">
          <h3>Activité et situation</h3>
          <div className="info-grid">
            <div className="info-item">
              <span>Secteur d&apos;activité</span>
              <b>{valeur(dossier.secteurActivite)}</b>
            </div>
            <div className="info-item">
              <span>Autorisation spéciale</span>
              <b>{valeur(dossier.autorisationSpeciale)}</b>
            </div>
            <div className="info-item info-item--full">
              <span>Objet social</span>
              <b>{valeur(dossier.objetSocial)}</b>
            </div>
            <div className="info-item">
              <span>Début d&apos;activité souhaité</span>
              <b>{formatDateISO(dossier.dateSouhaitee)}</b>
            </div>
            <div className="info-item">
              <span>Régime d&apos;activité</span>
              <b>{label(REGIMES_ACTIVITE, dossier.regimeActivite)}</b>
            </div>
            <div className="info-item info-item--full">
              <span>Établissement / succursale / filiale</span>
              <b>{valeur(dossier.activitesEtablissement)}</b>
            </div>
            <div className="info-item">
              <span>TVA</span>
              <b>{valeur(dossier.tva)}</b>
            </div>
            <div className="info-item">
              <span>Autre société</span>
              <b>{valeur(dossier.autreSociete)}</b>
            </div>
            <div className="info-item">
              <span>Situation matrimoniale</span>
              <b>{valeur(dossier.situationMatrimoniale)}</b>
            </div>
            <div className="info-item">
              <span>Régime d&apos;imposition</span>
              <b>{valeur(dossier.regimeImposition)}</b>
            </div>
            <div className="info-item info-item--full">
              <span>Observations</span>
              <b>{valeur(dossier.observations)}</b>
            </div>
          </div>
        </section>
      </div>

      <section className="card" style={{ marginTop: 16 }}>
        <div className="card-pad" style={{ paddingBottom: 0 }}>
          <h3 style={{ margin: 0 }}>Associés</h3>
        </div>
        <div className="table-scroll-container no-global-scroll">
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Nom des associés</th>
                  <th>Part sociale</th>
                  <th>Adresse mail</th>
                  <th>Contact</th>
                </tr>
              </thead>
              <tbody>
                {dossier.associes
                  .filter((a) => a.nom.trim())
                  .map((a, i) => (
                    <tr key={i}>
                      <td className="fw-600">{a.nom}</td>
                      <td>{valeur(a.partSociale)}</td>
                      <td className="text-secondary">{valeur(a.email)}</td>
                      <td className="text-secondary">{valeur(a.contact)}</td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </>
  )
}

import { useMemo, useState } from 'react'
import { Alert, Spinner } from '@/components/ui'
import { Can } from '@/components/shared/Can'
import { useApi } from '@/hooks/useApi'
import { useDebounce } from '@/hooks/useDebounce'
import { formatDate, formatMontantMGA } from '@/utils/format'
import { telechargerBlob } from '@/utils/telechargement'
import { clientsApi } from '../services/clients.service'
import type { FactureFacturation, PaiementFacturation } from '../types'
import { codeSociete } from '../types'

type Tri = 'desc' | 'asc'

const TYPE_LABEL: Record<string, string> = {
  GENERALE: 'Générale',
  PRESTATION: 'Prestation',
  DEBOURS: 'Débours',
  ABONNEMENT_MENSUEL: 'Abonnement',
}

const TYPE_PILL: Record<string, string> = {
  GENERALE: 'pill info',
  PRESTATION: 'pill wt',
  DEBOURS: 'pill warning',
  ABONNEMENT_MENSUEL: 'pill ok',
}

function formatMois(mois: string | null | undefined): string {
  if (!mois) return ''
  const parsed = new Date(`${mois}-01T00:00:00`)
  if (Number.isNaN(parsed.getTime())) return mois
  const texte = parsed.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' })
  return texte.charAt(0).toUpperCase() + texte.slice(1)
}

function sommeFactures(arr: FactureFacturation[]): number {
  return arr.reduce((total, f) => total + (f.montant ?? 0), 0)
}

function sommePayeeFactures(arr: FactureFacturation[]): number {
  return arr.reduce((total, f) => total + (f.montantPaye ?? 0), 0)
}

function sommePaiements(arr: PaiementFacturation[]): number {
  return arr.reduce((total, p) => total + (p.montantPaye ?? 0), 0)
}

function statutFacture(montant: number, paye: number): { libelle: string; pill: string } {
  const reste = Math.max(0, montant - paye)
  if (montant <= 0) return { libelle: '—', pill: 'pill wt' }
  if (reste <= 0) return { libelle: 'Soldée', pill: 'pill ok' }
  if (paye > 0) return { libelle: 'Partielle', pill: 'pill warning' }
  return { libelle: 'Impayée', pill: 'pill danger' }
}

interface Groupe {
  /** Clé unique pour React. */
  key: string
  /** Facture générale parente (null pour une facture de paiement orpheline). */
  general: FactureFacturation | null
  /** Factures de paiement enfants, visibles en permanence sous la générale. */
  enfants: FactureFacturation[]
}

function construireGroupes(
  factures: FactureFacturation[],
  dateDebut: string,
  dateFin: string,
  tri: Tri,
  recherche: string,
): Groupe[] {
  const parId = new Map(factures.map((f) => [f.id, f]))
  const generales = factures.filter((f) => f.type === 'GENERALE')
  const enfants = factures.filter((f) => f.type !== 'GENERALE')

  // Filtre sur les dates de la facture GÉNÉRALE uniquement : un groupe entier
  // (générale + factures de paiement) est visible si et seulement si la date
  // de la facture générale est dans la plage. Les factures de paiement, elles,
  // ne jouent pas sur le filtre (mais gardent leur sous-contenu de règlements).
  const inRange = (d: string | null | undefined): boolean => {
    if (!dateDebut && !dateFin) return true
    if (!d) return false
    if (dateDebut && d < dateDebut) return false
    if (dateFin && d > dateFin) return false
    return true
  }
  // Filtre par recherche sur référence demande, facture ou paiement (insensible à la casse).
  const terme = recherche.trim().toLowerCase()
  const matchRecherche = (f: FactureFacturation): boolean => {
    if (!terme) return true
    const hay = [
      f.referenceFactureValidee ?? '',
      f.referenceDemande ?? '',
      ...f.paiements.map((p) => p.referencePaiement ?? ''),
    ].join(' ').toLowerCase()
    return hay.includes(terme)
  }

  // Factures enfants d'une générale : affichées quand la générale l'est (jamais elles-mêmes).
  // Une facture de paiement sans générale parente (orphelin) se filtre sur sa propre date.
  const matchGroupe = (g: FactureFacturation | null): boolean =>
    inRange(g ? g.dateFactureValidee : null)
  const matchOrphelin = (f: FactureFacturation): boolean => inRange(f.dateFactureValidee)

  const cmp = (a: string | null | undefined, b: string | null | undefined): number => {
    const sa = a ?? ''
    const sb = b ?? ''
    if (sa === sb) return 0
    const ordre = sa < sb ? -1 : 1
    return tri === 'desc' ? -ordre : ordre
  }

  const enfantsParGenerale = new Map<number, FactureFacturation[]>()
  const orphelins: FactureFacturation[] = []
  for (const e of enfants) {
    const parent = e.factureGeneraleId != null ? parId.get(e.factureGeneraleId) : undefined
    if (parent && parent.type === 'GENERALE') {
      const liste = enfantsParGenerale.get(parent.id) ?? []
      liste.push(e)
      enfantsParGenerale.set(parent.id, liste)
    } else {
      orphelins.push(e)
    }
  }

  const groupes: Groupe[] = []
  for (const g of [...generales].sort((a, b) => cmp(a.dateFactureValidee, b.dateFactureValidee))) {
    if (!matchGroupe(g)) continue
    const enfants = (enfantsParGenerale.get(g.id) ?? [])
      .sort((a, b) => cmp(a.dateFactureValidee, b.dateFactureValidee))
    if (terme && !matchRecherche(g) && !enfants.some(matchRecherche)) continue
    groupes.push({ key: `g-${g.id}`, general: g, enfants })
  }
  for (const e of orphelins.filter(matchOrphelin)) {
    if (terme && !matchRecherche(e)) continue
    groupes.push({ key: `o-${e.id}`, general: null, enfants: [e] })
  }
  groupes.sort((a, b) => {
    const dateA = a.general?.dateFactureValidee ?? a.enfants[0]?.dateFactureValidee ?? null
    const dateB = b.general?.dateFactureValidee ?? b.enfants[0]?.dateFactureValidee ?? null
    return cmp(dateA, dateB)
  })
  return groupes
}


type LigneAplatie =
  | { kind: 'general'; key: string; general: FactureFacturation; enfants: FactureFacturation[]; estStandIn: boolean; repliee: boolean }
  | { kind: 'enfant'; key: string; enfant: FactureFacturation; estStandIn: boolean }
  | { kind: 'reglement'; key: string; paiement: PaiementFacturation }

/** Aplatit les groupes de facturation en lignes affichables (générale → enfants → règlements). */
function aplatirLignes(
  groupes: Groupe[],
  repliees: Set<number>,
  ouverts: Set<number>,
  tri: Tri,
): LigneAplatie[] {
  const lignes: LigneAplatie[] = []
  for (const groupe of groupes) {
    const general = groupe.general
    const enfants = groupe.enfants
    const estStandIn = enfants.length === 0 && !!general
    const enfantsAffiches = estStandIn && general
      ? [{ ...general, type: general.objetType ?? 'PRESTATION' as FactureFacturation['type'] }]
      : enfants
    const repliee = general ? repliees.has(general.id) : false
    if (general) {
      lignes.push({ kind: 'general', key: `g-${general.id}`, general, enfants, estStandIn, repliee })
      if (!estStandIn && !repliee) {
        for (const p of general.paiements) {
          lignes.push({ kind: 'reglement', key: `gp-${general.id}-${p.id}`, paiement: p })
        }
      }
      if (!repliee) {
        for (const enfant of enfantsAffiches) {
          lignes.push({ kind: 'enfant', key: `e-${enfant.id}`, enfant, estStandIn })
          const triesPaiements = [...enfant.paiements].sort((a, b) => {
            const da = a.datePaiement ?? ''
            const db = b.datePaiement ?? ''
            const ordre = da < db ? -1 : da > db ? 1 : 0
            return tri === 'desc' ? -ordre : ordre
          })
          if (ouverts.has(enfant.id)) {
            for (const p of triesPaiements) {
              lignes.push({ kind: 'reglement', key: `ep-${enfant.id}-${p.id}`, paiement: p })
            }
          }
        }
      }
    } else {
      for (const enfant of enfants) {
        lignes.push({ kind: 'enfant', key: `o-${enfant.id}`, enfant, estStandIn: false })
        const triesPaiements = [...enfant.paiements].sort((a, b) => {
          const da = a.datePaiement ?? ''
          const db = b.datePaiement ?? ''
          const ordre = da < db ? -1 : da > db ? 1 : 0
          return tri === 'desc' ? -ordre : ordre
        })
        if (ouverts.has(enfant.id)) {
          for (const p of triesPaiements) {
            lignes.push({ kind: 'reglement', key: `ep-${enfant.id}-${p.id}`, paiement: p })
          }
        }
      }
    }
  }
  return lignes
}

const PAGE_SIZE_LIGNES = 15

/** Onglet « Facturation » de la fiche client : toutes les factures depuis l'enregistrement. */
export function ClientFacturationTab({ societeId, nomClient }: { societeId: number | null; nomClient: string }) {
  const { data, loading, error } = useApi<FactureFacturation[] | null>(
    () => (societeId === null ? Promise.resolve([]) : clientsApi.factures(societeId)),
    [societeId],
  )
  const [dateDebut, setDateDebut] = useState('')
  const [dateFin, setDateFin] = useState('')
  const [recherche, setRecherche] = useState('')
  const [tri, setTri] = useState<Tri>('desc')
  const [ouverts, setOuverts] = useState<Set<number>>(new Set())
  const [groupesRepliees, setGroupesRepliees] = useState<Set<number>>(new Set())
  const [page, setPage] = useState(0)
  const [erreurPdf, setErreurPdf] = useState<string | null>(null)
  const [pleinEcran, setPleinEcran] = useState(false)
  const [showFilters, setShowFilters] = useState(true)

  const terme = useDebounce(recherche, 300)

  const factures = useMemo(() => data ?? [], [data])
  const groupes = useMemo(
    () => construireGroupes(factures, dateDebut, dateFin, tri, terme),
    [factures, dateDebut, dateFin, tri, terme],
  )

  const estOuverte = (id: number): boolean => ouverts.has(id)
  const toggle = (id: number): void =>
    setOuverts((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  const toggleGroupe = (id: number): void =>
    setGroupesRepliees((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  const toutesFvAvecPaiements = useMemo(
    () => factures.filter((f) => f.paiements.length > 0),
    [factures],
  )
  const toutEstDeplie = groupesRepliees.size === 0
    && toutesFvAvecPaiements.every((f) => ouverts.has(f.id))
  const deplierTout = (): void => {
    setGroupesRepliees(new Set())
    setOuverts(new Set(toutesFvAvecPaiements.map((f) => f.id)))
  }
  const replierTout = (): void => {
    setGroupesRepliees(new Set(factures.filter((f) => f.type === 'GENERALE').map((f) => f.id)))
    setOuverts(new Set())
  }

  const lignesAffichees = useMemo(
    () => aplatirLignes(groupes, groupesRepliees, ouverts, tri),
    [groupes, groupesRepliees, ouverts, tri],
  )
  const totalPages = Math.max(1, Math.ceil(lignesAffichees.length / PAGE_SIZE_LIGNES))
  const pageActuelle = Math.min(page, totalPages - 1)
  const lignesPage = lignesAffichees.slice(
    pageActuelle * PAGE_SIZE_LIGNES,
    pageActuelle * PAGE_SIZE_LIGNES + PAGE_SIZE_LIGNES,
  )

  const totaux = useMemo(
    () => groupes.reduce((acc, g) => {
      if (g.general) {
        acc.montant += sommeFactures(g.enfants) === 0 && g.enfants.length === 0
          ? (g.general.montant ?? 0)
          : sommeFactures(g.enfants)
        acc.paye += sommePayeeFactures(g.enfants) + sommePaiements(g.general.paiements)
      } else {
        acc.montant += sommeFactures(g.enfants)
        acc.paye += sommePayeeFactures(g.enfants)
      }
      return acc
    }, { montant: 0, paye: 0 }),
    [groupes],
  )

  if (loading) {
    return <div className="card card-pad"><Spinner label="Chargement de la facturation…" /></div>
  }

  if (error) {
    return (
      <div className="card card-pad">
        <Alert tone="danger">{error}</Alert>
      </div>
    )
  }

  const resetPage = (): void => setPage(0)
  const onDateDebut = (v: string): void => { setDateDebut(v); resetPage() }
  const onDateFin = (v: string): void => { setDateFin(v); resetPage() }
  const onRecherche = (v: string): void => { setRecherche(v); resetPage() }
  const onTri = (v: Tri): void => { setTri(v); resetPage() }

  const telechargerPdf = async (): Promise<void> => {
    if (societeId === null) return
    setErreurPdf(null)
    try {
      const pdf = await clientsApi.downloadPdfFacturation(societeId, {
        dateDebut: dateDebut || null,
        dateFin: dateFin || null,
        ordre: tri,
        recherche,
      })
      telechargerBlob(pdf, `facturation-${codeSociete(societeId)}.pdf`)
    } catch {
      setErreurPdf("Téléchargement du PDF de facturation impossible.")
    }
  }

  const telechargerExcel = async (): Promise<void> => {
    if (societeId === null) return
    setErreurPdf(null)
    try {
      const excel = await clientsApi.downloadExcelFacturation(societeId, {
        dateDebut: dateDebut || null,
        dateFin: dateFin || null,
        ordre: tri,
        recherche,
      })
      telechargerBlob(excel, `facturation-${codeSociete(societeId)}.xlsx`)
    } catch {
      setErreurPdf("Téléchargement de l'Excel de facturation impossible.")
    }
  }

  const aFiltres = Boolean(dateDebut || dateFin || terme) || tri !== 'desc'

  const ligneTriFiltres = (
    <form className="filters-inline" onSubmit={(e) => e.preventDefault()} aria-label="Filtrer la facturation">
      <div className="field filter-field">
        <label htmlFor="fact-recherche">Rechercher</label>
        <input id="fact-recherche" type="text" value={recherche}
               placeholder="Réf. demande, facture ou paiement…"
               onChange={(e) => onRecherche(e.target.value)} />
      </div>
      <div className="field filter-field">
        <label htmlFor="fact-date-debut">Date facture Générale (début)</label>
        <input id="fact-date-debut" type="date" value={dateDebut}
               onChange={(e) => onDateDebut(e.target.value)} />
      </div>
      <div className="field filter-field">
        <label htmlFor="fact-date-fin">Date facture Générale (fin)</label>
        <input id="fact-date-fin" type="date" value={dateFin}
               onChange={(e) => onDateFin(e.target.value)} />
      </div>
      <div className="field filter-field">
        <label htmlFor="fact-tri">Ordre de la date de facture Générale</label>
        <select id="fact-tri" value={tri} onChange={(e) => onTri(e.target.value as Tri)}>
          <option value="desc">Décroissante (récente en haut)</option>
          <option value="asc">Croissante (ancienne en haut)</option>
        </select>
      </div>
      <div className="filter-actions filter-actions--bottom">
        <button type="button" className="btn-ghost" disabled={!aFiltres && groupesRepliees.size === 0 && toutesFvAvecPaiements.every((f) => !ouverts.has(f.id))}
                onClick={() => { setDateDebut(''); setDateFin(''); setRecherche(''); setTri('desc'); setOuverts(new Set()); setGroupesRepliees(new Set()) }}>
          Réinitialiser
        </button>
      </div>
    </form>
  )

  return (
    <section className={`card${pleinEcran ? ' facturation-plein-ecran' : ''}`}>
      {erreurPdf && (
        <div style={{ padding: '16px 16px 0' }}>
          <Alert tone="danger">{erreurPdf}</Alert>
        </div>
      )}
      <div className="facture-toolbar">
        <div className="facturation-toolbar__info">
          <span className="facture-toolbar__titre">Facturation du client</span>
        </div>
        <div className="facture-toolbar__actions">
          <button type="button" className="btn-ghost"
                  onClick={() => setShowFilters((prev) => !prev)}
                  aria-pressed={showFilters}>
            {showFilters ? 'Masquer les filtres' : 'Afficher les filtres'}
          </button>
          <button type="button" className="btn-ghost"
                  onClick={() => (toutEstDeplie ? replierTout() : deplierTout())}>
            {toutEstDeplie ? 'Tout replier' : 'Déplier tout'}
          </button>
          <button type="button" className="btn-ghost"
                  onClick={() => setPleinEcran((prev) => !prev)}
                  aria-pressed={pleinEcran}>
            {pleinEcran ? '✕ Fermer le plein écran' : '⛶ Plein écran'}
          </button>
          <Can permission="FACTURE_VIEW">
            <button type="button" className="btn-primary" onClick={() => void telechargerPdf()}
                    title="Exporter en PDF">
              <svg className="sv" viewBox="0 0 24 24">
                <path d="M12 3v12m0 0l-4-4m4 4l4-4M4 21h16" />
              </svg>
              Exporter en PDF
            </button>
            <button type="button" className="btn-ghost" onClick={() => void telechargerExcel()}
                    title="Exporter en Excel (.xlsx)">
              <svg className="sv" viewBox="0 0 24 24">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                <path d="M14 2v6h6" />
                <path d="M9 15l2-2m0 0l2 2m-2-2v6" />
              </svg>
              Exporter en Excel
            </button>
          </Can>
        </div>
      </div>
      {showFilters && (
        <div style={{ padding: '0 16px' }}>
          {ligneTriFiltres}
        </div>
      )}
      <div className="table-scroll-container no-global-scroll">
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Date facture</th>
                <th>Référence facture</th>
                <th>Type</th>
                <th>Client</th>
                <th>Désignation</th>
                <th>Montant facturé</th>
                <th>Montant encaissé</th>
                <th>Montant impayé</th>
                <th>État de paiement</th>
                <th>Observation</th>
              </tr>
            </thead>
            <tbody>
              {lignesPage.map((ligne) => {
                switch (ligne.kind) {
                  case 'general': {
                    const general = ligne.general
                    const enfants = ligne.enfants
                    const estStandIn = ligne.estStandIn
                    const repliee = ligne.repliee
                    const montantGeneral = enfants.length > 0
                      ? sommeFactures(enfants)
                      : (general.montant ?? 0)
                    const encaisseGeneral = (enfants.length > 0
                      ? sommePayeeFactures(enfants)
                      : (general.montantPaye ?? 0)) + sommePaiements(general.paiements ?? [])
                    const resteGeneral = Math.max(0, montantGeneral - encaisseGeneral)
                    const statutGeneral = statutFacture(montantGeneral, encaisseGeneral)
                    const enfantsAffiches = estStandIn && general
                      ? [{ ...general, type: general.objetType ?? 'PRESTATION' as FactureFacturation['type'] }]
                      : enfants
                    const sousContenu = enfantsAffiches.length > 0 || general.paiements.length > 0

                    return (
                      <tr key={ligne.key} className="facture-rang-general">
                        <td className="text-secondary">
                          {sousContenu && (
                            <button type="button" className="expand-toggle"
                                    onClick={() => toggleGroupe(general.id)}
                                    aria-expanded={!repliee}
                                    aria-label={repliee
                                      ? `Déplier ${general.referenceFactureValidee ?? 'la facture'} et son contenu`
                                      : `Replier ${general.referenceFactureValidee ?? 'la facture'} et son contenu`}>
                              {repliee ? '▸' : '▾'}
                            </button>
                          )}
                          {formatDate(general.dateFactureValidee)}
                        </td>
                        <td className="fw-700">{general.referenceFactureValidee ?? '—'}</td>
                        <td><span className={TYPE_PILL[general.type] ?? 'pill info'}>● {TYPE_LABEL[general.type] ?? general.type}</span></td>
                        <td className="text-secondary">{nomClient}</td>
                        <td>{`Facture générale ${general.referenceDemande ?? '—'}`}</td>
                        <td className="cell-number fw-700">{formatMontantMGA(montantGeneral)}</td>
                        <td className="cell-number fw-700">{formatMontantMGA(encaisseGeneral)}</td>
                        <td className="cell-number" style={resteGeneral > 0 ? { color: 'var(--danger)' } : undefined}>
                          {formatMontantMGA(resteGeneral)}
                        </td>
                        <td><span className={statutGeneral.pill}>{statutGeneral.libelle}</span></td>
                        <td className="text-secondary">—</td>
                      </tr>
                    )
                  }
                  case 'enfant': {
                    const enfant = ligne.enfant
                    const estStandIn = ligne.estStandIn
                    const afficherRefGenerale = !estStandIn && enfant.referenceFactureGenerale
                    const payeEnfant = enfant.montantPaye ?? sommePaiements(enfant.paiements)
                    const montantEnfant = enfant.montant ?? 0
                    const resteEnfant = Math.max(0, montantEnfant - payeEnfant)
                    const statutEnfant = statutFacture(montantEnfant, payeEnfant)
                    const aPaiements = enfant.paiements.length > 0
                    const designation = enfant.type === 'ABONNEMENT_MENSUEL'
                      ? `${enfant.objet ?? 'Abonnement'}${enfant.mois ? ` (${formatMois(enfant.mois)})` : ''}`
                      : enfant.objet ?? '—'

                    return (
                      <tr key={ligne.key}>
                        <td className="facture-indent-1 text-secondary">
                          {aPaiements && (
                            <button type="button" className="expand-toggle"
                                    onClick={() => toggle(enfant.id)}
                                    aria-expanded={estOuverte(enfant.id)}
                                    aria-label={estOuverte(enfant.id)
                                      ? `Replier les paiements de ${enfant.referenceFactureValidee ?? 'la facture'}`
                                      : `Déplier les paiements de ${enfant.referenceFactureValidee ?? 'la facture'}`}>
                              {estOuverte(enfant.id) ? '▾' : '▸'}
                            </button>
                          )}
                          {formatDate(enfant.dateFactureValidee)}
                        </td>
                        <td className="fw-700">
                          {enfant.referenceFactureValidee ?? '—'}
                          {afficherRefGenerale && (
                            <span className="facture-sousligne">Générale {enfant.referenceFactureGenerale}</span>
                          )}
                        </td>
                        <td><span className={TYPE_PILL[enfant.type] ?? 'pill wt'}>● {TYPE_LABEL[enfant.type] ?? enfant.type}</span></td>
                        <td className="text-secondary">{nomClient}</td>
                        <td>{designation}</td>
                        <td className="cell-number">{formatMontantMGA(montantEnfant)}</td>
                        <td className="cell-number">{formatMontantMGA(payeEnfant)}</td>
                        <td className="cell-number" style={resteEnfant > 0 ? { color: 'var(--danger)' } : undefined}>
                          {formatMontantMGA(resteEnfant)}
                        </td>
                        <td><span className={statutEnfant.pill}>{statutEnfant.libelle}</span></td>
                        <td className="text-secondary">—</td>
                      </tr>
                    )
                  }
                  case 'reglement':
                    return <LigneReglement key={ligne.key} paiement={ligne.paiement} nomClient={nomClient} />
                  default:
                    return null
                }
              })}
              {groupes.length === 0 && (
                <tr>
                  <td colSpan={11} className="text-muted">
                    {aFiltres
                      ? (terme ? `Aucune facture ne correspond à « ${terme} ».`
                                : 'Aucune facture ne correspond aux filtres.')
                      : 'Aucune facture pour ce client depuis son enregistrement.'}
                  </td>
                </tr>
              )}
            </tbody>
            {groupes.length > 0 && (
              <tfoot>
                <tr>
                  <td colSpan={5}>Totaux (vue courante)</td>
                  <td className="cell-number fw-700">{formatMontantMGA(totaux.montant)}</td>
                  <td className="cell-number fw-700">{formatMontantMGA(totaux.paye)}</td>
                  <td className="cell-number fw-700" style={totaux.montant - totaux.paye > 0 ? { color: 'var(--danger)' } : undefined}>
                    {formatMontantMGA(Math.max(0, totaux.montant - totaux.paye))}
                  </td>
                  <td />
                  <td />
                </tr>
              </tfoot>
            )}
          </table>
        </div>
        {lignesAffichees.length > 0 && (
          <div className="table-footer">
            <span>
              Affichage {pageActuelle * PAGE_SIZE_LIGNES + 1} à{' '}
              {Math.min(lignesAffichees.length, pageActuelle * PAGE_SIZE_LIGNES + PAGE_SIZE_LIGNES)}{' '}
              sur {lignesAffichees.length} ligne{lignesAffichees.length > 1 ? 's' : ''}
            </span>
            <div className="pagination">
              {Array.from({ length: totalPages }).map((_, i) => (
                <button
                  key={i}
                  type="button"
                  className={`pagination-item ${i === pageActuelle ? 'active' : ''}`}
                  onClick={() => setPage(i)}
                  aria-label={`Page ${i + 1}`}
                >
                  {i + 1}
                </button>
              ))}
            </div>
          </div>
        )}
      </div>
    </section>
  )
}

function LigneReglement({ paiement, nomClient }: { paiement: PaiementFacturation; nomClient: string }) {
  const montant = paiement.montantPaye ?? 0
  const libelleMode = paiement.libelleMode ?? '—'
  const estVirement = paiement.codeMode?.toUpperCase() === 'VIREMENT'
  return (
    <tr className="facture-rang-reglement">
      <td className="facture-indent-2 text-secondary">{formatDate(paiement.datePaiement)}</td>
      <td className="text-secondary">
        {paiement.referencePaiement}
        {estVirement && paiement.referenceVirement && (
          <span className="facture-sousligne">Rép. virement {paiement.referenceVirement}</span>
        )}
        {!estVirement && paiement.numeroCheque && (
          <span className="facture-sousligne">Chèque n° {paiement.numeroCheque} — {paiement.banque ?? '—'}</span>
        )}
      </td>
      <td><span className="pill ok">● Règlement</span></td>
      <td className="text-secondary">{nomClient}</td>
      <td>{libelleMode}</td>
      <td className="text-secondary">—</td>
      <td className="cell-number">{formatMontantMGA(montant)}</td>
      <td className="text-secondary">—</td>
      <td><span className="pill ok">Encaissé</span></td>
      <td className="facture-observation">{paiement.commentaire?.trim() || '—'}</td>
    </tr>
  )
}

import { Fragment, useMemo, useState } from 'react'
import { Alert, Spinner } from '@/components/ui'
import { useApi } from '@/hooks/useApi'
import { formatDate, formatMontantMGA } from '@/utils/format'
import { clientsApi } from '../services/clients.service'
import type { FactureFacturation, PaiementFacturation } from '../types'

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
    groupes.push({ key: `g-${g.id}`, general: g, enfants })
  }
  for (const e of orphelins.filter(matchOrphelin)) {
    groupes.push({ key: `o-${e.id}`, general: null, enfants: [e] })
  }
  groupes.sort((a, b) => {
    const dateA = a.general?.dateFactureValidee ?? a.enfants[0]?.dateFactureValidee ?? null
    const dateB = b.general?.dateFactureValidee ?? b.enfants[0]?.dateFactureValidee ?? null
    return cmp(dateA, dateB)
  })
  return groupes
}

const PAGE_SIZE_GROUPES = 6

/** Onglet « Facturation » de la fiche client : toutes les factures depuis l'enregistrement. */
export function ClientFacturationTab({ societeId, nomClient }: { societeId: number | null; nomClient: string }) {
  const { data, loading, error } = useApi<FactureFacturation[] | null>(
    () => (societeId === null ? Promise.resolve([]) : clientsApi.factures(societeId)),
    [societeId],
  )
  const [dateDebut, setDateDebut] = useState('')
  const [dateFin, setDateFin] = useState('')
  const [tri, setTri] = useState<Tri>('desc')
  const [ouverts, setOuverts] = useState<Set<number>>(new Set())
  const [toutDeplier, setToutDeplier] = useState(false)
  const [page, setPage] = useState(0)

  const factures = useMemo(() => data ?? [], [data])
  const groupes = useMemo(
    () => construireGroupes(factures, dateDebut, dateFin, tri),
    [factures, dateDebut, dateFin, tri],
  )

  const estOuverte = (id: number): boolean => toutDeplier || ouverts.has(id)
  const toggle = (id: number): void =>
    setOuverts((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })

  const totalPages = Math.max(1, Math.ceil(groupes.length / PAGE_SIZE_GROUPES))
  const groupesPage = groupes.slice(page * PAGE_SIZE_GROUPES, page * PAGE_SIZE_GROUPES + PAGE_SIZE_GROUPES)

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
  const onTri = (v: Tri): void => { setTri(v); resetPage() }

  const aFiltres = Boolean(dateDebut || dateFin) || tri !== 'desc'

  const ligneTriFiltres = (
    <form className="filters-inline" onSubmit={(e) => e.preventDefault()} aria-label="Filtrer la facturation">
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
        <button type="button" className="btn-ghost" disabled={!aFiltres && !toutDeplier}
                onClick={() => { setDateDebut(''); setDateFin(''); setTri('desc'); setOuverts(new Set()); setToutDeplier(false) }}>
          Réinitialiser
        </button>
        <button type="button" className="btn-ghost"
                onClick={() => setToutDeplier((prev) => !prev)}>
          {toutDeplier ? 'Tout replier' : 'Déplier tout'}
        </button>
      </div>
    </form>
  )

  return (
    <section className="card">
      <div style={{ padding: '16px 16px 0' }}>
        {ligneTriFiltres}
      </div>
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
                <th>Observation</th>
              </tr>
            </thead>
            <tbody>
              {groupesPage.map((groupe) => {
                const general = groupe.general
                const enfants = groupe.enfants
                const paiementGeneral = general ? general.paiements : []
                const montantGeneral = general
                  ? (enfants.length > 0 ? sommeFactures(enfants) : (general.montant ?? 0))
                  : 0
                const encaisseGeneral = general
                  ? sommePayeeFactures(enfants) + sommePaiements(paiementGeneral)
                  : 0
                const resteGeneral = general ? Math.max(0, montantGeneral - encaisseGeneral) : 0
                const statutGeneral = general ? statutFacture(montantGeneral, encaisseGeneral) : null

                return (
                  <Fragment key={groupe.key}>
                    {general && (
                      <tr className="facture-rang-general">
                        <td className="text-secondary">
                          {general.paiements.length > 0 && (
                            <button type="button" className="expand-toggle"
                                    onClick={() => toggle(general.id)}
                                    aria-expanded={estOuverte(general.id)}
                                    aria-label={estOuverte(general.id)
                                      ? `Replier les paiements de ${general.referenceFactureValidee ?? 'la facture'}`
                                      : `Déplier les paiements de ${general.referenceFactureValidee ?? 'la facture'}`}>
                              {estOuverte(general.id) ? '▾' : '▸'}
                            </button>
                          )}
                          {formatDate(general.dateFactureValidee)}
                        </td>
                        <td className="fw-700">{general.referenceFactureValidee ?? '—'}</td>
                        <td><span className={TYPE_PILL[general.type] ?? 'pill info'}>● {TYPE_LABEL[general.type] ?? general.type}</span></td>
                        <td className="text-secondary">{nomClient}</td>
                        <td>
                          {general.objet ?? 'Facture générale'}
                          {general.referenceDemande && (
                            <span className="facture-sousligne">Demande {general.referenceDemande}</span>
                          )}
                        </td>
                        <td className="cell-number fw-700">{formatMontantMGA(montantGeneral)}</td>
                        <td className="cell-number fw-700">{formatMontantMGA(encaisseGeneral)}</td>
                        <td className="cell-number" style={resteGeneral > 0 ? { color: 'var(--danger)' } : undefined}>
                          {formatMontantMGA(resteGeneral)}
                        </td>
                        <td>{statutGeneral && <span className={statutGeneral.pill}>{statutGeneral.libelle}</span>}</td>
                      </tr>
                    )}
                    {general && estOuverte(general.id) &&
                      paiementGeneral.map((p) => (
                        <LigneReglement key={`${general.id}-p-${p.id}`} paiement={p} nomClient={nomClient} />
                      ))}
                    {enfants.map((enfant) => {
                      const payeEnfant = enfant.montantPaye ?? sommePaiements(enfant.paiements)
                      const montantEnfant = enfant.montant ?? 0
                      const resteEnfant = Math.max(0, montantEnfant - payeEnfant)
                      const statutEnfant = statutFacture(montantEnfant, payeEnfant)
                      const aPaiements = enfant.paiements.length > 0
                      const designation = enfant.type === 'ABONNEMENT_MENSUEL'
                        ? `${enfant.objet ?? 'Abonnement'}${enfant.mois ? ` (${formatMois(enfant.mois)})` : ''}`
                        : enfant.objet ?? '—'
                      const triPaiements = [...enfant.paiements].sort((a, b) => {
                        const da = a.datePaiement ?? ''
                        const db = b.datePaiement ?? ''
                        const ordre = da < db ? -1 : da > db ? 1 : 0
                        return tri === 'desc' ? -ordre : ordre
                      })
                      return (
                        <Fragment key={enfant.id}>
                          <tr>
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
                              {enfant.referenceFactureGenerale && (
                                <span className="facture-sousligne">Générale {enfant.referenceFactureGenerale}</span>
                              )}
                            </td>
                            <td><span className={TYPE_PILL[enfant.type] ?? 'pill wt'}>● {TYPE_LABEL[enfant.type] ?? enfant.type}</span></td>
                            <td className="text-secondary">{nomClient}</td>
                            <td>
                              {designation}
                              {enfant.referenceDemande && (
                                <span className="facture-sousligne">Demande {enfant.referenceDemande}</span>
                              )}
                            </td>
                            <td className="cell-number">{formatMontantMGA(montantEnfant)}</td>
                            <td className="cell-number">{formatMontantMGA(payeEnfant)}</td>
                            <td className="cell-number" style={resteEnfant > 0 ? { color: 'var(--danger)' } : undefined}>
                              {formatMontantMGA(resteEnfant)}
                            </td>
                            <td><span className={statutEnfant.pill}>{statutEnfant.libelle}</span></td>
                          </tr>
                          {estOuverte(enfant.id) &&
                            triPaiements.map((p) => (
                              <LigneReglement key={`${enfant.id}-p-${p.id}`} paiement={p} nomClient={nomClient} />
                            ))}
                        </Fragment>
                      )
                    })}
                  </Fragment>
                )
              })}
              {groupes.length === 0 && (
                <tr>
                  <td colSpan={11} className="text-muted">
                    {aFiltres ? 'Aucune facture ne correspond aux filtres.'
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
                </tr>
              </tfoot>
            )}
          </table>
        </div>
        {groupes.length > 0 && (
          <div className="table-footer">
            <span>
              {groupes.length} groupe{groupes.length > 1 ? 's' : ''} de facturation
            </span>
            <div className="pagination">
              {Array.from({ length: totalPages }).map((_, i) => (
                <button
                  key={i}
                  type="button"
                  className={`pagination-item ${i === page ? 'active' : ''}`}
                  onClick={() => setPage(i)}
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
      <td>
        {libelleMode}
        {paiement.commentaire && paiement.commentaire.trim() && (
          <span className="facture-sousligne">{paiement.commentaire}</span>
        )}
      </td>
      <td className="text-secondary">—</td>
      <td className="cell-number">{formatMontantMGA(montant)}</td>
      <td className="text-secondary">—</td>
      <td><span className="pill ok">Encaissé</span></td>
    </tr>
  )
}

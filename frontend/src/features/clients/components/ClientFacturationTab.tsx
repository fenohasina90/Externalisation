import { useMemo, useState } from 'react'
import { Alert, Spinner } from '@/components/ui'
import { Can } from '@/components/shared/Can'
import { useApi } from '@/hooks/useApi'
import { useTaillePaginationFacturation } from '@/hooks/useTaillePaginationFacturation'
import { useDebounce } from '@/hooks/useDebounce'
import { formatDate, formatMontantMGA } from '@/utils/format'
import { telechargerBlob } from '@/utils/telechargement'
import { clientsApi } from '../services/clients.service'
import type { FactureFacturation, PaiementFacturation, ObjetLigneFacture } from '../types'
import { codeSociete } from '../types'

type Tri = 'desc' | 'asc'

function strlower(v: string): string {
  return (v ?? '').toLowerCase()
}

/** Préfixe lisible du type d'une ligne d'objet (« Prestation : … »). */
const OBJET_PREFIX: Record<string, string> = {
  PRESTATION: 'Prestation',
  DEBOURS: 'Débours',
  ABONNEMENT_MENSUEL: 'Abonnement',
}

function formatMois(mois: string | null | undefined): string | null {
  if (!mois) return null
  const parsed = new Date(`${mois}-01T00:00:00`)
  if (Number.isNaN(parsed.getTime())) return mois
  const texte = parsed.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' })
  return texte.charAt(0).toUpperCase() + texte.slice(1)
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

type LigneAplatie =
  | { kind: 'facture'; key: string; facture: FactureFacturation }
  | { kind: 'reglement'; key: string; paiement: PaiementFacturation }

/**
 * Designation de la facture : toutes les lignes facturées, une par ligne,
 * préfixées par leur type (« Prestation », « Abonnement », « Débours »).
 */
function renderObjets(f: FactureFacturation): { lignes: ObjetLigneFacture[] | null } {
  const objets = f.objets ?? []
  return { lignes: objets.length > 0 ? objets : null }
}

function designationFacture(f: FactureFacturation): string {
  const objets = f.objets ?? []
  if (objets.length > 0) {
    return objets
      .map((o) => `${OBJET_PREFIX[o.type] ?? o.type} : ${o.libelle}`)
      .join('\n')
  }
  // Repli (anciennes fiches) : désignation simple + mois éventuel.
  const mois = formatMois(f.mois)
  if (mois) {
    return `${f.objet ?? '—'} (${mois})`
  }
  return f.objet ?? '—'
}

function ObjetsDesignation({ facture }: { facture: FactureFacturation }) {
  const { lignes } = renderObjets(facture)
  if (!lignes) {
    return <>{designationFacture(facture)}</>
  }
  return (
    <span className="facture-designation">
      {lignes.map((o, i) => (
        <span key={`${facture.id}-${i}`} className="facture-designation__ligne">
          <span className={`objet-prefix objet-prefix--${strlower(o.type)}`}>
            {`${OBJET_PREFIX[o.type] ?? o.type} : `}
          </span>
          <span>{o.libelle}</span>
        </span>
      ))}
    </span>
  )
}

/** Onglet « Facturation » de la fiche client : toutes les factures validées du client. */
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
  const [page, setPage] = useState(0)
  const [erreurPdf, setErreurPdf] = useState<string | null>(null)
  const [pleinEcran, setPleinEcran] = useState(false)
  const [showFilters, setShowFilters] = useState(true)

  const terme = useDebounce(recherche, 300)
  const PAGE_SIZE_LIGNES = useTaillePaginationFacturation()

  const factures = useMemo(() => data ?? [], [data])

  const liste = useMemo(() => {
    const inRange = (d: string | null | undefined): boolean => {
      if (!dateDebut && !dateFin) return true
      if (!d) return false
      if (dateDebut && d < dateDebut) return false
      if (dateFin && d > dateFin) return false
      return true
    }
    const termeLocal = terme.trim().toLowerCase()
    const matchRecherche = (f: FactureFacturation): boolean => {
      if (!termeLocal) return true
      const hay = [
        f.referenceFactureValidee ?? '',
        f.referenceDemande ?? '',
        ...f.paiements.map((p) => p.referencePaiement ?? ''),
        ...(f.objets ?? []).map((o) => o.libelle ?? ''),
      ].join(' ').toLowerCase()
      return hay.includes(termeLocal)
    }
    const cmp = (a: string | null | undefined, b: string | null | undefined): number => {
      const sa = a ?? ''
      const sb = b ?? ''
      if (sa === sb) return 0
      const ordre = sa < sb ? -1 : 1
      return tri === 'desc' ? -ordre : ordre
    }
    return factures
      .filter((f) => inRange(f.dateFactureValidee))
      .filter(matchRecherche)
      .sort((a, b) => cmp(a.dateFactureValidee, b.dateFactureValidee))
  }, [factures, dateDebut, dateFin, terme, tri])

  const estOuverte = (id: number): boolean => ouverts.has(id)
  const toggle = (id: number): void =>
    setOuverts((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  const toutesFvAvecPaiements = useMemo(
    () => liste.filter((f) => f.paiements.length > 0),
    [liste],
  )
  const toutEstDeplie = toutesFvAvecPaiements.length > 0
    && toutesFvAvecPaiements.every((f) => ouverts.has(f.id))
  const deplierTout = (): void => setOuverts(new Set(toutesFvAvecPaiements.map((f) => f.id)))
  const replierTout = (): void => setOuverts(new Set())

  const lignesAffichees = useMemo(() => {
    const lignes: LigneAplatie[] = []
    for (const facture of liste) {
      lignes.push({
        kind: 'facture', key: `f-${facture.id}`, facture,
      })
      if (!estOuverte(facture.id)) {
        const triesPaiements = [...facture.paiements].sort((a, b) => {
          const da = a.datePaiement ?? ''
          const db = b.datePaiement ?? ''
          const ordre = da < db ? -1 : da > db ? 1 : 0
          return tri === 'desc' ? -ordre : ordre
        })
        for (const p of triesPaiements) {
          lignes.push({ kind: 'reglement', key: `rp-${facture.id}-${p.id}`, paiement: p })
        }
      }
    }
    return lignes
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [liste, ouverts, tri])

  const totalPages = Math.max(1, Math.ceil(lignesAffichees.length / PAGE_SIZE_LIGNES))
  const pageActuelle = Math.min(page, totalPages - 1)
  const lignesPage = lignesAffichees.slice(
    pageActuelle * PAGE_SIZE_LIGNES,
    pageActuelle * PAGE_SIZE_LIGNES + PAGE_SIZE_LIGNES,
  )

  const totaux = useMemo(
    () => liste.reduce((acc, f) => {
      acc.montant += f.montant ?? 0
      acc.paye += f.montantPaye ?? sommePaiements(f.paiements)
      return acc
    }, { montant: 0, paye: 0 }),
    [liste],
  )
  const resteTotal = Math.max(0, totaux.montant - totaux.paye)

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
               placeholder="Réf. demande, facture, paiement ou service…"
               onChange={(e) => onRecherche(e.target.value)} />
      </div>
      <div className="field filter-field">
        <label htmlFor="fact-date-debut">Date facture (début)</label>
        <input id="fact-date-debut" type="date" value={dateDebut}
               onChange={(e) => onDateDebut(e.target.value)} />
      </div>
      <div className="field filter-field">
        <label htmlFor="fact-date-fin">Date facture (fin)</label>
        <input id="fact-date-fin" type="date" value={dateFin}
               onChange={(e) => onDateFin(e.target.value)} />
      </div>
      <div className="field filter-field">
        <label htmlFor="fact-tri">Ordre de la date de facture</label>
        <select id="fact-tri" value={tri} onChange={(e) => onTri(e.target.value as Tri)}>
          <option value="desc">Décroissante (récente en haut)</option>
          <option value="asc">Croissante (ancienne en haut)</option>
        </select>
      </div>
      <div className="filter-actions filter-actions--bottom">
        <button type="button" className="btn-ghost" disabled={!aFiltres && ouverts.size === 0}
                onClick={() => { setDateDebut(''); setDateFin(''); setRecherche(''); setTri('desc'); setOuverts(new Set()) }}>
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
                if (ligne.kind === 'reglement') {
                  return <LigneReglement key={ligne.key} paiement={ligne.paiement} nomClient={nomClient} />
                }
                const { facture } = ligne
                const montant = facture.montant ?? 0
                const paye = facture.montantPaye ?? sommePaiements(facture.paiements)
                const reste = Math.max(0, montant - paye)
                const statut = statutFacture(montant, paye)
                const ouverte = estOuverte(facture.id)
                const aPaiements = facture.paiements.length > 0
                return (
                  <tr key={ligne.key}>
                    <td className="text-secondary">
                      {aPaiements && (
                        <button type="button" className="expand-toggle"
                                onClick={() => toggle(facture.id)}
                                aria-expanded={!ouverte}
                                aria-label={`${ouverte ? 'Replier' : 'Déplier'} les paiements de ${facture.referenceFactureValidee ?? 'la facture'}`}>
                          {ouverte ? '▸' : '▾'}
                        </button>
                      )}
                      {formatDate(facture.dateFactureValidee)}
                    </td>
                    <td className="fw-700">{facture.referenceFactureValidee ?? '—'}</td>
                    <td className="text-secondary">—</td>
                    <td className="text-secondary">{nomClient}</td>
                    <td><ObjetsDesignation facture={facture} /></td>
                    <td className="cell-number">{formatMontantMGA(montant)}</td>
                    <td className="cell-number">{formatMontantMGA(paye)}</td>
                    <td className="cell-number" style={reste > 0 ? { color: 'var(--danger)' } : undefined}>
                      {formatMontantMGA(reste)}
                    </td>
                    <td><span className={statut.pill}>{statut.libelle}</span></td>
                    <td className="text-secondary">—</td>
                  </tr>
                )
              })}
              {lignesAffichees.length === 0 && (
                <tr>
                  <td colSpan={10} className="text-muted">
                    Aucune facture ne correspond aux filtres appliqués.
                  </td>
                </tr>
              )}
            </tbody>
            {lignesAffichees.length > 0 && (
              <tfoot>
                <tr>
                  <td colSpan={5}>Totaux (vue courante)</td>
                  <td className="cell-number fw-700">{formatMontantMGA(totaux.montant)}</td>
                  <td className="cell-number fw-700">{formatMontantMGA(totaux.paye)}</td>
                  <td className="cell-number fw-700" style={resteTotal > 0 ? { color: 'var(--danger)' } : undefined}>
                    {formatMontantMGA(resteTotal)}
                  </td>
                  <td />
                  <td />
                </tr>
              </tfoot>
            )}
          </table>
        </div>
      </div>
      {totalPages > 1 && (
        <div className="table-footer">
          <span>
            Affichage {pageActuelle * PAGE_SIZE_LIGNES + 1} à{' '}
            {Math.min(lignesAffichees.length, pageActuelle * PAGE_SIZE_LIGNES + PAGE_SIZE_LIGNES)} sur{' '}
            {lignesAffichees.length} ligne{lignesAffichees.length > 1 ? 's' : ''}
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
      <td><span className="pill reglement">● Règlement</span></td>
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

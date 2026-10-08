import { useMemo, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useAuth } from '@/features/auth/hooks/useAuth'
import {
  ChangerStatutModal,
  DEMANDE_FORM_VIDE,
  DemandeForm,
  DemandeStepper,
  PaiementPill,
  StatutDemandePill,
  versDateFR,
  type DemandeFormValues,
} from '../components'
import { getDemandeDetail, type DocumentDetail } from '../data/mockDemandeDetail'
import { STATUT_LABEL, type StatutDemandeUI } from '../data/mockDemandes'

type Onglet = 'informations' | 'historique' | 'paiement' | 'documents'

function versISO(dateFR: string): string {
  const [j, m, a] = dateFR.split('/')
  return `${a}-${m}-${j}`
}

function formatMGA(n: number): string {
  return `${n.toLocaleString('fr-FR')} MGA`
}

/** Détail d'une demande : informations, historique, paiement, documents. */
export function DemandeDetailPage() {
  const { reference } = useParams<{ reference: string }>()
  const { utilisateur } = useAuth()
  const nomUtilisateur = utilisateur ? `${utilisateur.prenom} ${utilisateur.nom}` : 'Utilisateur'
  const fichierRef = useRef<HTMLInputElement>(null)

  const initial = useMemo(() => getDemandeDetail(reference ?? ''), [reference])
  const [demande, setDemande] = useState(() => initial?.demande ?? null)
  const [historique, setHistorique] = useState(() => initial?.historique ?? [])
  const [documents, setDocuments] = useState(() => initial?.documents ?? [])
  const [fichiers, setFichiers] = useState<Record<string, File>>({})
  const [onglet, setOnglet] = useState<Onglet>('informations')
  const [editOpen, setEditOpen] = useState(false)
  const [statutOpen, setStatutOpen] = useState(false)

  // Échéances fixes de la maquette pour REF-2026-014 (montants figés).
  const montantPaye = initial?.montantPaye ?? 0

  if (!demande || !initial) {
    return (
      <div className="app-page responsables-page">
        <div className="alert alert-danger">Demande introuvable.</div>
        <Link to="/demandes" className="btn-ghost" style={{ alignSelf: 'flex-start' }}>
          Retour à la liste
        </Link>
      </div>
    )
  }

  const resteAPayer = Math.max(0, demande.montantTotal - montantPaye)
  const progression = demande.montantTotal === 0 ? 0 : Math.round((montantPaye / demande.montantTotal) * 100)

  const ajouterHistorique = (type: 'Modification' | 'Statut', texte: string, commentaire = '—') => {
    const now = new Date()
    const dateHeure = `${now.toLocaleDateString('fr-FR')} ${now.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })}`
    setHistorique((h) => [{ dateHeure, type, texte, utilisateur: nomUtilisateur, commentaire }, ...h])
  }

  const handleEdit = (v: DemandeFormValues) => {
    setDemande((d) =>
      d
        ? {
            ...d,
            client: v.client,
            responsable: v.responsable,
            commercial: v.commercial,
            domaine: v.domaine,
            service: v.service,
            dateDemande: versDateFR(v.dateDemande),
            dateDebut: versDateFR(v.debutService),
            dateFin: versDateFR(v.finService),
            tarif: v.montantTotal,
            quantite: 1,
            montantTotal: v.montantTotal,
            commentaire: v.commentaire,
          }
        : d,
    )
    ajouterHistorique('Modification', 'Informations de la demande mises à jour')
    setEditOpen(false)
  }

  const handleStatut = (nouveau: StatutDemandeUI, commentaire: string) => {
    setDemande((d) => (d ? { ...d, statut: nouveau } : d))
    ajouterHistorique(
      'Statut',
      `${STATUT_LABEL[demande.statut]} → ${STATUT_LABEL[nouveau]}${nouveau === 'TERMINEE' || nouveau === 'ANNULEE' ? ' (manuel)' : ' (automatique)'}`,
      commentaire || '—',
    )
    setStatutOpen(false)
  }

  const handleFichiers = (files: FileList | null) => {
    if (!files) return
    const now = new Date().toLocaleDateString('fr-FR')
    const ajouts: DocumentDetail[] = []
    const map: Record<string, File> = {}
    for (const f of Array.from(files)) {
      const ext = f.name.split('.').pop()?.toUpperCase() ?? 'FICHIER'
      ajouts.push({ nom: f.name, type: ext, taille: formatTaille(f.size), dateAjout: now, utilisateur: nomUtilisateur })
      map[f.name] = f
    }
    setDocuments((d) => [...ajouts, ...d])
    setFichiers((m) => ({ ...m, ...map }))
  }

  const telecharger = (doc: DocumentDetail) => {
    const fichier = fichiers[doc.nom]
    const url = fichier
      ? URL.createObjectURL(fichier)
      : URL.createObjectURL(new Blob([`Document de démonstration : ${doc.nom}`], { type: 'text/plain;charset=utf-8' }))
    const a = document.createElement('a')
    a.href = url
    a.download = doc.nom
    a.click()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  }

  const editInitial: DemandeFormValues = {
    ...DEMANDE_FORM_VIDE,
    client: demande.client,
    responsable: demande.responsable,
    commercial: demande.commercial,
    domaine: demande.domaine,
    service: demande.service,
    dateDemande: versISO(demande.dateDemande),
    debutService: versISO(demande.dateDebut),
    finService: versISO(demande.dateFin),
    montantTotal: demande.montantTotal,
    commentaire: demande.commentaire,
  }

  return (
    <div className="app-page responsables-page">
      <header className="page-header page-header-sticky">
        <div>
          <h1>
            {demande.reference} <StatutDemandePill statut={demande.statut} />
          </h1>
          <p>
            {demande.client} — {demande.service}
          </p>
        </div>
        <div className="actions">
          <button type="button" className="btn-ghost" onClick={() => setEditOpen(true)}>
            Modifier
          </button>
          <button type="button" className="btn-primary" onClick={() => setStatutOpen(true)}>
            Changer le statut
          </button>
        </div>
      </header>

      <DemandeStepper statut={demande.statut} />

      <nav className="tabs" aria-label="Sections de la demande">
        {(
          [
            ['informations', 'Informations'],
            ['historique', 'Historique'],
            ['paiement', 'Paiement'],
            ['documents', 'Documents'],
          ] as Array<[Onglet, string]>
        ).map(([id, label]) => (
          <button
            key={id}
            type="button"
            role="tab"
            aria-selected={onglet === id}
            className={`tab${onglet === id ? ' tab--active' : ''}`}
            onClick={() => setOnglet(id)}
          >
            {label}
          </button>
        ))}
      </nav>

      {onglet === 'informations' && (
        <>
          <div className="detail-cards">
            <section className="card card-pad">
              <h3>Demande</h3>
              <div className="info-grid">
                <div className="info-item">
                  <span>Référence</span>
                  <b>{demande.reference}</b>
                </div>
                <div className="info-item">
                  <span>Client</span>
                  <b>{demande.client}</b>
                </div>
                <div className="info-item">
                  <span>Responsable</span>
                  <b>{demande.responsable}</b>
                </div>
                <div className="info-item">
                  <span>Commercial</span>
                  <b>{demande.commercial}</b>
                </div>
                <div className="info-item">
                  <span>Service</span>
                  <b>{demande.service}</b>
                </div>
                <div className="info-item">
                  <span>Domaine</span>
                  <b>{demande.domaine}</b>
                </div>
              </div>
            </section>

            <section className="card card-pad">
              <h3>Prestation et montant</h3>
              <div className="info-grid">
                <div className="info-item">
                  <span>Période</span>
                  <b>
                    {demande.dateDebut} → {demande.dateFin}
                  </b>
                </div>
                <div className="info-item">
                  <span>Tarif</span>
                  <b>{formatMGA(demande.tarif)}</b>
                </div>
                <div className="info-item">
                  <span>Quantité</span>
                  <b>{demande.quantite}</b>
                </div>
                <div className="info-item">
                  <span>Montant total</span>
                  <b>{formatMGA(demande.montantTotal)}</b>
                </div>
                <div className="info-item">
                  <span>Statut actuel</span>
                  <b>
                    <StatutDemandePill statut={demande.statut} />
                  </b>
                </div>
                <div className="info-item info-item--full">
                  <span>Commentaire</span>
                  <b>{demande.commentaire || '—'}</b>
                </div>
              </div>
            </section>
          </div>
          <div className="nt">
            <strong>Tarif figé</strong> : enregistré à la création de la demande, il ne change
            plus si le tarif du service évolue.
          </div>
        </>
      )}

      {onglet === 'historique' && (
        <section className="card">
          <div className="table-scroll-container no-global-scroll">
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Date et heure</th>
                    <th>Événement</th>
                    <th>Utilisateur</th>
                    <th>Commentaire</th>
                  </tr>
                </thead>
                <tbody>
                  {historique.map((h, i) => (
                    <tr key={i}>
                      <td className="text-secondary">{h.dateHeure}</td>
                      <td>
                        <span
                          className={
                            h.type === 'Création'
                              ? 'pill ok'
                              : h.type === 'Statut'
                                ? 'pill info'
                                : 'pill wt'
                          }
                        >
                          ● {h.type}
                        </span>{' '}
                        {h.texte}
                      </td>
                      <td>{h.utilisateur}</td>
                      <td className="text-secondary">{h.commentaire}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </section>
      )}

      {onglet === 'paiement' && (
        <>
          <section className="card card-pad">
            <div className="info-grid info-grid--5">
              <div className="info-item">
                <span>Plan de paiement</span>
                <b>{initial.echeances.length} échéances</b>
              </div>
              <div className="info-item">
                <span>Montant total</span>
                <b>{formatMGA(demande.montantTotal)}</b>
              </div>
              <div className="info-item">
                <span>Montant payé</span>
                <b style={{ color: 'var(--success)' }}>{formatMGA(montantPaye)}</b>
              </div>
              <div className="info-item">
                <span>Reste à payer</span>
                <b style={{ color: 'var(--danger)' }}>{formatMGA(resteAPayer)}</b>
              </div>
              <div className="info-item">
                <span>Statut de paiement</span>
                <b>
                  <PaiementPill situation={resteAPayer === 0 ? 'Payé' : 'Partiel'} />
                </b>
              </div>
            </div>
            <div className="progression">
              <div className="trk" role="progressbar" aria-valuenow={progression} aria-valuemin={0} aria-valuemax={100}>
                <i style={{ width: `${progression}%` }} />
              </div>
            </div>
          </section>

          <section className="card">
            <div className="card-pad" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h3 style={{ margin: 0 }}>Échéances</h3>
              <Link to="/echeances" className="lk">
                Voir l&apos;échéancier complet ›
              </Link>
            </div>
            <div className="table-scroll-container no-global-scroll">
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>N°</th>
                      <th>Date d&apos;échéance</th>
                      <th>Montant attendu</th>
                      <th>Montant payé</th>
                      <th>Reste</th>
                      <th>Statut</th>
                    </tr>
                  </thead>
                  <tbody>
                    {initial.echeances.map((e) => {
                      const reste = e.montantAttendu - e.montantPaye
                      return (
                        <tr key={e.numero}>
                          <td>{e.numero}</td>
                          <td className="text-secondary">{e.dateEcheance}</td>
                          <td className="cell-number">{formatMGA(e.montantAttendu)}</td>
                          <td className="cell-number">{formatMGA(e.montantPaye)}</td>
                          <td className="cell-number">{formatMGA(reste)}</td>
                          <td>
                            {reste === 0 ? (
                              <span className="pill ok">● Payée</span>
                            ) : e.montantPaye > 0 ? (
                              <span className="pill warning">● Partielle</span>
                            ) : (
                              <span className="pill wt">● À venir</span>
                            )}
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        </>
      )}

      {onglet === 'documents' && (
        <section className="card">
          <div className="card-pad" style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <input
              ref={fichierRef}
              type="file"
              multiple
              accept=".pdf,.jpg,.jpeg,.png,.docx"
              style={{ display: 'none' }}
              onChange={(e) => {
                handleFichiers(e.target.files)
                e.target.value = ''
              }}
            />
            <button type="button" className="btn-primary" onClick={() => fichierRef.current?.click()}>
              <svg className="sv" viewBox="0 0 24 24">
                <path d="M12 5v14M5 12h14" />
              </svg>
              Ajouter un document
            </button>
          </div>
          <div className="table-scroll-container no-global-scroll">
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Nom</th>
                    <th>Type</th>
                    <th>Taille</th>
                    <th>Date d&apos;ajout</th>
                    <th>Utilisateur</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {documents.map((doc) => (
                    <tr key={doc.nom}>
                      <td className="fw-600">{doc.nom}</td>
                      <td className="text-secondary">{doc.type}</td>
                      <td className="text-secondary">{doc.taille}</td>
                      <td className="text-secondary">{doc.dateAjout}</td>
                      <td>{doc.utilisateur}</td>
                      <td>
                        <div className="table-actions">
                          <button type="button" className="btn-link success" onClick={() => telecharger(doc)}>
                            Télécharger
                          </button>
                          <button
                            type="button"
                            className="btn-link danger"
                            onClick={() => setDocuments((d) => d.filter((x) => x.nom !== doc.nom))}
                          >
                            Supprimer
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                  {documents.length === 0 && (
                    <tr>
                      <td colSpan={6} className="text-muted">
                        Aucun document pour cette demande.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
            <div className="table-footer">
              <span>Suppression selon les permissions de l&apos;utilisateur.</span>
            </div>
          </div>
        </section>
      )}

      {editOpen && (
        <div className="modal-backdrop" onClick={() => setEditOpen(false)}>
          <div
            className="modal-card"
            style={{ maxWidth: 720 }}
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-label="Modifier la demande"
          >
            <header className="modal-header">
              <div>
                <h3 className="modal-title">Modifier la demande {demande.reference}</h3>
              </div>
              <button type="button" className="btn-ghost" onClick={() => setEditOpen(false)} aria-label="Fermer">
                ✕
              </button>
            </header>
            <div className="modal-body">
              <DemandeForm
                initial={editInitial}
                submitLabel="Enregistrer"
                onCancel={() => setEditOpen(false)}
                onSubmit={handleEdit}
              />
            </div>
          </div>
        </div>
      )}

      <ChangerStatutModal
        open={statutOpen}
        demande={demande}
        onClose={() => setStatutOpen(false)}
        onConfirm={handleStatut}
      />
    </div>
  )
}

function formatTaille(octets: number): string {
  if (octets < 1024) return `${octets} o`
  const ko = octets / 1024
  if (ko < 1024) return `${Math.round(ko)} Ko`
  return `${(ko / 1024).toLocaleString('fr-FR', { maximumFractionDigits: 1 })} Mo`
}

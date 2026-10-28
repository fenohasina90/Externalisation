import { useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { FormEtapes } from '@/components/shared/FormEtapes'
import {
  RecapitulatifForm,
  type SectionRecapitulatif,
} from '@/components/shared/RecapitulatifForm'
import { Autocomplete } from '@/components/ui'
import { useApi } from '@/hooks/useApi'
import { formatDate, formatMontantMGA } from '@/utils/format'
import type { ApiPage } from '@/types'
import type { CreateSocietePayload, Societe } from '@/features/clients/types'
import type { Commercial } from '@/features/commerciaux/types'
import type { Responsable } from '@/features/responsables/types'
import {
  estTypeServiceSolo,
  type Domaine,
  type Service,
} from '@/features/services/types'
import { clientsApi } from '@/features/clients/services/clients.service'
import { commerciauxApi } from '@/features/commerciaux/services/commerciaux.service'
import { responsablesApi } from '@/features/responsables/services/responsables.service'
import { domainesApi, servicesApi } from '@/features/services/services/services.service'
import { ClientFormModal } from '@/features/clients/components/ClientFormModal'
import { ResponsableFormModal } from '@/features/responsables/components/ResponsableFormModal'
import { ServiceFormModal } from '@/features/services/components/ServiceFormModal'
import { SelectionFichiers, documentsApi } from '@/features/documents'
import type { ModeFacturation } from '../types'
import { demandesApi } from '../services/demandes.service'

const OPTION_CREATION = 'creation'
const LABEL_CREATION = "Création d'entreprise (sans société)"
const OPTION_SERVICE_AUTRE = 'autre'

interface LigneSaisie {
  serviceId: number | ''
  domaineId: string
  tarif: string
  debutService: string
  finService: string
}

const LIGNE_VIDE: LigneSaisie = {
  serviceId: '',
  domaineId: '',
  tarif: '',
  debutService: '',
  finService: '',
}

/**
 * Nouvelle demande multi-services branchée sur l'API :
 * étape 1 (client, responsable, commercial, date, mode de facturation),
 * étape 2 (une ou plusieurs lignes service + tarif + période).
 */
export function NouvelleDemandePage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()

  const [societeId, setSocieteId] = useState<number | null>(
    params.get('societeId') ? Number(params.get('societeId')) : null,
  )
  const [clientTexte, setClientTexte] = useState('')
  const [creationSansSociete, setCreationSansSociete] = useState(false)
  const [responsableId, setResponsableId] = useState<number | null>(null)
  const [responsableTexte, setResponsableTexte] = useState('')
  const [commercialId, setCommercialId] = useState<number | ''>('')
  const [domaineId, setDomaineId] = useState('')
  const [dateDemande, setDateDemande] = useState(() => new Date().toISOString().slice(0, 10))
  const [commentaire, setCommentaire] = useState('')
  const [mode, setMode] = useState<ModeFacturation>('PAR_SERVICE')
  const [lignes, setLignes] = useState<LigneSaisie[]>([{ ...LIGNE_VIDE }])
  const [erreur, setErreur] = useState<string | null>(null)
  const [enregistrement, setEnregistrement] = useState(false)
  const [fichiers, setFichiers] = useState<File[]>([])
  const [demandeCreee, setDemandeCreee] = useState<{ id: number; reference: string } | null>(null)

  const { data: societes, reload: reloadSocietes } = useApi<ApiPage<Societe>>(
    () => clientsApi.list({ page: 0, size: 100, recherche: null }, {}, { champ: 'nomSociete', sens: 'asc' }),
    [],
  )
  const { data: commerciaux } = useApi<ApiPage<Commercial>>(
    () => commerciauxApi.list({ page: 0, size: 100, recherche: null }, { actif: true }),
    [],
  )
  const { data: pageResponsables, reload: reloadResponsables } = useApi<ApiPage<Responsable>>(
    () => responsablesApi.list({ page: 0, size: 100, recherche: null }, { actif: true }),
    [],
  )
  const { data: pageDomaines, reload: reloadDomaines } = useApi<ApiPage<Domaine>>(
    () => domainesApi.list({ page: 0, size: 100, recherche: null }, {}),
    [],
  )
  const { data: pageServices, reload: reloadServices } = useApi<ApiPage<Service>>(
    () => servicesApi.list({ page: 0, size: 100, recherche: null }, { actif: true }),
    [],
  )

  const nomsServices = useMemo(
    () => new Map((pageServices?.content ?? []).map((s) => [s.id, s.nomService])),
    [pageServices],
  )
  const typesServices = useMemo(
    () => new Map((pageServices?.content ?? []).map((s) => [s.id, s.typeService])),
    [pageServices],
  )

  const total = lignes.reduce((somme, l) => somme + (Number(l.tarif) || 0), 0)

  const optionsClients = useMemo(
    () => [
      ...(societes?.content ?? []).map((s) => ({ id: s.id, label: s.nomSociete })),
      { id: OPTION_CREATION, label: LABEL_CREATION },
    ],
    [societes],
  )

  // Pré-remplit le nom du client quand ?societeId est fourni (ajustement pendant
  // le rendu, pas dans un effet : pas de rendus en cascade).
  const [texteInit, setTexteInit] = useState(false)
  if (!texteInit && (societeId === null || (societes?.content ?? []).length > 0)) {
    setTexteInit(true)
    if (societeId !== null && clientTexte === '') {
      const trouve = (societes?.content ?? []).find((s) => s.id === societeId)
      if (trouve) setClientTexte(trouve.nomSociete)
    }
  }
  const optionsResponsables = useMemo(() => {
    const tous = pageResponsables?.content ?? []
    const filtres =
      societeId !== null
        ? tous.filter((r) => r.societeId === societeId)
        : creationSansSociete
          ? tous.filter((r) => r.societeId === null)
          : tous
    return filtres.map((r) => ({ id: r.id, label: r.nomClient }))
  }, [pageResponsables, societeId, creationSansSociete])
  const optionsDomaines = useMemo(
    () => (pageDomaines?.content ?? []).map((d) => ({ id: d.id, nom: d.nomDomaine })),
    [pageDomaines],
  )

  const [clientModalOpen, setClientModalOpen] = useState(false)
  const [responsableModalOpen, setResponsableModalOpen] = useState(false)
  const [serviceModalLigne, setServiceModalLigne] = useState<number | null>(null)

  const handleSaveService = async (payload: { nomService: string; domaineId?: number | null }) => {
    const cree = await servicesApi.create({
      nomService: payload.nomService,
      typeService: 'STANDARD',
      domaineId: payload.domaineId ?? null,
    })
    reloadServices()
    if (serviceModalLigne !== null) {
      setLigne(serviceModalLigne, {
        serviceId: cree.id,
        domaineId: payload.domaineId ? String(payload.domaineId) : '',
      })
    }
    setServiceModalLigne(null)
  }

  /** Déduit un code domaine à partir du nom (ex. "Comptabilité" → "COMPT"). */
  const codeDomaineDepuisNom = (nom: string): string => {
    return nom
      .trim()
      .toUpperCase()
      .replace(/[^A-Z0-9]/g, '')
      .slice(0, 6) || 'DOM'
  }

  const resolveDomaine = async (nom: string): Promise<number> => {
    const exact = optionsDomaines.find((d) => d.nom.toLowerCase() === nom.trim().toLowerCase())
    if (exact) return exact.id
    const cree = await domainesApi.create({
      nomDomaine: nom.trim(),
      codeDomaine: codeDomaineDepuisNom(nom),
    })
    reloadDomaines()
    return cree.id
  }

  const texteClientPropre = clientTexte.trim()
  const correspondanceClientExacte =
    texteClientPropre !== '' &&
    optionsClients.some((o) => o.label.toLowerCase() === texteClientPropre.toLowerCase())
  const texteResponsablePropre = responsableTexte.trim()
  const correspondanceResponsableExacte =
    texteResponsablePropre !== '' &&
    optionsResponsables.some((o) => o.label.toLowerCase() === texteResponsablePropre.toLowerCase())

  const choisirClient = (id: number | string | null) => {
    if (id === OPTION_CREATION) {
      setCreationSansSociete(true)
      setSocieteId(null)
      setClientTexte(LABEL_CREATION)
      setResponsableId(null)
      setResponsableTexte('')
    } else if (typeof id === 'number') {
      const trouve = (societes?.content ?? []).find((s) => s.id === id)
      setSocieteId(id)
      setCreationSansSociete(false)
      setClientTexte(trouve?.nomSociete ?? '')
    } else {
      setSocieteId(null)
      setCreationSansSociete(false)
    }
    setErreur(null)
  }

  const choisirResponsable = (id: number | string | null) => {
    if (typeof id === 'number') {
      const trouve = (pageResponsables?.content ?? []).find((r) => r.id === id)
      setResponsableId(id)
      setResponsableTexte(trouve?.nomClient ?? '')
    } else {
      setResponsableId(null)
    }
    setErreur(null)
  }

  const handleSaveClient = async (payload: CreateSocietePayload & { responsable: string }) => {
    const { responsable, ...societe } = payload
    const cree = await clientsApi.create(societe)
    const responsableCree = await responsablesApi.create({ societeId: cree.id, nomClient: responsable })
    reloadSocietes()
    reloadDomaines()
    reloadResponsables()
    setSocieteId(cree.id)
    setCreationSansSociete(false)
    setClientTexte(cree.nomSociete)
    setResponsableId(responsableCree.id)
    setResponsableTexte(responsableCree.nomClient)
    setClientModalOpen(false)
  }

  const handleResponsableCree = async () => {
    setResponsableModalOpen(false)
    const cherche = responsableTexte.trim()
    try {
      const page = await responsablesApi.list(
        { page: 0, size: 5, recherche: cherche || null },
        { actif: true },
      )
      const trouve = page.content.find(
        (r) => r.nomClient.toLowerCase() === cherche.toLowerCase(),
      )
      if (trouve) {
        setResponsableId(trouve.id)
        setResponsableTexte(trouve.nomClient)
      }
    } catch {
      // La liste sera rechargée ; sélection manuelle sinon.
    }
    reloadResponsables()
  }

  // Services déjà choisis : masqués des autres selects (pas de doublon).
  const selectionnes = useMemo(
    () => new Set(lignes.map((l) => l.serviceId).filter((id): id is number => id !== '')),
    [lignes],
  )
  // const aujourdhui = useMemo(() => new Date().toISOString().slice(0, 10), [])

  const setLigne = (index: number, patch: Partial<LigneSaisie>) => {
    setLignes((liste) => liste.map((l, i) => (i === index ? { ...l, ...patch } : l)))
    setErreur(null)
  }

  const validerEtape = (index: number): string | null => {
    if (index === 0) {
      if (!dateDemande) return 'La date de demande est obligatoire'
      if (societeId === null && !creationSansSociete) {
        if (texteClientPropre === '') return null
        return 'Sélectionnez le client, « Création d’entreprise », ou créez la société'
      }
      if (responsableId === null && texteResponsablePropre !== '') {
        return 'Sélectionnez le responsable ou créez-le'
      }
      return null
    }
    if (lignes.length === 0) return 'Ajoutez au moins un service'
    const vus = new Set<number>()
    let typeSolo = false
    for (const [i, l] of lignes.entries()) {
      if (l.serviceId === '') return `Ligne ${i + 1} : service obligatoire`
      if (vus.has(l.serviceId)) return 'Un service ne peut pas être sélectionné deux fois'
      vus.add(l.serviceId)
      if (!l.tarif || Number(l.tarif) < 0 || Number.isNaN(Number(l.tarif))) {
        return `Ligne ${i + 1} : tarif positif ou nul obligatoire`
      }
      if (!l.debutService || !l.finService) return `Ligne ${i + 1} : période obligatoire`
      // if (l.debutService < aujourdhui) {
      //   return `Ligne ${i + 1} : le début doit être postérieur ou égal à aujourd'hui`
      // }
      if (l.finService <= l.debutService) {
        return `Ligne ${i + 1} : la fin doit être strictement postérieure au début`
      }
      if (estTypeServiceSolo(typesServices.get(l.serviceId))) typeSolo = true
    }
    if (typeSolo && lignes.length > 1) {
      return "Un service de création d'entreprise ou de transaction immobilière ne peut pas être combiné avec d'autres services"
    }
    return null
  }

  const handleTerminer = async () => {
    setEnregistrement(true)
    setErreur(null)
    try {
      let cible = demandeCreee
      if (!cible) {
        const payloadLignes = lignes.map((l) => ({
          serviceId: l.serviceId as number,
          domaineId: l.domaineId ? Number(l.domaineId) : null,
          tarif: Number(l.tarif),
          debutService: l.debutService,
          finService: l.finService,
        }))
        const creee = await demandesApi.create({
          societeId,
          clientResponsableId: responsableId,
          commercialId: commercialId === '' ? null : commercialId,
          dateDemande,
          commentaire: commentaire.trim() || null,
          lignes: payloadLignes,
          modeFacturation: mode,
        })
        cible = { id: creee.id, reference: creee.referenceDemande }
        setDemandeCreee(cible)
      }
      for (const fichier of fichiers) {
        await documentsApi.uploadDemande(cible.id, fichier, null)
      }
      const typeSolo = lignes
        .map((l) => typesServices.get(l.serviceId as number))
        .find(estTypeServiceSolo)
      navigate(
        typeSolo === 'CREATION'
          ? `/demandes/${cible.reference}?onglet=dossier`
          : typeSolo === 'TRANSACTION_IMMOBILIERE'
            ? `/demandes/${cible.reference}?onglet=dossier-transaction`
            : `/demandes/${cible.reference}`,
      )
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Échec de la création'
      setErreur(
        demandeCreee
          ? `La demande ${demandeCreee.reference} a été créée, mais l'envoi des pièces jointes a échoué : ${message}`
          : message,
      )
    } finally {
      setEnregistrement(false)
    }
  }

  const commercialLabel =
    (commerciaux?.content ?? []).find((c) => c.id === commercialId)?.nomCommercial ?? ''

  const recapSections: SectionRecapitulatif[] = [
    {
      titre: 'Demande',
      lignes: [
        { label: 'Client', valeur: clientTexte },
        { label: 'Responsable', valeur: responsableTexte },
        { label: 'Commercial', valeur: commercialLabel },
        { label: 'Date', valeur: dateDemande ? formatDate(dateDemande) : '' },
        {
          label: 'Facturation',
          valeur: mode === 'PAR_SERVICE' ? 'Une facture par service' : 'Facture unique',
        },
        {
          label: 'Référence',
          valeur: demandeCreee?.reference ?? '— (générée à l’enregistrement)',
        },
        {
          label: 'Pièces jointes',
          valeur:
            fichiers.length === 0
              ? ''
              : `${fichiers.length} fichier${fichiers.length > 1 ? 's' : ''}`,
        },
      ],
    },
    {
      titre: 'Services',
      lignes: [
        ...lignes.map((ligne, index) => {
          const nom =
            ligne.serviceId !== ''
              ? nomsServices.get(ligne.serviceId as number)
              : undefined
          const tarif =
            ligne.tarif !== '' && !Number.isNaN(Number(ligne.tarif))
              ? formatMontantMGA(Number(ligne.tarif))
              : ''
          const periode =
            ligne.debutService !== '' || ligne.finService !== ''
              ? `${ligne.debutService ? formatDate(ligne.debutService) : '…'} → ${
                  ligne.finService ? formatDate(ligne.finService) : '…'
                }`
              : ''
          return {
            label: nom ?? `Service ${index + 1}`,
            valeur: [tarif, periode].filter(Boolean).join(' · '),
          }
        }),
        { label: 'Montant total', valeur: formatMontantMGA(total) },
      ],
    },
  ]

  return (
    <div className="app-page responsables-page">
      <header className="page-header page-header-sticky">
        <div>
          <h1>Nouvelle demande de service</h1>
          <p>Le statut initial de la demande est « En attente » ; la facture proforma est générée à la création.</p>
        </div>
      </header>

      <div className="split-layout">
        <section className="card card-pad">
          <FormEtapes
            etapes={[
              { titre: 'Demande', description: 'Client, responsable, commercial et facturation.' },
              { titre: 'Services', description: 'Un ou plusieurs services, chacun avec tarif et période.' },
            ]}
            validerEtape={validerEtape}
            onTerminer={() => void handleTerminer()}
            onAnnuler={() => navigate('/demandes')}
            enregistrement={enregistrement}
            erreur={erreur}
            libelleTerminer="Créer la demande"
          >
            {(etape) =>
              etape === 0 ? (
                <div className="form-grid form-grid--2">
                <div className="field">
                  <span className="field-label">Client (société)</span>
                  <Autocomplete
                    value={clientTexte}
                    placeholder="Rechercher une société…"
                    onTextChange={(texte) => {
                      setClientTexte(texte)
                      setSocieteId(null)
                      setCreationSansSociete(false)
                      setErreur(null)
                    }}
                    onPick={(option) => choisirClient(option ? option.id : null)}
                    options={optionsClients}
                    footer={
                      texteClientPropre !== '' && !correspondanceClientExacte ? (
                        <button
                          type="button"
                          className="btn-link"
                          onClick={() => setClientModalOpen(true)}
                        >
                          Créer « {texteClientPropre} » comme nouvelle société
                        </button>
                      ) : undefined
                    }
                  />
                </div>

                <div className="field">
                  <span className="field-label">Responsable</span>
                  <Autocomplete
                    value={responsableTexte}
                    placeholder={
                      societeId !== null
                        ? 'Rechercher un responsable…'
                        : 'Rechercher ou créer un responsable…'
                    }
                    onTextChange={(texte) => {
                      setResponsableTexte(texte)
                      setResponsableId(null)
                      setErreur(null)
                    }}
                    onPick={(option) => choisirResponsable(option ? option.id : null)}
                    options={optionsResponsables}
                    footer={
                      texteResponsablePropre !== '' && !correspondanceResponsableExacte ? (
                        <button
                          type="button"
                          className="btn-link"
                          onClick={() => setResponsableModalOpen(true)}
                        >
                          Créer « {texteResponsablePropre} » comme nouveau responsable
                        </button>
                      ) : undefined
                    }
                  />
                </div>

                  <label className="field">
                    Commercial
                    <select
                      value={commercialId}
                      onChange={(e) => {
                        setCommercialId(e.target.value ? Number(e.target.value) : '')
                        setErreur(null)
                      }}
                    >
                      <option value="">— Aucun —</option>
                      {(commerciaux?.content ?? []).map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.nomCommercial}
                        </option>
                      ))}
                    </select>
                  </label>

                  <label className="field">
                    Date de demande
                    <input
                      type="date"
                      value={dateDemande}
                      onChange={(e) => setDateDemande(e.target.value)}
                    />
                  </label>

                  <div className="field form-grid__full">
                    <span className="field-label">Mode de facturation</span>
                    <div style={{ display: 'flex', gap: 16 }}>
                      <label style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                        <input
                          type="radio"
                          name="mode"
                          checked={mode === 'PAR_SERVICE'}
                          onChange={() => setMode('PAR_SERVICE')}
                        />
                        Une facture par service
                      </label>
                      <label style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                        <input
                          type="radio"
                          name="mode"
                          checked={mode === 'UNIQUE'}
                          onChange={() => setMode('UNIQUE')}
                        />
                        Facture unique
                      </label>
                    </div>
                  </div>

                  <label className="field form-grid__full">
                    Commentaire
                    <input
                      value={commentaire}
                      onChange={(e) => setCommentaire(e.target.value)}
                      placeholder="Précisions éventuelles"
                    />
                  </label>

                  <SelectionFichiers
                    fichiers={fichiers}
                    onChange={setFichiers}
                    desactive={enregistrement}
                  />
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                  {lignes.map((l, i) => (
                    <div key={i} className="card card-pad" style={{ background: 'var(--c-bg)' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <strong>Service {i + 1}</strong>
                        {lignes.length > 1 && (
                          <button
                            type="button"
                            className="btn-ghost"
                            onClick={() => setLignes((liste) => liste.filter((_, j) => j !== i))}
                          >
                            Retirer
                          </button>
                        )}
                      </div>
                      <div className="form-grid form-grid--2" style={{ marginTop: 8 }}>


                        {/* Service (colonne gauche) */}
                        {/* Domaine + Service (côte à côte) */}
                        <label className="field">
                          Domaine
                          <select
                            value={l.domaineId}
                            onChange={(e) => {
                              const val = e.target.value
                              setLigne(i, { domaineId: val })
                              if (val === '') {
                                setLigne(i, { serviceId: '' })
                              }
                            }}
                          >
                            <option value="">Tous les domaines</option>
                            {(pageDomaines?.content ?? []).map((d) => (
                              <option key={d.id} value={String(d.id)}>
                                {d.nomDomaine}
                              </option>
                            ))}
                          </select>
                        </label>

                        <label className="field">
                          Service 
                          {/* <span className="required">*</span> */}
                          <select
                            value={l.serviceId}
                            onChange={(e) => {
                              if (e.target.value === OPTION_SERVICE_AUTRE) {
                                setServiceModalLigne(i)
                                return
                              }
                              setLigne(i, {
                                serviceId: e.target.value ? Number(e.target.value) : '',
                              })
                            }}
                          >
                            <option value="">— Choisir —</option>
                            {(pageServices?.content ?? [])
                              .filter(
                                (s) =>
                                  (l.domaineId === '' || s.domaineId === Number(l.domaineId)) &&
                                  (s.id === l.serviceId || !selectionnes.has(s.id)),
                              )
                              .map((s) => (
                                <option key={s.id} value={s.id}>
                                  {s.nomService}
                                </option>
                              ))}
                            <option value={OPTION_SERVICE_AUTRE}>Autre (créer un service)</option>
                          </select>
                        </label>

                        

                        <label className="field">
                          Tarif (MGA) <span className="required">*</span>
                          <input
                            type="number"
                            min={0}
                            value={l.tarif}
                            onChange={(e) => setLigne(i, { tarif: e.target.value })}
                            placeholder="Ex. 2000000"
                          />
                        </label>
                        <span className="field" />
                        <label className="field">
                          Début <span className="required">*</span>
                          <input
                            type="date"
                            // min={aujourdhui}
                            value={l.debutService}
                            onChange={(e) => setLigne(i, { debutService: e.target.value })}
                          />
                        </label>
                        <label className="field">
                          Fin <span className="required">*</span>
                          <input
                            type="date"
                            min={l.debutService}
                            value={l.finService}
                            onChange={(e) => setLigne(i, { finService: e.target.value })}
                          />
                        </label>
                      </div>
                    </div>
                  ))}
                  <button
                    type="button"
                    className="btn-ghost"
                    style={{ alignSelf: 'flex-start' }}
                    onClick={() => setLignes((liste) => [...liste, { ...LIGNE_VIDE }])}
                  >
                    + Ajouter un service
                  </button>
                </div>
              )
            }
          </FormEtapes>
        </section>

        <RecapitulatifForm
          sections={recapSections}
          note={
            <>
              <strong>Montants figés</strong> : les factures proforma sont générées à la
              création ; les échéances, paiements et rapports s&apos;appuient sur ces montants.
            </>
          }
        />
      </div>

      <ClientFormModal
        open={clientModalOpen}
        client={null}
        domaines={optionsDomaines}
        resolveDomaine={resolveDomaine}
        onCreerDomaine={resolveDomaine}
        onClose={() => setClientModalOpen(false)}
        onSave={handleSaveClient}
      />

      <ResponsableFormModal
        open={responsableModalOpen}
        responsable={null}
        societeDefaut={
          societeId !== null
            ? {
                id: societeId,
                nom:
                  (societes?.content ?? []).find((s) => s.id === societeId)?.nomSociete ?? '',
              }
            : null
        }
        onClose={() => setResponsableModalOpen(false)}
        onSaved={() => void handleResponsableCree()}
      />

      <ServiceFormModal
        open={serviceModalLigne !== null}
        service={null}
        domaines={pageDomaines?.content ?? []}
        onClose={() => setServiceModalLigne(null)}
        onSave={(payload) =>
          handleSaveService({
            nomService: payload.nomService,
            domaineId: payload.domaineId,
          })
        }
      />
    </div>
  )
}

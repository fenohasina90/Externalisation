import { buildQuery, http } from '@/api/http'
import type { ApiPage, PageQuery } from '@/types'
import type {
  ChangeStatutPayload,
  CreateDemandePayload,
  CreateLignePayload,
  Demande,
  Facture,
  FactureValidee,
  InclusionDisponible,
  ValiderFactureLignePayload,
  HistoriqueDemande,
  HistoriqueLigne,
  LigneService,
  ModeFacturation,
  Statut,
  UpdateDemandePayload,
  UpdateLignePayload,
  ValiderMontantDeboursPayload,
} from '../types'
import type { Dossier, DossierPayload } from '../types/dossier'
import type { DossierTransactionImmobiliere, DossierTransactionPayload } from '../types/dossierTransaction'

export interface DemandeFiltres {
  statut?: string
  /** Situation de règlement, calculée par le backend sur l'ensemble des résultats. */
  situation?: string
  societeId?: number
  serviceId?: number
  commercialId?: number
  dateDebut?: string
  dateFin?: string
  /** EXTERNALISATION ou DEBOURS (au moins une ligne de la demande). */
  categorie?: string
  /** PRESTATION ou ABONNEMENT_MENSUEL. */
  typePrestation?: string
}

export const demandesApi = {
  async list(page?: PageQuery, filtres?: DemandeFiltres): Promise<ApiPage<Demande>> {
    const { data } = await http.get<ApiPage<Demande>>('/demandes', {
      params: buildQuery({ ...filtres }, page),
    })
    return data
  },

  async get(id: number): Promise<Demande> {
    const { data } = await http.get<Demande>(`/demandes/${id}`)
    return data
  },

  async getByReference(reference: string): Promise<Demande> {
    const { data } = await http.get<Demande>(
      `/demandes/reference/${encodeURIComponent(reference)}`,
    )
    return data
  },

  async create(payload: CreateDemandePayload): Promise<Demande> {
    const { data } = await http.post<Demande>('/demandes', payload)
    return data
  },

  async update(id: number, payload: UpdateDemandePayload): Promise<Demande> {
    const { data } = await http.put<Demande>(`/demandes/${id}`, payload)
    return data
  },

  async changeStatut(id: number, payload: ChangeStatutPayload): Promise<Demande> {
    const { data } = await http.patch<Demande>(`/demandes/${id}/statut`, payload)
    return data
  },

  async archive(id: number, archive = true): Promise<Demande> {
    const { data } = await http.patch<Demande>(`/demandes/${id}/archive`, { archive })
    return data
  },

  async historique(id: number): Promise<HistoriqueDemande[]> {
    const { data } = await http.get<HistoriqueDemande[]>(`/demandes/${id}/historique`)
    return data
  },

  async listStatuts(): Promise<Statut[]> {
    const { data } = await http.get<Statut[]>('/demandes/statuts')
    return data
  },
}

export const lignesApi = {
  async list(demandeId: number): Promise<LigneService[]> {
    const { data } = await http.get<LigneService[]>(`/demandes/${demandeId}/services`)
    return data
  },

  async create(demandeId: number, payload: CreateLignePayload): Promise<LigneService> {
    const { data } = await http.post<LigneService>(`/demandes/${demandeId}/services`, payload)
    return data
  },

  async update(demandeId: number, ligneId: number, payload: UpdateLignePayload): Promise<LigneService> {
    const { data } = await http.put<LigneService>(
      `/demandes/${demandeId}/services/${ligneId}`,
      payload,
    )
    return data
  },

  async remove(demandeId: number, ligneId: number): Promise<void> {
    await http.delete(`/demandes/${demandeId}/services/${ligneId}`)
  },

  async historique(demandeId: number, ligneId: number): Promise<HistoriqueLigne[]> {
    const { data } = await http.get<HistoriqueLigne[]>(
      `/demandes/${demandeId}/services/${ligneId}/historique`,
    )
    return data
  },

  /** Bouton « Valider le montant » : EN_ATTENTE → NON_PAYEE (DEBOURS uniquement). */
  async validerMontantDebours(
    demandeId: number,
    ligneId: number,
    payload: ValiderMontantDeboursPayload,
  ): Promise<LigneService> {
    const { data } = await http.patch<LigneService>(
      `/demandes/${demandeId}/services/${ligneId}/valider-montant-debours`,
      payload,
    )
    return data
  },

  /** Annulation d'une ligne DEBOURS en EN_ATTENTE (EN_ATTENTE → ANNULEE). */
  async annulerDebours(demandeId: number, ligneId: number): Promise<LigneService> {
    const { data } = await http.patch<LigneService>(
      `/demandes/${demandeId}/services/${ligneId}/annuler-debours`,
    )
    return data
  },
}

export const facturesApi = {
  async list(demandeId: number): Promise<Facture[]> {
    const { data } = await http.get<Facture[]>(`/demandes/${demandeId}/factures`)
    return data
  },

  async get(id: number): Promise<Facture> {
    const { data } = await http.get<Facture>(`/factures/${id}`)
    return data
  },

  async generer(demandeId: number, modeFacturation: ModeFacturation): Promise<Facture[]> {
    const { data } = await http.post<Facture[]>(`/demandes/${demandeId}/factures`, {
      modeFacturation,
    })
    return data
  },

  async valider(
    id: number,
    commentaire?: string | null,
    montant?: number | null,
    lignes?: ValiderFactureLignePayload[],
  ): Promise<Facture> {
    const { data } = await http.put<Facture>(`/factures/${id}/valider`, {
      commentaire: commentaire ?? null,
      montant: montant ?? null,
      lignes: lignes ?? null,
    })
    return data
  },

  /** URL de téléchargement du PDF proforma. */
  proformaUrl(id: number): string {
    return `/api/factures/${id}/document/proforma`
  },
}

export const facturesValideesApi = {
  async list(demandeId: number): Promise<FactureValidee[]> {
    const { data } = await http.get<FactureValidee[]>(`/demandes/${demandeId}/factures-validees`)
    return data
  },

  /** Éléments impayés (prestation / débours) de la même société, reprenables dans la facture du mois. */
  async inclusionsDisponibles(demandeId: number): Promise<InclusionDisponible[]> {
    const { data } = await http.get<InclusionDisponible[]>(
      `/demandes/${demandeId}/factures-abonnement/inclusions-disponibles`,
    )
    return data
  },

  /** Créer la facture du mois d'une ligne ABONNEMENT_MENSUEL (enfant de la facture générale validée). */
  async creerFactureAbonnement(
    demandeId: number,
    ligneServiceId: number,
    dateEmission: string,
    mois: string,
    commentaire?: string | null,
    inclusionLigneIds?: number[],
  ): Promise<FactureValidee> {
    const { data } = await http.post<FactureValidee>(
      `/demandes/${demandeId}/factures-abonnement`,
      { ligneServiceId, dateEmission, mois, commentaire: commentaire ?? null, inclusionLigneIds: inclusionLigneIds ?? [] },
    )
    return data
  },

  /** Télécharger le PDF de la facture mensuelle (références facture générale / proforma / demande). */
  abonnementUrl(id: number): string {
    return `/factures-validees/${id}/document/abonnement`
  },
}

export const dossierApi = {
  async get(demandeId: number): Promise<Dossier> {
    const { data } = await http.get<Dossier>(`/demandes/${demandeId}/dossier`)
    return data
  },

  async create(demandeId: number, payload: DossierPayload): Promise<Dossier> {
    const { data } = await http.post<Dossier>(`/demandes/${demandeId}/dossier`, payload)
    return data
  },

  async update(demandeId: number, payload: DossierPayload): Promise<Dossier> {
    const { data } = await http.put<Dossier>(`/demandes/${demandeId}/dossier`, payload)
    return data
  },
}
export const dossierTransactionApi = {
  async get(demandeId: number): Promise<DossierTransactionImmobiliere> {
    const { data } = await http.get<DossierTransactionImmobiliere>(
      `/demandes/${demandeId}/dossier-transaction-immobiliere`,
    )
    return data
  },

  async create(
    demandeId: number,
    payload: DossierTransactionPayload,
  ): Promise<DossierTransactionImmobiliere> {
    const { data } = await http.post<DossierTransactionImmobiliere>(
      `/demandes/${demandeId}/dossier-transaction-immobiliere`,
      payload,
    )
    return data
  },

  async update(
    demandeId: number,
    payload: DossierTransactionPayload,
  ): Promise<DossierTransactionImmobiliere> {
    const { data } = await http.put<DossierTransactionImmobiliere>(
      `/demandes/${demandeId}/dossier-transaction-immobiliere`,
      payload,
    )
    return data
  },
}

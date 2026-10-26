import { buildQuery, http } from '@/api/http'
import type { ApiPage, PageQuery } from '@/types'
import type {
  CreatePaiementPayload,
  ModePaiement,
  Paiement,
  PaiementDetail,
  UpdatePaiementPayload,
} from '../types'

export interface PaiementFiltres {
  /** Filtre le serveur : identifiant de la demande concernée. */
  demandeId?: number
  /** Filtre le serveur : identifiant du mode de paiement (et non son code). */
  modeId?: number
  dateDebut?: string
  dateFin?: string
}

export const paiementsApi = {
  async list(page?: PageQuery, filtres?: PaiementFiltres): Promise<ApiPage<Paiement>> {
    const { data } = await http.get<ApiPage<Paiement>>('/paiements', {
      params: buildQuery({ ...filtres }, page),
    })
    return data
  },

  async get(id: number): Promise<Paiement> {
    const { data } = await http.get<Paiement>(`/paiements/${id}`)
    return data
  },

  /** Détail par référence (PAY-2026-00020) : informations du reçu PDF. */
  async getByReference(reference: string): Promise<PaiementDetail> {
    const { data } = await http.get<PaiementDetail>(
      `/paiements/reference/${encodeURIComponent(reference)}`,
    )
    return data
  },

  async create(payload: CreatePaiementPayload): Promise<Paiement> {
    const { data } = await http.post<Paiement>('/paiements', payload)
    return data
  },

  async update(id: number, payload: UpdatePaiementPayload): Promise<Paiement> {
    const { data } = await http.put<Paiement>(`/paiements/${id}`, payload)
    return data
  },

  /** Archivage (soft-delete) réservé à SUPER_ADMIN — la ligne reste en base. */
  async remove(id: number): Promise<void> {
    await http.delete(`/paiements/${id}`)
  },

  /**
   * Modes de paiement actifs pour le formulaire de règlement.
   * `/paiements/modes` exige PAIEMENT_VIEW, contrairement à `/modes-paiement`
   * (administration, paginé) — l'Économat n'a pas forcément MODE_PAIEMENT_VIEW.
   */
  async listModes(): Promise<ModePaiement[]> {
    const { data } = await http.get<ModePaiement[]>('/paiements/modes')
    return data
  },

  /** Reçu de paiement PDF, téléchargé avec le jeton Bearer (DOCUMENT_PAIEMENT_VIEW). */
  async telechargerRecu(id: number): Promise<Blob> {
    const { data } = await http.get<Blob>(`/paiements/${id}/document/recepisse`, {
      responseType: 'blob',
    })
    return data
  },
}
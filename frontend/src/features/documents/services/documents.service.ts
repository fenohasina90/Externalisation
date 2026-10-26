import { http } from '@/api/http'
import type { PieceJointe } from '../types'

function construireFormulaire(fichier: File, description?: string | null): FormData {
  const donnees = new FormData()
  donnees.append('fichier', fichier)
  if (description && description.trim()) donnees.append('description', description.trim())
  return donnees
}

/** Pièces jointes d'une demande ou d'un paiement ; tous types de fichiers acceptés. */
export const documentsApi = {
  async listByDemande(demandeId: number): Promise<PieceJointe[]> {
    const { data } = await http.get<PieceJointe[]>(`/demandes/${demandeId}/documents`)
    return data
  },

  async uploadDemande(
    demandeId: number,
    fichier: File,
    description?: string | null,
  ): Promise<PieceJointe> {
    const { data } = await http.post<PieceJointe>(
      `/demandes/${demandeId}/documents`,
      construireFormulaire(fichier, description),
    )
    return data
  },

  async listByPaiement(paiementId: number): Promise<PieceJointe[]> {
    const { data } = await http.get<PieceJointe[]>(`/paiements/${paiementId}/documents`)
    return data
  },

  async uploadPaiement(
    paiementId: number,
    fichier: File,
    description?: string | null,
  ): Promise<PieceJointe> {
    const { data } = await http.post<PieceJointe>(
      `/paiements/${paiementId}/documents`,
      construireFormulaire(fichier, description),
    )
    return data
  },

  async download(id: number): Promise<Blob> {
    const { data } = await http.get<Blob>(`/documents/${id}/download`, {
      responseType: 'blob',
    })
    return data
  },

  async remove(id: number): Promise<void> {
    await http.delete(`/documents/${id}`)
  },
}

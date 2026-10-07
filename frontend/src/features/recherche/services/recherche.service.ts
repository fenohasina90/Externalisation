import { http } from '@/api/http'
import type { RechercheResultat } from '../types'

export const rechercheApi = {
  async rechercher(terme: string): Promise<RechercheResultat[]> {
    const { data } = await http.get<RechercheResultat[]>('/recherche', {
      params: { q: terme },
    })
    return data
  },
}
import { http } from '@/api/http'
import type { RechercheGlobale } from '../types'

export const rechercheApi = {
  async rechercher(terme: string): Promise<RechercheGlobale> {
    const { data } = await http.get<RechercheGlobale>('/recherche', {
      params: { q: terme },
    })
    return data
  },
}

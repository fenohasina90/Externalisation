import { buildQuery, http } from '@/api/http'
import type {
  CategorieRapport,
  FormatExport,
  RapportClientLigne,
  RapportDemande,
  RapportFinancier,
  RapportFiltres,
  RapportGeneral,
  RapportPaiementLigne,
  RapportServiceLigne,
} from '../types'

function params(filtres: RapportFiltres = {}): URLSearchParams {
  return buildQuery({ ...filtres })
}

export const rapportsApi = {
  async general(filtres?: RapportFiltres): Promise<RapportGeneral> {
    const { data } = await http.get<RapportGeneral>('/rapports/general', {
      params: params(filtres),
    })
    return data
  },

  async financier(filtres?: RapportFiltres): Promise<RapportFinancier> {
    const { data } = await http.get<RapportFinancier>('/rapports/financier', {
      params: params(filtres),
    })
    return data
  },

  async clients(filtres?: RapportFiltres): Promise<RapportClientLigne[]> {
    const { data } = await http.get<RapportClientLigne[]>('/rapports/clients', {
      params: params(filtres),
    })
    return data
  },

  async services(filtres?: RapportFiltres): Promise<RapportServiceLigne[]> {
    const { data } = await http.get<RapportServiceLigne[]>('/rapports/services', {
      params: params(filtres),
    })
    return data
  },

  async demandes(filtres?: RapportFiltres): Promise<RapportDemande[]> {
    const { data } = await http.get<RapportDemande[]>('/rapports/demandes', {
      params: params(filtres),
    })
    return data
  },

  async paiements(filtres?: RapportFiltres): Promise<RapportPaiementLigne[]> {
    const { data } = await http.get<RapportPaiementLigne[]>('/rapports/paiements', {
      params: params(filtres),
    })
    return data
  },

  /** Export serveur du rapport courant (PDF, Excel ou CSV), téléchargé en blob. */
  async exporter(
    categorie: CategorieRapport,
    format: FormatExport,
    filtres?: RapportFiltres,
  ): Promise<Blob> {
    const { data } = await http.get<Blob>(`/rapports/${categorie}`, {
      params: buildQuery({ ...filtres, format }),
      responseType: 'blob',
    })
    return data
  },
}

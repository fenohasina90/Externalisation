import { buildQuery, http } from '@/api/http'
import type { PeriodeFiltre, RapportFinancier, RapportGeneral } from '../types'

export const rapportsApi = {
  async general(periode?: PeriodeFiltre): Promise<RapportGeneral> {
    const { data } = await http.get<RapportGeneral>('/rapports/general', {
      params: buildQuery({ ...(periode ?? {}) }),
    })
    return data
  },

  async financier(periode?: PeriodeFiltre): Promise<RapportFinancier> {
    const { data } = await http.get<RapportFinancier>('/rapports/financier', {
      params: buildQuery({ ...(periode ?? {}) }),
    })
    return data
  },
}
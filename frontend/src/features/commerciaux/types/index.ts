export interface Commercial {
  id: number
  nomCommercial: string
  immatricule: string | null
  email: string | null
  contact: string | null
  tauxCommission: number | null
  actif: boolean
}

export interface CreateCommercialPayload {
  nomCommercial: string
  email?: string | null
  contact?: string | null
  tauxCommission?: number | null
}

export type UpdateCommercialPayload = Partial<CreateCommercialPayload>
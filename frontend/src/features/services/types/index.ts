export interface Domaine {
  id: number
  nomDomaine: string
  description: string | null
  actif: boolean
  nombreSocietes: number
}

export interface Service {
  id: number
  nomService: string
  codeService: string | null
  description: string | null
  actif: boolean
}

export interface CreateServicePayload {
  nomService: string
  description?: string | null
}

export type UpdateServicePayload = {
  nomService: string
  description?: string | null
}
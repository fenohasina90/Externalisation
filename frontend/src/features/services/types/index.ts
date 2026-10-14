export interface Domaine {
  id: number
  nomDomaine: string
  description: string | null
  actif: boolean
  nombreSocietes: number
}

export type TypeService = 'STANDARD' | 'CREATION'

export interface Service {
  id: number
  nomService: string
  codeService: string | null
  typeService: TypeService | string | null
  description: string | null
  actif: boolean
}

export interface CreateServicePayload {
  nomService: string
  typeService?: string | null
  description?: string | null
}

export type UpdateServicePayload = {
  nomService: string
  typeService?: string | null
  description?: string | null
}
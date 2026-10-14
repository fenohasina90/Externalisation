export interface Responsable {
  id: number
  societeId: number | null
  nomSociete: string | null
  nomClient: string
  contact: string | null
  email: string | null
  fonction: string | null
  actif: boolean
}

export interface CreateResponsablePayload {
  societeId: number
  nomClient: string
  contact?: string | null
  email?: string | null
  fonction?: string | null
}

export interface UpdateResponsablePayload {
  nomClient: string
  contact?: string | null
  email?: string | null
  fonction?: string | null
}

export interface SocieteOption {
  id: number
  nom: string
}

export type StatutFilter = 'tous' | 'actifs' | 'inactifs'

export interface ResponsablesFilters {
  actif?: boolean
  societeId?: number
}
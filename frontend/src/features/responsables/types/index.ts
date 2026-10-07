export interface Responsable {
  id: number
  nom: string
  prenom: string | null
  fonction: string | null
  email: string | null
  telephone: string | null
  actif: boolean
  societeId: number | null
  nomSociete: string | null
}

export interface CreateResponsablePayload {
  nom: string
  prenom?: string | null
  fonction?: string | null
  email?: string | null
  telephone?: string | null
  societeId?: number | null
}

export type UpdateResponsablePayload = Partial<CreateResponsablePayload>
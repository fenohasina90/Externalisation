export interface Societe {
  id: number
  nomSociete: string
  activite: string | null
  contact: string | null
  email: string | null
  fb: string | null
  adresse: string | null
  dateCreation: string | null
  nombrePersonnel: number | null
  chiffreAffaireMensuel: number | null
  actif: boolean
  nombreDemandes: number
  createdAt: string
  domaineId: number | null
  nomDomaine: string | null
}

export interface CreateSocietePayload {
  nomSociete: string
  activite?: string | null
  contact?: string | null
  email?: string | null
  fb?: string | null
  adresse?: string | null
  dateCreation?: string | null
  nombrePersonnel?: number | null
  chiffreAffaireMensuel?: number | null
  domaineId?: number | null
}

export type UpdateSocietePayload = Partial<CreateSocietePayload>
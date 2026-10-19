export type TypeSauvegarde = 'manuel' | 'auto' | 'securite'

export interface Sauvegarde {
  nomFichier: string
  tailleOctets: number
  dateCreation: string
  type: TypeSauvegarde
  chiffre: boolean
}

export interface BackupInfos {
  baseDeDonnees: string
  repertoire: string
  retentionJours: number
  cron: string
  tailleMaxUpload: number
}

export interface RestaurationResultat {
  message: string
  sauvegardeSecurite: string
  dateRestauration: string
}

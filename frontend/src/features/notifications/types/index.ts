export interface NotificationUser {
  id: number
  typeNotification: string
  titre: string
  message: string
  referenceType: string | null
  referenceId: number | null
  lu: boolean
  dateLecture: string | null
  dateCreation: string
}
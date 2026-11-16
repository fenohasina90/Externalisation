import {
  LIBELLES_SITUATION_PAIEMENT,
  classeSituationPaiement,
} from '@/features/demandes/constants/labels'

/**
 * Source unique des libellés de situation de règlement : calculés par le
 * backend et utilisés à la fois comme filtre (`GET /demandes?situation=`) et
 * comme pastille affichée dans les listes de demandes et d'échéanciers.
 */
export const LIBELLES_SITUATION = LIBELLES_SITUATION_PAIEMENT

export const classeSituation = classeSituationPaiement

/** Le virement impose une référence (règle métier R3). */
export const VIREMENT = 'VIREMENT'

/** Le chèque impose la banque et le numéro de chèque. */
export const CHEQUE = 'CHEQUE'

export const classeMode = (code: string | null | undefined): string =>
  code === VIREMENT ? 'pill info' : code === CHEQUE ? 'pill ok' : 'pill wt'
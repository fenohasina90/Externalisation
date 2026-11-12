import { useApi } from '@/hooks/useApi'
import type { ApiPage } from '@/types'
import { demandesApi, facturesValideesApi } from '@/features/demandes/services/demandes.service'
import type { Demande, FactureValidee } from '@/features/demandes/types'
import { planPaiementApi } from '@/features/plan-paiement/services/plan-paiement.service'
import { paiementsApi } from '../services/paiements.service'
import type { EcheanceReglable, ModePaiement } from '../types'
import { comparerEcheances, echeancesOuvertes } from '../utils/situation'

/** Ligne DEBOURS réglable (montant validé, reste dû). */
export interface DeboursReglable {
  ligneId: number
  designation: string
  montantDebours: number
  montantPaye: number
  reste: number
  statut: string | null
  factureValideeId: number | null
  referenceFactureValidee: string | null
}

/** Ligne EXTERNALISATION (PRESTATION / ABONNEMENT_MENSUEL) réglable directement :
 *  pas de plan de paiement ni d'échéance, le règlement est lié à la facture validée
 *  (facture générale pour PRESTATION, facture du mois pour ABONNEMENT_MENSUEL). */
export interface LigneDirecteReglable {
  ligneId: number
  ligneLabel: string
  /** Nom du service catalogue (ex. « Abonnement comptabilité »). */
  nomService: string | null
  typePrestation: string | null
  montantPaye: number
  resteAPayer: number
  statut: string | null
  factureValideeId: number | null
  referenceFactureValidee: string | null
  /** ABONNEMENT_MENSUEL : aucune facture du mois encore ouverte. */
  factureMensuelleRequise?: boolean
  referenceFactureGenerale?: string | null
  referenceFactureProforma?: string | null
  demandeId: number
  referenceDemande: string
}

export interface DeboursEnAttenteValidation {
  ligneId: number
  designation: string
  statut: string | null
}

export interface DemandeReglable {
  /** null tant qu'aucune demande n'est sélectionnée. */
  demande: Demande | null
  /** Échéances non soldées de toutes les lignes, à plat. */
  echeances: EcheanceReglable[]
  /** Lignes DEBOURS réglables (EN_COURS / PARTIEL avec reste). */
  debours: DeboursReglable[]
  /** Lignes DEBOURS en attente de validation du montant (EN_ATTENTE) : pas encore réglables. */
  deboursEnAttente: DeboursEnAttenteValidation[]
  /** Lignes EXTERNALISATION (PRESTATION / ABONNEMENT_MENSUEL) réglables directement
   *  sur la facture validée (pas d'échéance ni de plan de paiement). */
  lignesDirectes: LigneDirecteReglable[]
}

/**
 * Charge une demande et aplatit ses échéances réglables (toutes lignes
 * confondues) pour alimenter le formulaire de règlement.
 */
export function useEcheancesReglables(demandeId: number | null) {
  return useApi<DemandeReglable>(
    async () => {
      if (!demandeId) return { demande: null, echeances: [], debours: [], deboursEnAttente: [], lignesDirectes: [] }
      const demande = await demandesApi.get(demandeId)
      const lignes = demande.lignes ?? []
      const plans = await planPaiementApi.getByLignes(lignes.map((l) => l.id))
      // Factures validées : proforma validées (facture générale), factures du mois
      // d'abonnement et factures débours — avec payé / reste recalculés par le backend.
      const facturesValidees = await facturesValideesApi.list(demandeId).catch(() => [])
      const fvParLigne = new Map<number, FactureValidee[]>()
      const ajouterALaLigne = (ligneId: number, fv: FactureValidee) => {
        const arr = fvParLigne.get(ligneId) ?? []
        arr.push(fv)
        fvParLigne.set(ligneId, arr)
      }
      for (const fv of facturesValidees) {
        if (fv.ligneServiceId != null) {
          ajouterALaLigne(fv.ligneServiceId, fv)
          continue
        }
        // Facture générale validée mono-ligne (mode « par service ») : pas de
        // ligne ServiceId sur la facture, mais son unique ligne — la proforma
        // couvre exactement cette ligne — est la cible de règlement dans ce
        // mode. (Mode UNIQUE : une générale mono-ligne n'est jamais une cible
        // de paiement — seule la facture enfant l'est — d'où le filtre.)
        if (fv.type === 'GENERALE' && fv.modeFacturation === 'PAR_SERVICE'
            && fv.lignes?.length === 1 && fv.lignes[0]?.ligneServiceId != null) {
          ajouterALaLigne(fv.lignes[0].ligneServiceId, fv)
        }
      }
      // Facture enfant créée à la validation de la facture générale (mode « unique ») :
      // facture liée pour le paiement de ce service uniquement.
      const fvEnfantParLigne = new Map<number, FactureValidee>()
      for (const fv of facturesValidees) {
        if (fv.ligneServiceId != null && fv.factureGeneraleId != null && fv.mois == null
            && !fvEnfantParLigne.has(fv.ligneServiceId)) {
          fvEnfantParLigne.set(fv.ligneServiceId, fv)
        }
      }

      const echeances: EcheanceReglable[] = []
      const debours: DeboursReglable[] = []
      const deboursEnAttente: DeboursEnAttenteValidation[] = []
      const lignesDirectes: LigneDirecteReglable[] = []

      lignes.forEach((ligne, i) => {
        const nom = ligne.libelleLigne ?? ligne.nomService ?? `Service ${ligne.serviceId ?? i + 1}`
        const fvDeLaLigne = fvParLigne.get(ligne.id) ?? []
        // Facture générale validée (proforma validée) couvrant cette ligne :
        // cible de règlement directe en mode « par service ».
        const fvLigne = fvDeLaLigne.find((f) => f.type === 'GENERALE') ?? null

        if (ligne.categorie === 'DEBOURS') {
          // Débours créé automatiquement à la validation générale (montant annoncé à la
          // création) : sa facture apparaît en type DEBOURS avec facture générale liée.
          // À défaut, en mode « par service », sa facture générale mono-ligne est
          // acceptée uniquement si elle ne couvre que cette ligne.
          const fv = fvDeLaLigne.find((f) => f.type === 'DEBOURS')
            ?? fvDeLaLigne.find(
              (f) => f.type === 'GENERALE'
                && f.lignes?.length === 1
                && f.lignes[0]?.ligneServiceId === ligne.id,
            )
            ?? null
          if (ligne.statut === 'EN_ATTENTE') {
            deboursEnAttente.push({
              ligneId: ligne.id,
              designation: ligne.designationDebours ?? `Débours ${i + 1}`,
              statut: ligne.statut,
            })
          } else if (
            (ligne.statut === 'EN_COURS' || ligne.statut === 'PARTIEL') &&
            ligne.resteAPayer > 0
          ) {
            debours.push({
              ligneId: ligne.id,
              designation: ligne.designationDebours ?? `Débours ${i + 1}`,
              montantDebours: ligne.montantDebours ?? 0,
              montantPaye: ligne.montantPaye,
              reste: ligne.resteAPayer,
              statut: ligne.statut,
              factureValideeId: fv?.id ?? null,
              referenceFactureValidee: fv?.referenceFactureValidee ?? null,
            })
          }
          return
        }
        const estPrestationOuAbonnement =
          ligne.typePrestation === 'PRESTATION' || ligne.typePrestation === 'ABONNEMENT_MENSUEL'
        const estAbonnement = ligne.typePrestation === 'ABONNEMENT_MENSUEL'

        if (estPrestationOuAbonnement) {
          if (estAbonnement) {
            // Facture du mois : la plus ancienne encore due est réglée d'abord.
            const enfants = fvDeLaLigne
              .filter((f) => f.type === 'ABONNEMENT_MENSUEL')
              .sort((a, b) => a.id - b.id)
            const enfantOuvert = enfants.find((f) => f.resteAPayer > 0) ?? null
            if (enfantOuvert) {
              lignesDirectes.push({
                ligneId: ligne.id,
                ligneLabel: nom,
                nomService: ligne.nomService,
                typePrestation: ligne.typePrestation,
                montantPaye: enfantOuvert.montantPaye,
                resteAPayer: enfantOuvert.resteAPayer,
                statut: ligne.statut,
                factureValideeId: enfantOuvert.id,
                referenceFactureValidee: enfantOuvert.referenceFactureValidee,
                referenceFactureGenerale: enfantOuvert.referenceFactureGenerale,
                referenceFactureProforma: enfantOuvert.referenceFactureProforma,
                demandeId: demande.id,
                referenceDemande: demande.referenceDemande,
              })
            } else {
              // Aucune facture du mois ouverte : il faut la créer avant tout paiement.
              lignesDirectes.push({
                ligneId: ligne.id,
                ligneLabel: nom,
                nomService: ligne.nomService,
                typePrestation: ligne.typePrestation,
                montantPaye: enfants.reduce((s, f) => s + f.montantPaye, 0),
                resteAPayer: 0,
                statut: ligne.statut,
                factureValideeId: null,
                referenceFactureValidee: null,
                factureMensuelleRequise: true,
                referenceFactureGenerale: fvLigne?.referenceFactureValidee ?? null,
                referenceFactureProforma: fvLigne?.referenceFactureProforma ?? null,
                demandeId: demande.id,
                referenceDemande: demande.referenceDemande,
              })
            }
            return
          }
          // PRESTATION : règlement sur la facture enfant (mode unique, créée à la
          // validation générale) ou directement sur la facture générale (par service).
          const enfant = fvEnfantParLigne.get(ligne.id) ?? null
          const cible = enfant ?? fvLigne
          const reste = cible ? cible.resteAPayer : Math.max((ligne.tarif ?? 0) - (ligne.montantPaye ?? 0), 0)
          const paye = cible?.montantPaye ?? ligne.montantPaye ?? 0
          if (cible && reste > 0) {
            lignesDirectes.push({
              ligneId: ligne.id,
              ligneLabel: nom,
              nomService: ligne.nomService,
              typePrestation: ligne.typePrestation,
              montantPaye: paye,
              resteAPayer: reste,
              statut: ligne.statut,
              factureValideeId: cible.id,
              referenceFactureValidee: cible.referenceFactureValidee,
              referenceFactureGenerale: enfant
                ? enfant.referenceFactureGenerale
                : fvLigne?.referenceFactureGenerale ?? null,
              referenceFactureProforma: cible.referenceFactureProforma,
              demandeId: demande.id,
              referenceDemande: demande.referenceDemande,
            })
          }
          return
        }
        const plan = plans.get(ligne.id)
        if (!plan) return
        // Création / transaction : paiement lié à la facture enfant (mode unique)
        // ou directement à la facture générale validée (par service).
        const enfantPlan = fvEnfantParLigne.get(ligne.id) ?? null
        const ciblePlan = enfantPlan ?? fvLigne
        const factureValideeId = ciblePlan?.id ?? null
        echeancesOuvertes(plan.echeances ?? []).forEach((echeance) => {
          echeances.push({
            echeance,
            ligneId: ligne.id,
            ligneLabel: nom,
            demandeId: demande.id,
            referenceDemande: demande.referenceDemande,
            nomSociete: demande.nomSociete,
            debutService: ligne.debutService,
            finService: ligne.finService,
            factureValideeId,
            referenceFactureValidee: ciblePlan?.referenceFactureValidee ?? null,
          })
        })
      })
      // Les plans sont chargés service par service : on recolle les échéances
      // dans un ordre chronologique global pour le sélecteur de règlement.
      echeances.sort((a, b) => comparerEcheances(a.echeance, b.echeance))
      return { demande, echeances, debours, deboursEnAttente, lignesDirectes }
    },
    [demandeId],
  )
}

/** Demandes candidates à un règlement (terminées et annulées exclues). */
export function useDemandesReglables(size = 200) {
  return useApi<ApiPage<Demande>>(
    () =>
      demandesApi.list({ page: 0, size, recherche: null }, {}).then((page) => ({
        ...page,
        content: page.content.filter(
          (d) => d.statutActuel !== 'TERMINEE' && d.statutActuel !== 'ANNULEE',
        ),
      })),
    [size],
  )
}

/** Modes de paiement actifs (ESPECES / VIREMENT) pour le formulaire. */
export function useModesPaiement() {
  return useApi<ModePaiement[]>(() => paiementsApi.listModes(), [])
}
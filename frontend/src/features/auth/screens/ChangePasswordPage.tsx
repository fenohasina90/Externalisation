import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { Alert, Button, Card, CardTitle, PageHeader } from '@/components/ui'
import { messageErreur } from '@/constants/messages'
import { PasswordField } from '../components'
import { useAuth } from '../hooks/useAuth'
import { authApi } from '../services/auth.service'

export function ChangePasswordPage() {
  const { utilisateur, refresh } = useAuth()
  const navigate = useNavigate()

  const force = utilisateur?.doitChangerMotDePasse ?? false

  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault()
    setError(null)
    setMessage(null)

    if (newPassword.length < 6) {
      setError('Le nouveau mot de passe doit contenir au moins 6 caractères.')
      return
    }
    if (newPassword !== confirmation) {
      setError('Les deux mots de passe ne correspondent pas.')
      return
    }

    setSubmitting(true)
    try {
      await authApi.changePassword({ currentPassword, newPassword })
      await refresh()
      navigate('/', { replace: true })
    } catch (err) {
      setError(messageErreur(err))
      setSubmitting(false)
    }
  }

  return (
    <>
      <PageHeader
        titre={force ? 'Changement de mot de passe obligatoire' : 'Changer mon mot de passe'}
        sousTitre={
          force
            ? 'Vous utilisez un mot de passe temporaire. Définissez un nouveau mot de passe pour continuer.'
            : 'Mettez à jour le mot de passe de votre compte.'
        }
      />

      {force ? (
        <Alert tone="warning">
          Pour des raisons de sécurité, votre mot de passe temporaire doit être changé avant d’accéder
          au reste de l’application.
        </Alert>
      ) : null}

      <Card className="form-card">
        <CardTitle>Nouveau mot de passe</CardTitle>

        {message ? <Alert tone="success">{message}</Alert> : null}
        {error ? <Alert tone="danger">{error}</Alert> : null}

        <form className="stack-form" onSubmit={(event) => void handleSubmit(event)}>
          <PasswordField
            id="current-password"
            label={force ? 'Mot de passe temporaire' : 'Mot de passe actuel'}
            autoComplete="current-password"
            value={currentPassword}
            onChange={(event) => setCurrentPassword(event.target.value)}
            required
            autoFocus
          />
          <PasswordField
            id="new-password"
            label="Nouveau mot de passe"
            hint="6 caractères minimum"
            autoComplete="new-password"
            value={newPassword}
            onChange={(event) => setNewPassword(event.target.value)}
            minLength={6}
            required
          />
          <PasswordField
            id="confirm-password"
            label="Confirmer le nouveau mot de passe"
            autoComplete="new-password"
            value={confirmation}
            onChange={(event) => setConfirmation(event.target.value)}
            minLength={6}
            required
          />
          <div className="form-actions">
            <Button type="submit" loading={submitting} loadingLabel="Enregistrement…">
              Enregistrer le mot de passe
            </Button>
          </div>
        </form>
      </Card>
    </>
  )
}
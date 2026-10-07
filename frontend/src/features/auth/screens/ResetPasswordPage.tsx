import { useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Alert, Button } from '@/components/ui'
import { messageErreur } from '@/constants/messages'
import { AuthLayout, PasswordField } from '../components'
import { authApi } from '../services/auth.service'

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''

  const [newPassword, setNewPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault()
    setError(null)
    setMessage(null)
    if (newPassword !== confirmation) {
      setError('Les deux mots de passe ne correspondent pas.')
      return
    }
    setSubmitting(true)
    try {
      const response = await authApi.resetPassword({ token, newPassword })
      setMessage(response.message)
    } catch (err) {
      setError(messageErreur(err))
    } finally {
      setSubmitting(false)
    }
  }

  if (!token) {
    return (
      <AuthLayout
        title="Lien invalide"
        subtitle="Le lien de réinitialisation est incomplet ou a expiré."
      >
        <Alert tone="warning">
          Lien de réinitialisation invalide.{' '}
          <Link className="link" to="/mot-de-passe-oublie">
            Nouvelle demande
          </Link>
          .
        </Alert>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout
      title="Nouveau mot de passe"
      subtitle="Choisissez un nouveau mot de passe pour votre compte."
    >
      {message ? <Alert tone="success">{message}</Alert> : null}
      {error ? <Alert tone="danger">{error}</Alert> : null}

      <form className="auth-form" onSubmit={(event) => void handleSubmit(event)}>
        <PasswordField
          id="new-password"
          label="Nouveau mot de passe"
          hint="6 caractères minimum"
          autoComplete="new-password"
          value={newPassword}
          onChange={(event) => setNewPassword(event.target.value)}
          minLength={6}
          required
          autoFocus
        />
        <PasswordField
          id="confirm-password"
          label="Confirmer le mot de passe"
          autoComplete="new-password"
          value={confirmation}
          onChange={(event) => setConfirmation(event.target.value)}
          minLength={6}
          required
        />
        <Button type="submit" loading={submitting} loadingLabel="Enregistrement…">
          Réinitialiser
        </Button>
      </form>

      <p className="auth-alt">
        <Link className="link" to="/login">
          Retour à la connexion
        </Link>
      </p>
    </AuthLayout>
  )
}
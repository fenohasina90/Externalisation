import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { Alert, Button, Field, Input } from '@/components/ui'
import { messageErreur } from '@/constants/messages'
import { AuthLayout } from '../components'
import { authApi } from '../services/auth.service'

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault()
    setError(null)
    setMessage(null)
    setSubmitting(true)
    try {
      const response = await authApi.forgotPassword({ email })
      setMessage(response.message)
    } catch (err) {
      setError(messageErreur(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthLayout
      title="Mot de passe oublié"
      subtitle="Saisissez votre adresse e-mail pour recevoir un lien de réinitialisation."
    >
      {message ? <Alert tone="success">{message}</Alert> : null}
      {error ? <Alert tone="danger">{error}</Alert> : null}

      <form className="auth-form" onSubmit={(event) => void handleSubmit(event)}>
        <Field label="Adresse e-mail" id="email">
          <Input
            id="email"
            type="email"
            placeholder="votre.email@cabex.mg"
            autoComplete="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
            autoFocus
          />
        </Field>
        <Button type="submit" loading={submitting} loadingLabel="Envoi…">
          Envoyer le lien
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
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import { ErrorBox, Field } from '../components.jsx'

export default function ForTherapistsPage() {
  const { me, signIn } = useAuth()
  const navigate = useNavigate()
  const [mode, setMode] = useState('register')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (me) navigate(me.role === 'ADMIN' ? '/admin' : '/dashboard', { replace: true })
  }, [me, navigate])

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const result = mode === 'register' ? await api.register(email, password) : await api.login(email, password)
      signIn(result)
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  const fields = error?.fields || {}

  return (
    <div className="therapist-landing">
      <section>
        <p className="eyebrow">For licensed therapists</p>
        <h1>Be found by the people who need you most.</h1>
        <p className="lede">
          Many people give up on therapy because they can't find someone who speaks their language or
          understands where they come from. List your practice for free and reach them.
        </p>
        <ol className="steps">
          <li><strong>Create your profile.</strong> Languages, communities, specialties, insurance and fees.</li>
          <li><strong>We verify your license</strong> with your state board, usually within 2 business days.</li>
          <li><strong>Confirm availability monthly</strong> with one click, so clients know you're really taking people.</li>
        </ol>
      </section>

      <div className="card auth-card">
        <div className="tabs" role="tablist">
          <button role="tab" aria-selected={mode === 'register'} className={mode === 'register' ? 'on' : ''}
                  onClick={() => { setMode('register'); setError(null) }}>Create account</button>
          <button role="tab" aria-selected={mode === 'login'} className={mode === 'login' ? 'on' : ''}
                  onClick={() => { setMode('login'); setError(null) }}>Log in</button>
        </div>
        {error && !Object.keys(fields).length && <ErrorBox error={error} />}
        <form onSubmit={submit} className="form">
          <Field label="Email" error={fields.email}>
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" required />
          </Field>
          <Field label="Password" hint={mode === 'register' ? 'At least 10 characters' : undefined} error={fields.password}>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
                   autoComplete={mode === 'register' ? 'new-password' : 'current-password'} required />
          </Field>
          <button className="btn btn-primary btn-block" disabled={busy}>
            {busy ? 'Please wait…' : mode === 'register' ? 'Create account' : 'Log in'}
          </button>
        </form>
      </div>
    </div>
  )
}

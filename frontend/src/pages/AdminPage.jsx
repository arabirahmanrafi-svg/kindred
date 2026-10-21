import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import { ErrorBox, formatSessions, Loading, SampleBadge } from '../components.jsx'

const TABS = [['PENDING', 'Waiting for review'], ['REJECTED', 'Rejected'], ['VERIFIED', 'Verified']]

export default function AdminPage() {
  const { me, loading, signOut } = useAuth()
  const [tab, setTab] = useState('PENDING')
  const [items, setItems] = useState(null)
  const [error, setError] = useState(null)

  const load = useCallback(() => {
    setItems(null)
    api.reviewQueue(tab).then(setItems).catch(setError)
  }, [tab])

  useEffect(() => { if (me?.role === 'ADMIN') load() }, [me, load])

  if (loading) return <Loading />
  if (!me) return <Navigate to="/for-therapists" replace />
  if (me.role !== 'ADMIN') return <Navigate to="/dashboard" replace />

  return (
    <div className="admin">
      <div className="dash-head">
        <div>
          <p className="eyebrow">Admin</p>
          <h1>License verification</h1>
          <p className="muted small">
            Look up each license on the state licensing board's public verification site. Approve only when the
            name, license type, number and status all match.
          </p>
        </div>
        <button className="btn btn-ghost" onClick={signOut}>Log out</button>
      </div>

      <div className="tabs" role="tablist">
        {TABS.map(([value, label]) => (
          <button key={value} role="tab" aria-selected={tab === value} className={tab === value ? 'on' : ''}
                  onClick={() => setTab(value)}>{label}</button>
        ))}
      </div>

      <ErrorBox error={error} />
      {!items && !error && <Loading />}
      {items && items.length === 0 && <p className="card empty muted">Nothing here.</p>}
      {items && items.map((item) => <ReviewCard key={item.profile.id} item={item} onDone={load} />)}
    </div>
  )
}

function ReviewCard({ item, onDone }) {
  const { profile: p, accountEmail, status, submittedAt } = item
  const [reason, setReason] = useState('')
  const [rejecting, setRejecting] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)

  async function act(fn) {
    setBusy(true)
    setError(null)
    try { await fn(); onDone() } catch (e) { setError(e) } finally { setBusy(false) }
  }

  return (
    <article className="card review">
      <div className="review-head">
        <div>
          <h2>{p.fullName}, {p.credentials}</h2>
          <p className="muted small">Account: {accountEmail} · updated {new Date(submittedAt).toLocaleString()}</p>
        </div>
        <SampleBadge show={p.demo} />
      </div>

      <dl className="license-check">
        <div><dt>License type</dt><dd>{p.licenseType}</dd></div>
        <div><dt>License number</dt><dd className="mono">{p.licenseNumber}</dd></div>
        <div><dt>Primary state</dt><dd>{p.licenseState}</dd></div>
        <div><dt>All states</dt><dd>{p.statesLicensed.join(', ')}</dd></div>
      </dl>

      <p className="small">{formatSessions(p).join(' · ')}</p>
      <p className="small muted">Languages: {p.languages.join(', ')} · Communities: {p.communities.join(', ') || 'none'}</p>

      <ErrorBox error={error} />

      {status !== 'VERIFIED' && (
        <div className="contact-buttons">
          <button className="btn btn-primary" disabled={busy} onClick={() => act(() => api.approve(p.id))}>Approve</button>
          {!rejecting
            ? <button className="btn" disabled={busy} onClick={() => setRejecting(true)}>Reject…</button>
            : (
              <div className="reject-box">
                <input value={reason} onChange={(e) => setReason(e.target.value)} maxLength={500}
                       placeholder="Reason the therapist will see, e.g. license number not found" />
                <button className="btn btn-danger" disabled={busy || !reason.trim()}
                        onClick={() => act(() => api.reject(p.id, reason))}>Confirm reject</button>
              </div>
            )}
        </div>
      )}
      {status === 'VERIFIED' && <Link to={`/therapists/${p.id}`}>View public profile →</Link>}
    </article>
  )
}

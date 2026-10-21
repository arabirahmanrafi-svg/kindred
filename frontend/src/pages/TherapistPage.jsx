import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api.js'
import { AvailabilityBadge, ErrorBox, formatFee, formatSessions, Loading, SampleBadge, SaveButton } from '../components.jsx'

export default function TherapistPage() {
  const { id } = useParams()
  const [t, setT] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api.therapist(id).then(setT).catch(setError)
  }, [id])

  if (error) return <div className="narrow"><ErrorBox error={error} /><Link to="/">← Back to search</Link></div>
  if (!t) return <Loading />

  const freshness = staleness(t.availabilityConfirmedAt)

  return (
    <div className="profile-layout">
      <article className="profile-main">
        <div className="badges"><SampleBadge show={t.demo} /></div>
        <h1>{t.fullName}<span className="creds">, {t.credentials}</span></h1>
        <p className="muted">{t.licenseType}</p>

        <div className="badges">
          <AvailabilityBadge therapist={t} freshness={freshness} />
        </div>

        {t.bio && <p className="bio">{t.bio}</p>}

        <Section title="Languages" items={t.languages} />
        <Section title="Experience with" items={t.communities} />
        <Section title="Can help with" items={t.specialties} />

        <div className="facts">
          <div>
            <h3>Sessions</h3>
            {formatSessions(t).map((s) => <p key={s}>{s}</p>)}
          </div>
          <div>
            <h3>Cost</h3>
            <p>{formatFee(t)}</p>
            <p className="muted small">{t.insurances.length ? `Insurance: ${t.insurances.join(', ')}` : 'No insurance plans listed'}</p>
          </div>
        </div>

        <div className="license-box">
          <span aria-hidden="true">🛡️</span>
          <div>
            <strong>License verified</strong>
            <p className="small">
              {t.licenseType} · {t.licenseState} #{t.licenseNumber}
              {t.verifiedAt ? ` · checked ${new Date(t.verifiedAt).toLocaleDateString()}` : ''}
            </p>
            <p className="small muted">You can confirm any license yourself on your state licensing board's website.</p>
          </div>
        </div>
      </article>

      <aside className="profile-side">
        <div className="card contact-card">
          <h2>Reach out</h2>
          <div className="contact-buttons">
            {t.contactEmail && <a className="btn btn-primary" href={`mailto:${t.contactEmail}`}>Email {firstName(t.fullName)}</a>}
            {t.phone && <a className="btn" href={`tel:${t.phone.replace(/[^\d+]/g, '')}`}>Call {t.phone}</a>}
            {t.websiteUrl && <a className="btn" href={t.websiteUrl} target="_blank" rel="noopener noreferrer">Website ↗</a>}
          </div>
          <SaveButton id={t.id} />
        </div>
        <ReachOutHelper therapist={t} />
      </aside>
    </div>
  )
}

/**
 * Writing the first message is often the hardest step. This builds a short, editable message.
 * It runs entirely in the browser: nothing typed here is sent to Kindred.
 */
function ReachOutHelper({ therapist: t }) {
  const [opts, setOpts] = useState({ language: '', insurance: '', format: '', topic: '' })
  const [name, setName] = useState('')
  const [copied, setCopied] = useState(false)

  const message = useMemo(() => {
    const lines = [`Hi ${t.fullName.split(' ')[0]},`, '']
    lines.push("I found your profile and I'm looking for a therapist. I'd like to ask a few questions before scheduling:")
    lines.push('')
    lines.push('• Are you currently accepting new clients?')
    if (opts.language) lines.push(`• Would sessions in ${opts.language} be possible?`)
    if (opts.format) lines.push(`• Do you have openings for ${opts.format === 'telehealth' ? 'video sessions' : 'in-person sessions'}?`)
    if (opts.insurance) lines.push(`• Do you accept ${opts.insurance}? If not, what are your self-pay rates?`)
    else lines.push('• What are your rates, and do you offer a free first consultation?')
    if (opts.topic) lines.push(`• I'd mainly like to work on ${opts.topic.toLowerCase()}. Is that something you help with?`)
    lines.push('')
    lines.push('Thank you,')
    lines.push(name || '[Your name]')
    return lines.join('\n')
  }, [t, opts, name])

  async function copy() {
    try {
      await navigator.clipboard.writeText(message)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch { /* clipboard blocked */ }
  }

  const set = (k) => (e) => setOpts({ ...opts, [k]: e.target.value })

  return (
    <div className="card helper">
      <h2>Not sure what to say?</h2>
      <p className="muted small">Pick what matters to you and we'll draft a first message. It stays on your device.</p>

      <div className="helper-grid">
        <select value={opts.language} onChange={set('language')} aria-label="Language">
          <option value="">Language…</option>
          {t.languages.map((l) => <option key={l}>{l}</option>)}
        </select>
        <select value={opts.format} onChange={set('format')} aria-label="Session type">
          <option value="">Session type…</option>
          {t.offersInPerson && <option value="in_person">In person</option>}
          {t.offersTelehealth && <option value="telehealth">Video / phone</option>}
        </select>
        <select value={opts.insurance} onChange={set('insurance')} aria-label="Insurance">
          <option value="">Insurance…</option>
          {t.insurances.map((i) => <option key={i}>{i}</option>)}
        </select>
        <select value={opts.topic} onChange={set('topic')} aria-label="Topic">
          <option value="">Topic (optional)…</option>
          {t.specialties.map((s) => <option key={s}>{s}</option>)}
        </select>
        <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Your first name (optional)" aria-label="Your first name" />
      </div>

      <textarea className="draft" readOnly value={message} rows={12} aria-label="Draft message" />
      <div className="contact-buttons">
        <button className="btn" onClick={copy}>{copied ? 'Copied ✓' : 'Copy message'}</button>
        {t.contactEmail && (
          <a className="btn btn-primary"
             href={`mailto:${t.contactEmail}?subject=${encodeURIComponent('Inquiry about therapy')}&body=${encodeURIComponent(message)}`}>
            Open in email
          </a>
        )}
      </div>
    </div>
  )
}

function Section({ title, items }) {
  if (!items.length) return null
  return (
    <section className="profile-section">
      <h3>{title}</h3>
      <div className="tags">{items.map((i) => <span key={i} className="tag">{i}</span>)}</div>
    </section>
  )
}

function firstName(full) {
  return full.split(' ')[0]
}

function staleness(iso) {
  const days = (Date.now() - new Date(iso).getTime()) / 86_400_000
  return days > 60 ? 'STALE' : days > 30 ? 'AGING' : 'FRESH'
}

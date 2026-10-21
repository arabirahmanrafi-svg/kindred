import { useEffect, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { api, loadOptions } from '../api.js'
import { useAuth } from '../auth.jsx'
import { ChipPicker, daysAgo, ErrorBox, Field, Loading } from '../components.jsx'

const blank = {
  fullName: '', credentials: '', licenseType: '', licenseNumber: '', licenseState: 'MI', bio: '',
  languages: ['English'], communities: [], specialties: [], insurances: [], statesLicensed: [],
  offersInPerson: true, offersTelehealth: true, practiceCity: '', practiceState: 'MI',
  feeMin: '', feeMax: '', slidingScale: false, acceptingNewClients: true,
  contactEmail: '', phone: '', websiteUrl: '',
}

export default function DashboardPage() {
  const { me, loading, signOut } = useAuth()
  const [own, setOwn] = useState(undefined) // undefined = loading, null = no profile yet
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!me || me.role !== 'THERAPIST') return
    api.myProfile().then(setOwn).catch((e) => (e.status === 404 ? setOwn(null) : setError(e)))
  }, [me])

  if (loading) return <Loading />
  if (!me) return <Navigate to="/for-therapists" replace />
  if (me.role === 'ADMIN') return <Navigate to="/admin" replace />
  if (own === undefined && !error) return <Loading />

  return (
    <div className="dashboard">
      <div className="dash-head">
        <div>
          <p className="eyebrow">Signed in as {me.email}</p>
          <h1>{own ? 'Your listing' : 'Create your listing'}</h1>
        </div>
        <button className="btn btn-ghost" onClick={signOut}>Log out</button>
      </div>

      <ErrorBox error={error} />
      {own && <StatusPanel own={own} />}
      {own && own.status === 'VERIFIED' && <AvailabilityPanel own={own} onChange={setOwn} />}
      <ProfileEditor own={own} onSaved={setOwn} />
    </div>
  )
}

function StatusPanel({ own }) {
  const { status, reviewNote, profile } = own
  if (status === 'VERIFIED') {
    return (
      <div className="status status-ok">
        <strong>✓ Verified and live.</strong>
        <span>People can find you in search. <Link to={`/therapists/${profile.id}`}>View your public profile</Link></span>
      </div>
    )
  }
  if (status === 'REJECTED') {
    return (
      <div className="status status-bad">
        <strong>We couldn't verify your profile.</strong>
        <span>{reviewNote || 'Please review your license details.'} Fix the details below and save to resubmit.</span>
      </div>
    )
  }
  return (
    <div className="status status-wait">
      <strong>Waiting for license verification.</strong>
      <span>{reviewNote || "We're checking your license with your state board. Your profile goes live once it's verified."}</span>
    </div>
  )
}

function AvailabilityPanel({ own, onChange }) {
  const [busy, setBusy] = useState(false)
  const { profile, shouldConfirmAvailability } = own

  async function confirm(accepting) {
    setBusy(true)
    try { onChange(await api.confirmAvailability(accepting)) } finally { setBusy(false) }
  }

  return (
    <div className={`card availability ${shouldConfirmAvailability ? 'availability-due' : ''}`}>
      <div>
        <h2>Are you taking new clients?</h2>
        <p className="muted small">
          Currently showing: <strong>{profile.acceptingNewClients ? 'Accepting new clients' : 'Not accepting'}</strong>
          {' '}· last confirmed {daysAgo(profile.availabilityConfirmedAt)}.
          {shouldConfirmAvailability && ' Please confirm. Listings not confirmed for 60 days are paused automatically.'}
        </p>
      </div>
      <div className="contact-buttons">
        <button className="btn btn-primary" disabled={busy} onClick={() => confirm(true)}>Yes, I'm accepting</button>
        <button className="btn" disabled={busy} onClick={() => confirm(false)}>I'm full right now</button>
      </div>
    </div>
  )
}

function ProfileEditor({ own, onSaved }) {
  const [options, setOptions] = useState(null)
  const [form, setForm] = useState(() => (own ? fromProfile(own.profile) : blank))
  const [errors, setErrors] = useState({})
  const [message, setMessage] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => { loadOptions().then(setOptions) }, [])

  if (!options) return <Loading />

  const set = (key) => (value) => setForm((f) => ({ ...f, [key]: value }))
  const input = (key) => (e) => set(key)(e.target.type === 'checkbox' ? e.target.checked : e.target.value)

  async function submit(e) {
    e.preventDefault()
    setSaving(true)
    setErrors({})
    setMessage('')
    try {
      const saved = await api.saveProfile({
        ...form,
        feeMin: form.feeMin === '' ? null : Number(form.feeMin),
        feeMax: form.feeMax === '' ? null : Number(form.feeMax),
      })
      onSaved(saved)
      setForm(fromProfile(saved.profile))
      setMessage(saved.status === 'VERIFIED' ? 'Saved. Your listing is updated.' : 'Saved. Your profile is in the verification queue.')
      window.scrollTo({ top: 0, behavior: 'smooth' })
    } catch (err) {
      setErrors(err.fields || {})
      setMessage(err.message)
    } finally {
      setSaving(false)
    }
  }

  const ok = message && !Object.keys(errors).length

  return (
    <form className="card editor" onSubmit={submit} noValidate>
      {message && <p className={`alert ${ok ? 'alert-success' : 'alert-error'}`}>{message}</p>}

      <fieldset>
        <legend>License</legend>
        <p className="muted small">Changing these on a verified profile sends it back for a quick re-check.</p>
        <div className="grid-2">
          <Field label="Full name" error={errors.fullName}><input value={form.fullName} onChange={input('fullName')} /></Field>
          <Field label="Credentials" hint="e.g. LMSW, PhD" error={errors.credentials}><input value={form.credentials} onChange={input('credentials')} /></Field>
          <Field label="License type" hint="e.g. Licensed Professional Counselor" error={errors.licenseType}><input value={form.licenseType} onChange={input('licenseType')} /></Field>
          <Field label="License number" error={errors.licenseNumber}><input value={form.licenseNumber} onChange={input('licenseNumber')} /></Field>
          <Field label="License state" error={errors.licenseState}>
            <select value={form.licenseState} onChange={input('licenseState')}>
              {options.states.map((s) => <option key={s}>{s}</option>)}
            </select>
          </Field>
        </div>
        <Field label="Also licensed in" hint="Telehealth clients can only find you in states where you're licensed." error={errors.statesLicensed}>
          <ChipPicker label="States" options={options.states} value={form.statesLicensed} onChange={set('statesLicensed')} initialVisible={0} searchable />
        </Field>
      </fieldset>

      <fieldset>
        <legend>About you</legend>
        <Field label="Short bio" hint="Who you work with and how. Up to 3000 characters." error={errors.bio}>
          <textarea rows={5} value={form.bio} onChange={input('bio')} maxLength={3000} />
        </Field>
        <Field label="Languages you offer sessions in" error={errors.languages}>
          <ChipPicker label="Languages" options={options.languages} value={form.languages} onChange={set('languages')} max={10} searchable />
        </Field>
        <Field label="Communities you have experience serving" hint="Choose only where you have real training or lived experience." error={errors.communities}>
          <ChipPicker label="Communities" options={options.communities} value={form.communities} onChange={set('communities')} max={10} initialVisible={30} />
        </Field>
        <Field label="Specialties" error={errors.specialties}>
          <ChipPicker label="Specialties" options={options.specialties} value={form.specialties} onChange={set('specialties')} max={12} initialVisible={30} />
        </Field>
      </fieldset>

      <fieldset>
        <legend>Sessions and fees</legend>
        <div className="checks">
          <label className="checkbox"><input type="checkbox" checked={form.offersInPerson} onChange={input('offersInPerson')} /> In person</label>
          <label className="checkbox"><input type="checkbox" checked={form.offersTelehealth} onChange={input('offersTelehealth')} /> Telehealth</label>
        </div>
        {errors.offersInPerson && <p className="field-message">{errors.offersInPerson}</p>}
        {form.offersInPerson && (
          <div className="grid-2">
            <Field label="Office city" error={errors.practiceCity}><input value={form.practiceCity} onChange={input('practiceCity')} /></Field>
            <Field label="Office state" error={errors.practiceState}>
              <select value={form.practiceState} onChange={input('practiceState')}>
                {options.states.map((s) => <option key={s}>{s}</option>)}
              </select>
            </Field>
          </div>
        )}
        <div className="grid-2">
          <Field label="Self-pay fee from ($)" error={errors.feeMin}><input type="number" min={0} value={form.feeMin} onChange={input('feeMin')} /></Field>
          <Field label="Up to ($)" error={errors.feeMax}><input type="number" min={0} value={form.feeMax} onChange={input('feeMax')} /></Field>
        </div>
        <label className="checkbox"><input type="checkbox" checked={form.slidingScale} onChange={input('slidingScale')} /> I offer sliding-scale fees</label>
        <Field label="Insurance accepted" error={errors.insurances}>
          <ChipPicker label="Insurance" options={options.insurances} value={form.insurances} onChange={set('insurances')} initialVisible={30} />
        </Field>
        <label className="checkbox"><input type="checkbox" checked={form.acceptingNewClients} onChange={input('acceptingNewClients')} /> I'm accepting new clients</label>
      </fieldset>

      <fieldset>
        <legend>How clients reach you</legend>
        <p className="muted small">At least one. These are shown publicly on your profile.</p>
        <div className="grid-3">
          <Field label="Email" error={errors.contactEmail}><input type="email" value={form.contactEmail} onChange={input('contactEmail')} /></Field>
          <Field label="Phone" error={errors.phone}><input value={form.phone} onChange={input('phone')} /></Field>
          <Field label="Website or booking link" error={errors.websiteUrl}><input value={form.websiteUrl} onChange={input('websiteUrl')} placeholder="https://" /></Field>
        </div>
      </fieldset>

      <button className="btn btn-primary btn-block" disabled={saving}>
        {saving ? 'Saving…' : own ? 'Save changes' : 'Submit for verification'}
      </button>
    </form>
  )
}

function fromProfile(p) {
  return {
    fullName: p.fullName || '', credentials: p.credentials || '', licenseType: p.licenseType || '',
    licenseNumber: p.licenseNumber || '', licenseState: p.licenseState || 'MI', bio: p.bio || '',
    languages: p.languages, communities: p.communities, specialties: p.specialties, insurances: p.insurances,
    statesLicensed: p.statesLicensed,
    offersInPerson: p.offersInPerson, offersTelehealth: p.offersTelehealth,
    practiceCity: p.practiceCity || '', practiceState: p.practiceState || p.licenseState || 'MI',
    feeMin: p.feeMin ?? '', feeMax: p.feeMax ?? '', slidingScale: p.slidingScale,
    acceptingNewClients: p.acceptingNewClients,
    contactEmail: p.contactEmail || '', phone: p.phone || '', websiteUrl: p.websiteUrl || '',
  }
}

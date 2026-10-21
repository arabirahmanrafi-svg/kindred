import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api.js'
import { formatFee, Loading, SampleBadge, SaveButton } from '../components.jsx'
import { saved } from '../storage.js'

export default function SavedPage() {
  const [ids, setIds] = useState(saved.all())
  const [items, setItems] = useState(null)

  useEffect(() => {
    const sync = () => setIds(saved.all())
    window.addEventListener('kindred-saved', sync)
    return () => window.removeEventListener('kindred-saved', sync)
  }, [])

  useEffect(() => {
    // A saved therapist may have since been removed or paused; skip those quietly.
    Promise.all(ids.map((id) => api.therapist(id).catch(() => null)))
      .then((list) => setItems(list.filter(Boolean)))
  }, [ids])

  return (
    <div className="narrow-wide">
      <h1>Saved therapists</h1>
      <p className="muted">Saved only on this device. Kindred doesn't know who you saved.</p>

      {!items && <Loading />}
      {items && items.length === 0 && (
        <div className="card empty">
          <p>Nothing saved yet. Tap ♡ on any therapist to keep them here while you decide.</p>
          <Link to="/" className="btn btn-primary">Start a search</Link>
        </div>
      )}
      {items && items.map((t) => (
        <div key={t.id} className="saved-row card">
          <div>
            <Link to={`/therapists/${t.id}`}><strong>{t.fullName}</strong>, {t.credentials}</Link>
            <p className="muted small">{t.languages.join(', ')} · {formatFee(t)}</p>
            <SampleBadge show={t.demo} />
          </div>
          <SaveButton id={t.id} />
        </div>
      ))}
      {items && items.length > 0 && (
        <button className="btn btn-ghost" onClick={() => saved.clear()}>Clear all saved</button>
      )}
    </div>
  )
}

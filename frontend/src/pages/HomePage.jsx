import { useNavigate } from 'react-router-dom'
import SearchForm, { emptySearch, searchToParams } from '../SearchForm.jsx'

export default function HomePage() {
  const navigate = useNavigate()

  return (
    <div className="home">
      <section className="hero">
        <p className="eyebrow">For anyone who's felt unheard</p>
        <h1>Find a therapist who <em>understands</em> you.</h1>
        <p className="lede">
          Search licensed therapists by the language you speak, the communities they know, and what you'd
          like to talk about. Every license is checked before a therapist appears here.
        </p>
      </section>

      <div className="home-grid">
        <div className="card card-search">
          <SearchForm initial={emptySearch} onSubmit={(s) => navigate(`/search?${searchToParams(s)}`)} />
        </div>

        <aside className="how">
          <h2>How Kindred is different</h2>
          <ul>
            <li>
              <strong>Your language, your culture.</strong>
              <span>Filter by language and lived context, not just a diagnosis.</span>
            </li>
            <li>
              <strong>Licenses are verified.</strong>
              <span>We check every license with the state board before a profile goes live.</span>
            </li>
            <li>
              <strong>Real availability.</strong>
              <span>Listings that aren't confirmed regularly stop showing as "accepting," so you don't reach out into silence.</span>
            </li>
            <li>
              <strong>Nothing about you is stored.</strong>
              <span>No sign-up, no tracking of what you search. Saved therapists stay on your device.</span>
            </li>
          </ul>
        </aside>
      </div>
    </div>
  )
}

import { useState, useEffect, useRef, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { apiGet } from '../api/client';
import { getGlobalBookStatus, getMyPins, unpinRecipe, acceptPinUpdate, copyPinnedAsOwn, searchWebRecipes } from '../api/globalRecipes';
import type { Recipe } from './types';
import type { GlobalBookStatus, PinnedRecipe, WebRecipeResult } from './global-types';

export function RecipeListPage() {
  const [recipes, setRecipes] = useState<Recipe[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [globalStatus, setGlobalStatus] = useState<GlobalBookStatus | null>(null);
  const [pins, setPins] = useState<PinnedRecipe[]>([]);
  const [busy, setBusy] = useState<string | null>(null);
  const [confirmUnpin, setConfirmUnpin] = useState<string | null>(null);
  const [calendarWarning, setCalendarWarning] = useState<{ pinnedId: string; count: number } | null>(null);
  const [webResults, setWebResults] = useState<WebRecipeResult[]>([]);
  const [webLoading, setWebLoading] = useState(false);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    loadRecipes();
    getGlobalBookStatus()
      .then(status => {
        setGlobalStatus(status);
        if (status.enabled) loadPins();
      })
      .catch(() => setGlobalStatus({ enabled: false, reachable: false }));
  }, []);

  const loadRecipes = async (name?: string) => {
    setLoading(true);
    try {
      const params = name ? `?name=${encodeURIComponent(name)}` : '';
      const data = await apiGet<Recipe[]>(`/api/v1/recipes${params}`);
      setRecipes(data);
    } finally {
      setLoading(false);
    }
  };

  const loadPins = async () => {
    try {
      setPins(await getMyPins());
    } catch {
      setPins([]);
    }
  };

  const handleSearchChange = (value: string) => {
    setSearch(value);
    if (debounceRef.current !== null) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      loadRecipes(value || undefined);
      if (value.trim().length >= 2) {
        setWebLoading(true);
        searchWebRecipes(value.trim())
          .then(results => setWebResults(results))
          .catch(() => setWebResults([]))
          .finally(() => setWebLoading(false));
      } else {
        setWebResults([]);
        setWebLoading(false);
      }
    }, 300);
  };

  const handleCopyAsOwn = async (pinnedId: string) => {
    setBusy(pinnedId);
    try {
      await copyPinnedAsOwn(pinnedId);
      await Promise.all([loadRecipes(search || undefined), loadPins()]);
    } finally {
      setBusy(null);
    }
  };

  const handleUnpin = async (pinnedId: string) => {
    setBusy(pinnedId);
    setConfirmUnpin(null);
    try {
      const result = await unpinRecipe(pinnedId);
      if (result.conflict) {
        setCalendarWarning({ pinnedId, count: result.calendarEntryCount! });
        return;
      }
      await loadPins();
    } finally {
      setBusy(null);
    }
  };

  const handleCascadeUnpin = async (pinnedId: string) => {
    setBusy(pinnedId);
    setCalendarWarning(null);
    try {
      await unpinRecipe(pinnedId, true);
      await loadPins();
    } finally {
      setBusy(null);
    }
  };

  const handleAcceptUpdate = async (pinnedId: string) => {
    setBusy(pinnedId);
    try {
      await acceptPinUpdate(pinnedId);
      await loadPins();
    } finally {
      setBusy(null);
    }
  };

  const globalEnabled = globalStatus?.enabled ?? false;

  const filteredPins = search
    ? pins.filter(p => p.name.toLowerCase().includes(search.toLowerCase()))
    : pins;

  const groupedWebResults = useMemo(() => {
    const grouped = new Map<string, WebRecipeResult[]>();
    webResults.forEach(r => {
      const existing = grouped.get(r.site) || [];
      existing.push(r);
      grouped.set(r.site, existing);
    });
    return grouped;
  }, [webResults]);

  return (
    <div className="page">
      <div className="page-header">
        <h1>Recipes</h1>
        <div className="btn-group">
          {globalEnabled && (
            <Link to="/recipes/global" className="btn">Global Book</Link>
          )}
          <Link to="/recipes/scan" className="btn">Scan Card</Link>
          <Link to="/recipes/import" className="btn">Import URL</Link>
          <Link to="/recipes/new" className="btn btn-primary">New Recipe</Link>
        </div>
      </div>

      <div className="search-bar">
        <input
          type="text"
          placeholder="Search recipes and the web..."
          value={search}
          onChange={e => handleSearchChange(e.target.value)}
        />
      </div>

      {loading ? (
        <p>Loading...</p>
      ) : recipes.length === 0 && filteredPins.length === 0 ? (
        <p className="empty">No recipes found. Create your first recipe!</p>
      ) : (
        <div className="card-grid">
          {recipes.map(recipe => (
            <Link to={`/recipes/${recipe.id}`} key={recipe.id} className="card">
              <h3>{recipe.name}</h3>
              <p>{recipe.baseServings} servings</p>
              <p className="muted">{recipe.ingredients.length} ingredient{recipe.ingredients.length !== 1 ? 's' : ''}</p>
            </Link>
          ))}
          {filteredPins.map(pin => (
            <div key={`pin-${pin.id}`} className="card" style={{ position: 'relative' }}>
              {pin.hasUpdate && (
                <span style={{
                  position: 'absolute', top: '0.5rem', right: '0.5rem',
                  background: '#f59e0b', color: 'white', fontSize: '0.7rem',
                  padding: '0.15rem 0.5rem', borderRadius: '999px', fontWeight: 600,
                }}>
                  Update available
                </span>
              )}
              <Link to={`/recipes/global/${pin.sharedRecipeId}`} style={{ textDecoration: 'none', color: 'inherit' }}>
                <h3>{pin.name}</h3>
              </Link>
              <p style={{
                fontSize: '0.75rem', color: 'var(--primary)', fontWeight: 500,
                margin: '0.25rem 0 0.5rem',
              }}>
                Pinned from {pin.sourceInstanceName}
              </p>
              <p>{pin.baseServings} servings</p>
              <p className="muted">
                {pin.ingredients.length} ingredient{pin.ingredients.length !== 1 ? 's' : ''}
              </p>
              {(confirmUnpin === pin.id || calendarWarning?.pinnedId === pin.id) ? (
                <div style={{
                  marginTop: '0.5rem', padding: '0.5rem',
                  background: 'var(--surface)', borderRadius: 'var(--radius)',
                  border: '1px solid var(--border)',
                }}>
                  {calendarWarning?.pinnedId === pin.id ? (
                    <>
                      <p style={{ fontSize: '0.85rem', marginBottom: '0.5rem', color: 'var(--danger, #dc3545)' }}>
                        This recipe is on your calendar ({calendarWarning.count} meal{calendarWarning.count !== 1 ? 's' : ''}).
                        Unpinning will remove it from your calendar and shopping list.
                      </p>
                      <div className="btn-group">
                        <button
                          className="btn btn-small btn-danger"
                          disabled={busy === pin.id}
                          onClick={() => handleCascadeUnpin(pin.id)}
                        >
                          {busy === pin.id ? '...' : 'Unpin & Remove Meals'}
                        </button>
                        <button
                          className="btn btn-small"
                          onClick={() => { setCalendarWarning(null); setConfirmUnpin(null); }}
                        >
                          Cancel
                        </button>
                      </div>
                    </>
                  ) : (
                    <>
                      <p style={{ fontSize: '0.85rem', marginBottom: '0.5rem' }}>Remove from your recipes?</p>
                      <div className="btn-group">
                        <button
                          className="btn btn-small btn-danger"
                          disabled={busy === pin.id}
                          onClick={() => handleUnpin(pin.id)}
                        >
                          {busy === pin.id ? '...' : 'Yes, Unpin'}
                        </button>
                        <button
                          className="btn btn-small"
                          onClick={() => setConfirmUnpin(null)}
                        >
                          Cancel
                        </button>
                      </div>
                    </>
                  )}
                </div>
              ) : (
                <div className="btn-group" style={{ marginTop: '0.5rem', flexWrap: 'wrap' }}>
                  <button
                    className="btn btn-small btn-primary"
                    disabled={busy === pin.id}
                    onClick={() => handleCopyAsOwn(pin.id)}
                  >
                    {busy === pin.id ? '...' : 'Copy as My Own'}
                  </button>
                  {pin.hasUpdate && (
                    <button
                      className="btn btn-small"
                      disabled={busy === pin.id}
                      onClick={() => handleAcceptUpdate(pin.id)}
                    >
                      Accept Update
                    </button>
                  )}
                  <button
                    className="btn btn-small btn-danger"
                    disabled={busy === pin.id}
                    onClick={() => setConfirmUnpin(pin.id)}
                  >
                    Unpin
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {(webLoading || webResults.length > 0) && (
        <div style={{ marginTop: '2rem' }}>
          <div style={{ borderTop: '1px solid var(--border)', paddingTop: '1.5rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <h2 style={{ fontSize: '1.1rem', color: 'var(--muted)', margin: 0 }}>Online Recipes</h2>
            <Link to="/recipes/import" className="btn btn-small">Import URL</Link>
          </div>
          {webLoading ? (
            <p style={{ marginTop: '0.75rem' }}>Searching the web...</p>
          ) : (
            Array.from(groupedWebResults.entries()).map(([site, results]) => (
              <div key={site} style={{ marginBottom: '1.5rem' }}>
                <h3 style={{ fontSize: '0.95rem', color: 'var(--text)', marginBottom: '0.5rem', marginTop: '1rem' }}>
                  {site}
                </h3>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                  {results.map((result, i) => (
                    <a
                      key={i}
                      href={result.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      style={{
                        display: 'block',
                        padding: '0.75rem',
                        background: 'var(--surface)',
                        border: '1px solid var(--border)',
                        borderRadius: 'var(--radius)',
                        textDecoration: 'none',
                        color: 'var(--text)',
                      }}
                    >
                      <div style={{ fontWeight: 500, color: 'var(--primary)' }}>{result.title}</div>
                      {result.snippet && (
                        <div style={{ fontSize: '0.85rem', color: 'var(--muted)', marginTop: '0.25rem' }}>
                          {result.snippet}
                        </div>
                      )}
                    </a>
                  ))}
                </div>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}

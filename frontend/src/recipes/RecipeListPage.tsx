import { useState, useEffect, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { apiGet } from '../api/client';
import { getGlobalBookStatus, getMyPins, unpinRecipe, acceptPinUpdate, copyPinnedAsOwn, searchWebRecipes } from '../api/globalRecipes';
import { WebRecipePanel } from './WebRecipePanel';
import type { Recipe } from './types';
import type { GlobalBookStatus, PinnedRecipe, WebRecipeResult } from './global-types';

export function RecipeListPage() {
  const navigate = useNavigate();
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
  const [panel, setPanel] = useState<{ result: WebRecipeResult; mode: 'quicklook' | 'preview' } | null>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const webRequestRef = useRef(0);

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
        const requestId = ++webRequestRef.current;
        setWebLoading(true);
        searchWebRecipes(value.trim())
          .then(results => {
            if (webRequestRef.current === requestId) setWebResults(results);
          })
          .catch(() => {
            if (webRequestRef.current === requestId) setWebResults([]);
          })
          .finally(() => {
            if (webRequestRef.current === requestId) setWebLoading(false);
          });
      } else {
        webRequestRef.current++;
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
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', marginTop: '0.75rem' }}>
              {webResults.map((result, i) => (
                <div key={i} className="web-recipe-card">
                  <img
                    className="favicon"
                    src={`https://www.google.com/s2/favicons?domain=${result.site}&sz=32`}
                    alt=""
                  />
                  <div className="card-body">
                    <div className="card-title">{result.title}</div>
                    <div className="card-site">{result.site}</div>
                    {result.snippet && <div className="card-snippet">{result.snippet}</div>}
                    <div className="card-actions">
                      <button
                        className="btn btn-small"
                        onClick={() => setPanel({ result, mode: 'quicklook' })}
                      >
                        Quick Look
                      </button>
                      <button
                        className="btn btn-small"
                        onClick={() => setPanel({ result, mode: 'preview' })}
                      >
                        Preview
                      </button>
                      <button
                        className="btn btn-small btn-primary"
                        onClick={() => navigate(`/recipes/import?url=${encodeURIComponent(result.url)}`)}
                      >
                        Import
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {panel && (
        <WebRecipePanel
          result={panel.result}
          mode={panel.mode}
          onClose={() => setPanel(null)}
        />
      )}
    </div>
  );
}

import { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { apiGet } from '../api/client';
import { getGlobalBookStatus, getMyPins, unpinRecipe, acceptPinUpdate, copyPinnedAsOwn } from '../api/globalRecipes';
import type { Recipe } from './types';
import type { GlobalBookStatus, PinnedRecipe } from './global-types';

export function RecipeListPage() {
  const [recipes, setRecipes] = useState<Recipe[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [globalStatus, setGlobalStatus] = useState<GlobalBookStatus | null>(null);
  const [pins, setPins] = useState<PinnedRecipe[]>([]);
  const [busy, setBusy] = useState<string | null>(null);
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
    }, 300);
  };

  const handleCopyAsOwn = async (pinnedId: string) => {
    setBusy(pinnedId);
    try {
      await copyPinnedAsOwn(pinnedId);
      await Promise.all([loadRecipes(search || undefined), loadPins()]);
    } catch (err: any) {
      alert(err.message || 'Failed to copy recipe');
    } finally {
      setBusy(null);
    }
  };

  const handleUnpin = async (pinnedId: string) => {
    if (!confirm('Unpin this recipe?')) return;
    setBusy(pinnedId);
    try {
      await unpinRecipe(pinnedId);
      await loadPins();
    } catch (err: any) {
      alert(err.message || 'Failed to unpin');
    } finally {
      setBusy(null);
    }
  };

  const handleAcceptUpdate = async (pinnedId: string) => {
    setBusy(pinnedId);
    try {
      await acceptPinUpdate(pinnedId);
      await loadPins();
    } catch (err: any) {
      alert(err.message || 'Failed to accept update');
    } finally {
      setBusy(null);
    }
  };

  const globalEnabled = globalStatus?.enabled ?? false;

  // Filter pinned recipes by search term (client-side)
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
          placeholder="Search recipes..."
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
                  onClick={() => handleUnpin(pin.id)}
                >
                  Unpin
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

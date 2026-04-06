import { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { apiGet } from '../api/client';
import { getGlobalBookStatus, getMyPins } from '../api/globalRecipes';
import { PinnedRecipesSection } from './PinnedRecipesSection';
import type { Recipe } from './types';
import type { GlobalBookStatus, PinnedRecipe } from './global-types';

type Tab = 'mine' | 'global' | 'pinned';

export function RecipeListPage() {
  const [recipes, setRecipes] = useState<Recipe[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState<Tab>('mine');
  const [globalStatus, setGlobalStatus] = useState<GlobalBookStatus | null>(null);
  const [pins, setPins] = useState<PinnedRecipe[]>([]);
  const [pinsLoading, setPinsLoading] = useState(false);
  const debounceRef = useRef<ReturnType<typeof setTimeout>>();

  useEffect(() => {
    loadRecipes();
    getGlobalBookStatus().then(setGlobalStatus).catch(() => setGlobalStatus({ enabled: false, reachable: false }));
  }, []);

  useEffect(() => {
    if (tab === 'pinned' && globalStatus?.enabled) {
      loadPins();
    }
  }, [tab]);

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
    setPinsLoading(true);
    try {
      setPins(await getMyPins());
    } finally {
      setPinsLoading(false);
    }
  };

  const handleSearchChange = (value: string) => {
    setSearch(value);
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      loadRecipes(value || undefined);
    }, 300);
  };

  const updateCount = pins.filter(p => p.hasUpdate).length;
  const globalEnabled = globalStatus?.enabled ?? false;

  return (
    <div className="page">
      <div className="page-header">
        <h1>Recipes</h1>
        {tab === 'mine' && (
          <div className="btn-group">
            <Link to="/recipes/scan" className="btn">Scan Card</Link>
            <Link to="/recipes/import" className="btn">Import URL</Link>
            <Link to="/recipes/new" className="btn btn-primary">New Recipe</Link>
          </div>
        )}
      </div>

      {globalEnabled && (
        <div className="tab-nav" style={{ display: 'flex', gap: '0.25rem', marginBottom: '1.5rem', borderBottom: '2px solid var(--border)' }}>
          <button
            className={`tab-btn ${tab === 'mine' ? 'active' : ''}`}
            onClick={() => setTab('mine')}
            style={tabStyle(tab === 'mine')}
          >
            My Recipes
          </button>
          <Link
            to="/recipes/global"
            className="tab-btn"
            style={tabStyle(false)}
          >
            Global Book
          </Link>
          <button
            className={`tab-btn ${tab === 'pinned' ? 'active' : ''}`}
            onClick={() => setTab('pinned')}
            style={tabStyle(tab === 'pinned')}
          >
            Pinned{updateCount > 0 ? ` (${updateCount})` : ''}
          </button>
        </div>
      )}

      {tab === 'mine' && (
        <>
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
          ) : recipes.length === 0 ? (
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
            </div>
          )}
        </>
      )}

      {tab === 'pinned' && (
        <PinnedRecipesSection pins={pins} loading={pinsLoading} onRefresh={loadPins} />
      )}
    </div>
  );
}

function tabStyle(active: boolean): React.CSSProperties {
  return {
    padding: '0.5rem 1rem',
    border: 'none',
    background: 'none',
    cursor: 'pointer',
    fontWeight: active ? 600 : 400,
    borderBottom: active ? '2px solid var(--primary)' : '2px solid transparent',
    marginBottom: '-2px',
    color: active ? 'var(--primary)' : 'inherit',
    textDecoration: 'none',
  };
}

import { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { browseGlobalRecipes, pinRecipe, searchWebRecipes } from '../api/globalRecipes';
import type { SharedRecipe, WebRecipeResult } from './global-types';

export function GlobalRecipeBookPage() {
  const [recipes, setRecipes] = useState<SharedRecipe[]>([]);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [pinning, setPinning] = useState<string | null>(null);

  // Web search state
  const [webResults, setWebResults] = useState<Map<string, WebRecipeResult[]>>(new Map());
  const [webLoading, setWebLoading] = useState(false);
  const [webSearched, setWebSearched] = useState(false);

  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => { loadRecipes(); }, []);

  const loadRecipes = async (searchTerm?: string, pageNum = 0) => {
    setLoading(true);
    try {
      const data = await browseGlobalRecipes(searchTerm || undefined, pageNum);
      setRecipes(data);
      setPage(pageNum);
    } finally {
      setLoading(false);
    }
  };

  const handleSearchChange = (value: string) => {
    setSearch(value);
    if (debounceRef.current !== null) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      loadRecipes(value || undefined, 0);
      // Also trigger web search if there's a query
      if (value.trim().length >= 2) {
        doWebSearch(value.trim());
      } else {
        setWebResults(new Map());
        setWebSearched(false);
      }
    }, 400);
  };

  const doWebSearch = async (query: string) => {
    setWebLoading(true);
    setWebSearched(true);
    try {
      const results = await searchWebRecipes(query);
      // Group by site
      const grouped = new Map<string, WebRecipeResult[]>();
      results.forEach(r => {
        const existing = grouped.get(r.site) || [];
        existing.push(r);
        grouped.set(r.site, existing);
      });
      setWebResults(grouped);
    } catch {
      setWebResults(new Map());
    } finally {
      setWebLoading(false);
    }
  };

  const handlePin = async (sharedId: string) => {
    setPinning(sharedId);
    try {
      await pinRecipe(sharedId);
      alert('Recipe pinned to your list!');
    } catch (err: any) {
      alert(err.message || 'Failed to pin recipe');
    } finally {
      setPinning(null);
    }
  };

  return (
    <div className="page">
      <div className="page-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <Link to="/recipes" className="btn btn-small">&larr; Back</Link>
          <h1>Global Recipe Book</h1>
        </div>
      </div>
      <div className="search-bar">
        <input
          type="text"
          placeholder="Search shared recipes and the web..."
          value={search}
          onChange={e => handleSearchChange(e.target.value)}
        />
      </div>

      {/* Local shared recipes */}
      {loading ? (
        <p>Loading...</p>
      ) : recipes.length === 0 && !search ? (
        <p className="empty">No shared recipes yet. Share your recipes for others to discover!</p>
      ) : (
        <>
          {recipes.length > 0 && (
            <>
              <h2 style={{ fontSize: '1.1rem', marginBottom: '0.75rem', color: 'var(--muted)' }}>
                From Family Instances
              </h2>
              <div className="card-grid">
                {recipes.map(recipe => (
                  <div key={recipe.id} className="card">
                    <Link to={`/recipes/global/${recipe.id}`}>
                      <h3>{recipe.name}</h3>
                      <p className="muted">by {recipe.attribution}</p>
                      <p>{recipe.baseServings} servings</p>
                      <p className="muted">
                        {recipe.ingredients.length} ingredient{recipe.ingredients.length !== 1 ? 's' : ''}
                      </p>
                    </Link>
                    {!recipe.ownedByCurrentInstance && (
                      <button
                        className="btn btn-small"
                        disabled={pinning === recipe.id}
                        onClick={(e) => { e.preventDefault(); handlePin(recipe.id); }}
                      >
                        {pinning === recipe.id ? 'Pinning...' : 'Pin'}
                      </button>
                    )}
                  </div>
                ))}
              </div>
              <div className="pagination" style={{ marginTop: '1rem', display: 'flex', gap: '0.5rem' }}>
                {page > 0 && (
                  <button className="btn btn-small" onClick={() => loadRecipes(search, page - 1)}>
                    Previous
                  </button>
                )}
                {recipes.length === 20 && (
                  <button className="btn btn-small" onClick={() => loadRecipes(search, page + 1)}>
                    Next
                  </button>
                )}
              </div>
            </>
          )}

          {recipes.length === 0 && search && (
            <p className="muted" style={{ marginBottom: '1rem' }}>No shared recipes match "{search}".</p>
          )}
        </>
      )}

      {/* Web search results */}
      {webSearched && (
        <div style={{ marginTop: '2rem' }}>
          <h2 style={{ fontSize: '1.1rem', marginBottom: '0.75rem', color: 'var(--muted)', borderTop: '1px solid var(--border)', paddingTop: '1.5rem' }}>
            Online Recipes
          </h2>
          {webLoading ? (
            <p>Searching the web...</p>
          ) : webResults.size === 0 ? (
            <p className="muted">No web results found for "{search}".</p>
          ) : (
            Array.from(webResults.entries()).map(([site, results]) => (
              <div key={site} style={{ marginBottom: '1.5rem' }}>
                <h3 style={{ fontSize: '0.95rem', color: 'var(--text)', marginBottom: '0.5rem' }}>
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

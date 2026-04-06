import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { browseGlobalRecipes, pinRecipe } from '../api/globalRecipes';
import type { SharedRecipe } from './global-types';

export function GlobalRecipeBookPage() {
  const [recipes, setRecipes] = useState<SharedRecipe[]>([]);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [pinning, setPinning] = useState<string | null>(null);

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

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    loadRecipes(search, 0);
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
        <h1>Global Recipe Book</h1>
      </div>
      <form onSubmit={handleSearch} className="search-bar">
        <input
          type="text"
          placeholder="Search shared recipes..."
          value={search}
          onChange={e => setSearch(e.target.value)}
        />
        <button type="submit">Search</button>
      </form>
      {loading ? (
        <p>Loading...</p>
      ) : recipes.length === 0 ? (
        <p className="empty">No shared recipes found.</p>
      ) : (
        <>
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
    </div>
  );
}

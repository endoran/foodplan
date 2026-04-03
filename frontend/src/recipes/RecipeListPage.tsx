import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { apiGet } from '../api/client';
import type { Recipe } from './types';

export function RecipeListPage() {
  const [recipes, setRecipes] = useState<Recipe[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadRecipes();
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

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    loadRecipes(search || undefined);
  };

  return (
    <div className="page">
      <div className="page-header">
        <h1>Recipes</h1>
        <div className="btn-group">
          <Link to="/recipes/scan" className="btn">Scan Card</Link>
          <Link to="/recipes/import" className="btn">Import URL</Link>
          <Link to="/recipes/new" className="btn btn-primary">New Recipe</Link>
        </div>
      </div>
      <form onSubmit={handleSearch} className="search-bar">
        <input
          type="text"
          placeholder="Search recipes..."
          value={search}
          onChange={e => setSearch(e.target.value)}
        />
        <button type="submit">Search</button>
      </form>
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
    </div>
  );
}

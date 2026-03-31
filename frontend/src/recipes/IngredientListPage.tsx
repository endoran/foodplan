import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { apiGet, apiDelete } from '../api/client';
import { formatEnum } from '../utils/formatEnum';
import type { Ingredient } from './types';

export function IngredientListPage() {
  const [ingredients, setIngredients] = useState<Ingredient[]>([]);
  const [search, setSearch] = useState('');

  const load = async () => {
    const params = search ? `?name=${encodeURIComponent(search)}` : '';
    const data = await apiGet<Ingredient[]>(`/api/v1/ingredients${params}`);
    setIngredients(data);
  };

  useEffect(() => { load(); }, []);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    load();
  };

  const handleDelete = async (id: string) => {
    await apiDelete(`/api/v1/ingredients/${id}`);
    load();
  };

  return (
    <div className="page">
      <div className="page-header">
        <h1>Ingredients</h1>
        <Link to="/ingredients/new" className="btn btn-primary">New Ingredient</Link>
      </div>

      <form className="search-bar" onSubmit={handleSearch}>
        <input
          type="text"
          placeholder="Search ingredients..."
          value={search}
          onChange={e => setSearch(e.target.value)}
        />
        <button type="submit" className="btn">Search</button>
      </form>

      <table className="ingredients-table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Grocery</th>
            <th>Storage</th>
            <th>Dietary Tags</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {ingredients.map(ing => (
            <tr key={ing.id}>
              <td>
                <Link to={`/ingredients/${ing.id}/edit`}>{ing.name}</Link>
                {ing.needsReview && <span className="review-badge">needs review</span>}
              </td>
              <td>{formatEnum(ing.groceryCategory)}</td>
              <td>{formatEnum(ing.storageCategory)}</td>
              <td>{ing.dietaryTags.length > 0 ? ing.dietaryTags.join(', ') : <span className="muted">none</span>}</td>
              <td>
                <div className="btn-group">
                  <Link to={`/ingredients/${ing.id}/edit`} className="btn btn-small">Edit</Link>
                  <button className="btn btn-danger btn-small" onClick={() => handleDelete(ing.id)}>Delete</button>
                </div>
              </td>
            </tr>
          ))}
          {ingredients.length === 0 && (
            <tr><td colSpan={5} className="empty">No ingredients found</td></tr>
          )}
        </tbody>
      </table>
    </div>
  );
}

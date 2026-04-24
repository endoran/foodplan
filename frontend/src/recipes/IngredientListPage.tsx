import { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { apiGet, apiDelete } from '../api/client';
import { formatEnum } from '../utils/formatEnum';
import type { Ingredient } from './types';

export function IngredientListPage() {
  const [ingredients, setIngredients] = useState<Ingredient[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const load = async (name?: string) => {
    setLoading(true);
    try {
      const params = name ? `?name=${encodeURIComponent(name)}` : '';
      const data = await apiGet<Ingredient[]>(`/api/v1/ingredients${params}`);
      setIngredients(data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const handleSearchChange = (value: string) => {
    setSearch(value);
    if (debounceRef.current !== null) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      load(value || undefined);
    }, 300);
  };

  const handleDelete = async (id: string) => {
    await apiDelete(`/api/v1/ingredients/${id}`);
    load(search || undefined);
  };

  return (
    <div className="page">
      <div className="page-header">
        <h1>Ingredients</h1>
        <div className="btn-group">
          <Link to="/ingredients/bulk-edit" className="btn">Bulk Edit</Link>
          <Link to="/ingredients/new" className="btn btn-primary">New Ingredient</Link>
        </div>
      </div>

      <div className="search-bar">
        <input
          type="text"
          placeholder="Search ingredients..."
          value={search}
          onChange={e => handleSearchChange(e.target.value)}
        />
      </div>

      {loading ? (
        <p>Loading...</p>
      ) : (
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
      )}
    </div>
  );
}

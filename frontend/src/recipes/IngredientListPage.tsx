import { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { apiGet, apiDelete, apiPost } from '../api/client';
import { formatEnum } from '../utils/formatEnum';
import type { Ingredient } from './types';

interface NormalizationResult {
  renames: { ingredientId: string; oldName: string; newName: string }[];
  merges: { winnerId: string; winnerName: string; loserId: string; loserName: string; canonicalName: string }[];
  skipped: number;
}

export function IngredientListPage() {
  const [ingredients, setIngredients] = useState<Ingredient[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [normalizing, setNormalizing] = useState(false);
  const [normalizePreview, setNormalizePreview] = useState<NormalizationResult | null>(null);
  const [normalizeError, setNormalizeError] = useState('');

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

  const handleNormalizePreview = async () => {
    setNormalizeError('');
    setNormalizing(true);
    try {
      const result = await apiPost<NormalizationResult>('/api/v1/ingredients/normalize?dryRun=true', {});
      setNormalizePreview(result);
    } catch (err) {
      setNormalizeError(err instanceof Error ? err.message : 'Normalization preview failed');
    } finally {
      setNormalizing(false);
    }
  };

  const handleNormalizeConfirm = async () => {
    setNormalizeError('');
    setNormalizing(true);
    try {
      await apiPost<NormalizationResult>('/api/v1/ingredients/normalize?dryRun=false', {});
      setNormalizePreview(null);
      load(search || undefined);
    } catch (err) {
      setNormalizeError(err instanceof Error ? err.message : 'Normalization failed');
    } finally {
      setNormalizing(false);
    }
  };

  return (
    <div className="page">
      <div className="page-header">
        <h1>Ingredients</h1>
        <div className="btn-group">
          <button
            className="btn"
            onClick={handleNormalizePreview}
            disabled={normalizing}
          >
            {normalizing ? 'Checking...' : 'Normalize Names'}
          </button>
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

      {normalizeError && <div className="error">{normalizeError}</div>}

      {normalizePreview && (
        <div className="normalize-preview">
          <h3>Normalization Preview</h3>
          {normalizePreview.renames.length === 0 && normalizePreview.merges.length === 0 ? (
            <p className="muted">All ingredient names are already normalized.</p>
          ) : (
            <>
              {normalizePreview.renames.length > 0 && (
                <div>
                  <h4>Renames ({normalizePreview.renames.length})</h4>
                  <ul>
                    {normalizePreview.renames.map(r => (
                      <li key={r.ingredientId}>{r.oldName} &rarr; {r.newName}</li>
                    ))}
                  </ul>
                </div>
              )}
              {normalizePreview.merges.length > 0 && (
                <div>
                  <h4>Merges ({normalizePreview.merges.length})</h4>
                  <ul>
                    {normalizePreview.merges.map(m => (
                      <li key={m.loserId}>
                        "{m.loserName}" will merge into "{m.winnerName}" &rarr; {m.canonicalName}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
              <div className="btn-group" style={{ marginTop: '0.75rem' }}>
                <button className="btn btn-primary" onClick={handleNormalizeConfirm} disabled={normalizing}>
                  {normalizing ? 'Normalizing...' : 'Apply Changes'}
                </button>
                <button className="btn" onClick={() => setNormalizePreview(null)}>Cancel</button>
              </div>
            </>
          )}
        </div>
      )}

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

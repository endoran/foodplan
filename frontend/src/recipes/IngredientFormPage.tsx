import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { apiGet, apiPost, apiPut } from '../api/client';

const STORAGE_CATEGORIES = ['DRY', 'FROZEN', 'FRESH', 'REFRIGERATED'];
const GROCERY_CATEGORIES = ['PRODUCE', 'MEAT', 'DAIRY', 'BAKING', 'ETHNIC', 'BULK', 'CANNED', 'BAKERY', 'DELI', 'HOUSEHOLD'];
const DIETARY_TAGS = ['GLUTEN_FREE', 'DAIRY_FREE', 'NUT_FREE', 'VEGAN', 'VEGETARIAN', 'KOSHER', 'HALAL', 'LOW_SODIUM', 'SUGAR_FREE', 'ORGANIC'];

interface IngredientDetail {
  id: string;
  name: string;
  storageCategory: string;
  groceryCategory: string;
  dietaryTags: string[];
}

export function IngredientFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEdit = !!id;

  const [name, setName] = useState('');
  const [storageCategory, setStorageCategory] = useState('DRY');
  const [groceryCategory, setGroceryCategory] = useState('PRODUCE');
  const [dietaryTags, setDietaryTags] = useState<Set<string>>(new Set());
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isEdit) {
      apiGet<IngredientDetail>(`/api/v1/ingredients/${id}`).then(ing => {
        setName(ing.name);
        setStorageCategory(ing.storageCategory);
        setGroceryCategory(ing.groceryCategory);
        setDietaryTags(new Set(ing.dietaryTags));
      });
    }
  }, [id]);

  const toggleTag = (tag: string) => {
    setDietaryTags(prev => {
      const next = new Set(prev);
      if (next.has(tag)) next.delete(tag);
      else next.add(tag);
      return next;
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    const body = {
      name,
      storageCategory,
      groceryCategory,
      dietaryTags: Array.from(dietaryTags),
    };

    try {
      if (isEdit) {
        await apiPut(`/api/v1/ingredients/${id}`, body);
      } else {
        await apiPost('/api/v1/ingredients', body);
      }
      navigate('/ingredients');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page">
      <h1>{isEdit ? 'Edit Ingredient' : 'New Ingredient'}</h1>
      <form onSubmit={handleSubmit} className="recipe-form">
        {error && <div className="error">{error}</div>}

        <label>
          Name
          <input type="text" value={name} onChange={e => setName(e.target.value)} required maxLength={200} />
        </label>

        <label>
          Storage Category
          <select value={storageCategory} onChange={e => setStorageCategory(e.target.value)}>
            {STORAGE_CATEGORIES.map(c => (
              <option key={c} value={c}>{c.charAt(0) + c.slice(1).toLowerCase()}</option>
            ))}
          </select>
        </label>

        <label>
          Grocery Category
          <select value={groceryCategory} onChange={e => setGroceryCategory(e.target.value)}>
            {GROCERY_CATEGORIES.map(c => (
              <option key={c} value={c}>{c.charAt(0) + c.slice(1).toLowerCase()}</option>
            ))}
          </select>
        </label>

        <div className="section">
          <h2>Dietary Tags</h2>
          <div className="tag-grid">
            {DIETARY_TAGS.map(tag => (
              <label key={tag} className="tag-checkbox">
                <input
                  type="checkbox"
                  checked={dietaryTags.has(tag)}
                  onChange={() => toggleTag(tag)}
                />
                {tag.replace(/_/g, ' ')}
              </label>
            ))}
          </div>
        </div>

        <div className="btn-group">
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'Saving...' : (isEdit ? 'Save Changes' : 'Create Ingredient')}
          </button>
          <button type="button" onClick={() => navigate('/ingredients')} className="btn">Cancel</button>
        </div>
      </form>
    </div>
  );
}

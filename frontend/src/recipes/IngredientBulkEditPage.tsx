import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { apiGet, apiPut, apiPost } from '../api/client';
import { formatEnum } from '../utils/formatEnum';
import type { Ingredient } from './types';

const STORAGE_CATEGORIES = ['PANTRY', 'FROZEN', 'FRESH', 'REFRIGERATED', 'SPICE_RACK', 'COUNTER'];
const GROCERY_CATEGORIES = ['PRODUCE', 'MEAT', 'DAIRY', 'BAKING', 'SPICES', 'ETHNIC', 'BULK', 'CANNED', 'BAKERY', 'DELI', 'HOUSEHOLD', 'OILS_CONDIMENTS', 'FROZEN'];
const DIETARY_TAGS = [
  { value: 'GLUTEN_FREE', label: 'GF' },
  { value: 'DAIRY_FREE', label: 'DF' },
  { value: 'NUT_FREE', label: 'NF' },
  { value: 'VEGAN', label: 'V' },
  { value: 'VEGETARIAN', label: 'VG' },
  { value: 'SUGAR_FREE', label: 'SF' },
];

interface EditableIngredient extends Ingredient {
  dirty: boolean;
}

export function IngredientBulkEditPage() {
  const [ingredients, setIngredients] = useState<EditableIngredient[]>([]);
  const [saving, setSaving] = useState(false);
  const [categorizing, setCategorizing] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const load = useCallback(async () => {
    const data = await apiGet<Ingredient[]>('/api/v1/ingredients');
    setIngredients(data.map(i => ({ ...i, dirty: false })));
    setMessage(null);
  }, []);

  useEffect(() => { load(); }, [load]);

  const updateField = (id: string, field: keyof Ingredient, value: unknown) => {
    setIngredients(prev => prev.map(ing =>
      ing.id === id ? { ...ing, [field]: value, dirty: true } : ing
    ));
  };

  const toggleTag = (id: string, tag: string) => {
    setIngredients(prev => prev.map(ing => {
      if (ing.id !== id) return ing;
      const tags = new Set(ing.dietaryTags);
      if (tags.has(tag)) tags.delete(tag); else tags.add(tag);
      return { ...ing, dietaryTags: [...tags], dirty: true };
    }));
  };

  const handleSave = async () => {
    const dirtyItems = ingredients.filter(i => i.dirty);
    if (dirtyItems.length === 0) { setMessage({ type: 'success', text: 'No changes to save' }); return; }
    setSaving(true);
    setMessage(null);
    try {
      const body = {
        ingredients: dirtyItems.map(i => ({
          id: i.id,
          name: i.name,
          storageCategory: i.storageCategory,
          groceryCategory: i.groceryCategory,
          dietaryTags: i.dietaryTags,
          shoppingListExclude: i.shoppingListExclude,
        })),
      };
      await apiPut<Ingredient[]>('/api/v1/ingredients/bulk', body);
      setMessage({ type: 'success', text: `Saved ${dirtyItems.length} ingredient${dirtyItems.length > 1 ? 's' : ''}` });
      await load();
    } catch (err: unknown) {
      setMessage({ type: 'error', text: err instanceof Error ? err.message : 'Save failed' });
    } finally {
      setSaving(false);
    }
  };

  const handleAutoCategorize = async () => {
    setCategorizing(true);
    setMessage(null);
    try {
      await apiPost<Ingredient[]>('/api/v1/ingredients/auto-categorize', {});
      setMessage({ type: 'success', text: 'Auto-categorized ingredients needing review' });
      await load();
    } catch (err: unknown) {
      setMessage({ type: 'error', text: err instanceof Error ? err.message : 'Auto-categorize failed' });
    } finally {
      setCategorizing(false);
    }
  };

  const dirtyCount = ingredients.filter(i => i.dirty).length;
  const reviewCount = ingredients.filter(i => i.needsReview).length;

  return (
    <div className="page">
      <div className="page-header">
        <h1>Bulk Edit Ingredients</h1>
        <div className="btn-group">
          <button className="btn" onClick={handleAutoCategorize} disabled={categorizing || reviewCount === 0}>
            {categorizing ? 'Categorizing...' : `Auto-Categorize (${reviewCount})`}
          </button>
          <button className="btn btn-primary" onClick={handleSave} disabled={saving || dirtyCount === 0}>
            {saving ? 'Saving...' : `Save Changes (${dirtyCount})`}
          </button>
          <Link to="/ingredients" className="btn">Back</Link>
        </div>
      </div>

      {message && <div className={message.type}>{message.text}</div>}

      <div style={{ overflowX: 'auto' }}>
        <table className="ingredients-table bulk-edit-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Storage</th>
              <th>Grocery</th>
              <th>Tags</th>
              <th>Excl.</th>
            </tr>
          </thead>
          <tbody>
            {ingredients.map(ing => (
              <tr key={ing.id} className={ing.dirty ? 'row-dirty' : ''}>
                <td>
                  <input
                    type="text"
                    value={ing.name}
                    onChange={e => updateField(ing.id, 'name', e.target.value)}
                    className="bulk-input"
                  />
                  {ing.needsReview && <span className="review-badge">review</span>}
                </td>
                <td>
                  <select
                    value={ing.storageCategory}
                    onChange={e => updateField(ing.id, 'storageCategory', e.target.value)}
                    className="bulk-select"
                  >
                    {STORAGE_CATEGORIES.map(c => (
                      <option key={c} value={c}>{formatEnum(c)}</option>
                    ))}
                  </select>
                </td>
                <td>
                  <select
                    value={ing.groceryCategory}
                    onChange={e => updateField(ing.id, 'groceryCategory', e.target.value)}
                    className="bulk-select"
                  >
                    {GROCERY_CATEGORIES.map(c => (
                      <option key={c} value={c}>{formatEnum(c)}</option>
                    ))}
                  </select>
                </td>
                <td>
                  <div className="tag-grid">
                    {DIETARY_TAGS.map(t => (
                      <label key={t.value} className="tag-checkbox" title={formatEnum(t.value)}>
                        <input
                          type="checkbox"
                          checked={ing.dietaryTags.includes(t.value)}
                          onChange={() => toggleTag(ing.id, t.value)}
                        />
                        {t.label}
                      </label>
                    ))}
                  </div>
                </td>
                <td style={{ textAlign: 'center' }}>
                  <input
                    type="checkbox"
                    checked={ing.shoppingListExclude}
                    onChange={e => updateField(ing.id, 'shoppingListExclude', e.target.checked)}
                  />
                </td>
              </tr>
            ))}
            {ingredients.length === 0 && (
              <tr><td colSpan={5} className="empty">No ingredients found</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

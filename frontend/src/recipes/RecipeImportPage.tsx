import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiPost } from '../api/client';
import { formatEnum } from '../utils/formatEnum';

interface ImportedIngredient {
  name: string;
  quantity: number;
  unit: string;
  rawText: string;
  prepNote?: string;
  section?: string;
}

interface ImportedPreview {
  name: string;
  instructions: string;
  baseServings: number;
  ingredients: ImportedIngredient[];
  sourceUrl: string;
}

interface IngredientPreparation {
  name: string;
  status: 'EXISTING' | 'NEW';
  storageCategory: string;
  groceryCategory: string;
  shoppingListExclude: boolean;
}

const UNITS = ['TSP', 'TBSP', 'CUP', 'PINT', 'QUART', 'GALLON', 'HALF_GALLON', 'FL_OZ', 'WHOLE', 'LBS', 'OZ', 'PINCH', 'PIECE', 'G', 'ML', 'KG', 'L'];
const STORAGE_CATEGORIES = ['PANTRY', 'FROZEN', 'FRESH', 'REFRIGERATED', 'SPICE_RACK', 'COUNTER'];
const GROCERY_CATEGORIES = ['PRODUCE', 'MEAT', 'DAIRY', 'BAKING', 'SPICES', 'ETHNIC', 'BULK', 'CANNED', 'BAKERY', 'DELI', 'HOUSEHOLD', 'OILS_CONDIMENTS', 'FROZEN'];

export function RecipeImportPage() {
  const navigate = useNavigate();
  const [url, setUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [preview, setPreview] = useState<ImportedPreview | null>(null);

  const [name, setName] = useState('');
  const [instructions, setInstructions] = useState('');
  const [baseServings, setBaseServings] = useState(1);
  const [ingredients, setIngredients] = useState<ImportedIngredient[]>([]);
  const [saving, setSaving] = useState(false);

  const [step, setStep] = useState<'edit' | 'review'>('edit');
  const [newIngredients, setNewIngredients] = useState<IngredientPreparation[]>([]);

  const handleImport = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    setPreview(null);
    setStep('edit');
    try {
      const data = await apiPost<ImportedPreview>('/api/v1/recipes/import', { url });
      setPreview(data);
      setName(data.name);
      setInstructions(data.instructions);
      setBaseServings(data.baseServings);
      setIngredients(data.ingredients);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Import failed');
    } finally {
      setLoading(false);
    }
  };

  const updateIngredient = (index: number, field: keyof ImportedIngredient, value: string | number) => {
    const updated = [...ingredients];
    updated[index] = { ...updated[index], [field]: value };
    setIngredients(updated);
  };

  const removeIngredient = (index: number) => {
    setIngredients(ingredients.filter((_, i) => i !== index));
  };

  const handleSave = async () => {
    setError('');
    setSaving(true);
    try {
      const names = ingredients.map(ing => ing.name);
      const preparations = await apiPost<IngredientPreparation[]>(
        '/api/v1/ingredients/prepare', { ingredientNames: names });
      const newOnes = preparations.filter(p => p.status === 'NEW');

      if (newOnes.length > 0) {
        setNewIngredients(newOnes);
        setStep('review');
      } else {
        await saveRecipe();
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed');
    } finally {
      setSaving(false);
    }
  };

  const saveRecipe = async () => {
    const body = {
      name,
      instructions: instructions || null,
      baseServings,
      ingredients: ingredients.map(ing => ({
        ingredientId: '',
        ingredientName: ing.name,
        quantity: ing.quantity,
        unit: ing.unit,
      })),
    };
    const created = await apiPost<{ id: string }>('/api/v1/recipes', body);
    navigate(`/recipes/${created.id}`);
  };

  const handleConfirmAndSave = async () => {
    setError('');
    setSaving(true);
    try {
      await apiPost('/api/v1/ingredients/batch', {
        ingredients: newIngredients.map(ing => ({
          name: ing.name,
          storageCategory: ing.storageCategory,
          groceryCategory: ing.groceryCategory,
          shoppingListExclude: ing.shoppingListExclude,
        })),
      });
      await saveRecipe();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed');
    } finally {
      setSaving(false);
    }
  };

  const updateNewIngredient = (index: number, field: keyof IngredientPreparation, value: string | boolean) => {
    const updated = [...newIngredients];
    updated[index] = { ...updated[index], [field]: value };
    setNewIngredients(updated);
  };

  return (
    <div className="page">
      <h1>Import Recipe from URL</h1>

      <form className="search-bar" onSubmit={handleImport} style={{ marginBottom: '1.5rem' }}>
        <input
          type="url"
          placeholder="Paste recipe URL (e.g. allrecipes.com/recipe/...)"
          value={url}
          onChange={e => setUrl(e.target.value)}
          required
        />
        <button type="submit" className="btn btn-primary" disabled={loading}>
          {loading ? 'Importing...' : 'Import'}
        </button>
      </form>

      {error && <div className="error">{error}</div>}

      {step === 'edit' && preview && (
        <div className="recipe-form">
          <p className="muted">Imported from: {preview.sourceUrl}</p>

          <label>
            Recipe Name
            <input type="text" value={name} onChange={e => setName(e.target.value)} required />
          </label>

          <label>
            Instructions
            <textarea value={instructions} onChange={e => setInstructions(e.target.value)} rows={6} />
          </label>

          <label>
            Base Servings
            <input
              type="number"
              value={baseServings}
              onChange={e => setBaseServings(parseInt(e.target.value) || 1)}
              min={1}
            />
          </label>

          <div className="section">
            <div className="section-header">
              <h2>Ingredients (review & edit)</h2>
            </div>
            <p className="muted">New ingredients will be reviewed in the next step.</p>
            {ingredients.map((ing, i) => (
              <div key={i}>
                {ing.section && (i === 0 || ingredients[i - 1]?.section !== ing.section) && (
                  <h3 style={{ margin: '1rem 0 0.5rem', fontSize: '0.95rem', color: '#666', borderBottom: '1px solid #ddd', paddingBottom: '0.25rem' }}>
                    {ing.section}
                  </h3>
                )}
                <div className="ingredient-row">
                  <div style={{ flex: 2, display: 'flex', flexDirection: 'column', gap: '0.15rem' }}>
                    <input
                      type="text"
                      value={ing.name}
                      onChange={e => updateIngredient(i, 'name', e.target.value)}
                      placeholder="Ingredient name"
                    />
                    {ing.prepNote && (
                      <span style={{ fontSize: '0.8rem', color: '#888', fontStyle: 'italic', paddingLeft: '0.25rem' }}>
                        Prep: {ing.prepNote}
                      </span>
                    )}
                  </div>
                  <input
                    type="number"
                    value={ing.quantity}
                    onChange={e => updateIngredient(i, 'quantity', parseFloat(e.target.value) || 0)}
                    step="0.01"
                    min="0"
                    style={{ flex: 1 }}
                  />
                  <select
                    value={ing.unit}
                    onChange={e => updateIngredient(i, 'unit', e.target.value)}
                    style={{ flex: 1 }}
                  >
                    {UNITS.map(u => <option key={u} value={u}>{u}</option>)}
                  </select>
                  <button type="button" className="btn btn-danger btn-small" onClick={() => removeIngredient(i)}>X</button>
                </div>
              </div>
            ))}
            {ingredients.length === 0 && <p className="muted">No ingredients parsed</p>}
          </div>

          <div className="btn-group">
            <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
              {saving ? 'Checking ingredients...' : 'Save Recipe'}
            </button>
            <button className="btn" onClick={() => { setPreview(null); setUrl(''); }}>Discard</button>
          </div>
        </div>
      )}

      {step === 'review' && (
        <div className="recipe-form">
          <h2>Review New Ingredients</h2>
          <p className="muted">These ingredients don't exist yet. Confirm their categories before saving.</p>

          <table className="ingredients-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Storage</th>
                <th>Grocery Aisle</th>
                <th>Exclude from List</th>
              </tr>
            </thead>
            <tbody>
              {newIngredients.map((ing, i) => (
                <tr key={i}>
                  <td>{ing.name}</td>
                  <td>
                    <select
                      value={ing.storageCategory}
                      onChange={e => updateNewIngredient(i, 'storageCategory', e.target.value)}
                    >
                      {STORAGE_CATEGORIES.map(c => (
                        <option key={c} value={c}>{formatEnum(c)}</option>
                      ))}
                    </select>
                  </td>
                  <td>
                    <select
                      value={ing.groceryCategory}
                      onChange={e => updateNewIngredient(i, 'groceryCategory', e.target.value)}
                    >
                      {GROCERY_CATEGORIES.map(c => (
                        <option key={c} value={c}>{formatEnum(c)}</option>
                      ))}
                    </select>
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    <input
                      type="checkbox"
                      checked={ing.shoppingListExclude}
                      onChange={e => updateNewIngredient(i, 'shoppingListExclude', e.target.checked)}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="btn-group">
            <button className="btn btn-primary" onClick={handleConfirmAndSave} disabled={saving}>
              {saving ? 'Saving...' : 'Confirm & Save'}
            </button>
            <button className="btn" onClick={() => setStep('edit')}>Back</button>
          </div>
        </div>
      )}
    </div>
  );
}

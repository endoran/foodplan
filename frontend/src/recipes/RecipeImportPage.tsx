import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiPost } from '../api/client';

interface ImportedIngredient {
  name: string;
  quantity: number;
  unit: string;
  rawText: string;
}

interface ImportedPreview {
  name: string;
  instructions: string;
  baseServings: number;
  ingredients: ImportedIngredient[];
  sourceUrl: string;
}

const UNITS = ['TSP', 'TBSP', 'CUP', 'PINT', 'QUART', 'GALLON', 'HALF_GALLON', 'UNIT', 'LBS', 'OZ', 'PINCH', 'PIECE'];

export function RecipeImportPage() {
  const navigate = useNavigate();
  const [url, setUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [preview, setPreview] = useState<ImportedPreview | null>(null);

  // Editable fields after import
  const [name, setName] = useState('');
  const [instructions, setInstructions] = useState('');
  const [baseServings, setBaseServings] = useState(1);
  const [ingredients, setIngredients] = useState<ImportedIngredient[]>([]);
  const [saving, setSaving] = useState(false);

  const handleImport = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    setPreview(null);
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
      // Save as a new recipe — ingredients reference by name, not ID, since these are imported
      // The user will need to map them to existing ingredients or create new ones
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
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed');
    } finally {
      setSaving(false);
    }
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

      {preview && (
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
            {ingredients.map((ing, i) => (
              <div key={i} className="ingredient-row">
                <input
                  type="text"
                  value={ing.name}
                  onChange={e => updateIngredient(i, 'name', e.target.value)}
                  style={{ flex: 2 }}
                  placeholder="Ingredient name"
                />
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
            ))}
            {ingredients.length === 0 && <p className="muted">No ingredients parsed</p>}
          </div>

          <div className="btn-group">
            <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
              {saving ? 'Saving...' : 'Save Recipe'}
            </button>
            <button className="btn" onClick={() => { setPreview(null); setUrl(''); }}>Discard</button>
          </div>
        </div>
      )}
    </div>
  );
}

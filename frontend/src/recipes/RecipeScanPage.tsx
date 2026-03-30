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

export function RecipeScanPage() {
  const navigate = useNavigate();
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [preview, setPreview] = useState<ImportedPreview | null>(null);

  const [name, setName] = useState('');
  const [instructions, setInstructions] = useState('');
  const [baseServings, setBaseServings] = useState(1);
  const [ingredients, setIngredients] = useState<ImportedIngredient[]>([]);
  const [saving, setSaving] = useState(false);

  const handleScan = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!file) return;
    setError('');
    setLoading(true);
    setPreview(null);

    const formData = new FormData();
    formData.append('file', file);

    try {
      const token = localStorage.getItem('token');
      const response = await fetch('/api/v1/recipes/scan', {
        method: 'POST',
        headers: token ? { 'Authorization': `Bearer ${token}` } : {},
        body: formData,
      });

      if (!response.ok) {
        const body = await response.json().catch(() => ({ error: 'Scan failed' }));
        throw new Error(body.error || `HTTP ${response.status}`);
      }

      const data: ImportedPreview = await response.json();
      setPreview(data);
      setName(data.name);
      setInstructions(data.instructions);
      setBaseServings(data.baseServings);
      setIngredients(data.ingredients);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Scan failed');
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
      <h1>Scan Recipe Card</h1>

      <form onSubmit={handleScan} className="recipe-form" style={{ marginBottom: '1.5rem' }}>
        <label>
          Upload photo or PDF of recipe
          <input
            type="file"
            accept="image/*,application/pdf"
            onChange={e => setFile(e.target.files?.[0] || null)}
            required
          />
        </label>
        <button type="submit" className="btn btn-primary" disabled={loading || !file}>
          {loading ? 'Scanning...' : 'Scan'}
        </button>
      </form>

      {error && <div className="error">{error}</div>}

      {preview && (
        <div className="recipe-form">
          <p className="muted">Scanned text extracted via OCR — review and edit below</p>

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
            <p className="muted">New ingredients were auto-created and flagged for review in the Ingredients page.</p>
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
            {ingredients.length === 0 && <p className="muted">No ingredients parsed from scan</p>}
          </div>

          <div className="btn-group">
            <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
              {saving ? 'Saving...' : 'Save Recipe'}
            </button>
            <button className="btn" onClick={() => { setPreview(null); setFile(null); }}>Discard</button>
          </div>
        </div>
      )}
    </div>
  );
}

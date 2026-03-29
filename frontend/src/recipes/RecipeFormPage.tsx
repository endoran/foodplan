import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { apiGet, apiPost, apiPut } from '../api/client';
import type { Recipe, Ingredient } from './types';

interface IngredientRow {
  ingredientId: string;
  ingredientName: string;
  quantity: string;
  unit: string;
}

const UNITS = ['TSP', 'TBSP', 'CUP', 'PINT', 'QUART', 'GALLON', 'HALF_GALLON', 'UNIT', 'LBS', 'OZ', 'PINCH', 'PIECE'];

export function RecipeFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEdit = !!id;

  const [name, setName] = useState('');
  const [instructions, setInstructions] = useState('');
  const [baseServings, setBaseServings] = useState(1);
  const [ingredients, setIngredients] = useState<IngredientRow[]>([]);
  const [availableIngredients, setAvailableIngredients] = useState<Ingredient[]>([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadIngredients();
    if (isEdit) loadRecipe();
  }, [id]);

  const loadIngredients = async () => {
    const data = await apiGet<Ingredient[]>('/api/v1/ingredients');
    setAvailableIngredients(data);
  };

  const loadRecipe = async () => {
    const recipe = await apiGet<Recipe>(`/api/v1/recipes/${id}`);
    setName(recipe.name);
    setInstructions(recipe.instructions || '');
    setBaseServings(recipe.baseServings);
    setIngredients(recipe.ingredients.map(ing => ({
      ingredientId: ing.ingredientId,
      ingredientName: ing.ingredientName,
      quantity: String(ing.quantity),
      unit: ing.unit,
    })));
  };

  const addRow = () => {
    setIngredients([...ingredients, { ingredientId: '', ingredientName: '', quantity: '', unit: 'CUP' }]);
  };

  const removeRow = (index: number) => {
    setIngredients(ingredients.filter((_, i) => i !== index));
  };

  const updateRow = (index: number, field: keyof IngredientRow, value: string) => {
    const updated = [...ingredients];
    updated[index] = { ...updated[index], [field]: value };
    if (field === 'ingredientId') {
      const found = availableIngredients.find(i => i.id === value);
      updated[index].ingredientName = found?.name || '';
    }
    setIngredients(updated);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    const body = {
      name,
      instructions: instructions || null,
      baseServings,
      ingredients: ingredients
        .filter(row => row.ingredientId && row.quantity)
        .map(row => ({
          ingredientId: row.ingredientId,
          ingredientName: row.ingredientName,
          quantity: parseFloat(row.quantity),
          unit: row.unit,
        })),
    };

    try {
      if (isEdit) {
        await apiPut<Recipe>(`/api/v1/recipes/${id}`, body);
        navigate(`/recipes/${id}`);
      } else {
        const created = await apiPost<Recipe>('/api/v1/recipes', body);
        navigate(`/recipes/${created.id}`);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page">
      <h1>{isEdit ? 'Edit Recipe' : 'New Recipe'}</h1>
      <form onSubmit={handleSubmit} className="recipe-form">
        {error && <div className="error">{error}</div>}

        <label>
          Name
          <input type="text" value={name} onChange={e => setName(e.target.value)} required maxLength={200} />
        </label>

        <label>
          Instructions
          <textarea value={instructions} onChange={e => setInstructions(e.target.value)} rows={4} />
        </label>

        <label>
          Base Servings
          <input type="number" value={baseServings} onChange={e => setBaseServings(parseInt(e.target.value) || 1)} min={1} />
        </label>

        <div className="section">
          <div className="section-header">
            <h2>Ingredients</h2>
            <button type="button" onClick={addRow} className="btn btn-small">Add Ingredient</button>
          </div>
          {ingredients.map((row, i) => (
            <div key={i} className="ingredient-row">
              <select
                value={row.ingredientId}
                onChange={e => updateRow(i, 'ingredientId', e.target.value)}
                required
              >
                <option value="">Select ingredient...</option>
                {availableIngredients.map(ing => (
                  <option key={ing.id} value={ing.id}>{ing.name}</option>
                ))}
              </select>
              <input
                type="number"
                placeholder="Qty"
                value={row.quantity}
                onChange={e => updateRow(i, 'quantity', e.target.value)}
                step="0.01"
                min="0.01"
                required
              />
              <select value={row.unit} onChange={e => updateRow(i, 'unit', e.target.value)}>
                {UNITS.map(u => <option key={u} value={u}>{u}</option>)}
              </select>
              <button type="button" onClick={() => removeRow(i)} className="btn btn-danger btn-small">X</button>
            </div>
          ))}
        </div>

        <div className="btn-group">
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'Saving...' : (isEdit ? 'Save Changes' : 'Create Recipe')}
          </button>
          <button type="button" onClick={() => navigate(-1)} className="btn">Cancel</button>
        </div>
      </form>
    </div>
  );
}

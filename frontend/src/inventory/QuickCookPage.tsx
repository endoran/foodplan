import { useState, useEffect } from 'react';
import { apiGet, apiPost } from '../api/client';
import type { Ingredient } from '../recipes/types';

const UNITS = ['TSP', 'TBSP', 'CUP', 'PINT', 'QUART', 'GALLON', 'HALF_GALLON', 'UNIT', 'LBS', 'OZ', 'PINCH', 'PIECE'];

interface DeductRow {
  ingredientId: string;
  quantity: string;
  unit: string;
}

export function QuickCookPage() {
  const [ingredients, setIngredients] = useState<Ingredient[]>([]);
  const [rows, setRows] = useState<DeductRow[]>([{ ingredientId: '', quantity: '', unit: 'CUP' }]);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    apiGet<Ingredient[]>('/api/v1/ingredients').then(setIngredients);
  }, []);

  const addRow = () => {
    setRows([...rows, { ingredientId: '', quantity: '', unit: 'CUP' }]);
  };

  const removeRow = (index: number) => {
    if (rows.length > 1) setRows(rows.filter((_, i) => i !== index));
  };

  const updateRow = (index: number, field: keyof DeductRow, value: string) => {
    const updated = [...rows];
    updated[index] = { ...updated[index], [field]: value };
    setRows(updated);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);

    const items = rows
      .filter(r => r.ingredientId && r.quantity)
      .map(r => ({
        ingredientId: r.ingredientId,
        quantity: parseFloat(r.quantity),
        unit: r.unit,
      }));

    if (items.length === 0) {
      setError('Add at least one ingredient');
      setLoading(false);
      return;
    }

    try {
      await apiPost('/api/v1/inventory/deduct', items);
      setSuccess(`Deducted ${items.length} ingredient${items.length !== 1 ? 's' : ''} from pantry`);
      setRows([{ ingredientId: '', quantity: '', unit: 'CUP' }]);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Deduction failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page">
      <div className="page-header">
        <h1>Quick Cook</h1>
      </div>
      <p className="muted" style={{ marginBottom: '1rem' }}>
        Made something? Enter the ingredients you used and we'll subtract them from your pantry.
      </p>

      {error && <div className="error">{error}</div>}
      {success && <div className="success">{success}</div>}

      <form onSubmit={handleSubmit} className="recipe-form">
        <div className="section">
          <div className="section-header">
            <h2>Ingredients Used</h2>
            <button type="button" onClick={addRow} className="btn btn-small">Add Row</button>
          </div>
          {rows.map((row, i) => (
            <div key={i} className="ingredient-row">
              <select
                value={row.ingredientId}
                onChange={e => updateRow(i, 'ingredientId', e.target.value)}
                required
              >
                <option value="">Select ingredient...</option>
                {ingredients.map(ing => (
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
              {rows.length > 1 && (
                <button type="button" className="btn btn-danger btn-small" onClick={() => removeRow(i)}>X</button>
              )}
            </div>
          ))}
        </div>

        <button type="submit" className="btn btn-primary" disabled={loading}>
          {loading ? 'Deducting...' : 'Deduct from Pantry'}
        </button>
      </form>
    </div>
  );
}

import { useState, useEffect } from 'react';
import { apiGet, apiPost, apiPut, apiDelete } from '../api/client';
import type { Ingredient } from '../recipes/types';
import type { InventoryItem } from './types';

const UNITS = ['TSP', 'TBSP', 'CUP', 'PINT', 'QUART', 'GALLON', 'HALF_GALLON', 'UNIT', 'LBS', 'OZ', 'PINCH', 'PIECE'];

export function InventoryPage() {
  const [items, setItems] = useState<InventoryItem[]>([]);
  const [ingredients, setIngredients] = useState<Ingredient[]>([]);
  const [adding, setAdding] = useState(false);
  const [editId, setEditId] = useState<string | null>(null);
  const [error, setError] = useState('');

  // Add form state
  const [newIngredientId, setNewIngredientId] = useState('');
  const [newQuantity, setNewQuantity] = useState('');
  const [newUnit, setNewUnit] = useState('CUP');

  // Edit form state
  const [editQuantity, setEditQuantity] = useState('');
  const [editUnit, setEditUnit] = useState('');

  const load = async () => {
    const [inv, ing] = await Promise.all([
      apiGet<InventoryItem[]>('/api/v1/inventory'),
      apiGet<Ingredient[]>('/api/v1/ingredients'),
    ]);
    setItems(inv);
    setIngredients(ing);
  };

  useEffect(() => { load(); }, []);

  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      await apiPost('/api/v1/inventory', {
        ingredientId: newIngredientId,
        quantity: parseFloat(newQuantity),
        unit: newUnit,
      });
      setAdding(false);
      setNewIngredientId('');
      setNewQuantity('');
      setNewUnit('CUP');
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Add failed');
    }
  };

  const startEdit = (item: InventoryItem) => {
    setEditId(item.id);
    setEditQuantity(String(item.quantity));
    setEditUnit(item.unit);
  };

  const handleUpdate = async (id: string) => {
    setError('');
    try {
      await apiPut(`/api/v1/inventory/${id}`, {
        quantity: parseFloat(editQuantity),
        unit: editUnit,
      });
      setEditId(null);
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Update failed');
    }
  };

  const handleDelete = async (id: string) => {
    await apiDelete(`/api/v1/inventory/${id}`);
    load();
  };

  return (
    <div className="page">
      <div className="page-header">
        <h1>Pantry</h1>
        {!adding && (
          <button className="btn btn-primary" onClick={() => setAdding(true)}>Add Item</button>
        )}
      </div>

      {error && <div className="error">{error}</div>}

      <table className="ingredients-table">
        <thead>
          <tr>
            <th>Ingredient</th>
            <th>Quantity</th>
            <th>Unit</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {adding && (
            <tr>
              <td>
                <select value={newIngredientId} onChange={e => setNewIngredientId(e.target.value)} required>
                  <option value="">Select ingredient...</option>
                  {ingredients.map(ing => (
                    <option key={ing.id} value={ing.id}>{ing.name}</option>
                  ))}
                </select>
              </td>
              <td>
                <input
                  type="number"
                  value={newQuantity}
                  onChange={e => setNewQuantity(e.target.value)}
                  placeholder="Qty"
                  step="0.01"
                  min="0.01"
                  required
                  style={{ width: '80px' }}
                />
              </td>
              <td>
                <select value={newUnit} onChange={e => setNewUnit(e.target.value)}>
                  {UNITS.map(u => <option key={u} value={u}>{u}</option>)}
                </select>
              </td>
              <td>
                <div className="btn-group">
                  <button className="btn btn-primary btn-small" onClick={handleAdd}>Save</button>
                  <button className="btn btn-small" onClick={() => setAdding(false)}>Cancel</button>
                </div>
              </td>
            </tr>
          )}
          {items.map(item => (
            <tr key={item.id}>
              <td>{item.ingredientName}</td>
              <td>
                {editId === item.id ? (
                  <input
                    type="number"
                    value={editQuantity}
                    onChange={e => setEditQuantity(e.target.value)}
                    step="0.01"
                    min="0.01"
                    style={{ width: '80px' }}
                  />
                ) : item.quantity}
              </td>
              <td>
                {editId === item.id ? (
                  <select value={editUnit} onChange={e => setEditUnit(e.target.value)}>
                    {UNITS.map(u => <option key={u} value={u}>{u}</option>)}
                  </select>
                ) : item.unit}
              </td>
              <td>
                {editId === item.id ? (
                  <div className="btn-group">
                    <button className="btn btn-primary btn-small" onClick={() => handleUpdate(item.id)}>Save</button>
                    <button className="btn btn-small" onClick={() => setEditId(null)}>Cancel</button>
                  </div>
                ) : (
                  <div className="btn-group">
                    <button className="btn btn-small" onClick={() => startEdit(item)}>Edit</button>
                    <button className="btn btn-danger btn-small" onClick={() => handleDelete(item.id)}>Delete</button>
                  </div>
                )}
              </td>
            </tr>
          ))}
          {items.length === 0 && !adding && (
            <tr><td colSpan={4} className="empty">No items in pantry</td></tr>
          )}
        </tbody>
      </table>
    </div>
  );
}

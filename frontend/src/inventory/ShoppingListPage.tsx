import { useState, useEffect } from 'react';
import { apiGet } from '../api/client';
import { formatEnum } from '../utils/formatEnum';
import type { ShoppingListResponse } from './types';

function getMonday(d: Date): string {
  const date = new Date(d);
  const day = date.getDay();
  const diff = (day === 0 ? -6 : 1) - day;
  date.setDate(date.getDate() + diff);
  return date.toISOString().slice(0, 10);
}

function getSunday(d: Date): string {
  const date = new Date(d);
  const day = date.getDay();
  const diff = day === 0 ? 0 : 7 - day;
  date.setDate(date.getDate() + diff);
  return date.toISOString().slice(0, 10);
}

function formatCategory(cat: string): string {
  return formatEnum(cat);
}

export function ShoppingListPage() {
  const now = new Date();
  const [from, setFrom] = useState(getMonday(now));
  const [to, setTo] = useState(getSunday(now));
  const [result, setResult] = useState<ShoppingListResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const generate = async (fromDate: string, toDate: string) => {
    setError('');
    setLoading(true);
    try {
      const data = await apiGet<ShoppingListResponse>(`/api/v1/shopping-list?from=${fromDate}&to=${toDate}`);
      setResult(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to generate');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { generate(from, to); }, []); // auto-generate on mount

  const handleGenerate = () => generate(from, to);

  return (
    <div className="page">
      <div className="page-header">
        <h1>Shopping List</h1>
      </div>

      <div className="shopping-controls">
        <label>
          From
          <input type="date" value={from} onChange={e => setFrom(e.target.value)} />
        </label>
        <label>
          To
          <input type="date" value={to} onChange={e => setTo(e.target.value)} />
        </label>
        <button className="btn btn-primary" onClick={handleGenerate} disabled={loading}>
          {loading ? 'Generating...' : 'Generate'}
        </button>
      </div>

      {error && <div className="error">{error}</div>}

      {result && result.aisles.length === 0 && (
        <p className="empty">No items needed for this period</p>
      )}

      {result && result.aisles.map(aisle => (
        <div key={aisle.category} className="section">
          <h2>{formatCategory(aisle.category)}</h2>
          <table className="ingredients-table">
            <thead>
              <tr>
                <th>Ingredient</th>
                <th>Quantity</th>
                <th>Unit</th>
              </tr>
            </thead>
            <tbody>
              {aisle.items.map(item => (
                <tr key={item.ingredientId}>
                  <td>{item.ingredientName}</td>
                  <td>{Number(item.quantity) % 1 === 0 ? item.quantity : Number(item.quantity).toFixed(2)}</td>
                  <td>{item.unit}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ))}
    </div>
  );
}

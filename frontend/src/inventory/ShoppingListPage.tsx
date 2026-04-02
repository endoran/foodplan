import { useState, useEffect, Fragment } from 'react';
import { apiGet } from '../api/client';
import { formatEnum } from '../utils/formatEnum';
import type { ShoppingListResponse } from './types';

const STORES = [
  { value: '', label: 'No Store' },
  { value: 'CHEF_STORE', label: "Cash 'n Carry" },
  { value: 'FRED_MEYER', label: 'Fred Meyer' },
];

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

function formatQty(q: number): string {
  return q % 1 === 0 ? String(q) : q.toFixed(2);
}

function formatPrice(p: number | undefined): string {
  return p != null ? `$${p.toFixed(2)}` : '';
}

export function ShoppingListPage() {
  const now = new Date();
  const [from, setFrom] = useState(getMonday(now));
  const [to, setTo] = useState(getSunday(now));
  const [store, setStore] = useState(() => localStorage.getItem('preferredStore') || '');
  const [result, setResult] = useState<ShoppingListResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const generate = async (fromDate: string, toDate: string, storeVal: string) => {
    setError('');
    setLoading(true);
    try {
      let url = `/api/v1/shopping-list?from=${fromDate}&to=${toDate}`;
      if (storeVal) url += `&store=${storeVal}`;
      const data = await apiGet<ShoppingListResponse>(url);
      setResult(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to generate');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { generate(from, to, store); }, []);

  const handleGenerate = () => generate(from, to, store);

  const handleStoreChange = (value: string) => {
    setStore(value);
    localStorage.setItem('preferredStore', value);
    generate(from, to, value);
  };

  const hasStore = !!result?.storeName;
  const colCount = hasStore ? 6 : 3;

  const total = hasStore
    ? result!.aisles.flatMap(a => a.items).reduce((sum, item) => {
        const unitPrice = item.storePromoPrice ?? item.storePrice;
        return sum + (unitPrice != null ? unitPrice * Number(item.quantity) : 0);
      }, 0)
    : 0;

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
        <label>
          Store
          <select value={store} onChange={e => handleStoreChange(e.target.value)}>
            {STORES.map(s => (
              <option key={s.value} value={s.value}>{s.label}</option>
            ))}
          </select>
        </label>
        <button className="btn btn-primary" onClick={handleGenerate} disabled={loading}>
          {loading ? 'Generating...' : 'Generate'}
        </button>
      </div>

      {error && <div className="error">{error}</div>}

      {result && hasStore && (
        <p className="muted" style={{ marginBottom: '0.75rem' }}>
          Showing prices for <strong>{result.storeName}</strong>
        </p>
      )}

      {result && result.aisles.length === 0 && (
        <p className="empty">No items needed for this period</p>
      )}

      {result && result.aisles.length > 0 && (
        <table className="ingredients-table">
          <thead>
            <tr>
              <th>Ingredient</th>
              {hasStore && <th>Aisle</th>}
              <th>Quantity</th>
              <th>Unit</th>
              {hasStore && <th>Price</th>}
              {hasStore && <th>Stock</th>}
            </tr>
          </thead>
          <tbody>
            {result.aisles.map(aisle => {
              const qty = (item: typeof aisle.items[0]) => Number(item.quantity);
              return (
                <Fragment key={aisle.category}>
                  <tr className="aisle-header-row">
                    <td colSpan={colCount}><strong>{formatEnum(aisle.category)}</strong></td>
                  </tr>
                  {aisle.items.map(item => {
                    const linePrice = item.storePromoPrice != null
                      ? item.storePromoPrice * qty(item)
                      : item.storePrice != null
                        ? item.storePrice * qty(item)
                        : undefined;
                    const lineRegular = item.storePromoPrice != null && item.storePrice != null
                      ? item.storePrice * qty(item)
                      : undefined;
                    return (
                      <tr key={item.ingredientId} title={item.storeProductName || undefined}>
                        <td>{item.ingredientName}</td>
                        {hasStore && <td>{item.storeAisle || '-'}</td>}
                        <td>{formatQty(qty(item))}</td>
                        <td>{formatEnum(item.unit)}</td>
                        {hasStore && (
                          <td>
                            {item.storePromoPrice != null ? (
                              <>
                                <span className="price-promo">{formatPrice(linePrice)}</span>
                                <span className="price-regular-struck">{formatPrice(lineRegular)}</span>
                              </>
                            ) : (
                              formatPrice(linePrice) || '-'
                            )}
                          </td>
                        )}
                        {hasStore && (
                          <td>
                            {item.storeStockLevel === 'HIGH' && <span className="stock-high">In Stock</span>}
                            {item.storeStockLevel === 'LOW' && <span className="stock-low">Low</span>}
                            {item.storeStockLevel === 'OUT' && <span className="stock-out">Out</span>}
                            {!item.storeStockLevel && '-'}
                          </td>
                        )}
                      </tr>
                    );
                  })}
                </Fragment>
              );
            })}
          </tbody>
          {hasStore && total > 0 && (
            <tfoot>
              <tr className="shopping-total">
                <td colSpan={4} style={{ textAlign: 'right' }}>Estimated Total</td>
                <td><strong>{formatPrice(total)}</strong></td>
                <td></td>
              </tr>
            </tfoot>
          )}
        </table>
      )}
    </div>
  );
}

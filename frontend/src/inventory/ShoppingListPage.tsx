import { useState, useEffect, useRef, useCallback, Fragment } from 'react';
import { apiGet } from '../api/client';
import { formatEnum } from '../utils/formatEnum';
import type { ShoppingItem, ShoppingListResponse, StoreProductAlternative } from './types';

const STORES = [
  { value: '', label: 'No Store' },
  { value: 'CHEF_STORE', label: "Cash 'n Carry" },
  { value: 'FRED_MEYER', label: 'Fred Meyer' },
  { value: 'FRED_MEYER_ONLINE', label: 'Fred Meyer (Online)' },
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

function formatPrice(p: number | undefined | null): string {
  return p != null ? `$${p.toFixed(2)}` : '';
}

function truncate(s: string, max: number): string {
  return s.length > max ? s.slice(0, max - 1) + '\u2026' : s;
}

function optionLabel(alt: StoreProductAlternative): string {
  let label = truncate(alt.productName || 'Unknown', 40);
  if (alt.packageSize) label += ` \u2014 ${alt.packageSize}`;
  if (alt.price != null) label += ` \u2014 $${(alt.promoPrice ?? alt.price).toFixed(2)}`;
  if (alt.stockLevel === 'OUT') label += ' (Out)';
  return label;
}

function itemKey(item: ShoppingItem): string {
  return item.ingredientId ?? (item.ingredientName + ':' + item.unit);
}

function getSelected(item: ShoppingItem, selections: Record<string, number>): StoreProductAlternative | null {
  if (!item.storeProducts?.length) return null;
  const idx = selections[itemKey(item)] ?? 0;
  return item.storeProducts[idx] ?? item.storeProducts[0];
}

type CacheEntry = { data: ShoppingListResponse; timestamp: number };

export function ShoppingListPage() {
  const now = new Date();
  const [from, setFrom] = useState(getMonday(now));
  const [to, setTo] = useState(getSunday(now));
  const [store, setStore] = useState(() => sessionStorage.getItem('selectedStore') || '');
  const [result, setResult] = useState<ShoppingListResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [selections, setSelections] = useState<Record<string, number>>({});

  const cache = useRef<Map<string, CacheEntry>>(new Map());
  const CACHE_TTL_MS = 5 * 60 * 1000;

  const cacheKey = useCallback((f: string, t: string, s: string) => `${f}|${t}|${s}`, []);

  const generate = useCallback(async (fromDate: string, toDate: string, storeVal: string, force = false) => {
    const key = cacheKey(fromDate, toDate, storeVal);

    if (!force) {
      const cached = cache.current.get(key);
      if (cached && Date.now() - cached.timestamp < CACHE_TTL_MS) {
        setResult(cached.data);
        setSelections({});
        return;
      }
    }

    setError('');
    setLoading(true);
    setSelections({});
    try {
      let url = `/api/v1/shopping-list?from=${fromDate}&to=${toDate}`;
      if (storeVal) url += `&store=${storeVal}`;
      const data = await apiGet<ShoppingListResponse>(url);
      setResult(data);
      cache.current.set(key, { data, timestamp: Date.now() });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to generate');
    } finally {
      setLoading(false);
    }
  }, [cacheKey]);

  const invalidateCache = useCallback(() => {
    cache.current.clear();
  }, []);

  useEffect(() => {
    generate(from, to, store);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleGenerate = () => {
    invalidateCache();
    generate(from, to, store, true);
  };

  const handleStoreChange = (value: string) => {
    setStore(value);
    if (value) {
      sessionStorage.setItem('selectedStore', value);
    } else {
      sessionStorage.removeItem('selectedStore');
    }
    generate(from, to, value);
  };

  const handleVariantChange = (key: string, index: number) => {
    setSelections(prev => ({ ...prev, [key]: index }));
  };

  const hasStore = !!result?.storeName;
  const colCount = hasStore ? 9 : 3;

  const total = hasStore
    ? result!.aisles.flatMap(a => a.items).reduce((sum, item) => {
        const sel = getSelected(item, selections);
        if (!sel) return sum;
        const price = sel.totalPromoPrice ?? sel.totalPrice;
        return sum + (price ?? 0);
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
              {hasStore && <th>Store Product</th>}
              {hasStore && <th>Pkg Size</th>}
              {hasStore && <th>Pkg Qty</th>}
              {hasStore && <th>Price</th>}
              {hasStore && <th>Stock</th>}
            </tr>
          </thead>
          <tbody>
            {result.aisles.map(aisle => (
              <Fragment key={aisle.category}>
                <tr className="aisle-header-row">
                  <td colSpan={colCount}><strong>{formatEnum(aisle.category)}</strong></td>
                </tr>
                {aisle.items.map(item => {
                  const sel = getSelected(item, selections);
                  const alts = item.storeProducts ?? [];
                  return (
                    <tr key={itemKey(item)}>
                      <td>{item.ingredientName}</td>
                      {hasStore && <td>{sel?.aisle || '-'}</td>}
                      <td>{formatQty(Number(item.quantity))}</td>
                      <td>{formatEnum(item.unit)}</td>
                      {hasStore && (
                        <td className="muted" style={{ fontSize: '0.85rem' }}>
                          {alts.length === 0 && '-'}
                          {alts.length === 1 && (sel?.productName || '-')}
                          {alts.length > 1 && (
                            <select
                              className="variant-select"
                              value={selections[itemKey(item)] ?? 0}
                              onChange={e => handleVariantChange(itemKey(item), Number(e.target.value))}
                            >
                              {alts.map((alt, idx) => (
                                <option key={alt.productId ?? idx} value={idx}>
                                  {optionLabel(alt)}
                                </option>
                              ))}
                            </select>
                          )}
                        </td>
                      )}
                      {hasStore && <td>{sel?.packageSize || '-'}</td>}
                      {hasStore && <td>{sel?.qtyNeeded ?? '-'}</td>}
                      {hasStore && (
                        <td>
                          {sel?.totalPromoPrice != null ? (
                            <>
                              <span className="price-promo">{formatPrice(sel.totalPromoPrice)}</span>
                              <span className="price-regular-struck">{formatPrice(sel.totalPrice)}</span>
                            </>
                          ) : (
                            formatPrice(sel?.totalPrice) || '-'
                          )}
                        </td>
                      )}
                      {hasStore && (
                        <td>
                          {sel?.stockLevel === 'HIGH' && <span className="stock-high">In Stock</span>}
                          {sel?.stockLevel === 'LOW' && <span className="stock-low">Low</span>}
                          {sel?.stockLevel === 'OUT' && <span className="stock-out">Out</span>}
                          {!sel?.stockLevel && '-'}
                        </td>
                      )}
                    </tr>
                  );
                })}
              </Fragment>
            ))}
          </tbody>
          {hasStore && total > 0 && (
            <tfoot>
              <tr className="shopping-total">
                <td colSpan={7} style={{ textAlign: 'right' }}>Estimated Total</td>
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

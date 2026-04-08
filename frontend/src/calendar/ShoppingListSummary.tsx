import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { apiGet } from '../api/client';
import { formatEnum } from '../utils/formatEnum';
import type { ShoppingListResponse } from '../inventory/types';

interface Props {
  from: string;
  to: string;
  refreshKey: number;
}

export function ShoppingListSummary({ from, to, refreshKey }: Props) {
  const [data, setData] = useState<ShoppingListResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [expanded, setExpanded] = useState(false);
  const [expandedAisles, setExpandedAisles] = useState<Set<string>>(new Set());

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    apiGet<ShoppingListResponse>(`/api/v1/shopping-list?from=${from}&to=${to}`)
      .then(res => { if (!cancelled) setData(res); })
      .catch(() => { if (!cancelled) setData(null); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [from, to, refreshKey]);

  const totalItems = data?.aisles.reduce((sum, a) => sum + a.items.length, 0) ?? 0;

  const toggleAisle = (cat: string) => {
    setExpandedAisles(prev => {
      const next = new Set(prev);
      if (next.has(cat)) next.delete(cat); else next.add(cat);
      return next;
    });
  };

  return (
    <div className="shopping-summary">
      <div className="shopping-summary-header" onClick={() => setExpanded(!expanded)}>
        <span>
          {expanded ? '\u25BE' : '\u25B8'}{' '}
          Shopping List {loading ? '...' : `(${totalItems} item${totalItems !== 1 ? 's' : ''})`}
        </span>
        <Link
          to={`/shopping-list`}
          className="btn btn-small"
          onClick={e => e.stopPropagation()}
        >
          View Full
        </Link>
      </div>

      {expanded && data && (
        <div className="shopping-summary-body">
          {data.aisles.length === 0 && <p className="muted">No items needed</p>}
          {data.aisles.map(aisle => (
            <div key={aisle.category}>
              <div className="shopping-aisle-header" onClick={() => toggleAisle(aisle.category)}>
                <span>{expandedAisles.has(aisle.category) ? '\u25BE' : '\u25B8'}</span>
                <span>{formatEnum(aisle.category)} ({aisle.items.length})</span>
              </div>
              {expandedAisles.has(aisle.category) && (
                <ul className="shopping-aisle-items">
                  {aisle.items.map(item => (
                    <li key={item.ingredientId}>
                      {item.ingredientName} &times; {Number(item.quantity) % 1 === 0 ? item.quantity : Number(item.quantity).toFixed(2)} {formatEnum(item.unit)}
                    </li>
                  ))}
                </ul>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

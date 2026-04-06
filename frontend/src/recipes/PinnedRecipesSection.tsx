import { useState } from 'react';
import { Link } from 'react-router-dom';
import { unpinRecipe, acceptPinUpdate } from '../api/globalRecipes';
import type { PinnedRecipe } from './global-types';

interface Props {
  pins: PinnedRecipe[];
  loading: boolean;
  onRefresh: () => void;
}

export function PinnedRecipesSection({ pins, loading, onRefresh }: Props) {
  const [busy, setBusy] = useState<string | null>(null);

  const handleUnpin = async (pinnedId: string) => {
    if (!confirm('Unpin this recipe?')) return;
    setBusy(pinnedId);
    try {
      await unpinRecipe(pinnedId);
      onRefresh();
    } catch (err: any) {
      alert(err.message || 'Failed to unpin');
    } finally {
      setBusy(null);
    }
  };

  const handleAcceptUpdate = async (pinnedId: string) => {
    setBusy(pinnedId);
    try {
      await acceptPinUpdate(pinnedId);
      onRefresh();
    } catch (err: any) {
      alert(err.message || 'Failed to accept update');
    } finally {
      setBusy(null);
    }
  };

  if (loading) return <p>Loading pinned recipes...</p>;

  if (pins.length === 0) {
    return <p className="empty">No pinned recipes. Browse the Global Book to pin recipes from other families!</p>;
  }

  return (
    <div className="card-grid">
      {pins.map(pin => (
        <div key={pin.id} className="card" style={{ position: 'relative' }}>
          {pin.hasUpdate && (
            <span style={{
              position: 'absolute', top: '0.5rem', right: '0.5rem',
              background: '#f59e0b', color: 'white', fontSize: '0.7rem',
              padding: '0.15rem 0.5rem', borderRadius: '999px', fontWeight: 600,
            }}>
              Update available
            </span>
          )}
          {pin.sourceRemoved && (
            <span style={{
              position: 'absolute', top: '0.5rem', right: '0.5rem',
              background: '#94a3b8', color: 'white', fontSize: '0.7rem',
              padding: '0.15rem 0.5rem', borderRadius: '999px', fontWeight: 600,
            }}>
              Source removed
            </span>
          )}
          <Link to={`/recipes/global/${pin.sharedRecipeId}`}>
            <h3>{pin.name}</h3>
          </Link>
          <p className="muted">by {pin.attribution}</p>
          <p>{pin.baseServings} servings</p>
          <p className="muted">
            {pin.ingredients.length} ingredient{pin.ingredients.length !== 1 ? 's' : ''}
            {' '}&middot; v{pin.pinnedVersion}
          </p>
          <div className="btn-group" style={{ marginTop: '0.5rem' }}>
            {pin.hasUpdate && (
              <button
                className="btn btn-small btn-primary"
                disabled={busy === pin.id}
                onClick={() => handleAcceptUpdate(pin.id)}
              >
                {busy === pin.id ? 'Updating...' : 'Accept Update'}
              </button>
            )}
            <button
              className="btn btn-small btn-danger"
              disabled={busy === pin.id}
              onClick={() => handleUnpin(pin.id)}
            >
              Unpin
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}

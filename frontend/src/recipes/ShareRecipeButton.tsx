import { useState } from 'react';
import { shareRecipe, unshareRecipe } from '../api/globalRecipes';

interface Props {
  recipeId: string;
  initialShared: boolean;
}

export function ShareRecipeButton({ recipeId, initialShared }: Props) {
  const [shared, setShared] = useState(initialShared);
  const [busy, setBusy] = useState(false);

  const toggle = async () => {
    setBusy(true);
    try {
      if (shared) {
        await unshareRecipe(recipeId);
        setShared(false);
      } else {
        await shareRecipe(recipeId);
        setShared(true);
      }
    } catch (err: any) {
      alert(err.message || 'Failed to update sharing');
    } finally {
      setBusy(false);
    }
  };

  return (
    <button className={`btn ${shared ? '' : 'btn-primary'}`} disabled={busy} onClick={toggle}>
      {busy ? 'Updating...' : shared ? 'Shared (Unshare)' : 'Share to Global Book'}
    </button>
  );
}

import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getSharedRecipe, pinRecipe } from '../api/globalRecipes';
import type { SharedRecipe } from './global-types';

export function SharedRecipeDetailPage() {
  const { sharedId } = useParams<{ sharedId: string }>();
  const [recipe, setRecipe] = useState<SharedRecipe | null>(null);
  const [loading, setLoading] = useState(true);
  const [pinning, setPinning] = useState(false);
  const [isPinned, setIsPinned] = useState(false);

  useEffect(() => { loadRecipe(); }, [sharedId]);

  const loadRecipe = async () => {
    setLoading(true);
    try {
      const data = await getSharedRecipe(sharedId!);
      setRecipe(data);
    } finally {
      setLoading(false);
    }
  };

  const handlePin = async () => {
    setPinning(true);
    try {
      await pinRecipe(sharedId!);
      setIsPinned(true);
    } catch {
      // Already pinned or other error — show as pinned
      setIsPinned(true);
    } finally {
      setPinning(false);
    }
  };

  if (loading && !recipe) return <p>Loading...</p>;
  if (!recipe) return <p>Shared recipe not found.</p>;

  const groups: { section: string | null; items: typeof recipe.ingredients }[] = [];
  recipe.ingredients.forEach((ing) => {
    const last = groups[groups.length - 1];
    if (last && last.section === (ing.section ?? null)) {
      last.items.push(ing);
    } else {
      groups.push({ section: ing.section ?? null, items: [ing] });
    }
  });

  return (
    <div className="page">
      <div className="page-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <Link to="/recipes/global" className="btn btn-small">&larr; Back</Link>
          <h1>{recipe.name}</h1>
        </div>
        <div className="btn-group">
          {!recipe.ownedByCurrentInstance && (
            isPinned ? (
              <span style={{
                display: 'inline-flex', alignItems: 'center',
                padding: '0.5rem 1rem', borderRadius: 'var(--radius)',
                background: 'var(--primary)', color: 'white',
                fontSize: '0.9rem', fontWeight: 500,
              }}>
                Pinned
              </span>
            ) : (
              <button className="btn btn-primary" disabled={pinning} onClick={handlePin}>
                {pinning ? 'Pinning...' : 'Pin to My Recipes'}
              </button>
            )
          )}
        </div>
      </div>

      <p className="muted">
        Shared by {recipe.attribution} &middot; v{recipe.version}
      </p>

      <div className="serving-scaler">
        <label>Servings:</label>
        <span className="serving-count">{recipe.baseServings}</span>
      </div>

      {recipe.instructions && (
        <div className="section">
          <h2>Instructions</h2>
          <p className="instructions">{recipe.instructions}</p>
        </div>
      )}

      {recipe.dietaryLabels?.length > 0 && (
        <div style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap', marginBottom: '1rem' }}>
          {recipe.dietaryLabels.map(label => (
            <span key={label} style={{
              fontSize: '0.8rem', padding: '0.2rem 0.6rem',
              borderRadius: '999px', background: '#16a34a',
              color: 'white', fontWeight: 500,
            }}>
              {label}
            </span>
          ))}
        </div>
      )}

      <div className="section">
        <h2>Ingredients</h2>
        {recipe.ingredients.length === 0 ? (
          <p className="muted">No ingredients listed.</p>
        ) : (
          groups.map((group, gi) => (
            <div key={gi}>
              {group.section && <h3>{group.section}</h3>}
              <table className="ingredients-table">
                <thead>
                  <tr><th>Ingredient</th><th>Quantity</th><th>Unit</th></tr>
                </thead>
                <tbody>
                  {group.items.map((ing, i) => (
                    <tr key={i}>
                      <td>{ing.ingredientName}</td>
                      <td>{ing.quantity}</td>
                      <td>{ing.unit}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ))
        )}
      </div>
    </div>
  );
}

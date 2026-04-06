import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getSharedRecipe, pinRecipe } from '../api/globalRecipes';
import type { SharedRecipe } from './global-types';

export function SharedRecipeDetailPage() {
  const { sharedId } = useParams<{ sharedId: string }>();
  const [recipe, setRecipe] = useState<SharedRecipe | null>(null);
  const [loading, setLoading] = useState(true);
  const [pinning, setPinning] = useState(false);

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
      alert('Recipe pinned to your list!');
    } catch (err: any) {
      alert(err.message || 'Failed to pin recipe');
    } finally {
      setPinning(false);
    }
  };

  if (loading && !recipe) return <p>Loading...</p>;
  if (!recipe) return <p>Shared recipe not found.</p>;

  // Group ingredients by section
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
        <h1>{recipe.name}</h1>
        <div className="btn-group">
          {!recipe.ownedByCurrentInstance && (
            <button className="btn btn-primary" disabled={pinning} onClick={handlePin}>
              {pinning ? 'Pinning...' : 'Pin to My Recipes'}
            </button>
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

      <Link to="/recipes/global" className="btn">Back to Global Book</Link>
    </div>
  );
}

import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { apiGet, apiDelete } from '../api/client';
import { getGlobalBookStatus } from '../api/globalRecipes';
import { ShareRecipeButton } from './ShareRecipeButton';
import type { Recipe } from './types';

export function RecipeDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [recipe, setRecipe] = useState<Recipe | null>(null);
  const [servings, setServings] = useState<number>(0);
  const [loading, setLoading] = useState(true);
  const [globalEnabled, setGlobalEnabled] = useState(false);

  useEffect(() => {
    loadRecipe();
    getGlobalBookStatus()
      .then(s => setGlobalEnabled(s.enabled))
      .catch(() => setGlobalEnabled(false));
  }, [id]);

  const loadRecipe = async (targetServings?: number) => {
    setLoading(true);
    try {
      const params = targetServings ? `?servings=${targetServings}` : '';
      const data = await apiGet<Recipe>(`/api/v1/recipes/${id}${params}`);
      setRecipe(data);
      if (!targetServings) setServings(data.baseServings);
    } finally {
      setLoading(false);
    }
  };

  const handleServingsChange = (newServings: number) => {
    if (newServings < 1) return;
    setServings(newServings);
    loadRecipe(newServings);
  };

  const handleDelete = async () => {
    if (!confirm('Delete this recipe?')) return;
    await apiDelete(`/api/v1/recipes/${id}`);
    navigate('/recipes');
  };

  if (loading && !recipe) return <p>Loading...</p>;
  if (!recipe) return <p>Recipe not found.</p>;

  return (
    <div className="page">
      <div className="page-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <Link to="/recipes" className="btn btn-small">&larr; Back</Link>
          <h1>{recipe.name}</h1>
        </div>
        <div className="btn-group">
          {globalEnabled && (
            <ShareRecipeButton recipeId={id!} initialShared={recipe.shared ?? false} />
          )}
          <Link to={`/recipes/${id}/edit`} className="btn">Edit</Link>
          <button onClick={handleDelete} className="btn btn-danger">Delete</button>
        </div>
      </div>

      <div className="serving-scaler">
        <label>Servings:</label>
        <button onClick={() => handleServingsChange(servings - 1)}>-</button>
        <span className="serving-count">{servings}</span>
        <button onClick={() => handleServingsChange(servings + 1)}>+</button>
        {servings !== recipe.baseServings && (
          <span className="muted">(base: {recipe.baseServings})</span>
        )}
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
          <p className="muted">No ingredients added.</p>
        ) : (
          (() => {
            const groups: { section: string | null; items: typeof recipe.ingredients }[] = [];
            recipe.ingredients.forEach((ing) => {
              const last = groups[groups.length - 1];
              if (last && last.section === (ing.section ?? null)) {
                last.items.push(ing);
              } else {
                groups.push({ section: ing.section ?? null, items: [ing] });
              }
            });

            return groups.map((group, gi) => (
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
            ));
          })()
        )}
      </div>
    </div>
  );
}

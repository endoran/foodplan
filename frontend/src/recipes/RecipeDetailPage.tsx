import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { apiGet, apiDelete } from '../api/client';
import type { Recipe } from './types';

export function RecipeDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [recipe, setRecipe] = useState<Recipe | null>(null);
  const [servings, setServings] = useState<number>(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadRecipe();
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
        <h1>{recipe.name}</h1>
        <div className="btn-group">
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
          <table className="ingredients-table">
            <thead>
              <tr><th>Ingredient</th><th>Quantity</th><th>Unit</th></tr>
            </thead>
            <tbody>
              {recipe.ingredients.map((ing, i) => (
                <tr key={i}>
                  <td>{ing.ingredientName}</td>
                  <td>{ing.quantity}</td>
                  <td>{ing.unit}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <Link to="/recipes" className="btn">Back to Recipes</Link>
    </div>
  );
}

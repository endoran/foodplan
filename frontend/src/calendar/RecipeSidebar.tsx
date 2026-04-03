import { useEffect, useState } from 'react';
import { apiGet } from '../api/client';
import { Recipe } from '../recipes/types';

export function RecipeSidebar() {
  const [recipes, setRecipes] = useState<Recipe[]>([]);
  const [search, setSearch] = useState('');

  useEffect(() => {
    apiGet<Recipe[]>('/api/v1/recipes').then(setRecipes);
  }, []);

  const filtered = recipes.filter(r =>
    r.name.toLowerCase().includes(search.toLowerCase())
  );

  const handleDragStart = (e: React.DragEvent, recipe: Recipe) => {
    e.dataTransfer.setData('application/json', JSON.stringify({
      recipeId: recipe.id,
      recipeName: recipe.name,
    }));
    e.dataTransfer.effectAllowed = 'copy';
  };

  return (
    <aside className="recipe-sidebar">
      <h3>Recipes</h3>
      <input
        type="text"
        placeholder="Search..."
        value={search}
        onChange={e => setSearch(e.target.value)}
        className="sidebar-search"
      />
      <div className="sidebar-list">
        {filtered.map(recipe => (
          <div
            key={recipe.id}
            className="sidebar-recipe"
            draggable
            onDragStart={e => handleDragStart(e, recipe)}
          >
            {recipe.name}
          </div>
        ))}
        {filtered.length === 0 && <p className="muted">No recipes found</p>}
      </div>
    </aside>
  );
}

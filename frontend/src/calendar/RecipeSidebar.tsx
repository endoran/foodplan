import { useEffect, useState } from 'react';
import { apiGet } from '../api/client';
import { getGlobalBookStatus, getMyPins } from '../api/globalRecipes';
import { Recipe } from '../recipes/types';
import type { PinnedRecipe } from '../recipes/global-types';

interface SidebarItem {
  id: string;
  name: string;
  pinnedId?: string; // set for pinned recipes
}

export function RecipeSidebar() {
  const [items, setItems] = useState<SidebarItem[]>([]);
  const [search, setSearch] = useState('');

  useEffect(() => {
    const loadAll = async () => {
      const recipesPromise = apiGet<Recipe[]>('/api/v1/recipes');
      let pinsPromise: Promise<PinnedRecipe[]> = Promise.resolve([]);

      try {
        const status = await getGlobalBookStatus();
        if (status.enabled) {
          pinsPromise = getMyPins();
        }
      } catch {
        // global book not available
      }

      const [recipes, pins] = await Promise.all([recipesPromise, pinsPromise]);

      const recipeItems: SidebarItem[] = recipes.map(r => ({
        id: r.id,
        name: r.name,
      }));

      const pinItems: SidebarItem[] = pins.map(p => ({
        id: p.sharedRecipeId,
        name: p.name,
        pinnedId: p.id,
      }));

      setItems([...recipeItems, ...pinItems]);
    };

    loadAll();
  }, []);

  const filtered = items.filter(r =>
    r.name.toLowerCase().includes(search.toLowerCase())
  );

  const handleDragStart = (e: React.DragEvent, item: SidebarItem) => {
    const data: Record<string, string> = {
      recipeId: item.id,
      recipeName: item.name,
    };
    if (item.pinnedId) {
      data.pinnedId = item.pinnedId;
    }
    e.dataTransfer.setData('application/json', JSON.stringify(data));
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
        {filtered.map(item => (
          <div
            key={item.pinnedId ? `pin-${item.pinnedId}` : item.id}
            className="sidebar-recipe"
            draggable
            onDragStart={e => handleDragStart(e, item)}
          >
            {item.name}
            {item.pinnedId && (
              <span style={{
                fontSize: '0.65rem', color: 'var(--primary)', fontWeight: 500,
                marginLeft: '0.35rem',
              }}>
                (pinned)
              </span>
            )}
          </div>
        ))}
        {filtered.length === 0 && <p className="muted">No recipes found</p>}
      </div>
    </aside>
  );
}

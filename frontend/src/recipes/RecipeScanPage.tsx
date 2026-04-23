import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiPost } from '../api/client';
import { parseFraction } from '../utils/parseFraction';
import { formatEnum } from '../utils/formatEnum';

interface ImportedIngredient {
  section: string | null;
  name: string;
  quantity: number | string;
  unit: string;
  rawText: string;
  prepNote?: string;
}

interface ImportedPreview {
  name: string;
  instructions: string;
  baseServings: number;
  ingredients: ImportedIngredient[];
  sourceUrl: string;
}

interface ScanResponse {
  scanSessionId: string;
  recipes: ImportedPreview[];
}

interface IngredientPreparation {
  name: string;
  status: 'EXISTING' | 'NEW';
  storageCategory: string;
  groceryCategory: string;
  shoppingListExclude: boolean;
}

const UNITS = ['TSP', 'TBSP', 'CUP', 'PINT', 'QUART', 'GALLON', 'HALF_GALLON', 'FL_OZ', 'WHOLE', 'LBS', 'OZ', 'PINCH', 'PIECE', 'G', 'ML', 'KG', 'L'];
const STORAGE_CATEGORIES = ['PANTRY', 'FROZEN', 'FRESH', 'REFRIGERATED', 'SPICE_RACK', 'COUNTER'];
const GROCERY_CATEGORIES = ['PRODUCE', 'MEAT', 'DAIRY', 'BAKING', 'SPICES', 'ETHNIC', 'BULK', 'CANNED', 'BAKERY', 'DELI', 'HOUSEHOLD', 'OILS_CONDIMENTS', 'FROZEN'];

export function RecipeScanPage() {
  const navigate = useNavigate();
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Multi-recipe state
  const [scanSessionId, setScanSessionId] = useState<string | null>(null);
  const [recipes, setRecipes] = useState<ImportedPreview[]>([]);
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [savedIndices, setSavedIndices] = useState<Set<number>>(new Set());

  // Edit state for selected recipe
  const [name, setName] = useState('');
  const [instructions, setInstructions] = useState('');
  const [baseServingsText, setBaseServingsText] = useState('1');
  const [ingredients, setIngredients] = useState<ImportedIngredient[]>([]);
  const [saving, setSaving] = useState(false);

  const [step, setStep] = useState<'upload' | 'select' | 'edit' | 'review'>('upload');
  const [newIngredients, setNewIngredients] = useState<IngredientPreparation[]>([]);

  const loadRecipe = (index: number, source?: ImportedPreview[]) => {
    const recipe = (source || recipes)[index];
    setSelectedIndex(index);
    setName(recipe.name);
    setInstructions(recipe.instructions);
    setBaseServingsText(String(recipe.baseServings));
    setIngredients(recipe.ingredients);
    setStep('edit');
  };

  const handleScan = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!file) return;
    setError('');
    setLoading(true);
    setRecipes([]);
    setScanSessionId(null);
    setSavedIndices(new Set());
    setStep('upload');

    const formData = new FormData();
    formData.append('file', file);

    try {
      const token = localStorage.getItem('token');
      const response = await fetch('/api/v1/recipes/scan', {
        method: 'POST',
        headers: token ? { 'Authorization': `Bearer ${token}` } : {},
        body: formData,
      });

      if (!response.ok) {
        const body = await response.json().catch(() => ({ error: 'Scan failed' }));
        throw new Error(body.error || `HTTP ${response.status}`);
      }

      const data: ScanResponse = await response.json();
      setScanSessionId(data.scanSessionId);
      setRecipes(data.recipes);

      if (data.recipes.length === 0) {
        setError('No recipes could be extracted from the image');
      } else if (data.recipes.length === 1) {
        loadRecipe(0, data.recipes);
      } else {
        setStep('select');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Scan failed');
    } finally {
      setLoading(false);
    }
  };

  const updateIngredient = (index: number, field: keyof ImportedIngredient, value: string | number) => {
    const updated = [...ingredients];
    updated[index] = { ...updated[index], [field]: value };
    setIngredients(updated);
  };

  const removeIngredient = (index: number) => {
    setIngredients(ingredients.filter((_, i) => i !== index));
  };

  const handleSave = async () => {
    setError('');
    setSaving(true);
    try {
      const names = ingredients.map(ing => ing.name);
      const preparations = await apiPost<IngredientPreparation[]>(
        '/api/v1/ingredients/prepare', { ingredientNames: names });
      const newOnes = preparations.filter(p => p.status === 'NEW');

      if (newOnes.length > 0) {
        setNewIngredients(newOnes);
        setStep('review');
      } else {
        await saveRecipe();
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed');
    } finally {
      setSaving(false);
    }
  };

  const saveRecipe = async () => {
    const baseServings = parseInt(baseServingsText);
    if (isNaN(baseServings) || baseServings < 1) {
      setError('Base servings must be at least 1');
      return;
    }

    const parsed = ingredients.map(ing => ({
      section: ing.section || null,
      ingredientId: '',
      ingredientName: ing.name,
      quantity: typeof ing.quantity === 'string' ? parseFraction(ing.quantity) : ing.quantity,
      unit: ing.unit,
    }));

    if (parsed.some(ing => isNaN(ing.quantity))) {
      setError('Invalid quantity — use a number or fraction (e.g. 1/2)');
      return;
    }

    const body: Record<string, unknown> = {
      name,
      instructions: instructions || null,
      baseServings,
      ingredients: parsed,
    };

    // Include scan session metadata for training pair generation
    if (scanSessionId) {
      body.scanSessionId = scanSessionId;
      body.scanRecipeIndex = selectedIndex;
    }

    const created = await apiPost<{ id: string }>('/api/v1/recipes', body);

    // Track saved recipe
    const newSaved = new Set(savedIndices);
    newSaved.add(selectedIndex);
    setSavedIndices(newSaved);

    // If multi-recipe and more unsaved, go back to selection
    const unsaved = recipes.filter((_, i) => !newSaved.has(i));
    if (recipes.length > 1 && unsaved.length > 0) {
      setStep('select');
    } else {
      navigate(`/recipes/${created.id}`);
    }
  };

  const handleConfirmAndSave = async () => {
    setError('');
    setSaving(true);
    try {
      await apiPost('/api/v1/ingredients/batch', {
        ingredients: newIngredients.map(ing => ({
          name: ing.name,
          storageCategory: ing.storageCategory,
          groceryCategory: ing.groceryCategory,
          shoppingListExclude: ing.shoppingListExclude,
        })),
      });
      await saveRecipe();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed');
    } finally {
      setSaving(false);
    }
  };

  const updateNewIngredient = (index: number, field: keyof IngredientPreparation, value: string | boolean) => {
    const updated = [...newIngredients];
    updated[index] = { ...updated[index], [field]: value };
    setNewIngredients(updated);
  };

  return (
    <div className="page">
      <h1>Scan Recipe Card</h1>

      <form onSubmit={handleScan} className="recipe-form" style={{ marginBottom: '1.5rem' }}>
        <label>
          Upload photo or PDF of recipe
          <input
            type="file"
            accept="image/*,.heic,.heif,application/pdf"
            onChange={e => setFile(e.target.files?.[0] || null)}
            required
          />
        </label>
        <button type="submit" className="btn btn-primary" disabled={loading || !file}>
          {loading ? 'Scanning...' : 'Scan'}
        </button>
      </form>

      {error && <div className="error">{error}</div>}

      {/* Recipe selection step (multi-recipe) */}
      {step === 'select' && recipes.length > 1 && (
        <div className="recipe-form">
          <h2>{recipes.length} Recipes Found</h2>
          <p className="muted">Select a recipe to review and save.</p>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {recipes.map((recipe, i) => (
              <button
                key={i}
                className={`btn ${savedIndices.has(i) ? '' : 'btn-primary'}`}
                style={{
                  textAlign: 'left',
                  padding: '1rem',
                  opacity: savedIndices.has(i) ? 0.5 : 1,
                }}
                onClick={() => loadRecipe(i)}
                disabled={savedIndices.has(i)}
              >
                <strong>{recipe.name}</strong>
                <span style={{ fontSize: '0.85rem', color: '#888', marginLeft: '0.75rem' }}>
                  {recipe.ingredients.length} ingredients
                  {savedIndices.has(i) && ' \u2014 saved'}
                </span>
              </button>
            ))}
          </div>
          {savedIndices.size > 0 && savedIndices.size < recipes.length && (
            <p className="muted" style={{ marginTop: '1rem' }}>
              {savedIndices.size} of {recipes.length} saved
            </p>
          )}
          {savedIndices.size === recipes.length && (
            <div style={{ marginTop: '1rem' }}>
              <p className="muted">All recipes saved!</p>
              <button className="btn" onClick={() => navigate('/recipes')}>Go to Recipes</button>
            </div>
          )}
        </div>
      )}

      {/* Edit step */}
      {step === 'edit' && (
        <div className="recipe-form">
          <p className="muted">
            {recipes.length > 1
              ? `Editing recipe ${selectedIndex + 1} of ${recipes.length} \u2014 review and save below`
              : 'Scanned text extracted \u2014 review and edit below'}
          </p>

          <label>
            Recipe Name
            <input type="text" value={name} onChange={e => setName(e.target.value)} required />
          </label>

          <label>
            Instructions
            <textarea value={instructions} onChange={e => setInstructions(e.target.value)} rows={6} />
          </label>

          <label>
            Base Servings
            <input
              type="number"
              value={baseServingsText}
              onChange={e => setBaseServingsText(e.target.value)}
              min={1}
            />
          </label>

          <div className="section">
            <div className="section-header">
              <h2>Ingredients (review & edit)</h2>
            </div>
            <p className="muted">New ingredients will be reviewed in the next step.</p>
            {(() => {
              const groups: { section: string | null; startIndex: number }[] = [];
              ingredients.forEach((ing, i) => {
                if (i === 0 || ing.section !== ingredients[i - 1].section) {
                  groups.push({ section: ing.section, startIndex: i });
                }
              });

              return groups.map((group, gi) => {
                const nextStart = gi + 1 < groups.length ? groups[gi + 1].startIndex : ingredients.length;
                const groupIngs = ingredients.slice(group.startIndex, nextStart);
                return (
                  <div key={gi} style={{ marginBottom: '1rem' }}>
                    {group.section && (
                      <input
                        type="text"
                        value={group.section}
                        onChange={e => {
                          const newSection = e.target.value;
                          const updated = [...ingredients];
                          for (let j = group.startIndex; j < nextStart; j++) {
                            updated[j] = { ...updated[j], section: newSection };
                          }
                          setIngredients(updated);
                        }}
                        className="section-label-input"
                        style={{ fontWeight: 'bold', fontSize: '1rem', marginBottom: '0.25rem', border: 'none', borderBottom: '1px solid #ccc', background: 'transparent', width: '100%' }}
                      />
                    )}
                    {groupIngs.map((ing, j) => {
                      const i = group.startIndex + j;
                      return (
                        <div key={i}>
                          <div className="ingredient-row">
                            <input
                              type="text"
                              value={ing.name}
                              onChange={e => updateIngredient(i, 'name', e.target.value)}
                              style={{ flex: 2 }}
                              placeholder="Ingredient name"
                            />
                            <input
                              type="text"
                              value={ing.quantity}
                              onChange={e => updateIngredient(i, 'quantity', e.target.value)}
                              placeholder="e.g. 1/2"
                              style={{ flex: 1 }}
                            />
                            <select
                              value={ing.unit}
                              onChange={e => updateIngredient(i, 'unit', e.target.value)}
                              style={{ flex: 1 }}
                            >
                              {UNITS.map(u => <option key={u} value={u}>{u}</option>)}
                            </select>
                            <button type="button" className="btn btn-danger btn-small" onClick={() => removeIngredient(i)}>X</button>
                          </div>
                          {ing.prepNote && (
                            <span style={{ fontSize: '0.8rem', color: '#888', fontStyle: 'italic', paddingLeft: '0.25rem' }}>
                              Prep: {ing.prepNote}
                            </span>
                          )}
                        </div>
                      );
                    })}
                  </div>
                );
              });
            })()}
            {ingredients.length === 0 && <p className="muted">No ingredients parsed from scan</p>}
          </div>

          <div className="btn-group">
            <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
              {saving ? 'Checking ingredients...' : 'Save Recipe'}
            </button>
            {recipes.length > 1 && (
              <button className="btn" onClick={() => setStep('select')}>Back to Selection</button>
            )}
            <button className="btn" onClick={() => { setRecipes([]); setScanSessionId(null); setFile(null); setStep('upload'); }}>Discard</button>
          </div>
        </div>
      )}

      {/* Review new ingredients step */}
      {step === 'review' && (
        <div className="recipe-form">
          <h2>Review New Ingredients</h2>
          <p className="muted">These ingredients don't exist yet. Confirm their categories before saving.</p>

          <table className="ingredients-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Storage</th>
                <th>Grocery Aisle</th>
                <th>Exclude from List</th>
              </tr>
            </thead>
            <tbody>
              {newIngredients.map((ing, i) => (
                <tr key={i}>
                  <td>{ing.name}</td>
                  <td>
                    <select
                      value={ing.storageCategory}
                      onChange={e => updateNewIngredient(i, 'storageCategory', e.target.value)}
                    >
                      {STORAGE_CATEGORIES.map(c => (
                        <option key={c} value={c}>{formatEnum(c)}</option>
                      ))}
                    </select>
                  </td>
                  <td>
                    <select
                      value={ing.groceryCategory}
                      onChange={e => updateNewIngredient(i, 'groceryCategory', e.target.value)}
                    >
                      {GROCERY_CATEGORIES.map(c => (
                        <option key={c} value={c}>{formatEnum(c)}</option>
                      ))}
                    </select>
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    <input
                      type="checkbox"
                      checked={ing.shoppingListExclude}
                      onChange={e => updateNewIngredient(i, 'shoppingListExclude', e.target.checked)}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="btn-group">
            <button className="btn btn-primary" onClick={handleConfirmAndSave} disabled={saving}>
              {saving ? 'Saving...' : 'Confirm & Save'}
            </button>
            <button className="btn" onClick={() => setStep('edit')}>Back</button>
          </div>
        </div>
      )}
    </div>
  );
}

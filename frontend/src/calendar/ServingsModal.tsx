import { useState } from 'react';
import { MealType } from './types';

interface ServingsModalProps {
  recipeName: string;
  onConfirm: (servings: number, mealType: MealType) => void;
  onCancel: () => void;
}

const MEAL_TYPES: MealType[] = ['BREAKFAST', 'LUNCH', 'DINNER', 'SNACK'];

export function ServingsModal({ recipeName, onConfirm, onCancel }: ServingsModalProps) {
  const [servingsText, setServingsText] = useState('2');
  const [mealType, setMealType] = useState<MealType>('DINNER');
  const [error, setError] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const n = parseInt(servingsText);
    if (isNaN(n) || n < 1) {
      setError('Servings must be at least 1');
      return;
    }
    onConfirm(n, mealType);
  };

  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <h3>Add "{recipeName}"</h3>
        {error && <div className="error">{error}</div>}
        <form onSubmit={handleSubmit} className="modal-form">
          <label>
            Meal
            <select value={mealType} onChange={e => setMealType(e.target.value as MealType)}>
              {MEAL_TYPES.map(mt => (
                <option key={mt} value={mt}>{mt.charAt(0) + mt.slice(1).toLowerCase()}</option>
              ))}
            </select>
          </label>
          <label>
            Servings
            <input
              type="number"
              min={1}
              value={servingsText}
              onChange={e => setServingsText(e.target.value)}
            />
          </label>
          <div className="btn-group">
            <button type="submit" className="btn btn-primary">Add</button>
            <button type="button" className="btn" onClick={onCancel}>Cancel</button>
          </div>
        </form>
      </div>
    </div>
  );
}

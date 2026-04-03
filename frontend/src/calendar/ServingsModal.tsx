import { useState } from 'react';
import { MealType } from './types';

interface ServingsModalProps {
  recipeName: string;
  onConfirm: (servings: number, mealType: MealType) => void;
  onCancel: () => void;
}

const MEAL_TYPES: MealType[] = ['BREAKFAST', 'LUNCH', 'DINNER', 'SNACK'];

export function ServingsModal({ recipeName, onConfirm, onCancel }: ServingsModalProps) {
  const [servings, setServings] = useState(2);
  const [mealType, setMealType] = useState<MealType>('DINNER');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (servings >= 1) {
      onConfirm(servings, mealType);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <h3>Add "{recipeName}"</h3>
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
              value={servings}
              onChange={e => setServings(parseInt(e.target.value) || 1)}
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

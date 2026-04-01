import { MealPlanEntry, DropData } from './types';
import { apiPost, apiDelete } from '../api/client';
import { formatEnum } from '../utils/formatEnum';

interface MealEntryProps {
  entry: MealPlanEntry;
  compact?: boolean;
  onUpdate: () => void;
}

const STATUS_CLASS: Record<string, string> = {
  PLANNED: 'status-planned',
  CONFIRMED: 'status-confirmed',
  SKIPPED: 'status-skipped',
};

export function MealEntry({ entry, compact, onUpdate }: MealEntryProps) {
  const handleDragStart = (e: React.DragEvent) => {
    const data: DropData = {
      recipeId: entry.recipeId,
      recipeName: entry.recipeName,
      entryId: entry.id,
    };
    e.dataTransfer.setData('application/json', JSON.stringify(data));
    e.dataTransfer.effectAllowed = 'move';
  };

  const handleConfirm = async (e: React.MouseEvent) => {
    e.stopPropagation();
    await apiPost(`/api/v1/meal-plan/${entry.id}/confirm`, {});
    onUpdate();
  };

  const handleDelete = async (e: React.MouseEvent) => {
    e.stopPropagation();
    await apiDelete(`/api/v1/meal-plan/${entry.id}`);
    onUpdate();
  };

  if (compact) {
    return (
      <div className={`meal-entry meal-entry-compact ${STATUS_CLASS[entry.status]}`} draggable onDragStart={handleDragStart}>
        <span className="meal-entry-name">{entry.recipeName}</span>
      </div>
    );
  }

  return (
    <div className={`meal-entry ${STATUS_CLASS[entry.status]}`} draggable onDragStart={handleDragStart}>
      <div className="meal-entry-header">
        <span className="meal-entry-type">{entry.mealType.charAt(0) + entry.mealType.slice(1).toLowerCase()}</span>
        <div className="meal-entry-actions">
          {entry.status === 'PLANNED' && (
            <button className="btn-icon" onClick={handleConfirm} title="Confirm">&#10003;</button>
          )}
          <button className="btn-icon btn-icon-danger" onClick={handleDelete} title="Delete">&times;</button>
        </div>
      </div>
      <span className="meal-entry-name">{entry.recipeName}</span>
      <span className="meal-entry-servings">{entry.servings} serving{entry.servings !== 1 ? 's' : ''}</span>
      {entry.warnings.length > 0 && (
        <div className="dietary-badges">
          <span className="dietary-badge">
            {[...new Set(entry.warnings.flatMap(w => w.tags))].map(t => formatEnum(t)).join(', ')}
          </span>
        </div>
      )}
    </div>
  );
}

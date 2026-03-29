import { useState } from 'react';
import { MealPlanEntry, DropData } from './types';
import { MealEntry } from './MealEntry';

interface DayCellProps {
  date: Date;
  entries: MealPlanEntry[];
  compact?: boolean;
  onDrop: (date: Date, data: DropData) => void;
  onUpdate: () => void;
}

function formatDate(d: Date): string {
  return d.toISOString().slice(0, 10);
}

function isToday(d: Date): boolean {
  return formatDate(d) === formatDate(new Date());
}

export function DayCell({ date, entries, compact, onDrop, onUpdate }: DayCellProps) {
  const [dragOver, setDragOver] = useState(false);

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'copy';
    setDragOver(true);
  };

  const handleDragLeave = () => setDragOver(false);

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    try {
      const data: DropData = JSON.parse(e.dataTransfer.getData('application/json'));
      onDrop(date, data);
    } catch { /* ignore bad data */ }
  };

  const todayClass = isToday(date) ? ' day-today' : '';
  const dragClass = dragOver ? ' day-dragover' : '';

  return (
    <div
      className={`day-cell${todayClass}${dragClass}`}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
    >
      <div className="day-header">
        <span className="day-number">{date.getDate()}</span>
        {!compact && <span className="day-name">{date.toLocaleDateString('en-US', { weekday: 'short' })}</span>}
      </div>
      <div className="day-entries">
        {entries.map(entry => (
          <MealEntry key={entry.id} entry={entry} compact={compact} onUpdate={onUpdate} />
        ))}
      </div>
    </div>
  );
}

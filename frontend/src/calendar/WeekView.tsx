import { MealPlanEntry, DropData } from './types';
import { DayCell } from './DayCell';

interface WeekViewProps {
  weekStart: Date;
  entries: MealPlanEntry[];
  onDrop: (date: Date, data: DropData) => void;
  onUpdate: () => void;
}

function getWeekDays(start: Date): Date[] {
  const days: Date[] = [];
  for (let i = 0; i < 7; i++) {
    const d = new Date(start);
    d.setDate(d.getDate() + i);
    days.push(d);
  }
  return days;
}

function formatDate(d: Date): string {
  return d.toISOString().slice(0, 10);
}

export function WeekView({ weekStart, entries, onDrop, onUpdate }: WeekViewProps) {
  const days = getWeekDays(weekStart);

  const entriesByDate = new Map<string, MealPlanEntry[]>();
  for (const entry of entries) {
    const key = entry.date;
    if (!entriesByDate.has(key)) entriesByDate.set(key, []);
    entriesByDate.get(key)!.push(entry);
  }

  return (
    <div className="week-grid">
      {days.map(day => (
        <DayCell
          key={formatDate(day)}
          date={day}
          entries={entriesByDate.get(formatDate(day)) || []}
          onDrop={onDrop}
          onUpdate={onUpdate}
        />
      ))}
    </div>
  );
}

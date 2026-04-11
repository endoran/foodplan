import { MealPlanEntry, DropData } from './types';
import { DayCell } from './DayCell';

interface MonthViewProps {
  year: number;
  month: number; // 0-indexed
  entries: MealPlanEntry[];
  onDrop: (date: Date, data: DropData) => void;
  onUpdate: () => void;
}

function getMonthGrid(year: number, month: number): (Date | null)[][] {
  const firstDay = new Date(year, month, 1);
  const lastDay = new Date(year, month + 1, 0);
  const startDow = (firstDay.getDay() + 6) % 7; // Monday = 0

  const weeks: (Date | null)[][] = [];
  let currentWeek: (Date | null)[] = [];

  for (let i = 0; i < startDow; i++) {
    currentWeek.push(null);
  }

  for (let d = 1; d <= lastDay.getDate(); d++) {
    currentWeek.push(new Date(year, month, d));
    if (currentWeek.length === 7) {
      weeks.push(currentWeek);
      currentWeek = [];
    }
  }

  if (currentWeek.length > 0) {
    while (currentWeek.length < 7) currentWeek.push(null);
    weeks.push(currentWeek);
  }

  return weeks;
}

function formatDate(d: Date): string {
  return d.toISOString().slice(0, 10);
}

const DOW_HEADERS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

export function MonthView({ year, month, entries, onDrop, onUpdate }: MonthViewProps) {
  const weeks = getMonthGrid(year, month);

  const entriesByDate = new Map<string, MealPlanEntry[]>();
  for (const entry of entries) {
    if (!entriesByDate.has(entry.date)) entriesByDate.set(entry.date, []);
    entriesByDate.get(entry.date)!.push(entry);
  }

  return (
    <div className="month-view">
      <div className="month-header-row">
        {DOW_HEADERS.map(d => <div key={d} className="month-dow">{d}</div>)}
      </div>
      {weeks.map((week, wi) => (
        <div key={wi} className="month-week-row">
          {week.map((day, di) =>
            day ? (
              <DayCell
                key={formatDate(day)}
                date={day}
                entries={entriesByDate.get(formatDate(day)) || []}
                compact
                onDrop={onDrop}
                onUpdate={onUpdate}
              />
            ) : (
              <div key={`empty-${di}`} className="day-cell day-empty" />
            )
          )}
        </div>
      ))}
    </div>
  );
}

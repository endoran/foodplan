import { useState, useCallback, useEffect, useMemo } from 'react';
import { apiGet, apiPost, apiPut } from '../api/client';
import { MealPlanEntry, CalendarView, DropData, CreateMealPlanRequest, MealType } from './types';
import { RecipeSidebar } from './RecipeSidebar';
import { WeekView } from './WeekView';
import { MonthView } from './MonthView';
import { ServingsModal } from './ServingsModal';
import { ShoppingListSummary } from './ShoppingListSummary';

function getMonday(d: Date): Date {
  const date = new Date(d);
  const day = date.getDay();
  const diff = (day === 0 ? -6 : 1) - day;
  date.setDate(date.getDate() + diff);
  date.setHours(0, 0, 0, 0);
  return date;
}

function formatDate(d: Date): string {
  return d.toISOString().slice(0, 10);
}

function addDays(d: Date, n: number): Date {
  const r = new Date(d);
  r.setDate(r.getDate() + n);
  return r;
}

export function CalendarPage() {
  const [view, setView] = useState<CalendarView>('week');
  const [currentDate, setCurrentDate] = useState(() => getMonday(new Date()));
  const [entries, setEntries] = useState<MealPlanEntry[]>([]);
  const [pendingDrop, setPendingDrop] = useState<{ date: Date; data: DropData } | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  const getDateRange = useCallback((): { from: string; to: string } => {
    if (view === 'week') {
      return { from: formatDate(currentDate), to: formatDate(addDays(currentDate, 6)) };
    }
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();
    const first = new Date(year, month, 1);
    const last = new Date(year, month + 1, 0);
    return { from: formatDate(first), to: formatDate(last) };
  }, [view, currentDate]);

  const loadEntries = useCallback(async () => {
    const { from, to } = getDateRange();
    const data = await apiGet<MealPlanEntry[]>(`/api/v1/meal-plan?from=${from}&to=${to}`);
    setEntries(data);
  }, [getDateRange]);

  useEffect(() => { loadEntries(); }, [loadEntries]);

  const handleMealChange = useCallback(() => {
    loadEntries();
    setRefreshKey(k => k + 1);
  }, [loadEntries]);

  const dateRange = useMemo(() => getDateRange(), [getDateRange]);

  const handlePrev = () => {
    if (view === 'week') {
      setCurrentDate(prev => addDays(prev, -7));
    } else {
      setCurrentDate(prev => {
        const d = new Date(prev);
        d.setMonth(d.getMonth() - 1, 1);
        return d;
      });
    }
  };

  const handleNext = () => {
    if (view === 'week') {
      setCurrentDate(prev => addDays(prev, 7));
    } else {
      setCurrentDate(prev => {
        const d = new Date(prev);
        d.setMonth(d.getMonth() + 1, 1);
        return d;
      });
    }
  };

  const handleToday = () => {
    setCurrentDate(view === 'week' ? getMonday(new Date()) : new Date());
  };

  const handleDrop = async (date: Date, data: DropData) => {
    if (data.entryId) {
      // Move existing meal to new date
      const entry = entries.find(e => e.id === data.entryId);
      if (entry) {
        await apiPut(`/api/v1/meal-plan/${data.entryId}`, {
          date: formatDate(date),
          mealType: entry.mealType,
          recipeId: entry.recipeId,
          servings: entry.servings,
          notes: entry.notes,
        });
        handleMealChange();
      }
    } else {
      // New recipe from sidebar
      setPendingDrop({ date, data });
    }
  };

  const handleModalConfirm = async (servings: number, mealType: MealType) => {
    if (!pendingDrop) return;
    const body: CreateMealPlanRequest = {
      date: formatDate(pendingDrop.date),
      mealType,
      recipeId: pendingDrop.data.recipeId,
      servings,
    };
    await apiPost('/api/v1/meal-plan', body);
    setPendingDrop(null);
    handleMealChange();
  };

  const getTitle = (): string => {
    if (view === 'week') {
      const end = addDays(currentDate, 6);
      const opts: Intl.DateTimeFormatOptions = { month: 'short', day: 'numeric' };
      return `${currentDate.toLocaleDateString('en-US', opts)} - ${end.toLocaleDateString('en-US', opts)}, ${end.getFullYear()}`;
    }
    return currentDate.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
  };

  return (
    <div className="calendar-page">
      <div className="page-header">
        <h1>Meal Calendar</h1>
        <div className="btn-group">
          <button
            className={`btn btn-small${view === 'week' ? ' btn-primary' : ''}`}
            onClick={() => setView('week')}
          >Week</button>
          <button
            className={`btn btn-small${view === 'month' ? ' btn-primary' : ''}`}
            onClick={() => setView('month')}
          >Month</button>
        </div>
      </div>

      <div className="calendar-nav">
        <button className="btn btn-small" onClick={handlePrev}>&lsaquo; Prev</button>
        <button className="btn btn-small" onClick={handleToday}>Today</button>
        <span className="calendar-title">{getTitle()}</span>
        <button className="btn btn-small" onClick={handleNext}>Next &rsaquo;</button>
      </div>

      <div className="calendar-layout">
        <RecipeSidebar />
        <div className="calendar-grid-area">
          {view === 'week' ? (
            <WeekView
              weekStart={currentDate}
              entries={entries}
              onDrop={handleDrop}
              onUpdate={handleMealChange}
            />
          ) : (
            <MonthView
              year={currentDate.getFullYear()}
              month={currentDate.getMonth()}
              entries={entries}
              onDrop={handleDrop}
              onUpdate={handleMealChange}
            />
          )}
        </div>
      </div>

      <ShoppingListSummary from={dateRange.from} to={dateRange.to} refreshKey={refreshKey} />

      {pendingDrop && (
        <ServingsModal
          recipeName={pendingDrop.data.recipeName}
          onConfirm={handleModalConfirm}
          onCancel={() => setPendingDrop(null)}
        />
      )}
    </div>
  );
}

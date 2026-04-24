import { useState, useEffect } from 'react';
import { getSettings, updateSettings } from '../api/settings';

export function SettingsPage() {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState('');

  const [timezone, setTimezone] = useState('');
  const [defaultServings, setDefaultServings] = useState('4');
  const [sites, setSites] = useState<string[]>([]);
  const [defaultSites, setDefaultSites] = useState<string[]>([]);
  const [newSite, setNewSite] = useState('');

  useEffect(() => {
    loadSettings();
  }, []);

  const loadSettings = async () => {
    try {
      const data = await getSettings();
      setTimezone(data.timezone);
      setDefaultServings(String(data.defaultServings));
      setSites(data.allowedRecipeSites);
      setDefaultSites(data.defaultRecipeSites);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load settings');
    } finally {
      setLoading(false);
    }
  };

  const missingSuggestions = defaultSites.filter(s => !sites.includes(s));

  const removeSite = (site: string) => {
    setSites(sites.filter(s => s !== site));
    setSaved(false);
  };

  const addSite = () => {
    const domain = newSite.trim().toLowerCase()
      .replace(/^https?:\/\//, '')
      .replace(/^www\./, '')
      .replace(/\/.*$/, '');
    if (!domain || !/^[a-z0-9.-]+\.[a-z]{2,}$/.test(domain)) {
      setError('Enter a valid domain (e.g. myrecipes.com)');
      return;
    }
    if (sites.includes(domain)) {
      setError('Site already in list');
      return;
    }
    setError('');
    setSites([...sites, domain]);
    setNewSite('');
    setSaved(false);
  };

  const addSuggestion = (site: string) => {
    if (!sites.includes(site)) {
      setSites([...sites, site]);
      setSaved(false);
    }
  };

  const resetToDefaults = () => {
    setSites([...defaultSites]);
    setSaved(false);
  };

  const handleSave = async () => {
    setError('');
    setSaving(true);
    setSaved(false);
    try {
      const servings = parseInt(defaultServings);
      if (isNaN(servings) || servings < 1) {
        setError('Default servings must be at least 1');
        return;
      }
      const data = await updateSettings({
        timezone,
        defaultServings: servings,
        allowedRecipeSites: sites,
      });
      setTimezone(data.timezone);
      setDefaultServings(String(data.defaultServings));
      setSites(data.allowedRecipeSites);
      setSaved(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save settings');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div className="page"><p>Loading settings...</p></div>;

  return (
    <div className="page">
      <h1>Settings</h1>

      {error && <div className="error">{error}</div>}

      <div className="settings-section">
        <h2>General</h2>
        <label>
          Timezone
          <input type="text" value={timezone} onChange={e => { setTimezone(e.target.value); setSaved(false); }} />
        </label>
        <label>
          Default Servings
          <input
            type="number"
            value={defaultServings}
            onChange={e => { setDefaultServings(e.target.value); setSaved(false); }}
            min={1}
          />
        </label>
      </div>

      <div className="settings-section">
        <h2>Internet Recipe Search Sites</h2>
        <p className="muted">These sites are searched when looking for recipes online in the Global Recipe Book.</p>

        {sites.length > 0 ? (
          <ul className="site-list">
            {sites.map(site => (
              <li key={site} className="site-item">
                <span>{site}</span>
                <button type="button" onClick={() => removeSite(site)} className="btn btn-danger btn-small">X</button>
              </li>
            ))}
          </ul>
        ) : (
          <p className="muted" style={{ marginBottom: '0.75rem' }}>No sites configured. Web search is disabled.</p>
        )}

        <div className="add-site-row">
          <input
            type="text"
            placeholder="e.g. myrecipes.com"
            value={newSite}
            onChange={e => setNewSite(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter') { e.preventDefault(); addSite(); } }}
          />
          <button type="button" onClick={addSite} className="btn btn-small">Add</button>
        </div>

        {missingSuggestions.length > 0 && (
          <div className="site-suggestions">
            <span className="muted" style={{ fontSize: '0.85rem' }}>Suggestions:</span>
            {missingSuggestions.map(s => (
              <button key={s} type="button" onClick={() => addSuggestion(s)} className="site-chip">{s}</button>
            ))}
          </div>
        )}

        {sites.length === 0 && (
          <button type="button" onClick={resetToDefaults} className="btn" style={{ marginTop: '0.5rem' }}>
            Reset to Defaults
          </button>
        )}
      </div>

      <div className="btn-group" style={{ marginTop: '1.5rem' }}>
        <button type="button" onClick={handleSave} className="btn btn-primary" disabled={saving}>
          {saving ? 'Saving...' : 'Save Settings'}
        </button>
        {saved && <span style={{ padding: '0.5rem', color: 'var(--primary)', fontWeight: 500 }}>Saved!</span>}
      </div>
    </div>
  );
}

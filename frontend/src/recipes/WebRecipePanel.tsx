import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiGet, apiPost } from '../api/client';
import type { WebRecipeResult } from './global-types';

interface ImportedIngredient {
  name: string;
  quantity: number | string;
  unit: string;
  rawText: string;
  section?: string;
}

interface ImportedPreview {
  name: string;
  instructions: string;
  baseServings: number;
  ingredients: ImportedIngredient[];
  sourceUrl: string;
}

interface Props {
  result: WebRecipeResult;
  mode: 'quicklook' | 'preview';
  onClose: () => void;
}

export function WebRecipePanel({ result, mode, onClose }: Props) {
  const navigate = useNavigate();
  const [ogImage, setOgImage] = useState<string | null>(null);
  const [preview, setPreview] = useState<ImportedPreview | null>(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [previewError, setPreviewError] = useState('');

  useEffect(() => {
    apiGet<{ image: string }>(`/api/v1/recipes/preview-meta?url=${encodeURIComponent(result.url)}`)
      .then(data => { if (data.image) setOgImage(data.image); })
      .catch(() => {});
  }, [result.url]);

  useEffect(() => {
    if (mode === 'preview') {
      setPreviewLoading(true);
      setPreviewError('');
      apiPost<ImportedPreview>('/api/v1/recipes/import', { url: result.url })
        .then(data => setPreview(data))
        .catch(err => setPreviewError(err instanceof Error ? err.message : 'Failed to parse recipe'))
        .finally(() => setPreviewLoading(false));
    }
  }, [mode, result.url]);

  const handleImport = () => {
    navigate(`/recipes/import?url=${encodeURIComponent(result.url)}`);
  };

  return (
    <>
      <div className="side-panel-backdrop" onClick={onClose} />
      <div className="side-panel">
        <div className="side-panel-header">
          <h3>{result.title}</h3>
          <button className="close-btn" onClick={onClose}>&times;</button>
        </div>

        <div className="side-panel-content">
          {ogImage && <img src={ogImage} alt="" className="og-image" />}

          {mode === 'quicklook' && (
            <iframe src={result.url} title={result.title} sandbox="allow-same-origin allow-scripts" />
          )}

          {mode === 'preview' && (
            <div className="side-panel-preview">
              {previewLoading && <p>Parsing recipe...</p>}
              {previewError && <p className="error">{previewError}</p>}
              {preview && (
                <>
                  <h4>{preview.name}</h4>
                  <p style={{ fontSize: '0.85rem', color: 'var(--muted)' }}>
                    {preview.baseServings} servings &middot; {preview.ingredients.length} ingredients
                  </p>
                  <ul className="preview-ingredients">
                    {preview.ingredients.map((ing, i) => (
                      <li key={i}>
                        {ing.quantity} {ing.unit} {ing.name}
                      </li>
                    ))}
                  </ul>
                </>
              )}
            </div>
          )}
        </div>

        <div className="side-panel-footer">
          <button className="btn btn-primary" onClick={handleImport} style={{ width: '100%' }}>
            Import This Recipe
          </button>
        </div>
      </div>
    </>
  );
}

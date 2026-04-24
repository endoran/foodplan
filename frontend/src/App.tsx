import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { LoginPage } from './auth/LoginPage';
import { RegisterPage } from './auth/RegisterPage';
import { Layout } from './layout/Layout';
import { RecipeListPage } from './recipes/RecipeListPage';
import { RecipeDetailPage } from './recipes/RecipeDetailPage';
import { RecipeFormPage } from './recipes/RecipeFormPage';
import { IngredientListPage } from './recipes/IngredientListPage';
import { IngredientFormPage } from './recipes/IngredientFormPage';
import { IngredientBulkEditPage } from './recipes/IngredientBulkEditPage';
import { RecipeImportPage } from './recipes/RecipeImportPage';
import { RecipeScanPage } from './recipes/RecipeScanPage';
import { GlobalRecipeBookPage } from './recipes/GlobalRecipeBookPage';
import { SharedRecipeDetailPage } from './recipes/SharedRecipeDetailPage';
import { CalendarPage } from './calendar/CalendarPage';
import { InventoryPage } from './inventory/InventoryPage';
import { ShoppingListPage } from './inventory/ShoppingListPage';
import { QuickCookPage } from './inventory/QuickCookPage';
import { SettingsPage } from './settings/SettingsPage';

export function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/recipes" element={<ProtectedRoute><RecipeListPage /></ProtectedRoute>} />
            <Route path="/recipes/new" element={<ProtectedRoute><RecipeFormPage /></ProtectedRoute>} />
            <Route path="/recipes/import" element={<ProtectedRoute><RecipeImportPage /></ProtectedRoute>} />
            <Route path="/recipes/scan" element={<ProtectedRoute><RecipeScanPage /></ProtectedRoute>} />
            <Route path="/recipes/global" element={<ProtectedRoute><GlobalRecipeBookPage /></ProtectedRoute>} />
            <Route path="/recipes/global/:sharedId" element={<ProtectedRoute><SharedRecipeDetailPage /></ProtectedRoute>} />
            <Route path="/recipes/:id" element={<ProtectedRoute><RecipeDetailPage /></ProtectedRoute>} />
            <Route path="/recipes/:id/edit" element={<ProtectedRoute><RecipeFormPage /></ProtectedRoute>} />
            <Route path="/ingredients" element={<ProtectedRoute><IngredientListPage /></ProtectedRoute>} />
            <Route path="/ingredients/bulk-edit" element={<ProtectedRoute><IngredientBulkEditPage /></ProtectedRoute>} />
            <Route path="/ingredients/new" element={<ProtectedRoute><IngredientFormPage /></ProtectedRoute>} />
            <Route path="/ingredients/:id/edit" element={<ProtectedRoute><IngredientFormPage /></ProtectedRoute>} />
            <Route path="/calendar" element={<ProtectedRoute><CalendarPage /></ProtectedRoute>} />
            <Route path="/inventory" element={<ProtectedRoute><InventoryPage /></ProtectedRoute>} />
            <Route path="/shopping-list" element={<ProtectedRoute><ShoppingListPage /></ProtectedRoute>} />
            <Route path="/quick-cook" element={<ProtectedRoute><QuickCookPage /></ProtectedRoute>} />
            <Route path="/settings" element={<ProtectedRoute><SettingsPage /></ProtectedRoute>} />
            <Route path="/" element={<Navigate to="/recipes" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

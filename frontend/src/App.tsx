import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { LoginPage } from './auth/LoginPage';
import { RegisterPage } from './auth/RegisterPage';
import { Layout } from './layout/Layout';
import { RecipeListPage } from './recipes/RecipeListPage';
import { RecipeDetailPage } from './recipes/RecipeDetailPage';
import { RecipeFormPage } from './recipes/RecipeFormPage';
import { CalendarPage } from './calendar/CalendarPage';

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
            <Route path="/recipes/:id" element={<ProtectedRoute><RecipeDetailPage /></ProtectedRoute>} />
            <Route path="/recipes/:id/edit" element={<ProtectedRoute><RecipeFormPage /></ProtectedRoute>} />
            <Route path="/calendar" element={<ProtectedRoute><CalendarPage /></ProtectedRoute>} />
            <Route path="/" element={<Navigate to="/recipes" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import Login from './Login';
import api from '../api';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
}));

vi.mock('../api', () => ({
  default: {
    post: vi.fn(),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() }
    }
  }
}));

describe('Login Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('renders login form elements correctly', () => {
    render(<Login />);
    
    expect(screen.getByText('Welcome Back')).toBeInTheDocument();
    expect(screen.getByText('Login to your TutorSys Portal')).toBeInTheDocument();
    expect(screen.getByLabelText(/username/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /login/i })).toBeInTheDocument();
  });

  it('navigates to /admin on successful ADMIN login', async () => {
    const mockResponse = {
      data: {
        token: 'mock-admin-token',
        id: 1,
        email: 'admin@tutorsys.com',
        role: 'ADMIN',
      },
    };
    vi.mocked(api.post).mockResolvedValueOnce(mockResponse);

    render(<Login />);

    fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'admin' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'password' } });
    fireEvent.click(screen.getByRole('button', { name: /login/i }));

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/auth/login', {
        username: 'admin',
        password: 'password',
      });
      expect(localStorage.getItem('token')).toBe('mock-admin-token');
      expect(JSON.parse(localStorage.getItem('user') || '{}')).toEqual({
        id: 1,
        username: 'admin',
        email: 'admin@tutorsys.com',
        role: 'ADMIN',
      });
      expect(mockNavigate).toHaveBeenCalledWith('/admin');
    });
  });

  it('navigates to /parent on successful PARENT login', async () => {
    const mockResponse = {
      data: {
        token: 'mock-parent-token',
        id: 2,
        email: 'parent@tutorsys.com',
        role: 'PARENT',
      },
    };
    vi.mocked(api.post).mockResolvedValueOnce(mockResponse);

    render(<Login />);

    fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'parent' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'password' } });
    fireEvent.click(screen.getByRole('button', { name: /login/i }));

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/parent');
    });
  });

  it('displays API error message on login failure', async () => {
    const errorResponse = {
      response: {
        data: {
          message: 'Invalid username or password credentials',
        },
      },
    };
    vi.mocked(api.post).mockRejectedValueOnce(errorResponse);

    render(<Login />);

    fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'baduser' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'badpass' } });
    fireEvent.click(screen.getByRole('button', { name: /login/i }));

    await waitFor(() => {
      expect(screen.getByText('Invalid username or password credentials')).toBeInTheDocument();
    });
  });
});

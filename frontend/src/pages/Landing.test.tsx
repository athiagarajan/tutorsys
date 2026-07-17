import { render, screen, fireEvent } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import Landing from './Landing';
import { ColorModeContext } from '../App';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
}));

describe('Landing Component', () => {
  const mockToggleColorMode = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  const renderLanding = () => {
    return render(
      <ColorModeContext.Provider value={{ toggleColorMode: mockToggleColorMode }}>
        <Landing />
      </ColorModeContext.Provider>
    );
  };

  it('renders landing page content correctly', () => {
    renderLanding();
    
    expect(screen.getByText('Student Tuition Management System')).toBeInTheDocument();
    expect(screen.getByText('Designed for Excellence')).toBeInTheDocument();
    expect(screen.getByText('Student Profile Management')).toBeInTheDocument();
    expect(screen.getByText('Secure Parent Portal')).toBeInTheDocument();
  });

  it('navigates to /login when Login Portal button is clicked', () => {
    renderLanding();
    
    const loginButton = screen.getByRole('button', { name: /login portal/i });
    fireEvent.click(loginButton);
    
    expect(mockNavigate).toHaveBeenCalledWith('/login');
  });

  it('navigates to /login when Go to Login button is clicked', () => {
    renderLanding();
    
    const heroLoginButton = screen.getByRole('button', { name: /go to login/i });
    fireEvent.click(heroLoginButton);
    
    expect(mockNavigate).toHaveBeenCalledWith('/login');
  });

  it('triggers theme color mode toggle when clicked', () => {
    renderLanding();
    
    const toggleButton = screen.getByRole('button', { name: /toggle theme/i });
    fireEvent.click(toggleButton);
    
    expect(mockToggleColorMode).toHaveBeenCalled();
  });
});

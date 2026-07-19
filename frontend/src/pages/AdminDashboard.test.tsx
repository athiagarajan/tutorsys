import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import AdminDashboard from './AdminDashboard';
import api from '../api';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
}));

vi.mock('../api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() }
    }
  }
}));

const mockStats = {
  totalStudents: 10,
  monthlyRevenue: 1500,
  totalOutstandingDues: 300,
  studentsByGrade: { 'Grade 10': 3 },
  studentsBySubject: { 'Math': 5 }
};

const mockStudents = [{ id: 1, name: 'Alice Smith', grade: 'Grade 10', parentId: 2, parentName: 'Bob Smith' }];
const mockParents = [{ id: 2, name: 'Bob Smith', email: 'bob@example.com', phone: '123' }];
const mockSchedules = [];
const mockInvoices = [];
const mockPayments = [];
const mockSubjects = [{ id: 1, name: 'Math', description: 'Algebra' }];

const mockSessions = [
  {
    id: 1,
    studentId: 1,
    studentName: 'Alice Smith',
    subjectId: 1,
    subjectName: 'Math',
    sessionDate: '2026-07-15',
    scheduledStartTime: '16:00:00',
    actualStartTime: '16:05:00',
    actualDurationMinutes: 45,
    status: 'CONDUCTED',
    rateCharged: 50.0,
    notes: 'Good progress'
  },
  {
    id: 2,
    studentId: 2,
    studentName: 'Charlie Brown',
    subjectId: 1,
    subjectName: 'Math',
    sessionDate: '2026-07-16',
    scheduledStartTime: '10:00:00',
    actualStartTime: null,
    actualDurationMinutes: null,
    status: 'CANCELLED',
    rateCharged: 0.0,
    notes: 'Cancelled by tutor'
  }
];

describe('AdminDashboard Component - Session Logs Filtering & Sorting', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    
    // Setup API mocks
    vi.mocked(api.get).mockImplementation((url) => {
      if (url === '/reports/dashboard') return Promise.resolve({ data: mockStats });
      if (url === '/students') return Promise.resolve({ data: mockStudents });
      if (url === '/parents') return Promise.resolve({ data: mockParents });
      if (url === '/schedules') return Promise.resolve({ data: mockSchedules });
      if (url === '/sessions') return Promise.resolve({ data: mockSessions });
      if (url === '/invoices') return Promise.resolve({ data: mockInvoices });
      if (url === '/payments') return Promise.resolve({ data: mockPayments });
      if (url === '/subjects') return Promise.resolve({ data: mockSubjects });
      return Promise.reject(new Error('Unknown url: ' + url));
    });
  });

  const selectTab = async (tabIndex: number) => {
    const tabs = screen.getAllByRole('tab');
    fireEvent.click(tabs[tabIndex]);
  };

  it('renders and displays formatted start and end time ranges correctly', async () => {
    render(<AdminDashboard />);
    
    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith('/sessions');
    });

    // Go to Tab 4: Attendance (index 4)
    await selectTab(4);

    expect(screen.getByText('Attendance & Session Logs')).toBeInTheDocument();
    
    // Verify first row session is displayed with calculated end time: 4:05 PM + 45 mins = 4:50 PM
    expect(screen.getByText('4:05 PM - 4:50 PM')).toBeInTheDocument();

    // Verify second row cancelled session shows scheduled start time: 10:00 AM - 11:00 AM (default 60 min duration fallback)
    expect(screen.getByText('10:00 AM - 11:00 AM')).toBeInTheDocument();
  });

  it('filters session logs by student name', async () => {
    render(<AdminDashboard />);
    await waitFor(() => expect(api.get).toHaveBeenCalled());
    await selectTab(4);

    // Verify both are present initially
    expect(screen.getByText('Alice Smith')).toBeInTheDocument();
    expect(screen.getByText('Charlie Brown')).toBeInTheDocument();

    // Filter by "Alice"
    const filterInput = screen.getByLabelText(/Filter by Student/i);
    fireEvent.change(filterInput, { target: { value: 'Alice' } });

    // Alice remains, Charlie is filtered out
    expect(screen.getByText('Alice Smith')).toBeInTheDocument();
    expect(screen.queryByText('Charlie Brown')).not.toBeInTheDocument();
  });

  it('filters session logs by status', async () => {
    render(<AdminDashboard />);
    await waitFor(() => expect(api.get).toHaveBeenCalled());
    await selectTab(4);

    // Filter by "Cancelled" status
    const statusSelect = screen.getByText('All Statuses');
    fireEvent.mouseDown(statusSelect);
    
    const option = await screen.findByRole('option', { name: /cancelled/i });
    fireEvent.click(option);

    // Charlie (CANCELLED) is present, Alice (CONDUCTED) is filtered out
    expect(screen.getByText('Charlie Brown')).toBeInTheDocument();
    expect(screen.queryByText('Alice Smith')).not.toBeInTheDocument();
  });
});

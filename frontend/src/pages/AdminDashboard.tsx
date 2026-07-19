import { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box, Container, Typography, Card, CardContent, Button, Tabs, Tab,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper,
  Dialog, DialogTitle, DialogContent, DialogActions, TextField, Select,
  MenuItem, FormControl, InputLabel, CircularProgress, Alert, IconButton,
  Chip, TableSortLabel
} from '@mui/material';
import LogoutIcon from '@mui/icons-material/Logout';
import RefreshIcon from '@mui/icons-material/Refresh';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import SendIcon from '@mui/icons-material/Send';
import CloudDownloadIcon from '@mui/icons-material/CloudDownload';
import api from '../api';

interface DashboardStats {
  totalStudents: number;
  monthlyRevenue: number;
  totalOutstandingDues: number;
  studentsByGrade: Record<string, number>;
  studentsBySubject: Record<string, number>;
  overdueParents: Array<{
    parentName: string;
    invoiceNumber: string;
    balanceDue: number;
    dueDate: string;
  }>;
}
const getLastSixMonthsDateString = () => {
  const d = new Date();
  d.setMonth(d.getMonth() - 6);
  return d.toISOString().split('T')[0];
};

const getTodayDateString = () => {
  return new Date().toISOString().split('T')[0];
};

export default function AdminDashboard() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState(0);
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [students, setStudents] = useState<any[]>([]);
  const [parents, setParents] = useState<any[]>([]);
  const [schedules, setSchedules] = useState<any[]>([]);
  const [sessions, setSessions] = useState<any[]>([]);
  const [invoices, setInvoices] = useState<any[]>([]);
  const [payments, setPayments] = useState<any[]>([]);
  const [subjects, setSubjects] = useState<any[]>([]);

  // Session Filtering & Sorting states
  const [sessionStudentFilter, setSessionStudentFilter] = useState('');
  const [sessionStatusFilter, setSessionStatusFilter] = useState('ALL');
  const [sessionStartFilter, setSessionStartFilter] = useState(getLastSixMonthsDateString());
  const [sessionEndFilter, setSessionEndFilter] = useState(getTodayDateString());
  const [sessionOrderBy, setSessionOrderBy] = useState<'sessionDate' | 'studentName' | 'subjectName'>('sessionDate');
  const [sessionOrderDir, setSessionOrderDir] = useState<'asc' | 'desc'>('desc');

  const formatSessionTimeRange = (startTimeStr: string | null, durationMinutes: number) => {
    if (!startTimeStr) return '—';
    const parts = startTimeStr.split(':');
    if (parts.length < 2) return startTimeStr;
    const hours = parseInt(parts[0], 10);
    const minutes = parseInt(parts[1], 10);
    
    const startDate = new Date();
    startDate.setHours(hours, minutes, 0, 0);
    
    const endDate = new Date(startDate.getTime() + (durationMinutes || 60) * 60 * 1000);
    
    const formatTime = (d: Date) => {
      let hh = d.getHours();
      let mm = d.getMinutes();
      const ampm = hh >= 12 ? 'PM' : 'AM';
      hh = hh % 12;
      hh = hh ? hh : 12;
      const mmStr = mm < 10 ? '0' + mm : mm;
      return `${hh}:${mmStr} ${ampm}`;
    };
    
    return `${formatTime(startDate)} - ${formatTime(endDate)}`;
  };

  const handleSessionSort = (property: 'sessionDate' | 'studentName' | 'subjectName') => {
    const isAsc = sessionOrderBy === property && sessionOrderDir === 'asc';
    setSessionOrderDir(isAsc ? 'desc' : 'asc');
    setSessionOrderBy(property);
  };


  const filteredAndSortedSessions = useMemo(() => {
    let result = [...sessions];

    if (sessionStudentFilter.trim() !== '') {
      result = result.filter(s => 
        s.studentName && s.studentName.toLowerCase().includes(sessionStudentFilter.toLowerCase())
      );
    }

    if (sessionStatusFilter !== 'ALL') {
      result = result.filter(s => s.status === sessionStatusFilter);
    }

    if (sessionStartFilter) {
      result = result.filter(s => s.sessionDate >= sessionStartFilter);
    }

    if (sessionEndFilter) {
      result = result.filter(s => s.sessionDate <= sessionEndFilter);
    }

    result.sort((a, b) => {
      let valA = a[sessionOrderBy] || '';
      let valB = b[sessionOrderBy] || '';

      if (typeof valA === 'string') {
        return sessionOrderDir === 'asc'
          ? valA.localeCompare(valB)
          : valB.localeCompare(valA);
      } else {
        return sessionOrderDir === 'asc'
          ? (valA > valB ? 1 : -1)
          : (valA < valB ? 1 : -1);
      }
    });

    return result;
  }, [sessions, sessionStudentFilter, sessionStatusFilter, sessionStartFilter, sessionEndFilter, sessionOrderBy, sessionOrderDir]);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Modals state
  const [openStudentModal, setOpenStudentModal] = useState(false);
  const [openParentModal, setOpenParentModal] = useState(false);
  const [openScheduleModal, setOpenScheduleModal] = useState(false);
  const [openSessionModal, setOpenSessionModal] = useState(false);
  const [openInvoiceModal, setOpenInvoiceModal] = useState(false);
  const [openPaymentModal, setOpenPaymentModal] = useState(false);
  const [openRateModal, setOpenRateModal] = useState(false);

  // Forms state
  const [studentForm, setStudentForm] = useState({
    id: null as any,
    firstName: '',
    lastName: '',
    preferredName: '',
    grade: '',
    school: '',
    notes: '',
    parentId: '',
    subjectIds: [] as number[],
  });

  const [parentForm, setParentForm] = useState({
    username: '',
    password: '',
    email: '',
    name: '',
    phone: '',
    address: '',
    preferredComm: 'EMAIL',
  });

  const [scheduleForm, setScheduleForm] = useState({
    studentId: '',
    subjectId: '',
    dayOfWeek: 'SUNDAY',
    startTime: '09:00',
    endTime: '10:00',
    durationMinutes: 60,
    effectiveStartDate: new Date().toISOString().split('T')[0],
  });

  const [sessionForm, setSessionForm] = useState({
    studentId: '',
    subjectId: '',
    sessionDate: new Date().toISOString().split('T')[0],
    scheduledStartTime: '09:00',
    actualStartTime: '09:00',
    actualDurationMinutes: 60,
    status: 'CONDUCTED',
    notes: '',
  });

  const modalAvailableSubjects = useMemo(() => {
    if (sessionForm.studentId) {
      const student = students.find(s => String(s.id) === String(sessionForm.studentId));
      return student ? (student.subjects || []) : [];
    }
    return subjects;
  }, [sessionForm.studentId, students, subjects]);

  const modalAvailableStudents = useMemo(() => {
    if (sessionForm.subjectId) {
      return students.filter(student => 
        student.subjects?.some((sub: any) => String(sub.id) === String(sessionForm.subjectId))
      );
    }
    return students;
  }, [sessionForm.subjectId, students]);

  const handleModalStudentChange = (studentId: string) => {
    const student = students.find(s => String(s.id) === String(studentId));
    const studentSubs = student ? (student.subjects || []) : [];
    setSessionForm(prev => ({
      ...prev,
      studentId: studentId,
      subjectId: studentSubs.length > 0 ? String(studentSubs[0].id) : ''
    }));
  };

  const handleModalSubjectChange = (subjectId: string) => {
    const currentStudent = students.find(s => String(s.id) === String(sessionForm.studentId));
    const hasSubject = currentStudent?.subjects?.some((sub: any) => String(sub.id) === String(subjectId));
    if (!sessionForm.studentId || !hasSubject) {
      setSessionForm(prev => ({
        ...prev,
        studentId: '',
        subjectId: subjectId
      }));
    } else {
      setSessionForm(prev => ({
        ...prev,
        subjectId: subjectId
      }));
    }
  };

  const [invoiceForm, setInvoiceForm] = useState({
    parentId: '',
    startDate: new Date(new Date().setDate(1)).toISOString().split('T')[0],
    endDate: new Date().toISOString().split('T')[0],
  });

  const [paymentForm, setPaymentForm] = useState({
    parentId: '',
    amount: '',
    paymentMethod: 'VENMO',
    referenceNumber: '',
    notes: '',
  });

  const [rateForm, setRateForm] = useState({
    studentId: '',
    subjectId: '',
    ratePerSession: '',
    effectiveStartDate: new Date().toISOString().split('T')[0],
  });

  const [ratesList, setRatesList] = useState<any[]>([]);

  useEffect(() => {
    fetchStats();
    fetchData();
  }, [activeTab]);

  const fetchStats = async () => {
    try {
      const res = await api.get('/reports/dashboard');
      setStats(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  const fetchData = async () => {
    setLoading(true);
    setError('');
    try {
      const [studentsRes, parentsRes, schedulesRes, sessionsRes, invoicesRes, paymentsRes, subjectsRes] = await Promise.all([
        api.get('/students'),
        api.get('/parents'),
        api.get('/schedules'),
        api.get('/sessions'),
        api.get('/invoices'),
        api.get('/payments'),
        api.get('/subjects'),
      ]);
      setStudents(studentsRes.data);
      setParents(parentsRes.data);
      setSchedules(schedulesRes.data);
      setSessions(sessionsRes.data);
      setInvoices(invoicesRes.data);
      setPayments(paymentsRes.data);
      setSubjects(subjectsRes.data);
    } catch (err: any) {
      setError('Failed to fetch data');
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  // Student Actions
  const handleSaveStudent = async () => {
    try {
      const payload = {
        firstName: studentForm.firstName,
        lastName: studentForm.lastName,
        preferredName: studentForm.preferredName,
        grade: studentForm.grade,
        school: studentForm.school,
        notes: studentForm.notes,
        parentId: studentForm.parentId,
        subjects: studentForm.subjectIds.map(id => ({ id })),
      };

      if (studentForm.id) {
        await api.put(`/students/${studentForm.id}`, payload);
        setSuccess('Student updated successfully!');
      } else {
        await api.post('/students', payload);
        setSuccess('Student created successfully!');
      }
      setOpenStudentModal(false);
      fetchData();
    } catch (err: any) {
      setError('Failed to save student: ' + (err.response?.data?.message || err.message));
    }
  };

  const handleDeleteStudent = async (id: number) => {
    if (!window.confirm('Are you sure you want to delete this student?')) return;
    try {
      await api.delete(`/students/${id}`);
      setSuccess('Student deleted successfully!');
      fetchData();
    } catch (err) {
      setError('Failed to delete student');
    }
  };

  const handleViewRates = async (studentId: number) => {
    try {
      setRateForm(prev => ({ ...prev, studentId: String(studentId) }));
      const res = await api.get(`/students/${studentId}/rates`);
      setRatesList(res.data);
      setOpenRateModal(true);
    } catch (err) {
      setError('Failed to fetch pricing rates');
    }
  };

  const handleAddRate = async () => {
    try {
      await api.post('/students/rates', {
        studentId: Number(rateForm.studentId),
        subjectId: Number(rateForm.subjectId),
        ratePerSession: Number(rateForm.ratePerSession),
        effectiveStartDate: rateForm.effectiveStartDate,
      });
      setSuccess('Pricing rate added successfully!');
      // reload rates
      const res = await api.get(`/students/${rateForm.studentId}/rates`);
      setRatesList(res.data);
      setRateForm(prev => ({ ...prev, ratePerSession: '', subjectId: '' }));
    } catch (err) {
      setError('Failed to add rate');
    }
  };

  // Parent Actions
  const handleSaveParent = async () => {
    try {
      const query = `username=${parentForm.username}&password=${parentForm.password}&email=${parentForm.email}&name=${parentForm.name}&phone=${parentForm.phone}&address=${parentForm.address}&preferredComm=${parentForm.preferredComm}`;
      await api.post(`/auth/register-parent?${query}`);
      setSuccess('Parent profile registered successfully!');
      setOpenParentModal(false);
      fetchData();
    } catch (err: any) {
      setError('Failed to save parent: ' + (err.response?.data || err.message));
    }
  };

  // Schedule Actions
  const handleSaveSchedule = async () => {
    try {
      await api.post('/schedules', {
        studentId: Number(scheduleForm.studentId),
        subjectId: Number(scheduleForm.subjectId),
        dayOfWeek: scheduleForm.dayOfWeek,
        startTime: scheduleForm.startTime + ':00',
        endTime: scheduleForm.endTime + ':00',
        durationMinutes: Number(scheduleForm.durationMinutes),
        effectiveStartDate: scheduleForm.effectiveStartDate,
      });
      setSuccess('Recurring schedule created successfully!');
      setOpenScheduleModal(false);
      fetchData();
    } catch (err) {
      setError('Failed to create schedule');
    }
  };

  const handleDeleteSchedule = async (id: number) => {
    if (!window.confirm('Are you sure you want to cancel this schedule?')) return;
    try {
      await api.delete(`/schedules/${id}`);
      setSuccess('Schedule cancelled successfully!');
      fetchData();
    } catch (err) {
      setError('Failed to delete schedule');
    }
  };

  // Attendance/Session Actions
  const handleSaveSession = async () => {
    try {
      await api.post('/sessions', {
        studentId: Number(sessionForm.studentId),
        subjectId: Number(sessionForm.subjectId),
        sessionDate: sessionForm.sessionDate,
        scheduledStartTime: sessionForm.scheduledStartTime + ':00',
        actualStartTime: sessionForm.actualStartTime + ':00',
        actualDurationMinutes: Number(sessionForm.actualDurationMinutes),
        status: sessionForm.status,
        notes: sessionForm.notes,
      });
      setSuccess('Attendance logged successfully!');
      setOpenSessionModal(false);
      fetchData();
    } catch (err) {
      setError('Failed to log attendance');
    }
  };

  // Billing Actions
  const handleGenerateInvoice = async () => {
    try {
      await api.post(`/invoices/generate?parentId=${invoiceForm.parentId}&startDate=${invoiceForm.startDate}&endDate=${invoiceForm.endDate}`);
      setSuccess('Invoice draft generated successfully!');
      setOpenInvoiceModal(false);
      fetchData();
    } catch (err: any) {
      setError('Failed to generate invoice: ' + (err.response?.data?.message || err.message));
    }
  };

  const handleRegenerateInvoice = async (id: number) => {
    try {
      await api.post(`/invoices/${id}/regenerate`);
      setSuccess('Invoice draft regenerated successfully!');
      fetchData();
    } catch (err) {
      setError('Failed to regenerate invoice');
    }
  };

  const handleFinalizeInvoice = async (id: number) => {
    try {
      await api.post(`/invoices/${id}/finalize`);
      setSuccess('Invoice finalized, sent to parent, and email triggered!');
      fetchData();
    } catch (err) {
      setError('Failed to send invoice');
    }
  };

  const handleDownloadInvoice = (id: number) => {
    window.open(`http://localhost:8080/api/invoices/${id}/download?Authorization=Bearer ${localStorage.getItem('token')}`, '_blank');
  };

  // Payment Actions
  const handleRecordPayment = async () => {
    try {
      await api.post('/payments', {
        parentId: Number(paymentForm.parentId),
        amount: Number(paymentForm.amount),
        paymentMethod: paymentForm.paymentMethod,
        referenceNumber: paymentForm.referenceNumber,
        notes: paymentForm.notes,
      });
      setSuccess('Payment recorded and allocated successfully!');
      setOpenPaymentModal(false);
      fetchData();
    } catch (err) {
      setError('Failed to record payment');
    }
  };

  // Export Reports
  const handleExportStudents = () => {
    window.open('http://localhost:8080/api/reports/export/students', '_blank');
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh', bgcolor: 'background.default' }}>
      {/* Top Navbar */}
      <Box sx={{ py: 2, px: 4, bgcolor: 'background.paper', borderBottom: 1, borderColor: 'divider', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h5" sx={{ fontWeight: 'bold', color: 'primary.main' }}>
          TutorSys Admin Dashboard
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Button startIcon={<RefreshIcon />} size="small" onClick={fetchData}>Reload</Button>
          <Button variant="outlined" color="error" startIcon={<LogoutIcon />} onClick={handleLogout}>Logout</Button>
        </Box>
      </Box>

      {/* Main tabs */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider', bgcolor: 'background.paper' }}>
        <Tabs value={activeTab} onChange={(_, v) => setActiveTab(v)} variant="scrollable" scrollButtons="auto">
          <Tab label="Dashboard" />
          <Tab label="Students" />
          <Tab label="Parents" />
          <Tab label="Weekly Schedules" />
          <Tab label="Attendance Entry" />
          <Tab label="Invoicing & Billing" />
          <Tab label="Payments Log" />
        </Tabs>
      </Box>

      <Container maxWidth="xl" sx={{ mt: 4, mb: 4, flexGrow: 1 }}>
        {success && <Alert severity="success" onClose={() => setSuccess('')} sx={{ mb: 3 }}>{success}</Alert>}
        {error && <Alert severity="error" onClose={() => setError('')} sx={{ mb: 3 }}>{error}</Alert>}
        {loading && <Box sx={{ display: 'flex', justifyContent: 'center', my: 5 }}><CircularProgress /></Box>}

        {/* Tab 0: General Dashboard Stats */}
        {activeTab === 0 && (
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 3 }}>
            <Box sx={{ width: { xs: '100%', sm: 'calc(50% - 12px)', md: 'calc(25% - 18px)' } }}>
              <Card sx={{ bgcolor: 'primary.main', color: 'white' }}>
                <CardContent>
                  <Typography variant="subtitle2" sx={{ opacity: 0.8 }}>Total Active Students</Typography>
                  <Typography variant="h3" sx={{ fontWeight: 'bold' }}>{stats?.totalStudents ?? 0}</Typography>
                </CardContent>
              </Card>
            </Box>
            <Box sx={{ width: { xs: '100%', sm: 'calc(50% - 12px)', md: 'calc(25% - 18px)' } }}>
              <Card sx={{ bgcolor: 'success.main', color: 'white' }}>
                <CardContent>
                  <Typography variant="subtitle2" sx={{ opacity: 0.8 }}>Collected Revenue (This Month)</Typography>
                  <Typography variant="h3" sx={{ fontWeight: 'bold' }}>${stats?.monthlyRevenue ?? 0}</Typography>
                </CardContent>
              </Card>
            </Box>
            <Box sx={{ width: { xs: '100%', sm: 'calc(50% - 12px)', md: 'calc(25% - 18px)' } }}>
              <Card sx={{ bgcolor: 'error.main', color: 'white' }}>
                <CardContent>
                  <Typography variant="subtitle2" sx={{ opacity: 0.8 }}>Total Outstanding Dues</Typography>
                  <Typography variant="h3" sx={{ fontWeight: 'bold' }}>${stats?.totalOutstandingDues ?? 0}</Typography>
                </CardContent>
              </Card>
            </Box>
            <Box sx={{ width: { xs: '100%', sm: 'calc(50% - 12px)', md: 'calc(25% - 18px)' } }}>
              <Card>
                <CardContent sx={{ textAlign: 'center' }}>
                  <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>Report Exports</Typography>
                  <Button variant="outlined" startIcon={<CloudDownloadIcon />} onClick={handleExportStudents} size="small" fullWidth sx={{ mb: 1 }}>
                    Student Excel List
                  </Button>
                </CardContent>
              </Card>
            </Box>

            {/* Overdue list */}
            <Box sx={{ width: { xs: '100%', md: 'calc(66.6% - 12px)' } }}>
              <Card>
                <CardContent>
                  <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 2 }}>Parents with Overdue Balances</Typography>
                  <TableContainer>
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell>Parent Name</TableCell>
                          <TableCell>Invoice #</TableCell>
                          <TableCell>Balance Due</TableCell>
                          <TableCell>Due Date</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {stats?.overdueParents && stats.overdueParents.length > 0 ? (
                          stats.overdueParents.map((op, idx) => (
                            <TableRow key={idx}>
                              <TableCell>{op.parentName}</TableCell>
                              <TableCell>{op.invoiceNumber}</TableCell>
                              <TableCell sx={{ color: 'error.main', fontWeight: 'bold' }}>${op.balanceDue}</TableCell>
                              <TableCell>{op.dueDate}</TableCell>
                            </TableRow>
                          ))
                        ) : (
                          <TableRow>
                            <TableCell colSpan={4} align="center">No overdue accounts. All caught up!</TableCell>
                          </TableRow>
                        )}
                      </TableBody>
                    </Table>
                  </TableContainer>
                </CardContent>
              </Card>
            </Box>
          </Box>
        )}

        {/* Tab 1: Students */}
        {activeTab === 1 && (
          <Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
              <Typography variant="h6" sx={{ fontWeight: 'bold' }}>Students Directory</Typography>
              <Button variant="contained" startIcon={<AddIcon />} onClick={() => {
                setStudentForm({ id: null, firstName: '', lastName: '', preferredName: '', grade: '', school: '', notes: '', parentId: '', subjectIds: [] });
                setOpenStudentModal(true);
              }}>
                Add Student
              </Button>
            </Box>
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Name</TableCell>
                    <TableCell>Parent</TableCell>
                    <TableCell>Grade</TableCell>
                    <TableCell>School</TableCell>
                    <TableCell>Subjects</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {students.map((student) => (
                    <TableRow key={student.id}>
                      <TableCell sx={{ fontWeight: 600 }}>{student.firstName} {student.lastName}</TableCell>
                      <TableCell>{student.parentName}</TableCell>
                      <TableCell>{student.grade}</TableCell>
                      <TableCell>{student.school}</TableCell>
                      <TableCell>
                        {student.subjects?.map((sub: any) => (
                          <Chip key={sub.id} label={sub.name} size="small" color="primary" variant="outlined" sx={{ mr: 0.5 }} />
                        ))}
                      </TableCell>
                      <TableCell>
                        <Chip label={student.status} color={student.status === 'ACTIVE' ? 'success' : 'default'} size="small" />
                      </TableCell>
                      <TableCell align="right">
                        <IconButton color="primary" onClick={() => handleViewRates(student.id)} title="Tuition Rates">
                          <Chip label="Rates" size="small" color="secondary" />
                        </IconButton>
                        <IconButton color="info" onClick={() => {
                          setStudentForm({
                            id: student.id,
                            firstName: student.firstName,
                            lastName: student.lastName,
                            preferredName: student.preferredName || '',
                            grade: student.grade || '',
                            school: student.school || '',
                            notes: student.notes || '',
                            parentId: student.parentId,
                            subjectIds: student.subjects?.map((s: any) => s.id) || [],
                          });
                          setOpenStudentModal(true);
                        }}>
                          <EditIcon />
                        </IconButton>
                        <IconButton color="error" onClick={() => handleDeleteStudent(student.id)}>
                          <DeleteIcon />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Box>
        )}

        {/* Tab 2: Parents */}
        {activeTab === 2 && (
          <Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
              <Typography variant="h6" sx={{ fontWeight: 'bold' }}>Parent Profiles</Typography>
              <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpenParentModal(true)}>
                Add/Register Parent
              </Button>
            </Box>
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Name</TableCell>
                    <TableCell>Email</TableCell>
                    <TableCell>Phone</TableCell>
                    <TableCell>Address</TableCell>
                    <TableCell>Preferred Comm</TableCell>
                    <TableCell>Notes</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {parents.map((parent) => (
                    <TableRow key={parent.id}>
                      <TableCell sx={{ fontWeight: 600 }}>{parent.name}</TableCell>
                      <TableCell>{parent.email}</TableCell>
                      <TableCell>{parent.phone}</TableCell>
                      <TableCell>{parent.address}</TableCell>
                      <TableCell>{parent.preferredCommunication}</TableCell>
                      <TableCell>{parent.notes}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Box>
        )}

        {/* Tab 3: Schedules */}
        {activeTab === 3 && (
          <Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
              <Typography variant="h6" sx={{ fontWeight: 'bold' }}>Weekly Recurring Schedules</Typography>
              <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpenScheduleModal(true)}>
                Create Schedule
              </Button>
            </Box>
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Student</TableCell>
                    <TableCell>Subject</TableCell>
                    <TableCell>Day</TableCell>
                    <TableCell>Time</TableCell>
                    <TableCell>Duration</TableCell>
                    <TableCell>Effective Date</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {schedules.map((sched) => (
                    <TableRow key={sched.id}>
                      <TableCell sx={{ fontWeight: 600 }}>{sched.studentName}</TableCell>
                      <TableCell>{sched.subjectName}</TableCell>
                      <TableCell><Chip label={sched.dayOfWeek} color="primary" size="small" /></TableCell>
                      <TableCell>{sched.startTime} - {sched.endTime}</TableCell>
                      <TableCell>{sched.durationMinutes} mins</TableCell>
                      <TableCell>{sched.effectiveStartDate}</TableCell>
                      <TableCell align="right">
                        <IconButton color="error" onClick={() => handleDeleteSchedule(sched.id)}>
                          <DeleteIcon />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Box>
        )}

        {/* Tab 4: Attendance / Session entry */}
        {activeTab === 4 && (
          <Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
              <Typography variant="h6" sx={{ fontWeight: 'bold' }}>Attendance & Session Logs</Typography>
              <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpenSessionModal(true)}>
                Record Tutoring Session
              </Button>
            </Box>

            {/* Filtering Controls */}
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, mb: 3, p: 2, bgcolor: 'background.paper', borderRadius: 1, boxShadow: 1 }}>
              <TextField
                label="Filter by Student"
                size="small"
                value={sessionStudentFilter}
                onChange={(e) => setSessionStudentFilter(e.target.value)}
                sx={{ minWidth: 200 }}
              />
              <FormControl size="small" sx={{ minWidth: 150 }}>
                <InputLabel>Status</InputLabel>
                <Select
                  value={sessionStatusFilter}
                  onChange={(e) => setSessionStatusFilter(e.target.value)}
                  label="Status"
                >
                  <MenuItem value="ALL">All Statuses</MenuItem>
                  <MenuItem value="CONDUCTED">Conducted</MenuItem>
                  <MenuItem value="CANCELLED">Cancelled</MenuItem>
                  <MenuItem value="ABSENT_STUDENT">Absent (Student)</MenuItem>
                  <MenuItem value="ABSENT_TEACHER">Absent (Teacher)</MenuItem>
                  <MenuItem value="HOLIDAY">Holiday</MenuItem>
                  <MenuItem value="MAKEUP">Makeup</MenuItem>
                </Select>
              </FormControl>
              
              <TextField
                label="Start Date"
                type="date"
                size="small"
                value={sessionStartFilter}
                onChange={(e) => setSessionStartFilter(e.target.value)}
                {...({ InputLabelProps: { shrink: true } } as any)}
              />
              <TextField
                label="End Date"
                type="date"
                size="small"
                value={sessionEndFilter}
                onChange={(e) => setSessionEndFilter(e.target.value)}
                {...({ InputLabelProps: { shrink: true } } as any)}
              />

              <Button
                variant="outlined"
                size="small"
                onClick={() => {
                  setSessionStudentFilter('');
                  setSessionStatusFilter('ALL');
                  setSessionStartFilter(getLastSixMonthsDateString());
                  setSessionEndFilter(getTodayDateString());
                }}
              >
                Clear Filters
              </Button>
            </Box>

            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>
                      <TableSortLabel
                        active={sessionOrderBy === 'sessionDate'}
                        direction={sessionOrderBy === 'sessionDate' ? sessionOrderDir : 'asc'}
                        onClick={() => handleSessionSort('sessionDate')}
                      >
                        Date
                      </TableSortLabel>
                    </TableCell>
                    <TableCell>
                      <TableSortLabel
                        active={sessionOrderBy === 'studentName'}
                        direction={sessionOrderBy === 'studentName' ? sessionOrderDir : 'asc'}
                        onClick={() => handleSessionSort('studentName')}
                      >
                        Student Name
                      </TableSortLabel>
                    </TableCell>
                    <TableCell>
                      <TableSortLabel
                        active={sessionOrderBy === 'subjectName'}
                        direction={sessionOrderBy === 'subjectName' ? sessionOrderDir : 'asc'}
                        onClick={() => handleSessionSort('subjectName')}
                      >
                        Subject
                      </TableSortLabel>
                    </TableCell>
                    <TableCell>Time Range</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Rate Charged</TableCell>
                    <TableCell>Invoice #</TableCell>
                    <TableCell>Notes</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filteredAndSortedSessions.length > 0 ? (
                    filteredAndSortedSessions.map((sess) => (
                      <TableRow key={sess.id}>
                        <TableCell>{sess.sessionDate}</TableCell>
                        <TableCell sx={{ fontWeight: 600 }}>{sess.studentName}</TableCell>
                        <TableCell>{sess.subjectName}</TableCell>
                        <TableCell>
                          {sess.status === 'CONDUCTED' 
                            ? formatSessionTimeRange(sess.actualStartTime, sess.actualDurationMinutes) 
                            : formatSessionTimeRange(sess.scheduledStartTime, 60)}
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={sess.status}
                            size="small"
                            color={sess.status === 'CONDUCTED' ? 'success' : sess.status === 'CANCELLED' ? 'error' : 'warning'}
                          />
                        </TableCell>
                        <TableCell>${sess.rateCharged ?? '0.00'}</TableCell>
                        <TableCell>{sess.invoiceNumber ? <Chip label={sess.invoiceNumber} size="small" color="info" /> : <Chip label="Unbilled" size="small" />}</TableCell>
                        <TableCell>{sess.notes}</TableCell>
                      </TableRow>
                    ))
                  ) : (
                    <TableRow>
                      <TableCell colSpan={8} align="center">No sessions match the filter criteria.</TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </Box>
        )}

        {/* Tab 5: Invoices */}
        {activeTab === 5 && (
          <Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
              <Typography variant="h6" sx={{ fontWeight: 'bold' }}>Monthly Invoices</Typography>
              <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpenInvoiceModal(true)}>
                Generate Invoice
              </Button>
            </Box>
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Invoice #</TableCell>
                    <TableCell>Parent</TableCell>
                    <TableCell>Period</TableCell>
                    <TableCell>Subtotal</TableCell>
                    <TableCell>Previous Due</TableCell>
                    <TableCell>Balance Due</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {invoices.map((inv) => (
                    <TableRow key={inv.id}>
                      <TableCell sx={{ fontWeight: 600 }}>{inv.invoiceNumber}</TableCell>
                      <TableCell>{inv.parentName}</TableCell>
                      <TableCell>{inv.billingPeriodStart} to {inv.billingPeriodEnd}</TableCell>
                      <TableCell>${inv.subtotalAmount}</TableCell>
                      <TableCell>${inv.previousBalance}</TableCell>
                      <TableCell sx={{ fontWeight: 700, color: inv.balanceDue > 0 ? 'error.main' : 'success.main' }}>
                        ${inv.balanceDue}
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={inv.status}
                          size="small"
                          color={inv.status === 'PAID' ? 'success' : inv.status === 'DRAFT' ? 'default' : 'warning'}
                        />
                      </TableCell>
                      <TableCell align="right">
                        {inv.status === 'DRAFT' && (
                          <>
                            <Button size="small" onClick={() => handleRegenerateInvoice(inv.id)} sx={{ mr: 1 }}>Regen</Button>
                            <Button size="small" variant="contained" color="success" startIcon={<SendIcon />} onClick={() => handleFinalizeInvoice(inv.id)} sx={{ mr: 1 }}>
                              Send
                            </Button>
                          </>
                        )}
                        <IconButton color="primary" onClick={() => handleDownloadInvoice(inv.id)}>
                          <PictureAsPdfIcon />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Box>
        )}

        {/* Tab 6: Payments */}
        {activeTab === 6 && (
          <Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
              <Typography variant="h6" sx={{ fontWeight: 'bold' }}>Payment History</Typography>
              <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpenPaymentModal(true)}>
                Record Payment
              </Button>
            </Box>
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Date</TableCell>
                    <TableCell>Parent Name</TableCell>
                    <TableCell>Amount</TableCell>
                    <TableCell>Method</TableCell>
                    <TableCell>Reference #</TableCell>
                    <TableCell>Notes</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {payments.map((pmt) => (
                    <TableRow key={pmt.id}>
                      <TableCell>{pmt.paymentDate}</TableCell>
                      <TableCell sx={{ fontWeight: 600 }}>{pmt.parentName}</TableCell>
                      <TableCell sx={{ color: 'success.main', fontWeight: 'bold' }}>${pmt.amount}</TableCell>
                      <TableCell><Chip label={pmt.paymentMethod} size="small" variant="outlined" /></TableCell>
                      <TableCell>{pmt.referenceNumber}</TableCell>
                      <TableCell>{pmt.notes}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Box>
        )}
      </Container>

      {/* MODALS */}
      {/* 1. Student Modal */}
      <Dialog open={openStudentModal} onClose={() => setOpenStudentModal(false)}>
        <DialogTitle>{studentForm.id ? 'Edit Student' : 'Add Student'}</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2, width: 400 }}>
          <TextField label="First Name" value={studentForm.firstName} onChange={(e) => setStudentForm({ ...studentForm, firstName: e.target.value })} required />
          <TextField label="Last Name" value={studentForm.lastName} onChange={(e) => setStudentForm({ ...studentForm, lastName: e.target.value })} required />
          <TextField label="Preferred Name" value={studentForm.preferredName} onChange={(e) => setStudentForm({ ...studentForm, preferredName: e.target.value })} />
          <TextField label="Grade Level" value={studentForm.grade} onChange={(e) => setStudentForm({ ...studentForm, grade: e.target.value })} placeholder="e.g. Grade 9" />
          <TextField label="School Name" value={studentForm.school} onChange={(e) => setStudentForm({ ...studentForm, school: e.target.value })} />
          
          <FormControl required>
            <InputLabel>Parent / Guardian</InputLabel>
            <Select value={studentForm.parentId} onChange={(e) => setStudentForm({ ...studentForm, parentId: e.target.value })} label="Parent / Guardian">
              {parents.map(p => <MenuItem key={p.id} value={String(p.id)}>{p.name}</MenuItem>)}
            </Select>
          </FormControl>

          <FormControl>
            <InputLabel>Subjects Enrolled</InputLabel>
            <Select
              multiple
              value={studentForm.subjectIds}
              onChange={(e) => setStudentForm({ ...studentForm, subjectIds: e.target.value as number[] })}
              label="Subjects Enrolled"
              renderValue={(selected) => (
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                  {(selected as number[]).map((id) => (
                    <Chip key={id} label={subjects.find(s => s.id === id)?.name || id} size="small" />
                  ))}
                </Box>
              )}
            >
              {subjects.map(s => <MenuItem key={s.id} value={s.id}>{s.name}</MenuItem>)}
            </Select>
          </FormControl>

          <TextField label="Notes" value={studentForm.notes} onChange={(e) => setStudentForm({ ...studentForm, notes: e.target.value })} multiline rows={2} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenStudentModal(false)}>Cancel</Button>
          <Button onClick={handleSaveStudent} variant="contained">Save</Button>
        </DialogActions>
      </Dialog>

      {/* 2. Parent Modal */}
      <Dialog open={openParentModal} onClose={() => setOpenParentModal(false)}>
        <DialogTitle>Register Parent Account</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2, width: 400 }}>
          <TextField label="Portal Login Username" value={parentForm.username} onChange={(e) => setParentForm({ ...parentForm, username: e.target.value })} required />
          <TextField label="Password" type="password" value={parentForm.password} onChange={(e) => setParentForm({ ...parentForm, password: e.target.value })} required />
          <TextField label="Full Name" value={parentForm.name} onChange={(e) => setParentForm({ ...parentForm, name: e.target.value })} required />
          <TextField label="Email Address" type="email" value={parentForm.email} onChange={(e) => setParentForm({ ...parentForm, email: e.target.value })} required />
          <TextField label="Phone Number" value={parentForm.phone} onChange={(e) => setParentForm({ ...parentForm, phone: e.target.value })} />
          <TextField label="Home Address" value={parentForm.address} onChange={(e) => setParentForm({ ...parentForm, address: e.target.value })} multiline rows={2} />
          
          <FormControl>
            <InputLabel>Preferred Communication</InputLabel>
            <Select value={parentForm.preferredComm} onChange={(e) => setParentForm({ ...parentForm, preferredComm: e.target.value })} label="Preferred Communication">
              <MenuItem value="EMAIL">Email Only</MenuItem>
              <MenuItem value="SMS">SMS / Phone</MenuItem>
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenParentModal(false)}>Cancel</Button>
          <Button onClick={handleSaveParent} variant="contained">Register</Button>
        </DialogActions>
      </Dialog>

      {/* 3. Pricing Rates Modal */}
      <Dialog open={openRateModal} onClose={() => setOpenRateModal(false)}>
        <DialogTitle>Student Tuition Rates History</DialogTitle>
        <DialogContent sx={{ width: 450, pt: 2 }}>
          {/* Add rate form */}
          <Box sx={{ display: 'flex', gap: 1, mb: 3, alignItems: 'center' }}>
            <FormControl size="small" sx={{ minWidth: 120 }}>
              <InputLabel>Subject</InputLabel>
              <Select value={rateForm.subjectId} onChange={(e) => setRateForm({ ...rateForm, subjectId: e.target.value })} label="Subject">
                {subjects.map(s => <MenuItem key={s.id} value={String(s.id)}>{s.name}</MenuItem>)}
              </Select>
            </FormControl>
            <TextField label="Rate ($)" size="small" type="number" sx={{ width: 90 }} value={rateForm.ratePerSession} onChange={(e) => setRateForm({ ...rateForm, ratePerSession: e.target.value })} />
            <TextField label="Start Date" size="small" type="date" value={rateForm.effectiveStartDate} onChange={(e) => setRateForm({ ...rateForm, effectiveStartDate: e.target.value })} />
            <Button variant="contained" size="small" startIcon={<AddIcon />} onClick={handleAddRate}>Add</Button>
          </Box>

          <Typography variant="subtitle2" sx={{ mb: 1, fontWeight: 'bold' }}>Rates Log</Typography>
          <TableContainer component={Paper} variant="outlined" sx={{ maxHeight: 200 }}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Subject</TableCell>
                  <TableCell>Rate</TableCell>
                  <TableCell>Start Date</TableCell>
                  <TableCell>End Date</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {ratesList.map((r, idx) => (
                  <TableRow key={idx}>
                    <TableCell>{r.subjectName}</TableCell>
                    <TableCell>${r.ratePerSession}</TableCell>
                    <TableCell>{r.effectiveStartDate}</TableCell>
                    <TableCell>{r.effectiveEndDate ? r.effectiveEndDate : <Chip label="Current" size="small" color="success" />}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenRateModal(false)}>Close</Button>
        </DialogActions>
      </Dialog>

      {/* 4. Schedule Modal */}
      <Dialog open={openScheduleModal} onClose={() => setOpenScheduleModal(false)}>
        <DialogTitle>Create Student Schedule</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2, width: 400 }}>
          <FormControl required>
            <InputLabel>Student</InputLabel>
            <Select value={scheduleForm.studentId} onChange={(e) => setScheduleForm({ ...scheduleForm, studentId: e.target.value })} label="Student">
              {students.map(s => <MenuItem key={s.id} value={String(s.id)}>{s.firstName} {s.lastName}</MenuItem>)}
            </Select>
          </FormControl>

          <FormControl required>
            <InputLabel>Subject</InputLabel>
            <Select value={scheduleForm.subjectId} onChange={(e) => setScheduleForm({ ...scheduleForm, subjectId: e.target.value })} label="Subject">
              {subjects.map(s => <MenuItem key={s.id} value={String(s.id)}>{s.name}</MenuItem>)}
            </Select>
          </FormControl>

          <FormControl required>
            <InputLabel>Day of Week</InputLabel>
            <Select value={scheduleForm.dayOfWeek} onChange={(e) => setScheduleForm({ ...scheduleForm, dayOfWeek: e.target.value })} label="Day of Week">
              {['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'].map(day => (
                <MenuItem key={day} value={day}>{day}</MenuItem>
              ))}
            </Select>
          </FormControl>

          <Box sx={{ display: 'flex', gap: 2 }}>
            <TextField label="Start Time" type="time" value={scheduleForm.startTime} onChange={(e) => setScheduleForm({ ...scheduleForm, startTime: e.target.value })} fullWidth />
            <TextField label="End Time" type="time" value={scheduleForm.endTime} onChange={(e) => setScheduleForm({ ...scheduleForm, endTime: e.target.value })} fullWidth />
          </Box>
          <TextField label="Duration (Minutes)" type="number" value={scheduleForm.durationMinutes} onChange={(e) => setScheduleForm({ ...scheduleForm, durationMinutes: Number(e.target.value) || 0 })} />
          <TextField label="Effective Start Date" type="date" value={scheduleForm.effectiveStartDate} onChange={(e) => setScheduleForm({ ...scheduleForm, effectiveStartDate: e.target.value })} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenScheduleModal(false)}>Cancel</Button>
          <Button onClick={handleSaveSchedule} variant="contained">Create</Button>
        </DialogActions>
      </Dialog>

      {/* 5. Record Session / Attendance Modal */}
      <Dialog open={openSessionModal} onClose={() => setOpenSessionModal(false)}>
        <DialogTitle>Log Session Attendance</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2, width: 400 }}>
          <FormControl required>
            <InputLabel>Student</InputLabel>
            <Select 
              value={sessionForm.studentId} 
              onChange={(e) => handleModalStudentChange(e.target.value as string)} 
              label="Student"
            >
              {modalAvailableStudents.map((s: any) => <MenuItem key={s.id} value={String(s.id)}>{s.firstName} {s.lastName}</MenuItem>)}
            </Select>
          </FormControl>

          <FormControl required>
            <InputLabel>Subject</InputLabel>
            <Select 
              value={sessionForm.subjectId} 
              onChange={(e) => handleModalSubjectChange(e.target.value as string)} 
              label="Subject"
              disabled={modalAvailableSubjects.length <= 1}
            >
              {modalAvailableSubjects.map((s: any) => <MenuItem key={s.id} value={String(s.id)}>{s.name}</MenuItem>)}
            </Select>
          </FormControl>

          <TextField label="Session Date" type="date" value={sessionForm.sessionDate} onChange={(e) => setSessionForm({ ...sessionForm, sessionDate: e.target.value })} />
          
          <Box sx={{ display: 'flex', gap: 2 }}>
            <TextField label="Scheduled Start Time" type="time" value={sessionForm.scheduledStartTime} onChange={(e) => setSessionForm({ ...sessionForm, scheduledStartTime: e.target.value })} fullWidth />
            <TextField label="Actual Start Time" type="time" value={sessionForm.actualStartTime} onChange={(e) => setSessionForm({ ...sessionForm, actualStartTime: e.target.value })} fullWidth />
          </Box>
          <TextField label="Actual Duration (Mins)" type="number" value={sessionForm.actualDurationMinutes} onChange={(e) => setSessionForm({ ...sessionForm, actualDurationMinutes: Number(e.target.value) || 0 })} />
          
          <FormControl required>
            <InputLabel>Attendance Status</InputLabel>
            <Select value={sessionForm.status} onChange={(e) => setSessionForm({ ...sessionForm, status: e.target.value })} label="Attendance Status">
              <MenuItem value="CONDUCTED">Conducted</MenuItem>
              <MenuItem value="CANCELLED">Cancelled</MenuItem>
              <MenuItem value="ABSENT_STUDENT">Student Absent</MenuItem>
              <MenuItem value="ABSENT_TEACHER">Teacher Unavailable</MenuItem>
              <MenuItem value="HOLIDAY">Holiday</MenuItem>
              <MenuItem value="MAKEUP">Makeup Class</MenuItem>
            </Select>
          </FormControl>

          <TextField label="Notes / Comments" value={sessionForm.notes} onChange={(e) => setSessionForm({ ...sessionForm, notes: e.target.value })} multiline rows={2} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenSessionModal(false)}>Cancel</Button>
          <Button onClick={handleSaveSession} variant="contained">Save Attendance</Button>
        </DialogActions>
      </Dialog>

      {/* 6. Generate Invoice Modal */}
      <Dialog open={openInvoiceModal} onClose={() => setOpenInvoiceModal(false)}>
        <DialogTitle>Generate Monthly Invoice</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2, width: 400 }}>
          <FormControl required>
            <InputLabel>Parent</InputLabel>
            <Select value={invoiceForm.parentId} onChange={(e) => setInvoiceForm({ ...invoiceForm, parentId: e.target.value })} label="Parent">
              {parents.map(p => <MenuItem key={p.id} value={String(p.id)}>{p.name}</MenuItem>)}
            </Select>
          </FormControl>
          <TextField label="Start Date" type="date" value={invoiceForm.startDate} onChange={(e) => setInvoiceForm({ ...invoiceForm, startDate: e.target.value })} />
          <TextField label="End Date" type="date" value={invoiceForm.endDate} onChange={(e) => setInvoiceForm({ ...invoiceForm, endDate: e.target.value })} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenInvoiceModal(false)}>Cancel</Button>
          <Button onClick={handleGenerateInvoice} variant="contained">Generate</Button>
        </DialogActions>
      </Dialog>

      {/* 7. Record Payment Modal */}
      <Dialog open={openPaymentModal} onClose={() => setOpenPaymentModal(false)}>
        <DialogTitle>Record Payment Received</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2, width: 400 }}>
          <FormControl required>
            <InputLabel>Parent</InputLabel>
            <Select value={paymentForm.parentId} onChange={(e) => setPaymentForm({ ...paymentForm, parentId: e.target.value })} label="Parent">
              {parents.map(p => <MenuItem key={p.id} value={String(p.id)}>{p.name}</MenuItem>)}
            </Select>
          </FormControl>
          <TextField label="Amount Paid ($)" type="number" value={paymentForm.amount} onChange={(e) => setPaymentForm({ ...paymentForm, amount: e.target.value })} required />
          <FormControl required>
            <InputLabel>Payment Method</InputLabel>
            <Select value={paymentForm.paymentMethod} onChange={(e) => setPaymentForm({ ...paymentForm, paymentMethod: e.target.value })} label="Payment Method">
              <MenuItem value="VENMO">Venmo</MenuItem>
              <MenuItem value="CHECK">Check</MenuItem>
              <MenuItem value="CASH">Cash</MenuItem>
              <MenuItem value="BANK_TRANSFER">Bank Transfer</MenuItem>
            </Select>
          </FormControl>
          <TextField label="Reference / Check #" value={paymentForm.referenceNumber} onChange={(e) => setPaymentForm({ ...paymentForm, referenceNumber: e.target.value })} />
          <TextField label="Notes" value={paymentForm.notes} onChange={(e) => setPaymentForm({ ...paymentForm, notes: e.target.value })} multiline rows={2} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenPaymentModal(false)}>Cancel</Button>
          <Button onClick={handleRecordPayment} variant="contained">Record Payment</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

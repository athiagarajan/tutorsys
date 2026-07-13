import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box, Container, Typography, Card, CardContent, Button, Tabs, Tab,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper,
  CircularProgress, Alert, Chip, Divider, IconButton
} from '@mui/material';
import LogoutIcon from '@mui/icons-material/Logout';
import RefreshIcon from '@mui/icons-material/Refresh';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import api from '../api';

export default function ParentDashboard() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState(0);
  const [profile, setProfile] = useState<any>(null);
  const [students, setStudents] = useState<any[]>([]);
  const [schedules, setSchedules] = useState<any[]>([]);
  const [sessions, setSessions] = useState<any[]>([]);
  const [invoices, setInvoices] = useState<any[]>([]);
  const [payments, setPayments] = useState<any[]>([]);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Calculate parent totals dynamically
  const outstandingBalance = invoices
    .filter(inv => inv.status !== 'CANCELLED')
    .reduce((sum, inv) => sum + inv.balanceDue, 0);

  useEffect(() => {
    fetchParentData();
  }, [activeTab]);

  const fetchParentData = async () => {
    setLoading(true);
    setError('');
    try {
      // 1. Fetch parent profile
      const profileRes = await api.get('/parents/me');
      setProfile(profileRes.data);

      // 2. Fetch related data
      const [studentsRes, schedulesRes, sessionsRes, invoicesRes, paymentsRes] = await Promise.all([
        api.get('/students'),
        api.get('/schedules'),
        api.get('/sessions'),
        api.get('/invoices'),
        api.get('/payments'),
      ]);

      setStudents(studentsRes.data);
      setSchedules(schedulesRes.data);
      setSessions(sessionsRes.data);
      setInvoices(invoicesRes.data);
      setPayments(paymentsRes.data);
    } catch (err: any) {
      setError('Failed to fetch parent portal details');
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  const handleDownloadInvoice = (id: number) => {
    window.open(`http://localhost:8080/api/invoices/${id}/download?Authorization=Bearer ${localStorage.getItem('token')}`, '_blank');
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh', bgcolor: 'background.default' }}>
      {/* Top Header */}
      <Box sx={{ py: 2, px: 4, bgcolor: 'background.paper', borderBottom: 1, borderColor: 'divider', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Box sx={{ display: 'flex', flexDirection: 'column' }}>
          <Typography variant="h5" sx={{ fontWeight: 'bold', color: 'primary.main' }}>
            TutorSys Parent Portal
          </Typography>
          {profile && (
            <Typography variant="caption" color="text.secondary">
              Welcome, {profile.name}
            </Typography>
          )}
        </Box>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Button startIcon={<RefreshIcon />} size="small" onClick={fetchParentData}>Reload</Button>
          <Button variant="outlined" color="error" startIcon={<LogoutIcon />} onClick={handleLogout}>Logout</Button>
        </Box>
      </Box>

      {/* Portal Tabs */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider', bgcolor: 'background.paper' }}>
        <Tabs value={activeTab} onChange={(_, v) => setActiveTab(v)}>
          <Tab label="My Dashboard" />
          <Tab label="Schedules" />
          <Tab label="Attendance" />
          <Tab label="Invoices & Statements" />
          <Tab label="Payments Log" />
        </Tabs>
      </Box>

      <Container maxWidth="lg" sx={{ mt: 4, mb: 4, flexGrow: 1 }}>
        {error && <Alert severity="error" onClose={() => setError('')} sx={{ mb: 3 }}>{error}</Alert>}
        {loading && <Box sx={{ display: 'flex', justifyContent: 'center', my: 5 }}><CircularProgress /></Box>}

        {!loading && (
          <>
            {/* Tab 0: Dashboard Summary */}
            {activeTab === 0 && (
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 3 }}>
                  {/* Balance Card */}
                  <Box sx={{ width: { xs: '100%', sm: 'calc(50% - 12px)' } }}>
                    <Card sx={{ borderLeft: 5, borderColor: outstandingBalance > 0 ? 'warning.main' : 'success.main' }}>
                      <CardContent>
                        <Typography variant="subtitle2" color="text.secondary">Outstanding Dues Balance</Typography>
                        <Typography variant="h3" sx={{ fontWeight: 'bold', color: outstandingBalance > 0 ? 'warning.main' : 'success.main', my: 1 }}>
                          ${outstandingBalance.toFixed(2)}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          Please pay outstanding invoices to avoid tutoring service disruptions.
                        </Typography>
                      </CardContent>
                    </Card>
                  </Box>

                  {/* Profile Card */}
                  <Box sx={{ width: { xs: '100%', sm: 'calc(50% - 12px)' } }}>
                    <Card>
                      <CardContent>
                        <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 1 }}>Profile Details</Typography>
                        <Divider sx={{ mb: 2 }} />
                        {profile && (
                          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                            <Box sx={{ display: 'flex' }}>
                              <Typography variant="body2" sx={{ fontWeight: 600, width: '30%' }}>Email:</Typography>
                              <Typography variant="body2" sx={{ width: '70%' }}>{profile.email}</Typography>
                            </Box>
                            <Box sx={{ display: 'flex' }}>
                              <Typography variant="body2" sx={{ fontWeight: 600, width: '30%' }}>Phone:</Typography>
                              <Typography variant="body2" sx={{ width: '70%' }}>{profile.phone || 'None'}</Typography>
                            </Box>
                            <Box sx={{ display: 'flex' }}>
                              <Typography variant="body2" sx={{ fontWeight: 600, width: '30%' }}>Address:</Typography>
                              <Typography variant="body2" sx={{ width: '70%', whiteSpace: 'pre-line' }}>{profile.address || 'None'}</Typography>
                            </Box>
                          </Box>
                        )}
                      </CardContent>
                    </Card>
                  </Box>
                </Box>

                {/* My Children */}
                <Box sx={{ width: '100%' }}>
                  <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 2 }}>My Enrolled Children</Typography>
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 3 }}>
                    {students.map((student) => (
                      <Box sx={{ width: { xs: '100%', sm: 'calc(50% - 12px)', md: 'calc(33.3% - 16px)' } }} key={student.id}>
                        <Card variant="outlined">
                          <CardContent>
                            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}>
                              <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
                                {student.firstName} {student.lastName}
                              </Typography>
                              <Chip label={student.status} size="small" color={student.status === 'ACTIVE' ? 'success' : 'default'} />
                            </Box>
                            <Typography variant="body2" color="text.secondary"><strong>Grade:</strong> {student.grade || 'Not Specified'}</Typography>
                            <Typography variant="body2" color="text.secondary"><strong>School:</strong> {student.school || 'Not Specified'}</Typography>
                            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                              <strong>Enrolled Subjects:</strong>{' '}
                              {student.subjects?.map((sub: any) => sub.name).join(', ') || 'None'}
                            </Typography>
                          </CardContent>
                        </Card>
                      </Box>
                    ))}
                  </Box>
                </Box>
              </Box>
            ) /* Tab 0 end */}

            {/* Tab 1: Schedules */}
            {activeTab === 1 && (
              <Box>
                <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 2 }}>Weekly Class Schedules</Typography>
                <TableContainer component={Paper}>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell>Student</TableCell>
                        <TableCell>Subject</TableCell>
                        <TableCell>Day</TableCell>
                        <TableCell>Session Time</TableCell>
                        <TableCell>Duration</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {schedules.length > 0 ? (
                        schedules.map((sched) => (
                          <TableRow key={sched.id}>
                            <TableCell sx={{ fontWeight: 600 }}>{sched.studentName}</TableCell>
                            <TableCell>{sched.subjectName}</TableCell>
                            <TableCell><Chip label={sched.dayOfWeek} color="primary" size="small" /></TableCell>
                            <TableCell>{sched.startTime} - {sched.endTime}</TableCell>
                            <TableCell>{sched.durationMinutes} mins</TableCell>
                          </TableRow>
                        ))
                      ) : (
                        <TableRow>
                          <TableCell colSpan={5} align="center">No schedules defined yet.</TableCell>
                        </TableRow>
                      )}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Box>
            )}

            {/* Tab 2: Attendance */}
            {activeTab === 2 && (
              <Box>
                <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 2 }}>Attendance & Completed Lessons History</Typography>
                <TableContainer component={Paper}>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell>Date</TableCell>
                        <TableCell>Student Name</TableCell>
                        <TableCell>Subject</TableCell>
                        <TableCell>Status</TableCell>
                        <TableCell>Duration</TableCell>
                        <TableCell>Notes</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {sessions.length > 0 ? (
                        sessions.map((sess) => (
                          <TableRow key={sess.id}>
                            <TableCell>{sess.sessionDate}</TableCell>
                            <TableCell sx={{ fontWeight: 600 }}>{sess.studentName}</TableCell>
                            <TableCell>{sess.subjectName}</TableCell>
                            <TableCell>
                              <Chip
                                label={sess.status}
                                size="small"
                                color={sess.status === 'CONDUCTED' ? 'success' : sess.status === 'CANCELLED' ? 'error' : 'warning'}
                              />
                            </TableCell>
                            <TableCell>{sess.actualDurationMinutes || 60} mins</TableCell>
                            <TableCell>{sess.notes}</TableCell>
                          </TableRow>
                        ))
                      ) : (
                        <TableRow>
                          <TableCell colSpan={6} align="center">No sessions recorded yet.</TableCell>
                        </TableRow>
                      )}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Box>
            )}

            {/* Tab 3: Invoices */}
            {activeTab === 3 && (
              <Box>
                <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 2 }}>Monthly Tuition Invoices</Typography>
                <TableContainer component={Paper}>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell>Invoice #</TableCell>
                        <TableCell>Billing Period</TableCell>
                        <TableCell>Issue Date</TableCell>
                        <TableCell>Due Date</TableCell>
                        <TableCell>Subtotal</TableCell>
                        <TableCell>Payments Applied</TableCell>
                        <TableCell>Balance Due</TableCell>
                        <TableCell>Status</TableCell>
                        <TableCell align="right">PDF</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {invoices.length > 0 ? (
                        invoices.map((inv) => (
                          <TableRow key={inv.id}>
                            <TableCell sx={{ fontWeight: 600 }}>{inv.invoiceNumber}</TableCell>
                            <TableCell>{inv.billingPeriodStart} to {inv.billingPeriodEnd}</TableCell>
                            <TableCell>{inv.issueDate}</TableCell>
                            <TableCell>{inv.dueDate}</TableCell>
                            <TableCell>${inv.subtotalAmount}</TableCell>
                            <TableCell>${inv.paymentsApplied}</TableCell>
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
                              <IconButton color="primary" onClick={() => handleDownloadInvoice(inv.id)}>
                                <PictureAsPdfIcon />
                              </IconButton>
                            </TableCell>
                          </TableRow>
                        ))
                      ) : (
                        <TableRow>
                          <TableCell colSpan={9} align="center">No invoices issued yet.</TableCell>
                        </TableRow>
                      )}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Box>
            )}

            {/* Tab 4: Payments Log */}
            {activeTab === 4 && (
              <Box>
                <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 2 }}>Payments Log</Typography>
                <TableContainer component={Paper}>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell>Payment Date</TableCell>
                        <TableCell>Amount Paid</TableCell>
                        <TableCell>Method</TableCell>
                        <TableCell>Reference / Check #</TableCell>
                        <TableCell>Notes</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {payments.length > 0 ? (
                        payments.map((pmt) => (
                          <TableRow key={pmt.id}>
                            <TableCell>{pmt.paymentDate}</TableCell>
                            <TableCell sx={{ color: 'success.main', fontWeight: 'bold' }}>${pmt.amount}</TableCell>
                            <TableCell><Chip label={pmt.paymentMethod} size="small" variant="outlined" /></TableCell>
                            <TableCell>{pmt.referenceNumber}</TableCell>
                            <TableCell>{pmt.notes}</TableCell>
                          </TableRow>
                        ))
                      ) : (
                        <TableRow>
                          <TableCell colSpan={5} align="center">No payment history recorded yet.</TableCell>
                        </TableRow>
                      )}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Box>
            )}
          </>
        )}
      </Container>
    </Box>
  );
}

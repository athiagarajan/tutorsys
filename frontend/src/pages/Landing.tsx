import { useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { Container, Box, Typography, Button, Card, CardContent, useTheme, IconButton } from '@mui/material';
import { ColorModeContext } from '../App';
import Brightness4Icon from '@mui/icons-material/Brightness4';
import Brightness7Icon from '@mui/icons-material/Brightness7';
import SchoolIcon from '@mui/icons-material/School';
import EventAvailableIcon from '@mui/icons-material/EventAvailable';
import ReceiptIcon from '@mui/icons-material/Receipt';
import SecurityIcon from '@mui/icons-material/Security';

export default function Landing() {
  const navigate = useNavigate();
  const theme = useTheme();
  const colorMode = useContext(ColorModeContext);

  const features = [
    {
      icon: <SchoolIcon sx={{ fontSize: 40, color: theme.palette.primary.main }} />,
      title: 'Student Profile Management',
      desc: 'Keep track of grade levels, schools, notes, and individual learning goals for every student.',
    },
    {
      icon: <EventAvailableIcon sx={{ fontSize: 40, color: theme.palette.primary.main }} />,
      title: 'Smart Scheduling & Attendance',
      desc: 'Define flexible weekly recurring schedules and log detailed attendance status for every conducted lesson.',
    },
    {
      icon: <ReceiptIcon sx={{ fontSize: 40, color: theme.palette.primary.main }} />,
      title: 'Automated Monthly Invoicing',
      desc: 'Automatically compile session logs, apply customized student pricing plans, and email professional PDF invoices.',
    },
    {
      icon: <SecurityIcon sx={{ fontSize: 40, color: theme.palette.primary.main }} />,
      title: 'Secure Parent Portal',
      desc: 'Allow parents to log in to view their kids progress, active schedules, attendance logs, and pay balances.',
    },
  ];

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* Header / Navbar */}
      <Box sx={{ py: 2, px: 3, borderBottom: `1px solid ${theme.palette.divider}`, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <SchoolIcon sx={{ color: theme.palette.primary.main, fontSize: 32 }} />
          <Typography variant="h6" component="div" sx={{ fontWeight: 'bold' }}>
            TutorSys
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <IconButton onClick={colorMode.toggleColorMode} color="inherit" aria-label="Toggle Theme">
            {theme.palette.mode === 'dark' ? <Brightness7Icon /> : <Brightness4Icon />}
          </IconButton>
          <Button variant="outlined" color="primary" onClick={() => navigate('/login')}>
            Login Portal
          </Button>
        </Box>
      </Box>

      {/* Hero Section */}
      <Box
        sx={{
          py: 10,
          background: theme.palette.mode === 'light'
            ? 'linear-gradient(135deg, #e3f2fd 0%, #fff 100%)'
            : 'linear-gradient(135deg, #1a237e 0%, #121212 100%)',
          textAlign: 'center',
          flexGrow: 1,
          display: 'flex',
          alignItems: 'center',
        }}
      >
        <Container maxWidth="md">
          <Typography
            variant="h2"
            component="h1"
            sx={{
              fontWeight: 800,
              mb: 3,
              background: 'linear-gradient(45deg, #3f51b5 30%, #ff4081 90%)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
            }}
          >
            Student Tuition Management System
          </Typography>
          <Typography variant="h5" color="text.secondary" sx={{ mb: 6, lineHeight: 1.6 }}>
            Automate student scheduling, attendance logging, flexible pricing plans, monthly invoicing, and parent communication in one premium, modular dashboard.
          </Typography>
          <Box sx={{ display: 'flex', justifyContent: 'center', gap: 3 }}>
            <Button
              variant="contained"
              size="large"
              color="primary"
              onClick={() => navigate('/login')}
              sx={{ px: 4, py: 1.5, fontSize: '1.1rem', boxShadow: '0 4px 14px 0 rgba(63, 81, 181, 0.4)' }}
            >
              Go to Login
            </Button>
          </Box>
        </Container>
      </Box>

      {/* Features Grid */}
      <Container sx={{ py: 10 }}>
        <Typography variant="h3" sx={{ textAlign: 'center', fontWeight: 'bold', mb: 8 }}>
          Designed for Excellence
        </Typography>
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 4 }}>
          {features.map((f, i) => (
            <Box sx={{ width: { xs: '100%', sm: 'calc(50% - 16px)', md: 'calc(25% - 24px)' } }} key={i}>
              <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column', transition: 'transform 0.2s', '&:hover': { transform: 'translateY(-5px)' } }}>
                <CardContent sx={{ flexGrow: 1, textAlign: 'center', pt: 4 }}>
                  <Box sx={{ mb: 3 }}>{f.icon}</Box>
                  <Typography variant="h6" component="h3" sx={{ mb: 2, fontWeight: 'bold' }}>
                    {f.title}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {f.desc}
                  </Typography>
                </CardContent>
              </Card>
            </Box>
          ))}
        </Box>
      </Container>

      {/* Footer */}
      <Box sx={{ py: 4, mt: 'auto', borderTop: `1px solid ${theme.palette.divider}`, textAlign: 'center' }}>
        <Typography variant="body2" color="text.secondary">
          &copy; {new Date().getFullYear()} TutorSys. All rights reserved.
        </Typography>
      </Box>
    </Box>
  );
}

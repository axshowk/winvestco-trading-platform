import { BrowserRouter as Router, Routes, Route, useNavigate } from 'react-router-dom';
import { NotificationProvider } from './context/NotificationContext';
import { ThemeProvider } from './context/ThemeContext';
import NotificationToast from './components/NotificationToast';
import Navbar from './components/Navbar'
import Hero from './components/Hero'
import Ticker from './components/Ticker'
import Features from './components/Features'
import Footer from './components/Footer'
import CoinShower from './components/CoinShower'
import Login from './pages/Login'
import Signup from './pages/Signup'
import Profile from './pages/Profile'
import Stocks from './pages/Stocks'
import MarketData from './pages/MarketData'
import StockDetails from './pages/StockDetails'
import Portfolio from './pages/Portfolio'
import Orders from './pages/Orders'
import Trades from './pages/Trades'
import Wallet from './pages/Wallet'
import Reports from './pages/Reports'
import Notifications from './pages/Notifications'
import ChartTerminal from './pages/ChartTerminal'
import ErrorBoundary from './components/ErrorBoundary'
import './App.css'

const Home = () => {
  const navigate = useNavigate();

  return (
    <div className="app-wrapper">
      <CoinShower />
      <Navbar onLogin={() => navigate('/login')} />
      <main>
        <Hero onStart={() => navigate('/login')} />
        <Features />
      </main>
      <Footer />
    </div>
  );
};

function App() {
  return (
    <ThemeProvider>
      <NotificationProvider>
        <Router>
          <NotificationToast />
          <Ticker />
          <ErrorBoundary>
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/markets" element={<Stocks />} />
              <Route path="/market-data" element={<MarketData />} />
              <Route path="/stock/:symbol" element={<StockDetails />} />
              <Route path="/portfolio" element={<Portfolio />} />
              <Route path="/orders" element={<Orders />} />
              <Route path="/trades" element={<Trades />} />
              <Route path="/wallet" element={<Wallet />} />
              <Route path="/reports" element={<Reports />} />
              <Route path="/notifications" element={<Notifications />} />
              <Route path="/terminal/:symbol" element={<ChartTerminal />} />
              <Route path="/login" element={<Login />} />
              <Route path="/signup" element={<Signup />} />
              <Route path="/profile" element={<Profile />} />
            </Routes>
          </ErrorBoundary>
        </Router>
      </NotificationProvider>
    </ThemeProvider>
  )
}

export default App


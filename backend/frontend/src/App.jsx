import { BrowserRouter as Router, Routes, Route, useNavigate } from 'react-router-dom';
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
    <Router>
      <Ticker />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/markets" element={<Stocks />} />
        <Route path="/market-data" element={<MarketData />} />
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<Signup />} />
        <Route path="/profile" element={<Profile />} />
      </Routes>
    </Router>
  )
}

export default App

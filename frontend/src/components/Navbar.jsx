import React, { useState, useEffect } from 'react';
import { Menu, X, TrendingUp, User, Wallet } from 'lucide-react';
import NotificationBell from './NotificationBell';
import './Navbar.css';

const Navbar = ({ onLogin }) => {
    const [isOpen, setIsOpen] = useState(false);
    const [isVisible, setIsVisible] = useState(true);
    const [lastScrollY, setLastScrollY] = useState(0);
    const [isLoggedIn, setIsLoggedIn] = useState(false);

    useEffect(() => {
        // Check login status
        const token = localStorage.getItem('accessToken');
        setIsLoggedIn(!!token);

        const controlNavbar = () => {
            if (typeof window !== 'undefined') {
                if (window.scrollY > lastScrollY && window.scrollY > 100) { // if scroll down and past 100px
                    setIsVisible(false);
                } else { // if scroll up
                    setIsVisible(true);
                }
                setLastScrollY(window.scrollY);
            }
        };

        window.addEventListener('scroll', controlNavbar);

        return () => {
            window.removeEventListener('scroll', controlNavbar);
        };
    }, [lastScrollY]);

    return (
        <nav className={`navbar glass ${!isVisible ? 'navbar-hidden' : ''}`}>
            <div className="container navbar-container">
                <div className="navbar-content">
                    {/* Logo */}
                    <a href="/" className="logo-section">
                        <div className="logo-icon">
                            <TrendingUp className="icon-white" size={20} />
                        </div>
                        <span className="logo-text">Winvestco</span>
                    </a>

                    {/* Desktop Menu */}
                    <div className="desktop-menu">
                        <a href="/markets" className="nav-link">Indices</a>
                        <a href="/market-data" className="nav-link">Market Data</a>
                        <a href="/portfolio" className="nav-link">Portfolio</a>
                        <a href="/orders" className="nav-link">Orders</a>
                        <a href="/trades" className="nav-link">Trades</a>
                        <a href="/wallet" className="nav-link">Wallet</a>
                        <a href="https://zerodha.com/varsity/modules" target="_blank" rel="noopener noreferrer" className="nav-link">Learn</a>
                    </div>

                    {/* Auth Buttons */}
                    <div className="auth-buttons">
                        {isLoggedIn ? (
                            <>
                                <NotificationBell />
                                <a href="/profile" className="btn-primary" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', textDecoration: 'none' }}>
                                    <User size={18} />
                                    <span>Profile</span>
                                </a>
                            </>
                        ) : (
                            <>
                                <button type="button" className="btn-text" onClick={onLogin}>Log In</button>
                                <button type="button" className="btn-primary" onClick={onLogin}>Get Started</button>
                            </>
                        )}
                    </div>

                    {/* Mobile Menu Button */}
                    <div className="mobile-menu-btn">
                        <button onClick={() => setIsOpen(!isOpen)} className="icon-btn">
                            {isOpen ? <X color="white" /> : <Menu color="white" />}
                        </button>
                    </div>
                </div>
            </div>

            {/* Mobile Menu */}
            {isOpen && (
                <div className="mobile-menu glass">
                    <div className="mobile-menu-content">
                        <a href="/markets" className="nav-link mobile">Markets</a>
                        <a href="/market-data" className="nav-link mobile">Market Data</a>
                        <a href="/portfolio" className="nav-link mobile">Portfolio</a>
                        <a href="/orders" className="nav-link mobile">Orders</a>
                        <a href="/trades" className="nav-link mobile">Trades</a>
                        <a href="/wallet" className="nav-link mobile">Wallet</a>
                        <a href="https://zerodha.com/varsity/modules" target="_blank" rel="noopener noreferrer" className="nav-link mobile">Learn</a>
                        <hr className="divider" />
                        {isLoggedIn ? (
                            <a href="/profile" className="btn-primary mobile" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', textDecoration: 'none' }}>
                                <User size={18} />
                                <span>Profile</span>
                            </a>
                        ) : (
                            <>
                                <button className="btn-text mobile" onClick={onLogin}>Log In</button>
                                <button className="btn-primary mobile" onClick={onLogin}>Get Started</button>
                            </>
                        )}
                    </div>
                </div>
            )}
        </nav>
    );
};

export default Navbar;

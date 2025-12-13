import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { User, Mail, Calendar, LogOut, Shield, CreditCard, Settings, TrendingUp, Globe } from 'lucide-react';
import './Profile.css';
import Navbar from '../components/Navbar';

const Profile = () => {
    const navigate = useNavigate();
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const handleLogout = () => {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('user');
        navigate('/login');
    };

    useEffect(() => {
        const fetchUser = async () => {
            try {
                // Get JWT token from localStorage
                const token = localStorage.getItem('accessToken');

                const headers = {
                    'Content-Type': 'application/json'
                };

                // Add Authorization header if token exists
                if (token) {
                    headers['Authorization'] = `Bearer ${token}`;
                }

                const response = await fetch('http://localhost:8090/api/auth/me', {
                    credentials: 'include', // Still include for OAuth2 session-based auth
                    headers
                });

                if (!response.ok) {
                    // If unauthorized, clear token and redirect to login
                    if (response.status === 401) {
                        localStorage.removeItem('accessToken');
                        localStorage.removeItem('user');
                        throw new Error('Please log in to view your profile');
                    }
                    throw new Error('Failed to fetch user profile');
                }

                const data = await response.json();
                if (data.valid && data.user) {
                    setUser({
                        name: `${data.user.firstName} ${data.user.lastName}`,
                        email: data.user.email,
                        joinDate: "December 2024", // Placeholder as API doesn't return this yet
                        accountType: data.user.roles.includes('ROLE_ADMIN') ? 'Administrator' : 'Premium Investor',
                        avatar: `https://api.dicebear.com/7.x/avataaars/svg?seed=${data.user.firstName}`
                    });
                } else {
                    throw new Error('Invalid user data');
                }
            } catch (err) {
                console.error("Error fetching profile:", err);
                setError(err.message);
            } finally {
                setLoading(false);
            }
        };

        fetchUser();
    }, []);

    if (loading) {
        return (
            <div className="profile-wrapper">
                <Navbar onLogin={() => { }} />
                <div className="profile-container">
                    <div className="profile-card glass" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '400px' }}>
                        <div className="loading-spinner" style={{ width: '40px', height: '40px', border: '3px solid rgba(255,255,255,0.1)', borderTopColor: '#6366f1', borderRadius: '50%', animation: 'spin 1s linear infinite' }}></div>
                    </div>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="profile-wrapper">
                <Navbar onLogin={() => { }} />
                <div className="profile-container">
                    <div className="profile-card glass" style={{ textAlign: 'center', padding: '3rem' }}>
                        <div style={{ color: '#ef4444', marginBottom: '1rem' }}>
                            <Shield size={48} style={{ margin: '0 auto' }} />
                        </div>
                        <h2 style={{ color: 'white', marginBottom: '1rem' }}>Authentication Error</h2>
                        <p style={{ color: '#888', marginBottom: '2rem' }}>{error}</p>
                        <button className="btn-primary" onClick={() => window.location.href = '/login'}>
                            Go to Login
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="profile-wrapper">
            <Navbar onLogin={() => { }} /> {/* Navbar included for consistency */}

            <div className="profile-container">
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.5 }}
                    className="profile-card glass"
                >
                    <div className="profile-header">
                        <div className="profile-avatar-container">
                            <img src={user.avatar} alt="Profile" className="profile-avatar" />
                            <div className="avatar-glow"></div>
                        </div>
                        <h1 className="profile-name">{user.name}</h1>
                        <p className="profile-type">{user.accountType}</p>
                    </div>

                    <div className="info-grid">
                        <div className="detail-item">
                            <div className="detail-icon">
                                <Mail size={18} />
                            </div>
                            <div className="detail-info">
                                <span className="label">Email</span>
                                <span className="value">{user.email}</span>
                            </div>
                        </div>

                        <div className="detail-item">
                            <div className="detail-icon">
                                <User size={18} />
                            </div>
                            <div className="detail-info">
                                <span className="label">Username</span>
                                <span className="value">@alexmorgan_invest</span>
                            </div>
                        </div>

                        <div className="detail-item">
                            <div className="detail-icon">
                                <Calendar size={18} />
                            </div>
                            <div className="detail-info">
                                <span className="label">Joined</span>
                                <span className="value">{user.joinDate}</span>
                            </div>
                        </div>

                        <div className="detail-item">
                            <div className="detail-icon">
                                <TrendingUp size={18} />
                            </div>
                            <div className="detail-info">
                                <span className="label">Risk Profile</span>
                                <span className="value">Moderate - Aggressive</span>
                            </div>
                        </div>

                        <div className="detail-item full-width">
                            <div className="detail-icon">
                                <Globe size={18} />
                            </div>
                            <div className="detail-info">
                                <span className="label">Markets</span>
                                <span className="value">NSE / BSE</span>
                            </div>
                        </div>
                    </div>

                    <div className="profile-actions">
                        <button className="action-btn">
                            <Settings size={18} />
                            <span>Settings</span>
                        </button>
                        <button className="action-btn">
                            <Shield size={18} />
                            <span>Security</span>
                        </button>
                        <button className="action-btn">
                            <CreditCard size={18} />
                            <span>Billing</span>
                        </button>
                    </div>

                    <div className="profile-footer">
                        <button className="logout-btn" onClick={handleLogout}>
                            <LogOut size={18} />
                            <span>Sign Out</span>
                        </button>
                    </div>
                </motion.div>
            </div>

            {/* Background Elements */}
            <div className="profile-glow glow-1"></div>
            <div className="profile-glow glow-2"></div>
        </div>
    );
};

export default Profile;

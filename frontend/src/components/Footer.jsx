import React from 'react';
import { TrendingUp, Twitter, Facebook, Instagram, Linkedin } from 'lucide-react';
import './Footer.css';

const Footer = () => {
    return (
        <footer className="footer-section">
            <div className="container">
                <div className="footer-grid">
                    <div className="footer-brand">
                        <div className="footer-logo">
                            <div className="logo-icon-sm">
                                <TrendingUp color="white" size={18} />
                            </div>
                            <span className="logo-text-sm">Winvestco</span>
                        </div>
                        <p className="footer-desc">
                            The world's most trusted investment platform. Build and manage your portfolio with 500+ investment options securely.
                        </p>
                        <div className="social-links">
                            <a href="#" className="social-link"><Twitter size={20} /></a>
                            <a href="#" className="social-link"><Facebook size={20} /></a>
                            <a href="#" className="social-link"><Instagram size={20} /></a>
                            <a href="#" className="social-link"><Linkedin size={20} /></a>
                        </div>
                    </div>

                    <div className="footer-links">
                        <h4 className="footer-heading">Platform</h4>
                        <a href="#">Markets</a>
                        <a href="#">Portfolio</a>
                        <a href="#">Earn</a>
                        <a href="#">Wallet</a>
                    </div>

                    <div className="footer-links">
                        <h4 className="footer-heading">Support</h4>
                        <a href="#">Help Center</a>
                        <a href="#">API Documentation</a>
                        <a href="#">Fees</a>
                        <a href="#">Security</a>
                    </div>

                    <div className="footer-links">
                        <h4 className="footer-heading">Company</h4>
                        <a href="#">About Us</a>
                        <a href="#">Careers</a>
                        <a href="#">Blog</a>
                        <a href="#">Contact</a>
                    </div>
                </div>

                <div className="footer-bottom">
                    <p>&copy; 2025 Winvestco. All rights reserved.</p>
                    <div className="footer-legal">
                        <a href="#">Privacy Policy</a>
                        <a href="#">Terms of Service</a>
                    </div>
                </div>
            </div>
        </footer>
    );
};

export default Footer;

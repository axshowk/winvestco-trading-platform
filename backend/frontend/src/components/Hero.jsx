import React from 'react';
import { motion } from 'framer-motion';
import { ArrowRight, Activity } from 'lucide-react';
import './Hero.css';

const Hero = ({ onStart }) => {
    return (
        <section className="hero-section">
            <div className="container hero-container">
                <div className="hero-content">
                    <motion.div
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.5 }}
                        className="badge-wrapper"
                    >
                        <span className="badge-new">New</span>
                        <span className="badge-text">Smart investing is now live</span>
                        <ArrowRight size={14} className="badge-icon" />
                    </motion.div>

                    <motion.h1
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.5, delay: 0.1 }}
                        className="hero-title"
                    >
                        Invest in the Future <br />
                        <span className="text-gradient">With Confidence</span>
                    </motion.h1>

                    <motion.p
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.5, delay: 0.2 }}
                        className="hero-subtitle"
                    >
                        Experience lightning-fast execution, zero fees, and advanced analytics.

                    </motion.p>

                    <motion.div
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.5, delay: 0.3 }}
                        className="hero-actions"
                    >
                        <button type="button" className="btn-primary btn-lg" onClick={onStart}>
                            Start Investing Now
                        </button>
                    </motion.div>

                    <motion.div
                        initial={{ opacity: 0, scale: 0.95 }}
                        animate={{ opacity: 1, scale: 1 }}
                        transition={{ duration: 0.8, delay: 0.4 }}
                        className="hero-stats glass"
                    >
                        <div className="stat-item">
                            <span className="stat-value">$40B+</span>
                            <span className="stat-label">Quarterly Volume</span>
                        </div>
                        <div className="stat-divider"></div>
                        <div className="stat-item">
                            <span className="stat-value">100+</span>
                            <span className="stat-label">Countries Supported</span>
                        </div>
                        <div className="stat-divider"></div>
                        <div className="stat-item">
                            <span className="stat-value">0.05s</span>
                            <span className="stat-label">Latency</span>
                        </div>
                    </motion.div>
                </div>

                {/* Abstract Visual Background Elements */}
                <div className="hero-glow glow-1"></div>
                <div className="hero-glow glow-2"></div>
            </div>
        </section>
    );
};

export default Hero;

import React from 'react';
import { motion } from 'framer-motion';
import { ArrowRight } from 'lucide-react';
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
                        <span className="badge-new">✦</span>
                        <span className="badge-text">Our Promise to You</span>
                        <ArrowRight size={14} className="badge-icon" />
                    </motion.div>

                    <motion.h1
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.5, delay: 0.1 }}
                        className="hero-title"
                    >
                        Pay Good Money <br />
                        <span className="text-gradient">Get Best Service</span>
                    </motion.h1>

                    <motion.p
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.5, delay: 0.2 }}
                        className="hero-subtitle"
                    >
                        At Winvestco, we believe your money deserves the best. Transparent pricing, honest execution, and service that earns your trust — every single trade.
                    </motion.p>

                    <motion.div
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.5, delay: 0.3 }}
                        className="hero-actions"
                    >
                        <button type="button" className="btn-primary btn-lg" onClick={onStart}>
                            Start Trading Today
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

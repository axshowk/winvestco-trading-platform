import React from 'react';
import { Shield, Zap, BarChart2 } from 'lucide-react';
import './Features.css';

const Features = () => {
    const features = [
        {
            icon: <Zap size={32} className="text-primary" />,
            title: "Lightning Fast Execution",
            description: "Our matching engine handles millions of transactions per second with sub-millisecond latency."
        },
        {
            icon: <Shield size={32} className="text-accent" />,
            title: "Bank-Grade Security",
            description: "Your funds are protected by industry-leading encryption and cold storage protocols."
        },
        {
            icon: <BarChart2 size={32} className="text-purple" />,
            title: "Advanced Analytics",
            description: "Access professional-grade charting tools and real-time market data to make informed decisions."
        }
    ];

    return (
        <section className="features-section">
            <div className="container">
                <div className="section-header">
                    <h2 className="section-title">Why Invest With Us?</h2>
                    <p className="section-subtitle">Built for both beginners and professional investors.</p>
                </div>

                <div className="features-grid">
                    {features.map((feature, index) => (
                        <div className="feature-card glass" key={index}>
                            <div className="feature-icon-wrapper">
                                {feature.icon}
                            </div>
                            <h3 className="feature-title">{feature.title}</h3>
                            <p className="feature-desc">{feature.description}</p>
                        </div>
                    ))}
                </div>
            </div>
        </section>
    );
};

export default Features;

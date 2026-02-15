import React from 'react';
import { IndianRupee, ShieldCheck, Headphones } from 'lucide-react';
import './Features.css';

const Features = () => {
    const features = [
        {
            icon: <IndianRupee size={32} className="text-primary" />,
            title: "Transparent Pricing",
            description: "Every fee is clearly listed upfront. No hidden charges, no surprises. You always know exactly what you're paying for."
        },
        {
            icon: <ShieldCheck size={32} className="text-accent" />,
            title: "Reliable Execution",
            description: "Your orders are executed with precision and speed. Bank-grade infrastructure ensures your trades go through when it matters most."
        },
        {
            icon: <Headphones size={32} className="text-purple" />,
            title: "Dedicated Support",
            description: "Real people ready to help, not bots. Our support team is available to assist you with any questions, anytime."
        }
    ];

    return (
        <section className="features-section">
            <div className="container">
                <div className="section-header">
                    <h2 className="section-title">Why Our Clients Trust Us</h2>
                    <p className="section-subtitle">Real value. Real service. No hidden costs.</p>
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

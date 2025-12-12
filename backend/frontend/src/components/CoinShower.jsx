import React, { useEffect, useState, useRef } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import './CoinShower.css';

const CoinShower = () => {
    const [coins, setCoins] = useState([]);
    const mousePos = useRef({ x: 0, y: 0 });
    const lastCoinTime = useRef(0);

    useEffect(() => {
        const handleMouseMove = (e) => {
            mousePos.current = { x: e.clientX, y: e.clientY };

            const now = Date.now();
            if (now - lastCoinTime.current > 5) {
                createCoin(e.clientX, e.clientY);
                lastCoinTime.current = now;
            }
        };

        window.addEventListener('mousemove', handleMouseMove);
        return () => window.removeEventListener('mousemove', handleMouseMove);
    }, []);

    const createCoin = (x, y) => {
        const id = Date.now() + Math.random();
        const angle = Math.random() * Math.PI * 2; // Random angle in radians
        const speed = 100 + Math.random() * 150; // Random speed

        const newCoin = {
            id,
            x,
            y,
            rotation: Math.random() * 360,
            velocity: {
                x: Math.cos(angle) * speed, // Explode outwards x
                y: Math.sin(angle) * speed  // Explode outwards y
            }
        };

        setCoins(prev => [...prev.slice(-200), newCoin]); // Allow up to 200 coins

        // Remove coin after animation (longer duration to allow falling off screen)
        setTimeout(() => {
            setCoins(prev => prev.filter(c => c.id !== id));
        }, 2000);
    };

    return (
        <div className="coin-shower-container">
            <AnimatePresence>
                {coins.map(coin => (
                    <motion.div
                        key={coin.id}
                        className="coin"
                        initial={{
                            x: coin.x,
                            y: coin.y,
                            opacity: 1,
                            scale: 0,
                            rotate: coin.rotation
                        }}
                        animate={{
                            x: coin.x + coin.velocity.x,
                            y: coin.y + coin.velocity.y + 200, // Add gravity (200px drop)
                            opacity: 0,
                            scale: 0.5, // Shrink slightly as they fade
                            rotate: coin.rotation + 720
                        }}
                        exit={{ opacity: 0 }}
                        transition={{ duration: 0.8, ease: "easeOut" }}
                    >
                        ₹
                    </motion.div>
                ))}
            </AnimatePresence>
        </div>
    );
};

export default CoinShower;
